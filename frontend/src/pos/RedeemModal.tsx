import { MobileOutlined } from '@ant-design/icons'
import { Alert, Button, Input, InputNumber, Modal, Space, Spin, Steps, Typography } from 'antd'
import type { OTPRef } from 'antd/es/input/OTP'
import { useEffect, useRef, useState } from 'react'
import { ApiError, api, errorMessage } from '../shared/api'
import { num, vnd } from '../shared/format'
import type { MemberSummary, RedeemQuote, RedemptionHeld, RedemptionRequested } from './types'

type LineInput = { productId: number; quantity: number }

/**
 * Đổi điểm tại quầy (FR-30, FR-20):
 * 1. Báo giá – tối đa 50% giá trị hàng được đổi điểm, tối thiểu 50 điểm.
 * 2. Gửi OTP tới SĐT của khách (SMS giả lập) và nhập OTP.
 * 3. OTP đúng thì điểm được giữ (HELD); lần đầu OTP đúng thì hồ sơ chuyển Đã xác thực.
 */
export default function RedeemModal({ open, member, lines, terminalId, onClose, onHeld, onOpenPhone }: {
  open: boolean
  member: MemberSummary | null
  lines: LineInput[]
  terminalId: number
  onClose: () => void
  onHeld: (h: RedemptionHeld) => void
  onOpenPhone: () => void
}) {
  const [quote, setQuote] = useState<RedeemQuote | null>(null)
  const [points, setPoints] = useState<number | null>(null)
  const [request, setRequest] = useState<RedemptionRequested | null>(null)
  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const otpRef = useRef<OTPRef>(null)

  // Mở modal: lấy báo giá cho giỏ hàng hiện tại
  useEffect(() => {
    if (!open || !member) return
    setQuote(null)
    setRequest(null)
    setOtp('')
    setError(undefined)
    let alive = true
    api<RedeemQuote>('/api/pos/redemptions/quote', { body: { customerId: member.customerId, lines } })
      .then((q) => {
        if (!alive) return
        setQuote(q)
        setPoints(q.maxPoints)
      })
      .catch((e) => alive && setError(errorMessage(e)))
    return () => {
      alive = false
    }
    // lines chỉ đọc lúc mở modal; giỏ hàng không đổi được khi modal đang mở
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, member?.customerId])

  const minPoints = Math.min(50, quote?.maxPoints ?? 50)
  const vndPerPoint = quote && quote.maxPoints > 0 ? quote.maxAmount / quote.maxPoints : 100
  const pointsOk = quote?.allowed && points != null && points >= minPoints && points <= quote.maxPoints

  const sendOtp = async () => {
    if (!member || !pointsOk) return
    setLoading(true)
    setError(undefined)
    try {
      setRequest(await api<RedemptionRequested>('/api/pos/redemptions', {
        body: { customerId: member.customerId, terminalId, points, lines },
      }))
      setOtp('')
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const verify = async (code = otp) => {
    if (!request || !code.trim() || loading) return
    setLoading(true)
    setError(undefined)
    try {
      onHeld(await api<RedemptionHeld>(`/api/pos/redemptions/${request.redemptionId}/verify`, { body: { terminalId, otp: code.trim() } }))
    } catch (e) {
      setError(errorMessage(e))
      // OTP hết hạn hoặc sai quá số lần: yêu cầu đã bị hủy ở backend, quay về bước chọn điểm
      if (e instanceof ApiError && e.code.startsWith('OTP_') && e.code !== 'OTP_WRONG') setRequest(null)
      setOtp('')
      setTimeout(() => otpRef.current?.focus(), 50) // nhập lại ngay
    } finally {
      setLoading(false)
    }
  }

  // Bỏ yêu cầu đang chờ OTP (đóng modal hoặc chọn lại số điểm) để không treo ở trạng thái chờ
  const dropRequest = () => {
    if (request) api(`/api/pos/redemptions/${request.redemptionId}/cancel`, { body: { terminalId } }).catch(() => {})
    setRequest(null)
  }
  const close = () => {
    dropRequest()
    onClose()
  }

  const footer = !quote?.allowed
    ? [<Button key="c" onClick={close}>Đóng</Button>]
    : request
      ? [
          <Button key="b" onClick={dropRequest} disabled={loading}>Đổi số điểm</Button>,
          <Button key="ok" type="primary" loading={loading} disabled={!otp.trim()} onClick={() => verify()}>Xác nhận OTP</Button>,
        ]
      : [
          <Button key="c" onClick={close}>Hủy</Button>,
          <Button key="ok" type="primary" loading={loading} disabled={!pointsOk} onClick={sendOtp}>Gửi OTP cho khách</Button>,
        ]

  return (
    <Modal title={`Đổi điểm – ${member?.fullName ?? ''}`} open={open} onCancel={close} footer={footer} width={480} destroyOnHidden mask={{ closable: false }}>
      <Steps size="small" current={request ? 1 : 0} style={{ marginBottom: 16 }}
        items={[{ title: 'Chọn số điểm' }, { title: 'Nhập OTP' }, { title: 'Giữ điểm' }]} />
      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 12 }} />}
      {!quote && !error && <div style={{ textAlign: 'center', padding: 24 }}><Spin /></div>}

      {quote && !quote.allowed && <Alert type="warning" showIcon title="Không đổi điểm được" description={quote.reason} />}

      {quote?.allowed && !request && (
        <Space orientation="vertical" style={{ width: '100%' }} size={4}>
          <Row label="Điểm khả dụng" value={`${num(member?.pointBalance)} điểm`} />
          <Row label="Giá trị hàng được đổi điểm" value={vnd(quote.redeemableAmount)} />
          <Row label="Tối đa được dùng" value={`${num(quote.maxPoints)} điểm = ${vnd(quote.maxAmount)}`} />
          <Typography.Text style={{ marginTop: 10 }}>Số điểm khách muốn dùng (tối thiểu {minPoints})</Typography.Text>
          <InputNumber<number> size="large" style={{ width: '100%' }} min={minPoints} max={quote.maxPoints} step={10}
            value={points} onChange={setPoints} onPressEnter={sendOtp} autoFocus />
          <Row label="Trừ vào hóa đơn" value={pointsOk ? '−' + vnd(Math.round(points! * vndPerPoint)) : '—'} strong />
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Hàng hạn chế khuyến mại không được trừ bằng điểm. Phần trả bằng điểm không được tính để tích điểm.
          </Typography.Text>
        </Space>
      )}

      {request && (
        <Space orientation="vertical" style={{ width: '100%' }} size={8}>
          <Alert type="info" showIcon
            title={`Đã gửi OTP tới ${request.phoneMasked}`}
            description={`Đổi ${num(request.points)} điểm = ${vnd(request.amountVnd)}. Mã hiệu lực ${request.otpTtlMinutes} phút, sai tối đa 3 lần.`} />
          {/* Nhập đủ 6 số thì tự xác nhận */}
          <Input.OTP ref={otpRef} length={6} value={otp} disabled={loading} autoFocus size="large"
            onChange={(v) => {
              setOtp(v)
              verify(v)
            }} />
          <Button type="dashed" icon={<MobileOutlined />} onClick={onOpenPhone}>Xem OTP trên điện thoại khách (giả lập)</Button>
        </Space>
      )}
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
