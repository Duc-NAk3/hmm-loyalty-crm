import { LockOutlined, PlusOutlined, UnlockOutlined } from '@ant-design/icons'
import { Alert, App, Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../shared/api'
import { dateTime } from '../shared/format'
import { getSession } from '../shared/session'

interface UserView {
  userId: number
  username: string
  fullName: string
  phone: string | null
  role: string
  roleName: string
  storeId: number | null
  storeCode: string | null
  status: string
  lastLoginAt: string | null
}
interface Role {
  roleId: number
  code: string
  name: string
}
interface Store {
  storeId: number
  storeCode: string
  name: string
}
type FormValues = { username: string; fullName: string; phone?: string; role: string; storeId?: number; password?: string }

/** Vai trò bắt buộc gắn một cửa hàng. */
const STORE_ROLES = ['CASHIER', 'STORE_MANAGER']

/**
 * Quản lý người dùng (FR-65) – chỉ ADMIN: tạo, sửa (đổi vai trò, gán cửa hàng, đặt lại mật khẩu), khóa / mở khóa.
 * Bản demo bỏ xác thực hai lớp.
 */
export default function Users() {
  const { message } = App.useApp()
  const me = getSession('portal')!.user
  const [users, setUsers] = useState<UserView[]>([])
  const [roles, setRoles] = useState<Role[]>([])
  const [stores, setStores] = useState<Store[]>([])
  const [loading, setLoading] = useState(false)
  const [editing, setEditing] = useState<UserView | 'new' | null>(null)
  const [filterRole, setFilterRole] = useState<string>()

  const load = useCallback(() => {
    setLoading(true)
    api<UserView[]>('/api/portal/users').then(setUsers).catch((e) => message.error(errorMessage(e))).finally(() => setLoading(false))
  }, [message])
  useEffect(() => {
    load()
    api<Role[]>('/api/portal/users/roles').then(setRoles).catch(() => {})
    api<Store[]>('/api/portal/stores').then(setStores).catch(() => {})
  }, [load])

  const setStatus = async (u: UserView, status: 'ACTIVE' | 'LOCKED') => {
    try {
      await api(`/api/portal/users/${u.userId}/status`, { body: { status } })
      message.success(status === 'LOCKED' ? `Đã khóa ${u.username}` : `Đã mở khóa ${u.username}`)
      load()
    } catch (e) {
      message.error(errorMessage(e))
    }
  }

  const rows = filterRole ? users.filter((u) => u.role === filterRole) : users
  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>Người dùng</Typography.Title>
      <Card>
        <Space wrap style={{ marginBottom: 16 }}>
          <Select style={{ width: 220 }} placeholder="Lọc theo vai trò" allowClear value={filterRole} onChange={setFilterRole}
            options={roles.map((r) => ({ value: r.code, label: r.name }))} />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setEditing('new')}>Thêm người dùng</Button>
          <Typography.Text type="secondary">{rows.length} tài khoản · bản demo không bật xác thực hai lớp</Typography.Text>
        </Space>
        <Table<UserView> size="middle" rowKey="userId" loading={loading} dataSource={rows} pagination={false} scroll={{ x: 900 }}
          columns={[
            { title: 'Tên đăng nhập', dataIndex: 'username', width: 140 },
            { title: 'Họ tên', dataIndex: 'fullName', width: 180 },
            { title: 'SĐT', dataIndex: 'phone', width: 120, render: (x) => x ?? '—' },
            { title: 'Vai trò', dataIndex: 'roleName', width: 170 },
            { title: 'Cửa hàng', dataIndex: 'storeCode', width: 100, render: (x) => x ?? 'Toàn chuỗi' },
            { title: 'Trạng thái', dataIndex: 'status', width: 110, render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'red'}>{s === 'ACTIVE' ? 'Hoạt động' : 'Đã khóa'}</Tag> },
            { title: 'Đăng nhập gần nhất', dataIndex: 'lastLoginAt', width: 150, render: (x) => (x ? dateTime(x) : '—') },
            {
              title: '', width: 170,
              render: (_, u) => (
                <Space>
                  <Button size="small" onClick={() => setEditing(u)}>Sửa</Button>
                  {u.userId !== me.id && (u.status === 'ACTIVE' ? (
                    <Popconfirm title={`Khóa tài khoản ${u.username}? Người này sẽ không đăng nhập được.`} okText="Khóa" cancelText="Không"
                      onConfirm={() => setStatus(u, 'LOCKED')}>
                      <Button size="small" danger icon={<LockOutlined />}>Khóa</Button>
                    </Popconfirm>
                  ) : (
                    <Button size="small" icon={<UnlockOutlined />} onClick={() => setStatus(u, 'ACTIVE')}>Mở khóa</Button>
                  ))}
                </Space>
              ),
            },
          ]}
        />
      </Card>
      <UserModal user={editing} roles={roles} stores={stores} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); load() }} />
    </>
  )
}

