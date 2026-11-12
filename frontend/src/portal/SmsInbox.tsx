import { ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Input, Space, Switch, Table, Tag, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../shared/api'
import { dateTime } from '../shared/format'

interface SmsView {
  outboxId: number
  messageType: string
  phone: string
  text: string
  status: string
  createdAt: string
  sentAt: string | null
}

const TYPE: Record<string, { label: string; color: string }> = {
  SMS_OTP: { label: 'OTP đổi điểm', color: 'blue' },
  SMS_WELCOME: { label: 'Chào mừng', color: 'green' },
  SMS_TIER: { label: 'Lên hạng', color: 'gold' },
}

/**
 * Hộp thư SMS giả lập: 100 tin mới nhất trong shared.outbox_messages (OTP, chào mừng, lên hạng).
 * Bản demo không gửi SMS thật; tác vụ nền đánh dấu SENT. Tự làm mới mỗi 5 giây.
 */
export default function SmsInbox() {
  const [phone, setPhone] = useState('')
  const [rows, setRows] = useState<SmsView[]>([])
  const [auto, setAuto] = useState(true)
  const [error, setError] = useState<string>()

  const load = useCallback(() => {
    api<SmsView[]>('/api/portal/sms-outbox' + (phone ? `?phone=${encodeURIComponent(phone)}` : ''))
      .then((r) => { setRows(r); setError(undefined) })
      .catch((e) => setError(errorMessage(e)))
  }, [phone])

  useEffect(() => {
    load()
    if (!auto) return
    const t = setInterval(load, 5000)
    return () => clearInterval(t)
  }, [load, auto])

  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>Hộp thư SMS giả lập</Typography.Title>
      <Card>
        <Alert type="info" showIcon style={{ marginBottom: 16 }}
          title="Bản demo không gửi SMS thật. Mọi tin nhắn được ghi vào outbox (cùng giao dịch với nghiệp vụ) và hiện ở đây." />
        <Space wrap style={{ marginBottom: 16 }}>
          <Input.Search style={{ width: 260 }} placeholder="Lọc theo SĐT" allowClear onSearch={(v) => setPhone(v.trim())} />
          <Space size={6}><Switch size="small" checked={auto} onChange={setAuto} /> Tự làm mới 5 giây</Space>
          <Button icon={<ReloadOutlined />} onClick={load}>Làm mới</Button>
        </Space>
        {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 12 }} />}
        <Table<SmsView> size="middle" rowKey="outboxId" dataSource={rows} pagination={{ pageSize: 20, size: 'small' }} scroll={{ x: 800 }}
          locale={{ emptyText: 'Chưa có tin nhắn' }}
          columns={[
            { title: 'Thời điểm', dataIndex: 'createdAt', width: 140, render: dateTime },
            { title: 'Loại', dataIndex: 'messageType', width: 130, render: (t: string) => <Tag color={TYPE[t]?.color}>{TYPE[t]?.label ?? t}</Tag> },
            { title: 'SĐT nhận', dataIndex: 'phone', width: 120 },
            { title: 'Nội dung', dataIndex: 'text' },
            { title: 'Trạng thái', dataIndex: 'status', width: 110, render: (s: string) => <Tag color={s === 'SENT' ? 'green' : 'default'}>{s === 'SENT' ? 'Đã gửi' : 'Chờ gửi'}</Tag> },
          ]}
        />
      </Card>
    </>
  )
}
