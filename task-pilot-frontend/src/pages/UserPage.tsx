import { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Checkbox,
  Form,
  Input,
  Modal,
  Popconfirm,
  Radio,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { userApi } from '../services/api';
import { getErrorMessage, joinPermissions, parsePagePayload, parsePermissions } from '../utils/format';

interface UserMeta {
  groups: Array<{ id: number | string; title: string; appname: string }>;
  roleOptions: Array<{ label: string; value: string }>;
}

function UserPage() {
  const [filters, setFilters] = useState({ role: '-1', username: '' });
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 });
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [meta, setMeta] = useState<UserMeta>({ groups: [], roleOptions: [] });
  const [form] = Form.useForm();
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);

  useEffect(() => {
    loadMeta();
  }, []);

  useEffect(() => {
    loadData();
  }, [pagination.current, pagination.pageSize]);

  async function loadMeta() {
    try {
      const response = await userApi.meta();
      setMeta((response.data as UserMeta) || { groups: [], roleOptions: [] });
    } catch (error) {
      message.error(getErrorMessage(error, '加载用户元数据失败'));
    }
  }

  async function loadData(customFilters = filters, customPagination = pagination) {
    try {
      setLoading(true);
      const response = await userApi.page({
        offset: (customPagination.current - 1) * customPagination.pageSize,
        pagesize: customPagination.pageSize,
        username: customFilters.username,
        role: customFilters.role,
      });
      const page = parsePagePayload(response);
      setRows(page.list);
      setTotal(page.total);
    } catch (error) {
      message.error(getErrorMessage(error, '加载用户列表失败'));
    } finally {
      setLoading(false);
    }
  }

  function openCreateModal() {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({
      role: '0',
      permissionIds: [],
    });
    setModalOpen(true);
  }

  function openEditModal(record) {
    setEditingRecord(record);
    form.setFieldsValue({
      id: record.id,
      username: record.username,
      password: '',
      role: String(record.role),
      permissionIds: parsePermissions(record.permission),
    });
    setModalOpen(true);
  }

  async function handleSubmit() {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload = {
        id: values.id,
        username: values.username,
        password: values.password || '',
        role: values.role,
        permission: joinPermissions(values.permissionIds),
      };

      if (editingRecord) {
        await userApi.update(payload);
        message.success('用户已更新');
      } else {
        await userApi.create(payload);
        message.success('用户已创建');
      }
      setModalOpen(false);
      form.resetFields();
      loadData();
    } catch (error) {
      if (error?.errorFields) {
        return;
      }
      message.error(getErrorMessage(error, editingRecord ? '更新用户失败' : '创建用户失败'));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(record) {
    try {
      await userApi.remove(record.id);
      message.success('用户已删除');
      loadData();
    } catch (error) {
      message.error(getErrorMessage(error, '删除用户失败'));
    }
  }

  const columns = useMemo(
    () => [
      {
        title: '用户名',
        dataIndex: 'username',
      },
      {
        title: '密码',
        dataIndex: 'password',
        render: () => '******',
      },
      {
        title: '角色',
        dataIndex: 'role',
        width: 120,
        render: (value) =>
          String(value) === '1' ? <Tag color="blue">管理员</Tag> : <Tag>普通用户</Tag>,
      },
      {
        title: '执行器权限',
        dataIndex: 'permission',
        render: (value) => {
          const permissions = parsePermissions(value);
          if (!permissions.length) {
            return <Tag>全部受限</Tag>;
          }
          return `${permissions.length} 个任务组`;
        },
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
              title="确认删除该用户吗？"
              description="删除后将无法恢复。"
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
          <Radio.Group
            value={filters.role}
            onChange={(event) => setFilters((previous) => ({ ...previous, role: event.target.value }))}
          >
            <Radio.Button value="-1">全部角色</Radio.Button>
            <Radio.Button value="1">管理员</Radio.Button>
            <Radio.Button value="0">普通用户</Radio.Button>
          </Radio.Group>
          <Input
            className="toolbar-grow"
            placeholder="输入用户名筛选"
            value={filters.username}
            onChange={(event) =>
              setFilters((previous) => ({ ...previous, username: event.target.value }))
            }
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
              const nextFilters = { role: '-1', username: '' };
              const nextPagination = { ...pagination, current: 1 };
              setFilters(nextFilters);
              setPagination(nextPagination);
              loadData(nextFilters, nextPagination);
            }}
          >
            重置
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
            新增用户
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
        title={editingRecord ? '编辑用户' : '新增用户'}
        confirmLoading={submitting}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
        width={720}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            label="用户名"
            name="username"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 4, max: 20, message: '用户名长度需在 4 到 20 位之间' },
              {
                pattern: /^[a-z][a-z0-9]*$/,
                message: '用户名需以小写字母开头，且只允许小写字母和数字',
              },
            ]}
          >
            <Input placeholder="请输入用户名" disabled={Boolean(editingRecord)} />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={
              editingRecord
                ? [{ min: 4, max: 20, message: '密码长度需在 4 到 20 位之间' }]
                : [
                    { required: true, message: '请输入密码' },
                    { min: 4, max: 20, message: '密码长度需在 4 到 20 位之间' },
                  ]
            }
          >
            <Input.Password placeholder={editingRecord ? '留空表示不修改密码' : '请输入密码'} />
          </Form.Item>
          <Form.Item label="角色" name="role">
            <Radio.Group>
              <Radio value="1">管理员</Radio>
              <Radio value="0">普通用户</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item label="执行器权限" name="permissionIds">
            <Checkbox.Group style={{ width: '100%' }}>
              <Space direction="vertical" style={{ width: '100%' }}>
                {meta.groups.map((group) => (
                  <Checkbox key={group.id} value={String(group.id)}>
                    {group.title}（{group.appname}）
                  </Checkbox>
                ))}
              </Space>
            </Checkbox.Group>
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}

export default UserPage;
