import { Alert, Descriptions, InputNumber, Modal, Typography } from 'antd'
import { useState } from 'react'
import { api, errorMessage } from '../shared/api'
import { dateTime, vnd } from '../shared/format'
import type { Shift } from './types'

interface CloseResult {
  expectedCash: number
  countedCash: number
  difference: number
}

/** Đóng ca: nhập tiền mặt thực đếm, hệ thống so với tiền đầu ca + tiền mặt bán được. */
export default function CloseShiftModal({ shift, open, onCancel, onClosed }: {
  shift: Shift
  open: boolean
  onCancel: () => void
  onClosed: () => void
}) {
  const expected = shift.openingCash + shift.cashSales
  const [counted, setCounted] = useState<number | null>(expected)
  const [result, setResult] = useState<CloseResult>()
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    if (result) return onClosed()
    setLoading(true)
    setError(undefined)
    try {
      setResult(await api<CloseResult>(`/api/pos/shifts/${shift.shiftId}/close`, { body: { countedCash: counted ?? 0 } }))
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      title="Đóng ca"
      open={open}
      onCancel={result ? onClosed : onCancel}
      onOk={submit}
      okText={result ? 'Xong' : 'Xác nhận đóng ca'}
      cancelButtonProps={{ style: result ? { display: 'none' } : undefined }}
      confirmLoading={loading}
      destroyOnHidden
    >
      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 12 }} />}
      <Descriptions column={1} size="small" bordered>
        <Descriptions.Item label="Mở ca lúc">{dateTime(shift.openedAt)}</Descriptions.Item>
        <Descriptions.Item label="Số hóa đơn">{shift.receiptCount}</Descriptions.Item>
        <Descriptions.Item label="Tiền đầu ca">{vnd(shift.openingCash)}</Descriptions.Item>
        <Descriptions.Item label="Tiền mặt bán được">{vnd(shift.cashSales)}</Descriptions.Item>
        <Descriptions.Item label="Tiền mặt theo hệ thống"><b>{vnd(expected)}</b></Descriptions.Item>
      </Descriptions>
      {!result ? (
        <div style={{ marginTop: 16 }}>
          <Typography.Text>Tiền mặt thực đếm (đ)</Typography.Text>
          <InputNumber<number>
            style={{ width: '100%', marginTop: 6 }}
            size="large"
            min={0}
            value={counted}
            onChange={setCounted}
            formatter={(v) => `${v ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
            parser={(v) => Number((v ?? '').replace(/\./g, ''))}
            autoFocus
          />
        </div>
      ) : (
        <Alert
          style={{ marginTop: 16 }}
          showIcon
          type={result.difference === 0 ? 'success' : 'warning'}
          title={result.difference === 0
            ? 'Ca đã đóng, tiền mặt khớp với hệ thống.'
            : `Ca đã đóng, chênh lệch ${vnd(result.difference)}. Thu ngân cần báo quản lý cửa hàng để ghi lý do.`}
        />
      )}
    </Modal>
  )
}
