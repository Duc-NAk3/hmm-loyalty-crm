import { Alert, Button, Card, Form, InputNumber, Typography } from 'antd'
import { useState } from 'react'
import { api, errorMessage } from '../shared/api'
import type { Terminal } from '../shared/session'
import type { Shift } from './types'

/** Mở ca với số tiền đầu ca; mỗi quầy chỉ có một ca đang mở (FR-01). */
export default function OpenShift({ terminal, onOpened }: { terminal: Terminal; onOpened: (s: Shift) => void }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()

  const open = async (v: { openingCash: number }) => {
    setLoading(true)
    setError(undefined)
    try {
      onOpened(await api<Shift>('/api/pos/shifts/open', { body: { terminalId: terminal.terminal_id, openingCash: v.openingCash } }))
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 48 }}>
      <Card style={{ width: '100%', maxWidth: 420 }}>
        <Typography.Title level={4} style={{ marginTop: 0 }}>Mở ca bán hàng</Typography.Title>
        <Typography.Paragraph type="secondary">
          Quầy {terminal.terminal_code} chưa có ca đang mở. Đếm tiền trong két và nhập số tiền đầu ca.
        </Typography.Paragraph>
        {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
        <Form layout="vertical" initialValues={{ openingCash: 2_000_000 }} onFinish={open} size="large">
          <Form.Item name="openingCash" label="Tiền đầu ca (đ)" rules={[{ required: true, message: 'Nhập tiền đầu ca' }]}>
            <InputNumber<number>
              style={{ width: '100%' }}
              min={0}
              step={100_000}
              formatter={(v) => `${v ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
              parser={(v) => Number((v ?? '').replace(/\./g, ''))}
              autoFocus
            />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>Mở ca</Button>
        </Form>
      </Card>
    </div>
  )
}
