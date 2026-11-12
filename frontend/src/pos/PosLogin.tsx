import { Button, Card, Typography } from 'antd'
import { Navigate, useNavigate } from 'react-router-dom'
import { api } from '../shared/api'
import { Brand } from '../shared/Brand'
import { LoginForm, type LoginValues } from '../shared/LoginForm'
import { type Session, getTerminal, setSession } from '../shared/session'

/** Thu ngân đăng nhập bằng tài khoản cá nhân trên quầy đã đăng ký (FR-01). */
export default function PosLogin() {
  const nav = useNavigate()
  const terminal = getTerminal()
  if (!terminal) return <Navigate to="/pos/setup" replace />

  const login = async (v: LoginValues) => {
    const res = await api<Session>('/api/auth/login', { area: 'pos', body: { ...v, terminalCode: terminal.terminal_code } })
    setSession('pos', { token: res.token, user: res.user })
    nav('/pos')
  }

  return (
    <div className="center-page">
      <Card style={{ width: '100%', maxWidth: 400 }}>
        <Brand subtitle={`POS · ${terminal.terminal_code}`} />
        <Typography.Paragraph type="secondary" style={{ margin: '12px 0 20px' }}>
          {terminal.store_name}
        </Typography.Paragraph>
        <LoginForm onSubmit={login} />
        <Button type="link" block style={{ marginTop: 8 }} onClick={() => nav('/pos/setup')}>
          Đổi quầy
        </Button>
      </Card>
    </div>
  )
}