function UserModal({ user, roles, stores, onClose, onSaved }: {
  user: UserView | 'new' | null
  roles: Role[]
  stores: Store[]
  onClose: () => void
  onSaved: () => void
}) {
  const { message } = App.useApp()
  const [form] = Form.useForm<FormValues>()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string>()
  const isNew = user === 'new'
  const role = Form.useWatch('role', form)

  useEffect(() => {
    if (!user) return
    setError(undefined)
    form.resetFields()
    if (user !== 'new')
      form.setFieldsValue({ username: user.username, fullName: user.fullName, phone: user.phone ?? undefined, role: user.role, storeId: user.storeId ?? undefined })
  }, [user, form])

  const save = async (v: FormValues) => {
    setSaving(true)
    setError(undefined)
    try {
      const body = { ...v, storeId: v.storeId ?? null, password: v.password || null }
      if (isNew) await api('/api/portal/users', { body })
      else await api(`/api/portal/users/${(user as UserView).userId}`, { method: 'PUT', body })
      message.success(isNew ? `Đã tạo tài khoản ${v.username}` : 'Đã lưu')
      onSaved()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={!!user} title={isNew ? 'Thêm người dùng' : `Sửa ${(user as UserView | null)?.username ?? ''}`} onCancel={onClose}
      onOk={() => form.submit()} okText="Lưu" cancelText="Hủy" confirmLoading={saving} forceRender>
      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 12 }} />}
      <Form form={form} layout="vertical" onFinish={save}>
        <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, whitespace: true }, { max: 50 }]}>
          <Input autoComplete="off" />
        </Form.Item>
        <Form.Item name="fullName" label="Họ tên" rules={[{ required: true, whitespace: true }]}><Input /></Form.Item>
        <Form.Item name="phone" label="SĐT"><Input /></Form.Item>
        <div style={{ display: 'flex', gap: 12 }}>
          <Form.Item name="role" label="Vai trò" rules={[{ required: true }]} style={{ flex: 1 }}>
            <Select options={roles.map((r) => ({ value: r.code, label: r.name }))} />
          </Form.Item>
          <Form.Item name="storeId" label="Cửa hàng" style={{ flex: 1 }}
            rules={[{ required: STORE_ROLES.includes(role), message: 'Vai trò này phải gắn một cửa hàng' }]}>
            <Select allowClear placeholder="Toàn chuỗi" options={stores.map((s) => ({ value: s.storeId, label: `${s.storeCode} · ${s.name}` }))} />
          </Form.Item>
        </div>
        <Form.Item name="password" label={isNew ? 'Mật khẩu ban đầu' : 'Đặt lại mật khẩu (để trống nếu không đổi)'}
          rules={[{ required: isNew, message: 'Nhập mật khẩu ban đầu' }, { min: 6, message: 'Tối thiểu 6 ký tự' }]}>
          <Input.Password autoComplete="new-password" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
