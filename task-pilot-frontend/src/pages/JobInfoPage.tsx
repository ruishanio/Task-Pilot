import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Button,
  Card,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  ClockCircleOutlined,
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { jobGroupApi, jobInfoApi } from '../services/api';
import { formatDateTime, getErrorMessage, parsePagePayload } from '../utils/format';

const GLUE_SOURCE_TEMPLATES = {
  GLUE_GROOVY: `package com.ruishanio.taskpilot.service.handler;

import com.ruishanio.taskpilot.core.context.TaskPilotHelper;
import com.ruishanio.taskpilot.core.handler.IJobHandler;

public class DemoGlueJobHandler extends IJobHandler {
    @Override
    public void execute() throws Exception {
        TaskPilotHelper.log("TASK-PILOT, Hello World.");
    }
}`,
  GLUE_SHELL: `#!/bin/bash
echo "task-pilot: hello shell"`,
  GLUE_PYTHON: `print("task-pilot: hello python3")`,
  GLUE_PYTHON2: `print "task-pilot: hello python2"`,
  GLUE_NODEJS: `console.log("task-pilot: hello nodejs");`,
  GLUE_POWERSHELL: `Write-Host "task-pilot: hello powershell"`,
  GLUE_PHP: `<?php echo "task-pilot: hello php";`,
};

interface OptionItem {
  label: string;
  value: string | number;
}

interface JobGroupItem {
  id: number;
  title: string;
}

interface JobInfoMeta {
  groups: JobGroupItem[];
  selectedExecutorId: number;
  triggerStatusOptions: OptionItem[];
  scheduleTypeOptions: OptionItem[];
  glueTypeOptions: OptionItem[];
  executorRouteStrategyOptions: OptionItem[];
  executorBlockStrategyOptions: OptionItem[];
  misfireStrategyOptions: OptionItem[];
}

// 与后端 `ScheduleTypeEnum` 对齐，避免页面继续把任意字符串当成合法调度类型。
type ScheduleType = 'NONE' | 'CRON' | 'FIX_RATE';
// 与后端 `ExecutorRouteStrategyEnum` 对齐，限制表单与表格里的策略值范围。
type ExecutorRouteStrategy =
  | 'FIRST'
  | 'LAST'
  | 'ROUND'
  | 'RANDOM'
  | 'CONSISTENT_HASH'
  | 'LEAST_FREQUENTLY_USED'
  | 'LEAST_RECENTLY_USED'
  | 'FAILOVER'
  | 'BUSYOVER'
  | 'SHARDING_BROADCAST';
// 与后端 `MisfireStrategyEnum` 对齐，避免前端误传不存在的失火策略。
type MisfireStrategy = 'DO_NOTHING' | 'FIRE_ONCE_NOW';
// 与后端 `ExecutorBlockStrategyEnum` 对齐，约束执行器阻塞策略的可选值。
type ExecutorBlockStrategy = 'SERIAL_EXECUTION' | 'DISCARD_LATER' | 'COVER_EARLY';

interface JobInfoRow {
  id: number;
  executorId: number;
  taskName: string;
  taskDesc: string;
  author?: string;
  alarmEmail?: string;
  scheduleType?: ScheduleType;
  scheduleConf?: string;
  glueType?: string;
  executorHandler?: string;
  executorParam?: string;
  executorRouteStrategy?: ExecutorRouteStrategy;
  childTaskId?: string;
  misfireStrategy?: MisfireStrategy;
  executorBlockStrategy?: ExecutorBlockStrategy;
  executorTimeout?: number;
  executorFailRetryCount?: number;
  glueRemark?: string;
  glueSource?: string;
  triggerStatus?: number;
  triggerNextTime?: number | string;
  [key: string]: any;
}

const defaultJobInfoMeta: JobInfoMeta = {
  groups: [],
  selectedExecutorId: -1,
  triggerStatusOptions: [],
  scheduleTypeOptions: [],
  glueTypeOptions: [],
  executorRouteStrategyOptions: [],
  executorBlockStrategyOptions: [],
  misfireStrategyOptions: [],
};

