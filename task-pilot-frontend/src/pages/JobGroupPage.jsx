import { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Radio,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons';
import { jobGroupApi } from '../services/api';
import { formatDateTime, getErrorMessage, parsePagePayload } from '../utils/format';

function JobGroupPage() {
  const [filters, setFilters] = useState({ appname: '', title: '' });
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 });
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [form] = Form.useForm();
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [registryModal, setRegistryModal] = useState({ open: false, record: null });

  useEffect(() => {
    loadData();
  }, [pagination.current, pagination.pageSize]);

  async function loadData(customFilters = filters, customPagination = pagination) {
    try {
      setLoading(true);
      const response = await jobGroupApi.pageList({
        offset: (customPagination.current - 1) * customPagination.pageSize,
        pagesize: customPagination.pageSize,
        appname: customFilters.appname,
        title: customFilters.title,
      });
      const page = parsePagePayload(response);
      setRows(page.list);
      setTotal(page.total);
    } catch (error) {
      message.error(getErrorMessage(error, '加载执行器列表失败'));
    } finally {
      setLoading(false);
    }
  }

  function openCreateModal() {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({
      addressType: 0,
      addressList: '',
    });
    setModalOpen(true);
  }

  function openEditModal(record) {
    setEditingRecord(record);
    form.setFieldsValue({
      id: record.id,
      appname: record.appname,
      title: record.title,
      addressType: record.addressType,
      addressList: record.addressList || '',
    });
    setModalOpen(true);
  }

  async function handleSubmit() {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload = {
        ...values,
        addressList: values.addressList || '',
      };
      if (editingRecord) {
        await jobGroupApi.update(payload);
        message.success('执行器已更新');
      } else {
        await jobGroupApi.create(payload);
        message.success('执行器已创建');
      }
      setModalOpen(false);
      form.resetFields();
      loadData();
    } catch (error) {
      if (error?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, editingRecord ? '更新执行器失败' : '创建执行器失败'));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(record) {
    try {
      await jobGroupApi.remove(record.id);
      message.success('执行器已删除');
      loadData();
    } catch (error) {
      message.error(getErrorMessage(error, '删除执行器失败'));
    }
  }

  const addressType = Form.useWatch('addressType', form);
  const columns = useMemo(
    () => [
      {
        title: 'AppName',
        dataIndex: 'appname',
        width: 220,
      },
      {
        title: '执行器名称',
        dataIndex: 'title',
        width: 180,
      },
      {
        title: '地址方式',
        dataIndex: 'addressType',
        width: 120,
        render: (value) =>
          value === 0 ? <Tag color="blue">自动注册</Tag> : <Tag color="gold">手动录入</Tag>,
      },
      {
        title: '在线节点',
        dataIndex: 'registryList',
        render: (_, record) => (
          <Button type="link" onClick={() => setRegistryModal({ open: true, record })}>
            查看 {record.registryList?.length || 0} 个节点
          </Button>
        ),
      },
      {
        title: '更新时间',
        dataIndex: 'updateTime',
        width: 180,
        render: (value) => formatDateTime(value),
      },
      {
        title: '操作',
        key: 'action',
        width: 180,
        render: (_, record) => (
          <Space>
            <Button type="link" icon={<EditOutlined />} onClick={() => openEditModal(record)}>
              编辑
            </Button>
            <Popconfirm
              title="确认删除该执行器吗？"
              description="删除前请确保执行器下没有任务。"
              onConfirm={() => handleDelete(record)}
            >
              <Button type="link" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          </Space>
        ),
      },
    ],
    [],
  );

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card className="page-card">
        <div className="toolbar">
          <Input
            className="toolbar-grow"
            placeholder="按 AppName 搜索"
            value={filters.appname}
            onChange={(event) => setFilters((previous) => ({ ...previous, appname: event.target.value }))}
          />
          <Input
            className="toolbar-grow"
            placeholder="按执行器名称搜索"
            value={filters.title}
            onChange={(event) => setFilters((previous) => ({ ...previous, title: event.target.value }))}
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
              const nextFilters = { appname: '', title: '' };
              const nextPagination = { ...pagination, current: 1 };
              setFilters(nextFilters);
              setPagination(nextPagination);
              loadData(nextFilters, nextPagination);
            }}
          >
            重置
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
            新增执行器
          </Button>
        </div>

        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={rows}
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
        title={editingRecord ? '编辑执行器' : '新增执行器'}
        confirmLoading={submitting}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            label="AppName"
            name="appname"
            rules={[
              { required: true, message: '请输入 AppName' },
              { min: 4, max: 64, message: 'AppName 长度需在 4 到 64 位之间' },
            ]}
          >
            <Input placeholder="请输入 AppName" />
          </Form.Item>
          <Form.Item
            label="执行器名称"
            name="title"
            rules={[{ required: true, message: '请输入执行器名称' }]}
          >
            <Input placeholder="请输入执行器名称" />
          </Form.Item>
          <Form.Item label="地址方式" name="addressType">
            <Radio.Group>
              <Radio value={0}>自动注册</Radio>
              <Radio value={1}>手动录入</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item
            label="节点地址"
            name="addressList"
            extra="自动注册模式下该字段只读；手动录入模式请用英文逗号分隔多个地址。"
            rules={[
              {
                validator(_, value) {
                  if (addressType === 0 || value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('手动录入模式下请输入节点地址'));
                },
              },
            ]}
          >
            <Input.TextArea
              rows={5}
              placeholder="http://127.0.0.1:9999/,http://127.0.0.1:9998/"
              disabled={addressType === 0}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={registryModal.open}
        title="在线节点"
        footer={null}
        onCancel={() => setRegistryModal({ open: false, record: null })}
      >
        {registryModal.record?.registryList?.length ? (
          <Space direction="vertical" style={{ width: '100%' }}>
            {registryModal.record.registryList.map((item) => (
              <Typography.Text key={item} copyable>
                {item}
              </Typography.Text>
            ))}
          </Space>
        ) : (
          <Typography.Text type="secondary">当前没有在线节点</Typography.Text>
        )}
      </Modal>
    </Space>
  );
}

export default JobGroupPage;
