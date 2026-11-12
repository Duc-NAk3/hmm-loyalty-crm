import { ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Drawer, Empty, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../shared/api'
import { dateTime } from '../shared/format'
import type { MemberSummary, SmsView } from './types'

const TYPE: Record<string, string> = { SMS_OTP: 'Mã OTP', SMS_WELCOME: 'Chào mừng', SMS_TIER: 'Lên hạng' }

/**
 * CÔNG CỤ DEMO – "điện thoại của khách": hiện 5 SMS giả lập mới nhất gửi tới thành viên (OTP, chào mừng…).
 * POS thật không có màn hình này; khách đọc OTP trên điện thoại của mình.
 */
export default function MockPhone({ member, open, onClose }: { member: MemberSummary | null; open: boolean; onClose: () => void }) {
  const [items, setItems] = useState<SmsView[]>([])
  const [error, setError] = useState<string>()

  const load = useCallback(async () => {
    if (!member) return
    try {
      setItems(await api<SmsView[]>(`/api/pos/mock-phone?customerId=${member.customerId}`))
      setError(undefined)
    } catch (e) {
      setError(errorMessage(e))
    }
  }, [member])

  // Tự làm mới mỗi 3 giây khi đang mở, để thấy OTP vừa gửi
  useEffect(() => {
    if (!open) return
    load()
    const t = setInterval(load, 3000)
    return () => clearInterval(t)
  }, [open, load])

  return (
    <Drawer
      title="Điện thoại khách (giả lập)"
      open={open && !!member}
      onClose={onClose}
      size={300}
      mask={false}
      zIndex={1100}
      extra={<Button size="small" icon={<ReloadOutlined />} onClick={load}>Làm mới</Button>}
    >
      <Alert type="warning" showIcon style={{ marginBottom: 12 }}
        title="Công cụ demo"
        description="SMS chưa gửi thật. Màn hình này thay cho điện thoại của khách để lấy mã OTP khi trình diễn." />
      <Typography.Text type="secondary">{member?.fullName} · {member?.phoneMasked}</Typography.Text>
      {error && <Alert type="error" showIcon title={error} style={{ marginTop: 8 }} />}
      {!items.length && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có tin nhắn" />}
      {items.map((m) => (
        <div key={m.outboxId} style={{ marginTop: 10, padding: '10px 12px', borderRadius: 12, background: '#f0f5ff' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#8c8c8c', marginBottom: 4 }}>
            <span>HoaMaiMart · {TYPE[m.messageType] ?? m.messageType}</span>
            <span>{dateTime(m.createdAt)}</span>
          </div>
          <div style={{ whiteSpace: 'pre-wrap' }}>{highlightOtp(m.text)}</div>
        </div>
      ))}
    </Drawer>
  )
}

/** Tô đậm dãy 6 chữ số trong tin OTP cho dễ đọc khi demo. */
function highlightOtp(text: string) {
  const parts = text.split(/(\b\d{6}\b)/)
  return parts.map((p, i) => (i % 2 ? <b key={i} style={{ fontSize: 18, letterSpacing: 2 }}>{p}</b> : p))
}
