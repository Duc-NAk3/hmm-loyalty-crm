import { Card, Collapse, Table, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import { api } from '../shared/api'
import { Brand } from '../shared/Brand'
import { ROLE_NAMES } from '../shared/format'
import { LoginForm, type LoginValues } from '../shared/LoginForm'
import { type Session, setSession } from '../shared/session'
import { menuFor } from './menu'

const DEMO_ACCOUNTS = [
  { username: 'admin', role: 'ADMIN' },
  { username: 'ql.hm001', role: 'STORE_MANAGER' },
  { username: 'cskh', role: 'CSKH' },
  { username: 'marketing', role: 'MARKETING' },
  { username: 'danhmuc', role: 'CATALOG' },
]

export default function PortalLogin() {
  const nav = useNavigate()

  const login = async (v: LoginValues) => {
    const res = await api<Session>('/api/auth/login', { area: 'portal', body: v })
    setSession('portal', { token: res.token, user: res.user })
    nav('/portal/' + (menuFor(res.user.role)[0]?.key ?? ''))
  }

  return (
    <div className="center-page">
      <Card style={{ width: '100%', maxWidth: 420 }}>
        <Brand subtitle="Web Portal" />
        <div style={{ height: 20 }} />
        <LoginForm onSubmit={login} />
        <Collapse
          ghost
          style={{ marginTop: 12 }}
          items={[{
            key: 'demo',
            label: <Typography.Text type="secondary">Tài khoản demo (mật khẩu Demo@123)</Typography.Text>,
            children: (
              <Table
                size="small"
                pagination={false}
                rowKey="username"
                dataSource={DEMO_ACCOUNTS}
                columns={[
                  { title: 'Tên đăng nhập', dataIndex: 'username', render: (u: string) => <Typography.Text code>{u}</Typography.Text> },
                  { title: 'Vai trò', dataIndex: 'role', render: (r: string) => ROLE_NAMES[r] },
                ]}
              />
            ),
          }]}
        />
      </Card>
    </div>
  )
}
