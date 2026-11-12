import { LogoutOutlined } from '@ant-design/icons'
import { Button, Empty, Layout, Space, Typography } from 'antd'
import { Navigate, useNavigate } from 'react-router-dom'
import { Brand } from '../shared/Brand'
import { getSession, getTerminal, setSession } from '../shared/session'

/** Khung màn hình thu ngân. Phần mở ca và bán hàng làm ở bước B6. */
export default function PosShell() {
  const nav = useNavigate()
  const terminal = getTerminal()
  const session = getSession('pos')
  if (!terminal) return <Navigate to="/pos/setup" replace />
  if (!session) return <Navigate to="/pos/login" replace />

  const logout = () => {
    setSession('pos', null)
    nav('/pos/login')
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#3b2a12', paddingInline: 16 }}>
        <Brand light subtitle={`${terminal.terminal_code} · ${terminal.store_name}`} />
        <Space>
          <Typography.Text style={{ color: '#fff' }}>{session.user.fullName}</Typography.Text>
          <Button icon={<LogoutOutlined />} onClick={logout}>Đăng xuất</Button>
        </Space>
      </Layout.Header>
      <Layout.Content style={{ padding: 24 }}>
        <Empty description="Màn hình bán hàng sẽ có ở bước B6" style={{ marginTop: 80 }} />
      </Layout.Content>
    </Layout>
  )
}
