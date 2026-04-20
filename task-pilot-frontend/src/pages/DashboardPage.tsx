import { useEffect, useMemo, useState } from 'react';
import { Card, Col, DatePicker, Empty, Row, Space, Spin, Statistic, Typography, message } from 'antd';
import { CloudServerOutlined, PlayCircleOutlined, ScheduleOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import dayjs, { type Dayjs } from 'dayjs';
import { frontendApi } from '../services/api';
import { getErrorMessage } from '../utils/format';

interface DashboardSummary {
  taskInfoCount: number;
  taskLogCount: number;
  taskLogSuccessCount: number;
  executorCount: number;
}

interface DashboardChart {
  triggerDayList?: string[];
  triggerDayCountSucList?: number[];
  triggerDayCountFailList?: number[];
  triggerDayCountRunningList?: number[];
  triggerCountSucTotal?: number;
  triggerCountFailTotal?: number;
  triggerCountRunningTotal?: number;
}

type DateRange = [Dayjs, Dayjs];

const defaultSummary: DashboardSummary = {
  taskInfoCount: 0,
  taskLogCount: 0,
  taskLogSuccessCount: 0,
  executorCount: 0,
};

function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary>(defaultSummary);
  const [chart, setChart] = useState<DashboardChart | null>(null);
  const [loading, setLoading] = useState(true);
  const [chartLoading, setChartLoading] = useState(true);
  const [range, setRange] = useState<DateRange>([
    dayjs().subtract(6, 'day').startOf('day'),
    dayjs().endOf('day'),
  ]);

  useEffect(() => {
    loadSummary();
  }, []);

  useEffect(() => {
    loadChart(range);
  }, [range]);

  async function loadSummary() {
    try {
      setLoading(true);
      const response = await frontendApi.dashboard();
      setSummary({ ...defaultSummary, ...(response.data as Partial<DashboardSummary>) });
    } catch (error) {
      message.error(getErrorMessage(error, '加载仪表盘统计失败'));
    } finally {
      setLoading(false);
    }
  }

  async function loadChart(dateRange: DateRange) {
    try {
      setChartLoading(true);
      const response = await frontendApi.chartInfo({
        startDate: dateRange[0].format('YYYY-MM-DD HH:mm:ss'),
        endDate: dateRange[1].format('YYYY-MM-DD HH:mm:ss'),
      });
      setChart((response.data as DashboardChart | null) || null);
    } catch (error) {
      message.error(getErrorMessage(error, '加载图表数据失败'));
    } finally {
      setChartLoading(false);
    }
  }

  const lineOption = useMemo(() => {
    if (!chart) {
      return null;
    }
    return {
      tooltip: { trigger: 'axis' },
      legend: { top: 0, data: ['成功', '失败', '运行中'] },
      grid: { left: 36, right: 20, top: 48, bottom: 24, containLabel: true },
      xAxis: {
        type: 'category',
        data: chart.triggerDayList,
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { type: 'dashed' } },
      },
      series: [
        {
          name: '成功',
          type: 'line',
          smooth: true,
          data: chart.triggerDayCountSucList,
          areaStyle: { opacity: 0.08 },
        },
        {
          name: '失败',
          type: 'line',
          smooth: true,
          data: chart.triggerDayCountFailList,
          areaStyle: { opacity: 0.08 },
        },
        {
          name: '运行中',
          type: 'line',
          smooth: true,
          data: chart.triggerDayCountRunningList,
          areaStyle: { opacity: 0.08 },
        },
      ],
    };
  }, [chart]);

  const pieOption = useMemo(() => {
    if (!chart) {
      return null;
    }
    return {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['42%', '68%'],
          data: [
            { name: '成功', value: chart.triggerCountSucTotal || 0 },
            { name: '失败', value: chart.triggerCountFailTotal || 0 },
            { name: '运行中', value: chart.triggerCountRunningTotal || 0 },
          ],
          label: {
            formatter: '{b}\n{d}%',
          },
        },
      ],
    };
  }, [chart]);

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card className="dashboard-stat-card page-card" loading={loading}>
            <Statistic
              title="任务总数"
              value={summary.taskInfoCount}
              prefix={<ScheduleOutlined />}
              suffix="个"
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="dashboard-stat-card page-card" loading={loading}>
            <Statistic
              title="调度次数"
              value={summary.taskLogCount}
              prefix={<PlayCircleOutlined />}
              suffix="次"
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="dashboard-stat-card page-card" loading={loading}>
            <Statistic
              title="在线执行器"
              value={summary.executorCount}
              prefix={<CloudServerOutlined />}
              suffix="台"
            />
          </Card>
        </Col>
      </Row>

      <Card className="page-card dashboard-chart-card">
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Space
            align="center"
            style={{ width: '100%', justifyContent: 'space-between', flexWrap: 'wrap' }}
          >
            <div>
              <Typography.Title level={4} style={{ margin: 0 }}>
                调度报表
              </Typography.Title>
              <Typography.Text type="secondary">
                按时间区间查看成功、失败和运行中的分布趋势
              </Typography.Text>
            </div>
            <DatePicker.RangePicker
              allowClear={false}
              showTime
              value={range}
              onChange={(value) => {
                if (value?.[0] && value?.[1]) {
                  setRange([value[0], value[1]]);
                }
              }}
            />
          </Space>

          {chartLoading ? (
            <div className="page-loading" style={{ minHeight: 340 }}>
              <Spin />
            </div>
          ) : !chart ? (
            <Empty description="暂无图表数据" />
          ) : (
            <Row gutter={[16, 16]}>
              <Col xs={24} xl={15}>
                <Card bordered={false}>
                  <ReactECharts option={lineOption} className="dashboard-chart" />
                </Card>
              </Col>
              <Col xs={24} xl={9}>
                <Card bordered={false}>
                  <ReactECharts option={pieOption} className="dashboard-chart" />
                </Card>
              </Col>
            </Row>
          )}
        </Space>
      </Card>
    </Space>
  );
}

export default DashboardPage;
