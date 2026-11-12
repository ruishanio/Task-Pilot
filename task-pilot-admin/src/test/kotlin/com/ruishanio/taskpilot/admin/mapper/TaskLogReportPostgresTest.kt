package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskReport
import com.ruishanio.taskpilot.admin.service.impl.TaskPilotServiceImpl
import org.apache.ibatis.builder.xml.XMLMapperBuilder
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource
import org.apache.ibatis.io.Resources
import org.apache.ibatis.mapping.Environment
import org.apache.ibatis.session.Configuration
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactoryBuilder
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID

/**
 * 在真实 PostgreSQL 上验证新库初始化、列名映射和首页汇总，避免 Mock 掩盖数据库大小写行为。
 * 设置 TASK_PILOT_TEST_PG_URL、TASK_PILOT_TEST_PG_USER、TASK_PILOT_TEST_PG_PASSWORD 后运行；
 * 每个用例只操作独立的随机 schema，不启动调度线程，也不修改数据库中已有的业务表。
 */
@EnabledIfEnvironmentVariable(named = "TASK_PILOT_TEST_PG_URL", matches = ".+")
class TaskLogReportPostgresTest {
    private val migrationVersion = "1.0.7.001"
    private val schema = "report_test_" + UUID.randomUUID().toString().replace("-", "")
    private lateinit var dataSource: UnpooledDataSource
    private lateinit var connection: Connection
    private lateinit var session: SqlSession
    private lateinit var logMapper: TaskLogMapper
    private lateinit var reportMapper: TaskReportMapper

    @BeforeEach
    fun setUp() {
        dataSource = UnpooledDataSource(
            "org.postgresql.Driver",
            System.getenv("TASK_PILOT_TEST_PG_URL"),
            System.getenv("TASK_PILOT_TEST_PG_USER") ?: "postgres",
            System.getenv("TASK_PILOT_TEST_PG_PASSWORD") ?: ""
        )
        connection = dataSource.connection
        connection.createStatement().use {
            it.execute("CREATE SCHEMA $schema")
            it.execute("SET search_path TO $schema")
        }
        val configuration = Configuration(Environment("test", JdbcTransactionFactory(), dataSource))
        for (resource in listOf("mapper/TaskLogMapper.xml", "mapper/TaskReportMapper.xml")) {
            Resources.getResourceAsStream(resource).use { stream ->
                XMLMapperBuilder(stream, configuration, resource, configuration.sqlFragments).parse()
            }
        }
        session = SqlSessionFactoryBuilder().build(configuration).openSession(connection)
        logMapper = session.getMapper(TaskLogMapper::class.java)
        reportMapper = session.getMapper(TaskReportMapper::class.java)
    }

    @AfterEach
    fun tearDown() {
        if (::session.isInitialized) session.close()
        if (::connection.isInitialized && !connection.isClosed) connection.close()
        if (::dataSource.isInitialized) {
            dataSource.connection.use { cleanup ->
                cleanup.createStatement().use { it.execute("DROP SCHEMA IF EXISTS $schema CASCADE") }
            }
        }
    }

    @Test
    fun shouldReturnCamelCaseKeysAndZeroCountsOnFreshDatabase() {
        assertEquals(1, migrate())
        assertExecutorTitleLength()

        val result = requireNotNull(logMapper.findLogReport(time("2026-11-12"), time("2026-11-13")))

        assertEquals(setOf("triggerDayCount", "triggerDayCountRunning", "triggerDayCountSuc"), result.keys)
        assertTrue(result.values.all { (it as Number).toInt() == 0 })
        assertEquals(0, service().dashboardInfo()["taskLogCount"])
        assertEquals(0, migrate())
    }

    @Test
    fun shouldCountSuccessRunningAndFailuresUsingPostgresMapper() {
        migrate()
        insertLog("2026-11-12", 0, 0)
        insertLog("2026-11-12", 200, 0)
        insertLog("2026-11-12", 200, 200)
        insertLog("2026-11-12", 500, 0)
        insertLog("2026-11-12", 200, 500)
        insertLog("2026-11-11", 200, 200)
        insertLog("2026-11-13", 200, 200)

        val result = requireNotNull(
            logMapper.findLogReport(time("2026-11-12"), Timestamp.valueOf("2026-11-12 23:59:59.999"))
        )
        val total = (result.getValue("triggerDayCount") as Number).toInt()
        val running = (result.getValue("triggerDayCountRunning") as Number).toInt()
        val success = (result.getValue("triggerDayCountSuc") as Number).toInt()
        assertEquals(5, total)
        assertEquals(2, running)
        assertEquals(1, success)

        reportMapper.saveOrUpdate(
            TaskReport(
                triggerDay = time("2026-11-12"),
                runningCount = running,
                sucCount = success,
                failCount = total - running - success
            )
        )
        assertEquals(5, service().dashboardInfo()["taskLogCount"])
        assertChartTotals("2026-11-12", "2026-11-12", 2, 1, 2)
    }

    /**
     * 将当前用例的隔离 schema 迁移到合并后的初始化版本，返回实际执行的脚本数量。
     */
    private fun migrate(): Int = Flyway.configure().dataSource(dataSource)
        .schemas(schema).defaultSchema(schema).locations("classpath:db/migration")
        .target(migrationVersion).load().migrate().migrationsExecuted

    private fun assertExecutorTitleLength() {
        connection.prepareStatement("""
            SELECT character_maximum_length FROM information_schema.columns
            WHERE table_schema = ? AND table_name = 'executor' AND column_name = 'title'
        """.trimIndent()).use { statement ->
            statement.setString(1, schema)
            statement.executeQuery().use { rows ->
                assertTrue(rows.next())
                assertEquals(64, rows.getInt(1))
            }
        }
    }

    private fun insertLog(day: String, triggerCode: Int, handleCode: Int) {
        connection.prepareStatement(
            "INSERT INTO task_log (executor_id, task_id, trigger_time, trigger_code, handle_code) VALUES (1, 1, ?, ?, ?)"
        ).use {
            it.setTimestamp(1, time(day))
            it.setInt(2, triggerCode)
            it.setInt(3, handleCode)
            it.executeUpdate()
        }
    }

    private fun time(day: String): Timestamp = Timestamp.valueOf("$day 00:00:00")

    /**
     * 统计服务使用真实报表 Mapper，任务和执行器数量与本次统计修复无关，保持隔离。
     */
    private fun service(): TaskPilotServiceImpl {
        val taskInfoMapper = mock(TaskInfoMapper::class.java)
        val executorMapper = mock(ExecutorMapper::class.java)
        `when`(executorMapper.findAll()).thenReturn(emptyList())
        return TaskPilotServiceImpl().also {
            ReflectionTestUtils.setField(it, "taskReportMapper", reportMapper)
            ReflectionTestUtils.setField(it, "taskInfoMapper", taskInfoMapper)
            ReflectionTestUtils.setField(it, "executorMapper", executorMapper)
        }
    }

    private fun assertChartTotals(from: String, to: String, running: Int, success: Int, failure: Int) {
        val response = service().chartInfo(time(from), time(to))
        assertTrue(response.isSuccess)
        val chart = requireNotNull(response.data)
        assertEquals(running, chart["triggerCountRunningTotal"])
        assertEquals(success, chart["triggerCountSucTotal"])
        assertEquals(failure, chart["triggerCountFailTotal"])
    }
}
