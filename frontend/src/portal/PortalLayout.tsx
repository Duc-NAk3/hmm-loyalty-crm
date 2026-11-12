import { LogoutOutlined } from '@ant-design/icons'
import { Avatar, Button, Dropdown, Layout, Menu, Typography } from 'antd'
import { useState } from 'react'
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Brand } from '../shared/Brand'
import { ROLE_NAMES } from '../shared/format'
import { getSession, setSession } from '../shared/session'
import { menuFor } from './menu'

/** Khung Web Portal: thanh bên với menu theo vai trò, chặn trang không thuộc quyền. */
export default function PortalLayout() {
  const nav = useNavigate()
  const { pathname } = useLocation()
  const session = getSession('portal')
  const [collapsed, setCollapsed] = useState(false)
  if (!session) return <Navigate to="/portal/login" replace />

  const items = menuFor(session.user.role)
  const current = pathname.split('/')[2]
  if (!items.length) return <Navigate to="/portal/login" replace />
  if (!current || !items.some((m) => m.key === current)) return <Navigate to={'/portal/' + items[0].key} replace />

  const logout = () => {
    setSession('portal', null)
    nav('/portal/login')
  }
  const u = session.user

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Sider breakpoint="lg" collapsible collapsed={collapsed} onCollapse={setCollapsed} width={232}>
        <div style={{ padding: '16px 18px' }}>
          <Brand light subtitle={collapsed ? undefined : 'Web Portal'} />
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[current]}
          style={{ background: 'transparent' }}
          items={items.map((m) => ({ key: m.key, icon: m.icon, label: m.label }))}
          onClick={(e) => nav('/portal/' + e.key)}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header style={{ background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', paddingInline: 20, borderBottom: '1px solid #eee' }}>
          <Dropdown menu={{ items: [{ key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', onClick: logout }] }}>
            <Button type="text" style={{ height: 48 }}>
              <Avatar style={{ background: '#b45309' }}>{u.fullName.split(' ').pop()?.[0]?.toUpperCase()}</Avatar>
              <span style={{ textAlign: 'left', lineHeight: 1.2, display: 'inline-block' }}>
                <Typography.Text strong>{u.fullName}</Typography.Text>
                <br />
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {ROLE_NAMES[u.role]}{u.storeCode ? ` · ${u.storeCode}` : ' · Toàn chuỗi'}
                </Typography.Text>
              </span>
            </Button>
          </Dropdown>
        </Layout.Header>
        <Layout.Content style={{ padding: 24 }}>
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  )
}
