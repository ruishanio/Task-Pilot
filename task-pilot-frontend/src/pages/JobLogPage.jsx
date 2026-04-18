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
import dayjs from 'dayjs';
import { useSearchParams } from 'react-router-dom';
import { frontendApi, jobLogApi } from '../services/api';
import { formatDateTime, getErrorMessage, parsePagePayload } from '../utils/format';

function JobLogPage() {
  const [searchParams] = useSearchParams();
  const [meta, setMeta] = useState({
    groups: [],
    jobs: [],
    selectedJobGroup: 0,
    selectedJobId: 0,
    logStatusOptions: [],
    clearLogOptions: [],
  });
  const [filters, setFilters] = useState({
    jobGroup: 0,
    jobId: 0,
    logStatus: '-1',
    filterTime: [dayjs().subtract(7, 'day').startOf('day'), dayjs().endOf('day')],
  });
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 });
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [clearModalOpen, setClearModalOpen] = useState(false);
  const [clearSubmitting, setClearSubmitting] = useState(false);
  const [clearForm] = Form.useForm();
  const [liveModal, setLiveModal] = useState({ open: false, row: null, html: '' });
  const lineNumberRef = useRef(1);
  const timerRef = useRef(null);
  const metaLoadedRef = useRef(false);

  useEffect(() => {
    loadMeta({
      jobGroup: Number(searchParams.get('jobGroup') || 0),
      jobId: Number(searchParams.get('jobId') || 0),
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
        const payload = response.data;
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
          html: `${previous.html}<br/><span style="color:#f87171;">[Rolling Log Error] ${getErrorMessage(error)}</span>`,
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
      const payload = response.data || meta;
      metaLoadedRef.current = true;
      setMeta(payload);
      const nextFilters = {
        jobGroup: payload.selectedJobGroup,
        jobId: payload.selectedJobId,
        logStatus: '-1',
        filterTime: [dayjs().subtract(7, 'day').startOf('day'), dayjs().endOf('day')],
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
    if (!customFilters.jobId) {
      setRows([]);
      setTotal(0);
      return;
    }

    try {
      setLoading(true);
      const response = await jobLogApi.pageList({
        offset: (customPagination.current - 1) * customPagination.pageSize,
        pagesize: customPagination.pageSize,
        jobGroup: customFilters.jobGroup,
        jobId: customFilters.jobId,
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
        jobGroup: filters.jobGroup,
        jobId: filters.jobId,
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

  const jobMap = useMemo(() => new Map(meta.jobs.map((item) => [item.id, item.jobDesc])), [meta.jobs]);
  const columns = useMemo(
    () => [
      {
        title: '日志 ID',
        dataIndex: 'id',
        width: 100,
      },
      {
        title: '任务',
        dataIndex: 'jobId',
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
            value={filters.jobGroup}
            options={meta.groups.map((item) => ({ value: item.id, label: item.title }))}
            onChange={(value) => loadMeta({ jobGroup: value, jobId: 0 })}
          />
          <Select
            className="toolbar-grow"
            value={filters.jobId}
            options={meta.jobs.map((item) => ({ value: item.id, label: item.jobDesc }))}
            onChange={(value) => setFilters((previous) => ({ ...previous, jobId: value }))}
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
            onChange={(value) => value && setFilters((previous) => ({ ...previous, filterTime: value }))}
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
                jobGroup: meta.selectedJobGroup,
                jobId: meta.selectedJobId,
                logStatus: '-1',
                filterTime: [dayjs().subtract(7, 'day').startOf('day'), dayjs().endOf('day')],
              };
              const nextPagination = { ...pagination, current: 1 };
              setFilters(nextFilters);
              setPagination(nextPagination);
              loadData(nextFilters, nextPagination);
            }}
          >
            重置
          </Button>
          <Button disabled={!filters.jobId} onClick={() => setClearModalOpen(true)}>
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
            <Input value={meta.groups.find((item) => item.id === filters.jobGroup)?.title} disabled />
          </Form.Item>
          <Form.Item label="任务">
            <Input value={jobMap.get(filters.jobId) || ''} disabled />
          </Form.Item>
          <Form.Item label="清理策略" name="type" rules={[{ required: true, message: '请选择清理策略' }]}>
            <Select options={meta.clearLogOptions} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={liveModal.open}
        title={liveModal.row ? `滚动日志：${jobMap.get(liveModal.row.jobId) || liveModal.row.jobId}` : '滚动日志'}
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
