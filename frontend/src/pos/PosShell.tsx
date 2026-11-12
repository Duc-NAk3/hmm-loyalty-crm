import { LogoutOutlined, PoweroffOutlined } from '@ant-design/icons'
import { Alert, Button, Layout, Space, Spin, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { api, errorMessage } from '../shared/api'
import { Brand } from '../shared/Brand'
import { dateTime } from '../shared/format'
import { getSession, getTerminal, setSession } from '../shared/session'
import CloseShiftModal from './CloseShiftModal'
import OpenShift from './OpenShift'
import SaleScreen from './SaleScreen'
import type { Shift } from './types'

/** Khung màn hình thu ngân: kiểm tra quầy và phiên, mở ca nếu chưa có, rồi vào màn hình bán hàng. */
export default function PosShell() {
  const nav = useNavigate()
  const terminal = getTerminal()
  const session = getSession('pos')
  const [shift, setShift] = useState<Shift | null>()
  const [error, setError] = useState<string>()
  const [closing, setClosing] = useState(false)

  const loadShift = useCallback(async () => {
    if (!terminal || !session) return
    try {
      // 204 (không có nội dung) = quầy chưa có ca đang mở
      setShift(await api<Shift | null>(`/api/pos/shifts/current?terminalId=${terminal.terminalId}`))
    } catch (e) {
      setError(errorMessage(e))
    }
  }, [])

  useEffect(() => {
    loadShift()
  }, [loadShift])

  if (!terminal) return <Navigate to="/pos/setup" replace />
  if (!session) return <Navigate to="/pos/login" replace />

  const logout = () => {
    setSession('pos', null)
    nav('/pos/login')
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingInline: 16, gap: 12 }}>
        <Brand light subtitle={`${terminal.terminalCode} · ${terminal.storeName}`} />
        <Space wrap>
          {shift && (
            <Typography.Text style={{ color: '#f5e6c8' }}>
              Ca mở lúc {dateTime(shift.openedAt)} · {shift.receiptCount} hóa đơn
            </Typography.Text>
          )}
          <Typography.Text style={{ color: '#fff' }}>{session.user.fullName}</Typography.Text>
          {shift && <Button icon={<PoweroffOutlined />} onClick={() => setClosing(true)}>Đóng ca</Button>}
          <Button icon={<LogoutOutlined />} onClick={logout}>Đăng xuất</Button>
        </Space>
      </Layout.Header>
      <Layout.Content style={{ padding: 16 }}>
        {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
        {shift === undefined && !error && <Spin style={{ display: 'block', marginTop: 80 }} />}
        {shift === null && <OpenShift terminal={terminal} onOpened={setShift} />}
        {shift && <SaleScreen terminal={terminal} shift={shift} onSold={loadShift} />}
      </Layout.Content>
      {shift && closing && (
        <CloseShiftModal shift={shift} open onCancel={() => setClosing(false)} onClosed={() => {
          setClosing(false)
          setShift(null)
        }} />
      )}
    </Layout>
  )
}