function JobInfoPage() {
  const [meta, setMeta] = useState<JobInfoMeta>(defaultJobInfoMeta);
  const [filters, setFilters] = useState({
    executorId: -1,
    triggerStatus: '-1',
    taskName: '',
    taskDesc: '',
    executorHandler: '',
    author: '',
  });
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 });
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<JobInfoRow[]>([]);
  const [total, setTotal] = useState(0);
  const [form] = Form.useForm();
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingRecord, setEditingRecord] = useState<JobInfoRow | null>(null);
  const [triggerModal, setTriggerModal] = useState<{ open: boolean; record: JobInfoRow | null }>({ open: false, record: null });
  const [triggerSubmitting, setTriggerSubmitting] = useState(false);
  const [triggerForm] = Form.useForm();
  const [nextTimesModal, setNextTimesModal] = useState<{ open: boolean; list: string[]; title: string }>({ open: false, list: [], title: '' });
  const [registryModal, setRegistryModal] = useState<{ open: boolean; title: string; list: string[] }>({ open: false, title: '', list: [] });
  const metaLoadedRef = useRef(false);

  const scheduleType = Form.useWatch('scheduleType', form);
  const glueType = Form.useWatch('glueType', form);

  useEffect(() => {
    loadMeta();
  }, []);

  useEffect(() => {
    if (!metaLoadedRef.current) {
      return;
    }
    loadData();
  }, [pagination.current, pagination.pageSize]);

  useEffect(() => {
    if (!modalOpen || editingRecord || !glueType) {
      return;
    }
    const currentSource = form.getFieldValue('glueSource');
    if (!currentSource && GLUE_SOURCE_TEMPLATES[glueType]) {
      form.setFieldValue('glueSource', GLUE_SOURCE_TEMPLATES[glueType]);
    }
  }, [glueType, editingRecord, form, modalOpen]);

  async function loadMeta() {
    try {
      const response = await jobInfoApi.meta();
      const payload = (response.data as JobInfoMeta) || meta;
      metaLoadedRef.current = true;
      setMeta(payload);
      setFilters((previous) => ({
        ...previous,
        executorId: payload.selectedExecutorId,
      }));
      loadData(
        {
          ...filters,
          executorId: payload.selectedExecutorId,
        },
        { current: 1, pageSize: pagination.pageSize },
      );
    } catch (error) {
      message.error(getErrorMessage(error, '加载任务元数据失败'));
    }
  }

  async function loadData(customFilters = filters, customPagination = pagination) {
    try {
      setLoading(true);
      const response = await jobInfoApi.page({
        offset: (customPagination.current - 1) * customPagination.pageSize,
        pagesize: customPagination.pageSize,
        executorId: customFilters.executorId,
        triggerStatus: customFilters.triggerStatus,
        taskName: customFilters.taskName,
        taskDesc: customFilters.taskDesc,
        executorHandler: customFilters.executorHandler,
        author: customFilters.author,
      });
      const page = parsePagePayload(response);
      setRows(page.list as JobInfoRow[]);
      setTotal(page.total);
    } catch (error) {
      message.error(getErrorMessage(error, '加载任务列表失败'));
    } finally {
      setLoading(false);
    }
  }

  function openCreateModal() {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({
      executorId: meta.selectedExecutorId,
      scheduleType: 'CRON',
      glueType: 'BEAN',
      executorRouteStrategy: meta.executorRouteStrategyOptions[0]?.value,
      executorBlockStrategy: meta.executorBlockStrategyOptions[0]?.value,
      misfireStrategy: meta.misfireStrategyOptions[0]?.value,
      executorTimeout: 0,
      executorFailRetryCount: 0,
      glueRemark: 'GLUE代码初始化',
    });
    setModalOpen(true);
  }

  function openEditModal(record) {
    setEditingRecord(record);
    form.setFieldsValue({
      id: record.id,
      executorId: record.executorId,
      taskName: record.taskName,
      taskDesc: record.taskDesc,
      author: record.author,
      alarmEmail: record.alarmEmail,
      scheduleType: record.scheduleType,
      scheduleConf: record.scheduleConf,
      glueType: record.glueType,
      executorHandler: record.executorHandler,
      executorParam: record.executorParam,
      executorRouteStrategy: record.executorRouteStrategy,
      childTaskId: record.childTaskId,
      misfireStrategy: record.misfireStrategy,
      executorBlockStrategy: record.executorBlockStrategy,
      executorTimeout: record.executorTimeout,
      executorFailRetryCount: record.executorFailRetryCount,
      glueRemark: record.glueRemark,
      glueSource: record.glueSource,
    });
    setModalOpen(true);
  }

  async function handleSubmit() {
    try {
      const values = await form.validateFields();
      setSubmitting(true);

      const payload = {
        ...values,
        executorTimeout: values.executorTimeout || 0,
        executorFailRetryCount: values.executorFailRetryCount || 0,
        glueSource:
          values.glueType !== 'BEAN'
            ? values.glueSource || GLUE_SOURCE_TEMPLATES[values.glueType] || ''
            : values.glueSource || '',
        glueRemark: values.glueRemark || 'GLUE代码初始化',
        childTaskId: values.childTaskId || '',
        alarmEmail: values.alarmEmail || '',
        executorParam: values.executorParam || '',
      };

      if (editingRecord) {
        await jobInfoApi.update(payload);
        message.success('任务已更新');
      } else {
        await jobInfoApi.create(payload);
        message.success('任务已创建');
      }

      setModalOpen(false);
      form.resetFields();
      loadData();
    } catch (error) {
      if (error?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, editingRecord ? '更新任务失败' : '创建任务失败'));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(record) {
    try {
      await jobInfoApi.remove(record.id);
      message.success('任务已删除');
      loadData();
    } catch (error) {
      message.error(getErrorMessage(error, '删除任务失败'));
    }
  }

  async function handleStart(record) {
    try {
      await jobInfoApi.start(record.id);
      message.success('任务已启动');
      loadData();
    } catch (error) {
      message.error(getErrorMessage(error, '启动任务失败'));
    }
  }

  async function handleStop(record) {
    try {
      await jobInfoApi.stop(record.id);
      message.success('任务已停止');
      loadData();
    } catch (error) {
      message.error(getErrorMessage(error, '停止任务失败'));
    }
  }

  // 把次级动作收拢到下拉菜单，避免表格操作区过于拥挤。
  function openMoreActions(record) {
    return [
      {
        key: 'edit',
        icon: <EditOutlined />,
        label: '编辑',
        onClick: () => openEditModal(record),
      },
      {
        key: 'trigger',
        icon: <ThunderboltOutlined />,
        label: '执行一次',
        onClick: () => {
          triggerForm.setFieldsValue({
            executorParam: record.executorParam,
            addressList: '',
          });
          setTriggerModal({ open: true, record });
        },
      },
      {
        key: 'next-times',
        icon: <ClockCircleOutlined />,
        label: '下次时间',
        onClick: () => handlePreviewNextTimes(record),
      },
      {
        key: 'registry',
        icon: <EyeOutlined />,
        label: '注册节点',
        onClick: () => handleShowRegistry(record),
      },
      {
        key: 'delete',
        icon: <DeleteOutlined />,
        danger: true,
        label: '删除',
        onClick: () => {
          Modal.confirm({
            title: '确认删除该任务吗？',
            content: '删除后会一并移除调度日志和 GLUE 备份。',
            okText: '删除',
            okButtonProps: { danger: true },
            cancelText: '取消',
            onOk: () => handleDelete(record),
          });
        },
      },
    ];
  }

  async function handlePreviewNextTimes(record) {
    try {
      const response = await jobInfoApi.nextTriggerTime({
        scheduleType: record.scheduleType,
        scheduleConf: record.scheduleConf,
      });
      setNextTimesModal({
        open: true,
        list: (response.data as string[]) || [],
        title: `${record.taskDesc} 的下次触发时间`,
      });
    } catch (error) {
      message.error(getErrorMessage(error, '计算下次触发时间失败'));
    }
  }

  async function handleShowRegistry(record) {
    try {
      const response = await jobGroupApi.loadById(record.executorId);
      setRegistryModal({
        open: true,
        title: `${record.taskDesc} 的注册节点`,
        list: ((response.data as { registryList?: string[] } | undefined)?.registryList) || [],
      });
    } catch (error) {
      message.error(getErrorMessage(error, '加载注册节点失败'));
    }
  }

  async function handleTriggerSubmit() {
    try {
      const values = await triggerForm.validateFields();
      setTriggerSubmitting(true);
      await jobInfoApi.trigger({
        id: triggerModal.record.id,
        executorParam: values.executorParam || '',
        addressList: values.addressList || '',
      });
      message.success('任务已触发');
      setTriggerModal({ open: false, record: null });
      triggerForm.resetFields();
    } catch (error) {
      if (error?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, '触发任务失败'));
    } finally {
      setTriggerSubmitting(false);
    }
  }

  const groupMap = useMemo(
    () => new Map(meta.groups.map((item) => [item.id, item.title])),
    [meta.groups],
  );

  const scheduleConfLabel = scheduleType === 'FIX_RATE' ? '固定频率（秒）' : 'Cron 表达式';
  const columns = useMemo(
    () => [
      {
        title: 'ID',
        dataIndex: 'id',
        width: 80,
      },
      {
        title: '任务组',
        dataIndex: 'executorId',
        width: 140,
        render: (value) => groupMap.get(value) || value,
      },
      {
        title: '任务名称',
        dataIndex: 'taskName',
        width: 180,
      },
      {
        title: '任务描述',
        dataIndex: 'taskDesc',
        width: 220,
      },
      {
        title: 'JobHandler',
        dataIndex: 'executorHandler',
        width: 180,
      },
      {
        title: '调度配置',
        key: 'schedule',
        width: 220,
        render: (_, record) => `${record.scheduleType} / ${record.scheduleConf}`,
      },
      {
        title: '负责人',
        dataIndex: 'author',
        width: 120,
      },
      {
        title: '状态',
        dataIndex: 'triggerStatus',
        width: 120,
        render: (value) =>
          Number(value) === 1 ? <Tag color="green">运行中</Tag> : <Tag>已停止</Tag>,
      },
      {
        title: '下次触发',
        dataIndex: 'triggerNextTime',
        width: 180,
        render: (value) => formatDateTime(Number(value)),
      },
      {
        title: '操作',
        key: 'action',
        fixed: 'right' as const,
        width: 220,
        render: (_, record) => (
          <Space wrap>
            {Number(record.triggerStatus) === 1 ? (
              <Button type="link" danger onClick={() => handleStop(record)}>
                停止
              </Button>
            ) : (
              <Button type="link" icon={<PlayCircleOutlined />} onClick={() => handleStart(record)}>
                启动
              </Button>
            )}
            <Dropdown
              menu={{ items: openMoreActions(record) }}
              placement="bottomRight"
              trigger={['click']}
            >
              <Button type="link" icon={<DownOutlined />}>
                更多
              </Button>
            </Dropdown>
          </Space>
        ),
      },
    ],
    [groupMap],
  );

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card className="page-card">
        <div className="toolbar">
          <Select
            className="toolbar-grow"
            placeholder="任务组"
            value={filters.executorId}
            options={meta.groups.map((item) => ({ value: item.id, label: item.title }))}
            onChange={(value) => setFilters((previous) => ({ ...previous, executorId: value }))}
          />
          <Select
            style={{ minWidth: 140 }}
            value={filters.triggerStatus}
            options={meta.triggerStatusOptions}
            onChange={(value) => setFilters((previous) => ({ ...previous, triggerStatus: value }))}
          />
          <Input
            className="toolbar-grow"
            placeholder="按任务名称搜索"
            value={filters.taskName}
            onChange={(event) => setFilters((previous) => ({ ...previous, taskName: event.target.value }))}
          />
          <Input
            className="toolbar-grow"
            placeholder="按任务描述搜索"
            value={filters.taskDesc}
            onChange={(event) => setFilters((previous) => ({ ...previous, taskDesc: event.target.value }))}
          />
          <Input
            className="toolbar-grow"
            placeholder="按 JobHandler 搜索"
            value={filters.executorHandler}
            onChange={(event) =>
              setFilters((previous) => ({ ...previous, executorHandler: event.target.value }))
            }
          />
          <Input
            className="toolbar-grow"
            placeholder="按负责人搜索"
            value={filters.author}
            onChange={(event) => setFilters((previous) => ({ ...previous, author: event.target.value }))}
          />
          <Button
            type="primary"
            onClick={() => {
              const nextPagination = { ...pagination, current: 1 };
              setPagination(nextPagination);
              loadData(filters, nextPagination);
            }}
          >
            查询
          </Button>
          <Button
            onClick={() => {
              const nextFilters = {
                executorId: meta.selectedExecutorId,
                triggerStatus: '-1',
                taskName: '',
                taskDesc: '',
                executorHandler: '',
                author: '',
              };
              const nextPagination = { ...pagination, current: 1 };
              setFilters(nextFilters);
              setPagination(nextPagination);
              loadData(nextFilters, nextPagination);
            }}
          >
            重置
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
            新增任务
          </Button>
        </div>

        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 1600 }}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total,
            showSizeChanger: true,
          }}
          onChange={(nextPagination) =>
            setPagination({
              current: nextPagination.current,
              pageSize: nextPagination.pageSize,
            })
          }
        />
      </Card>

      <Modal
        open={modalOpen}
        title={editingRecord ? '编辑任务' : '新增任务'}
        confirmLoading={submitting}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
        width={920}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden>
            <Input />
          </Form.Item>
          <Space style={{ width: '100%' }} size="middle" align="start">
            <Form.Item
              label="任务组"
              name="executorId"
              style={{ width: '100%' }}
              rules={[{ required: true, message: '请选择任务组' }]}
            >
              <Select options={meta.groups.map((item) => ({ value: item.id, label: item.title }))} />
            </Form.Item>
            <Form.Item
              label="任务名称"
              name="taskName"
              style={{ width: '100%' }}
              rules={[
                { required: true, message: '请输入任务名称' },
                { max: 128, message: '任务名称长度不能超过 128 位' },
              ]}
            >
              <Input placeholder="请输入任务唯一名称" />
            </Form.Item>
          </Space>

          <Form.Item
            label="任务描述"
            name="taskDesc"
            rules={[{ required: true, message: '请输入任务描述' }]}
          >
            <Input placeholder="请输入任务描述" />
          </Form.Item>

          <Space style={{ width: '100%' }} size="middle" align="start">
            <Form.Item
              label="负责人"
              name="author"
              style={{ width: '100%' }}
              rules={[{ required: true, message: '请输入负责人' }]}
            >
              <Input placeholder="请输入负责人" />
            </Form.Item>
            <Form.Item label="报警邮箱" name="alarmEmail" style={{ width: '100%' }}>
              <Input placeholder="多个邮箱请用逗号分隔" />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="middle" align="start">
            <Form.Item
              label="调度类型"
              name="scheduleType"
              style={{ width: '100%' }}
              rules={[{ required: true, message: '请选择调度类型' }]}
            >
              <Select options={meta.scheduleTypeOptions} />
            </Form.Item>
            <Form.Item
              label={scheduleConfLabel}
              name="scheduleConf"
              style={{ width: '100%' }}
              rules={[{ required: true, message: '请输入调度配置' }]}
            >
              <Input placeholder={scheduleType === 'FIX_RATE' ? '请输入秒数' : '请输入 Cron 表达式'} />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="middle" align="start">
            <Form.Item
              label="GLUE 类型"
              name="glueType"
              style={{ width: '100%' }}
              rules={[{ required: true, message: '请选择 GLUE 类型' }]}
            >
              <Select disabled={Boolean(editingRecord)} options={meta.glueTypeOptions} />
            </Form.Item>
            <Form.Item
              label="JobHandler"
              name="executorHandler"
              style={{ width: '100%' }}
              rules={[
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (getFieldValue('glueType') === 'BEAN' && !value) {
                      return Promise.reject(new Error('BEAN 模式下必须填写 JobHandler'));
                    }
                    return Promise.resolve();
                  },
                }),
              ]}
            >
              <Input placeholder="请输入 JobHandler" />
            </Form.Item>
          </Space>

          <Form.Item label="任务参数" name="executorParam">
            <Input.TextArea rows={3} placeholder="可选，触发时会透传到执行器" />
          </Form.Item>

          <Space style={{ width: '100%' }} size="middle" align="start">
            <Form.Item label="路由策略" name="executorRouteStrategy" style={{ width: '100%' }}>
              <Select options={meta.executorRouteStrategyOptions} />
            </Form.Item>
            <Form.Item label="子任务 ID" name="childTaskId" style={{ width: '100%' }}>
              <Input placeholder="多个任务 ID 用英文逗号分隔" />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="middle" align="start">
            <Form.Item label="过期策略" name="misfireStrategy" style={{ width: '100%' }}>
              <Select options={meta.misfireStrategyOptions} />
            </Form.Item>
            <Form.Item label="阻塞策略" name="executorBlockStrategy" style={{ width: '100%' }}>
              <Select options={meta.executorBlockStrategyOptions} />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="middle" align="start">
            <Form.Item label="超时秒数" name="executorTimeout" style={{ width: '100%' }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="失败重试次数" name="executorFailRetryCount" style={{ width: '100%' }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>

          {glueType && glueType !== 'BEAN' ? (
            <>
              <Form.Item label="GLUE 备注" name="glueRemark">
                <Input placeholder="请输入 GLUE 备注" />
              </Form.Item>
              <Form.Item label="GLUE 源码" name="glueSource">
                <Input.TextArea rows={8} placeholder="请输入 GLUE 源码" />
              </Form.Item>
            </>
          ) : null}
        </Form>
      </Modal>

      <Modal
        open={triggerModal.open}
        title={triggerModal.record ? `执行任务：${triggerModal.record.taskDesc}` : '执行任务'}
        confirmLoading={triggerSubmitting}
        onOk={handleTriggerSubmit}
        onCancel={() => setTriggerModal({ open: false, record: null })}
        destroyOnClose
      >
        <Form form={triggerForm} layout="vertical">
          <Form.Item label="执行参数" name="executorParam">
            <Input.TextArea rows={4} placeholder="可选，覆盖当前任务默认参数" />
          </Form.Item>
          <Form.Item label="指定地址" name="addressList" extra="可选，多个地址用英文逗号分隔。">
            <Input.TextArea rows={3} placeholder="http://127.0.0.1:9999/" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={nextTimesModal.open}
        title={nextTimesModal.title}
        footer={null}
        onCancel={() => setNextTimesModal({ open: false, list: [], title: '' })}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          {nextTimesModal.list.length ? (
            nextTimesModal.list.map((item) => <Typography.Text key={item}>{item}</Typography.Text>)
          ) : (
            <Typography.Text type="secondary">当前配置没有可计算的下次触发时间</Typography.Text>
          )}
        </Space>
      </Modal>

      <Modal
        open={registryModal.open}
        title={registryModal.title}
        footer={null}
        onCancel={() => setRegistryModal({ open: false, title: '', list: [] })}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          {registryModal.list.length ? (
            registryModal.list.map((item) => (
              <Typography.Text key={item} copyable>
                {item}
              </Typography.Text>
            ))
          ) : (
            <Typography.Text type="secondary">当前没有注册节点</Typography.Text>
          )}
        </Space>
      </Modal>
    </Space>
  );
}

export default JobInfoPage;
