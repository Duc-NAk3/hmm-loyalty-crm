import { Alert, Button, Divider, InputNumber, Modal, Space, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { vnd } from '../shared/format'

/** Thanh toán tiền mặt (FR-04): nhập tiền khách đưa, tính tiền thừa. Phần trả bằng điểm có ở bước sau. */
export default function PaymentModal({ open, total, pointsAmount = 0, loading, error, onCancel, onConfirm }: {
  open: boolean
  total: number
  pointsAmount?: number
  loading: boolean
  error?: string
  onCancel: () => void
  onConfirm: (cashTendered: number) => void
}) {
  const due = total - pointsAmount
  const [tendered, setTendered] = useState<number | null>(due)
  useEffect(() => {
    if (open) setTendered(due)
  }, [open, due])

  // Gợi ý mệnh giá: đúng số tiền, làm tròn lên 10.000, 50.000, 100.000, 500.000
  const quick = useMemo(() => {
    const ups = [10_000, 50_000, 100_000, 500_000].map((u) => Math.ceil(due / u) * u)
    return [...new Set([due, ...ups])].filter((v) => v >= due).slice(0, 5)
  }, [due])

  const change = (tendered ?? 0) - due
  const ok = tendered != null && change >= 0

  return (
    <Modal
      title="Thanh toán"
      open={open}
      onCancel={onCancel}
      width={460}
      destroyOnHidden
      footer={[
        <Button key="c" onClick={onCancel}>Quay lại (Esc)</Button>,
        <Button key="ok" type="primary" disabled={!ok} loading={loading} onClick={() => ok && onConfirm(tendered!)}>
          Hoàn tất hóa đơn (Enter)
        </Button>,
      ]}
    >
      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 12 }} />}
      <Space orientation="vertical" style={{ width: '100%' }} size={4}>
        <Row label="Tổng tiền hàng" value={vnd(total)} />
        {pointsAmount > 0 && <Row label="Trừ bằng điểm" value={'− ' + vnd(pointsAmount)} />}
        <Row label="Khách cần trả tiền mặt" value={vnd(due)} strong />
      </Space>
      <Divider style={{ margin: '14px 0' }} />
      <Typography.Text>Tiền khách đưa</Typography.Text>
      <InputNumber<number>
        style={{ width: '100%', marginTop: 6 }}
        size="large"
        min={0}
        step={1000}
        value={tendered}
        onChange={setTendered}
        formatter={(v) => `${v ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
        parser={(v) => Number((v ?? '').replace(/\./g, ''))}
        onPressEnter={() => ok && onConfirm(tendered!)}
        autoFocus
      />
      <Space wrap style={{ marginTop: 10 }}>
        {quick.map((q) => (
          <Button key={q} onClick={() => setTendered(q)}>{vnd(q)}</Button>
        ))}
      </Space>
      <div style={{ marginTop: 16, padding: '12px 14px', borderRadius: 8, background: ok ? '#f6ffed' : '#fff2f0' }}>
        <Row label={ok ? 'Tiền thừa trả khách' : 'Còn thiếu'} value={vnd(Math.abs(change))} strong />
      </div>
    </Modal>
  )
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
      <Typography.Text type={strong ? undefined : 'secondary'}>{label}</Typography.Text>
      <Typography.Text strong={strong} className="tabular" style={{ fontSize: strong ? 18 : undefined }}>{value}</Typography.Text>
    </div>
  )
}
