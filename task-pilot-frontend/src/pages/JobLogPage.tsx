import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { EyeOutlined, StopOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { useSearchParams } from 'react-router-dom';
import { frontendApi, jobLogApi } from '../services/api';
import { formatDateTime, getErrorMessage, parsePagePayload } from '../utils/format';

interface JobLogMeta {
  groups: Array<{ id: number; title: string }>;
  jobs: Array<{ id: number; taskDesc: string }>;
  selectedExecutorId: number;
  selectedTaskId: number;
  logStatusOptions: Array<{ label: string; value: string }>;
  clearLogOptions: Array<{ label: string; value: string }>;
}

interface JobLogRow {
  id: number;
  taskId: number;
  triggerTime?: string | number;
  triggerCode?: number;
  handleCode?: number;
  executorAddress?: string;
  executorHandler?: string;
  title?: string;
  taskDesc?: string;
  [key: string]: any;
}

interface LiveLogPayload {
  fromLineNum: number;
  toLineNum: number;
  logContent?: string;
  end?: boolean;
}

interface LiveModalState {
  open: boolean;
  row: JobLogRow | null;
  html: string;
}

type DateRange = [Dayjs, Dayjs];

const defaultJobLogMeta: JobLogMeta = {
  groups: [],
  jobs: [],
  selectedExecutorId: 0,
  selectedTaskId: 0,
  logStatusOptions: [],
  clearLogOptions: [],
};

function JobLogPage() {
  const [searchParams] = useSearchParams();
  const [meta, setMeta] = useState<JobLogMeta>(defaultJobLogMeta);
  const [filters, setFilters] = useState({
    executorId: 0,
    taskId: 0,
    logStatus: '-1',
    filterTime: [dayjs().subtract(7, 'day').startOf('day'), dayjs().endOf('day')] as DateRange,
  });
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 });
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [clearModalOpen, setClearModalOpen] = useState(false);
  const [clearSubmitting, setClearSubmitting] = useState(false);
  const [clearForm] = Form.useForm();
  const [liveModal, setLiveModal] = useState<LiveModalState>({ open: false, row: null, html: '' });
  const lineNumberRef = useRef(1);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const metaLoadedRef = useRef(false);

  useEffect(() => {
    loadMeta({
      executorId: Number(searchParams.get('executorId') || 0),
      taskId: Number(searchParams.get('taskId') || 0),
    });
  }, [searchParams]);

  useEffect(() => {
    if (!metaLoadedRef.current) {
      return;
    }
    loadData();
  }, [pagination.current, pagination.pageSize]);

  useEffect(() => {
    if (!liveModal.open || !liveModal.row) {
      return undefined;
    }

    lineNumberRef.current = 1;
    let stopped = false;

    async function pullLog() {
      try {
        const response = await jobLogApi.detailCat({
          logId: liveModal.row.id,
          fromLineNum: lineNumberRef.current,
        });
        const payload = response.data as LiveLogPayload | undefined;
        if (!payload) {
          return;
        }

        if (lineNumberRef.current !== payload.fromLineNum) {
          return;
        }

        if (lineNumberRef.current <= payload.toLineNum) {
          lineNumberRef.current = payload.toLineNum + 1;
          setLiveModal((previous) => ({
            ...previous,
            html: `${previous.html}${payload.logContent || ''}`,
          }));
        }

        if (payload.end && timerRef.current) {
          clearInterval(timerRef.current);
          timerRef.current = null;
        }
      } catch (error) {
        setLiveModal((previous) => ({
          ...previous,
          html: `${previous.html}<br/><span style="color:#f87171;">[滚动日志拉取失败] ${getErrorMessage(error)}</span>`,
        }));
        if (timerRef.current) {
          clearInterval(timerRef.current);
          timerRef.current = null;
        }
      }
    }

    if (Number(liveModal.row.triggerCode) !== 200 && Number(liveModal.row.handleCode) === 0) {
      setLiveModal((previous) => ({
        ...previous,
        html: '<span style="color:#f87171;">[触发失败，未生成可拉取日志]</span>',
      }));
      return undefined;
    }

    pullLog();
    if (Number(liveModal.row.handleCode) <= 0) {
      timerRef.current = setInterval(() => {
        if (!stopped) {
          pullLog();
        }
      }, 3000);
    }

    return () => {
      stopped = true;
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [liveModal.open, liveModal.row]);

  async function loadMeta(params = {}) {
    try {
      const response = await frontendApi.jobLogMeta(params);
      const payload = (response.data as JobLogMeta) || meta;
      metaLoadedRef.current = true;
      setMeta(payload);
      const nextFilters = {
        executorId: payload.selectedExecutorId,
        taskId: payload.selectedTaskId,
        logStatus: '-1',
        filterTime: [
          dayjs().subtract(7, 'day').startOf('day'),
          dayjs().endOf('day'),
        ] as DateRange,
      };
      setFilters(nextFilters);
      const nextPagination = { current: 1, pageSize: pagination.pageSize };
      setPagination(nextPagination);
      loadData(nextFilters, nextPagination);
    } catch (error) {
      message.error(getErrorMessage(error, '加载日志元数据失败'));
    }
  }

  async function loadData(customFilters = filters, customPagination = pagination) {
    if (!customFilters.taskId) {
      setRows([]);
      setTotal(0);
      return;
    }

    try {
      setLoading(true);
      const response = await jobLogApi.pageList({
        offset: (customPagination.current - 1) * customPagination.pageSize,
        pagesize: customPagination.pageSize,
        executorId: customFilters.executorId,
        taskId: customFilters.taskId,
        logStatus: customFilters.logStatus,
        filterTime: `${customFilters.filterTime[0].format('YYYY-MM-DD HH:mm:ss')} - ${customFilters.filterTime[1].format('YYYY-MM-DD HH:mm:ss')}`,
      });
      const page = parsePagePayload(response);
      setRows(page.list);
      setTotal(page.total);
    } catch (error) {
      message.error(getErrorMessage(error, '加载日志列表失败'));
    } finally {
      setLoading(false);
    }
  }

  async function handleKill(record) {
    try {
      await jobLogApi.kill(record.id);
      message.success('日志对应执行已终止');
      loadData();
    } catch (error) {
      message.error(getErrorMessage(error, '终止执行失败'));
    }
  }

  async function handleClear() {
    try {
      const values = await clearForm.validateFields();
      setClearSubmitting(true);
      await jobLogApi.clear({
        executorId: filters.executorId,
        taskId: filters.taskId,
        type: values.type,
      });
      message.success('日志清理任务已提交');
      setClearModalOpen(false);
      loadData();
    } catch (error) {
      if (error?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, '清理日志失败'));
    } finally {
      setClearSubmitting(false);
    }
  }

  const jobMap = useMemo(() => new Map(meta.jobs.map((item) => [item.id, item.taskDesc])), [meta.jobs]);
  const columns = useMemo(
    () => [
      {
        title: '日志 ID',
        dataIndex: 'id',
        width: 100,
      },
      {
        title: '任务',
        dataIndex: 'taskId',
        width: 220,
        render: (value) => jobMap.get(value) || value,
      },
      {
        title: '触发时间',
        dataIndex: 'triggerTime',
        width: 180,
        render: (value) => formatDateTime(value),
      },
      {
        title: '触发状态',
        dataIndex: 'triggerCode',
        width: 120,
        render: (value) =>
          Number(value) === 200 ? <Tag color="green">成功</Tag> : <Tag color="red">失败</Tag>,
      },
      {
        title: '处理状态',
        dataIndex: 'handleCode',
        width: 120,
        render: (value) => {
          if (Number(value) === 0) {
            return <Tag color="processing">运行中</Tag>;
          }
          return Number(value) === 200 ? <Tag color="green">成功</Tag> : <Tag color="red">失败</Tag>;
        },
      },
      {
        title: '执行器地址',
        dataIndex: 'executorAddress',
        width: 220,
      },
      {
        title: 'JobHandler',
        dataIndex: 'executorHandler',
        width: 180,
      },
      {
        title: '操作',
        key: 'action',
        width: 180,
        render: (_, record) => (
          <Space>
            <Button
              type="link"
              icon={<EyeOutlined />}
              onClick={() => setLiveModal({ open: true, row: record, html: '' })}
            >
              滚动日志
            </Button>
            <Button
              type="link"
              danger
              icon={<StopOutlined />}
              disabled={Number(record.triggerCode) !== 200}
              onClick={() => handleKill(record)}
            >
              终止
            </Button>
          </Space>
        ),
      },
    ],
    [jobMap],
  );

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card className="page-card">
        <div className="toolbar">
          <Select
            className="toolbar-grow"
            value={filters.executorId}
            options={meta.groups.map((item) => ({ value: item.id, label: item.title }))}
            onChange={(value) => loadMeta({ executorId: value, taskId: 0 })}
          />
          <Select
            className="toolbar-grow"
            value={filters.taskId}
            options={meta.jobs.map((item) => ({ value: item.id, label: item.taskDesc }))}
            onChange={(value) => setFilters((previous) => ({ ...previous, taskId: value }))}
          />
          <Select
            style={{ minWidth: 140 }}
            value={filters.logStatus}
            options={meta.logStatusOptions}
            onChange={(value) => setFilters((previous) => ({ ...previous, logStatus: value }))}
          />
          <DatePicker.RangePicker
            allowClear={false}
            showTime
            value={filters.filterTime}
            onChange={(value) => {
              if (value?.[0] && value?.[1]) {
                setFilters((previous) => ({ ...previous, filterTime: [value[0], value[1]] }));
              }
            }}
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
                taskId: meta.selectedTaskId,
                logStatus: '-1',
                filterTime: [
                  dayjs().subtract(7, 'day').startOf('day'),
                  dayjs().endOf('day'),
                ] as DateRange,
              };
              const nextPagination = { ...pagination, current: 1 };
              setFilters(nextFilters);
              setPagination(nextPagination);
              loadData(nextFilters, nextPagination);
            }}
          >
            重置
          </Button>
          <Button disabled={!filters.taskId} onClick={() => setClearModalOpen(true)}>
            清理日志
          </Button>
        </div>

        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 1400 }}
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
        open={clearModalOpen}
        title="清理日志"
        confirmLoading={clearSubmitting}
        onOk={handleClear}
        onCancel={() => setClearModalOpen(false)}
        destroyOnClose
      >
        <Form form={clearForm} layout="vertical" initialValues={{ type: meta.clearLogOptions[0]?.value }}>
          <Form.Item label="任务组">
            <Input value={meta.groups.find((item) => item.id === filters.executorId)?.title} disabled />
          </Form.Item>
          <Form.Item label="任务">
            <Input value={jobMap.get(filters.taskId) || ''} disabled />
          </Form.Item>
          <Form.Item label="清理策略" name="type" rules={[{ required: true, message: '请选择清理策略' }]}>
            <Select options={meta.clearLogOptions} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={liveModal.open}
        title={liveModal.row ? `滚动日志：${jobMap.get(liveModal.row.taskId) || liveModal.row.taskId}` : '滚动日志'}
        footer={null}
        width={960}
        onCancel={() => setLiveModal({ open: false, row: null, html: '' })}
      >
        <div className="monospace-block" dangerouslySetInnerHTML={{ __html: liveModal.html || '日志加载中...' }} />
      </Modal>
    </Space>
  );
}

export default JobLogPage;
