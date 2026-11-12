import { CloseOutlined, GiftOutlined, MobileOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Input, type InputRef, Space, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from 'react'
import { ApiError, api, errorMessage } from '../shared/api'
import { CUSTOMER_STATUS, TIER_COLORS, date, num, vnd } from '../shared/format'
import type { MemberSummary, RedemptionHeld } from './types'

export interface MemberPanelHandle {
  focus: () => void
}

/** Chuỗi trông như SĐT (0…, 84…, +84…) – dùng để điền sẵn khi mở đăng ký nhanh. */
const looksLikePhone = (q: string) => /^(\+?84|0)\d{8,10}$/.test(q.replace(/[\s.-]/g, ''))

/**
 * Khung thành viên trên hóa đơn (FR-03, FR-18): tra cứu SĐT hoặc mã HM…, thẻ thành viên,
 * nút đổi điểm (FR-30) và trạng thái điểm đang giữ có đồng hồ đếm ngược.
 */
const MemberPanel = forwardRef<MemberPanelHandle, {
  member: MemberSummary | null
  hold: RedemptionHeld | null
  disabled?: boolean
  onAttach: (m: MemberSummary) => void
  onDetach: () => void
  onNotFound: (phone: string) => void
  onRedeem: () => void
  onCancelHold: () => void
  onHoldExpired: () => void
  onOpenPhone: () => void
}>(function MemberPanel({ member, hold, disabled, onAttach, onDetach, onNotFound, onRedeem, onCancelHold, onHoldExpired, onOpenPhone }, ref) {
  const [q, setQ] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const inputRef = useRef<InputRef>(null)

  // F4: focus và chọn hết chữ cũ để gõ đè
  useImperativeHandle(ref, () => ({ focus: () => inputRef.current?.focus({ cursor: 'all' }) }), [])

  const lookup = async () => {
    const text = q.trim()
    if (!text) return
    setLoading(true)
    setError(undefined)
    try {
      onAttach(await api<MemberSummary>(`/api/pos/members/lookup?q=${encodeURIComponent(text)}`))
      setQ('')
    } catch (e) {
      if (e instanceof ApiError && e.status === 404 && looksLikePhone(text)) {
        setQ('')
        onNotFound(text)
      } else setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  if (!member)
    return (
      <Card size="small" title={<><UserOutlined /> Thành viên</>} style={{ marginTop: 16 }}>
        <Input.Search
          ref={inputRef}
          placeholder="SĐT hoặc mã thành viên (F4)"
          enterButton="Tra cứu"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onSearch={lookup}
          loading={loading}
          disabled={disabled}
          allowClear
        />
        {error && <Alert type="warning" showIcon title={error} style={{ marginTop: 8 }} />}
        <Typography.Text type="secondary" style={{ display: 'block', marginTop: 6, fontSize: 12 }}>
          SĐT chưa có trong hệ thống sẽ mở form đăng ký nhanh.
        </Typography.Text>
      </Card>
    )

  const status = CUSTOMER_STATUS[member.status]
  return (
    <Card
      size="small"
      style={{ marginTop: 16, borderColor: '#f0c36d' }}
      title={<><UserOutlined /> {member.fullName}</>}
      extra={!hold && <Button type="text" size="small" icon={<CloseOutlined />} onClick={onDetach} disabled={disabled}>Bỏ gắn</Button>}
    >
      <Space size={[4, 4]} wrap style={{ marginBottom: 8 }}>
        <Tag color={TIER_COLORS[member.tierCode]}>{member.tierName} ×{member.tierMultiplier}</Tag>
        {!member.verified && status && <Tag color={status.color}>{status.label}</Tag>}
        {member.redeemLocked && <Tag color="red">Tạm khóa đổi điểm</Tag>}
      </Space>
      <Info label="Mã · SĐT" value={`${member.memberCode} · ${member.phoneMasked}`} />
      <Info label="Điểm" value={`${num(member.pointBalance)} (≈ ${vnd(member.pointValueVnd)})`} strong />
      {member.expiringPoints > 0 && (
        <Info label="Sắp hết hạn" value={`${num(member.expiringPoints)} điểm · ${date(member.expiringAt)}`} warn />
      )}
      {member.nextTierName && (
        <Info label={`Lên ${member.nextTierName}`} value={`còn ${vnd(member.spendToNextTier)}`} />
      )}

      {hold ? (
        <HoldBox hold={hold} disabled={disabled} onCancel={onCancelHold} onExpired={onHoldExpired} />
      ) : (
        <Button block icon={<GiftOutlined />} style={{ marginTop: 10 }} onClick={onRedeem} disabled={disabled}>
          Đổi điểm
        </Button>
      )}
      <Button block type="dashed" size="small" icon={<MobileOutlined />} style={{ marginTop: 8 }} onClick={onOpenPhone}>
        Điện thoại khách (giả lập)
      </Button>
    </Card>
  )
})

export default MemberPanel

/** Điểm đang giữ: số điểm, số tiền trừ, đồng hồ đếm ngược đến lúc hết hạn giữ (mặc định 15 phút). */
function HoldBox({ hold, disabled, onCancel, onExpired }: {
  hold: RedemptionHeld
  disabled?: boolean
  onCancel: () => void
  onExpired: () => void
}) {
  const [left, setLeft] = useState(() => dayjs(hold.holdExpiresAt).diff(dayjs(), 'second'))
  useEffect(() => {
    const t = setInterval(() => setLeft(dayjs(hold.holdExpiresAt).diff(dayjs(), 'second')), 1000)
    return () => clearInterval(t)
  }, [hold.holdExpiresAt])
  useEffect(() => {
    if (left <= 0) onExpired()
  }, [left, onExpired])

  const mm = String(Math.max(0, Math.floor(left / 60))).padStart(2, '0')
  const ss = String(Math.max(0, left % 60)).padStart(2, '0')
  return (
    <div style={{ marginTop: 10, padding: '8px 10px', borderRadius: 8, background: '#f6ffed', border: '1px solid #b7eb8f' }}>
      <Info label="Đang giữ" value={`${num(hold.points)} điểm = −${vnd(hold.amountVnd)}`} strong />
      <Info label="Hết hạn giữ sau" value={`${mm}:${ss}`} warn={left < 120} />
      <Button block danger size="small" style={{ marginTop: 6 }} onClick={onCancel} disabled={disabled}>
        Hủy đổi điểm (trả lại điểm)
      </Button>
    </div>
  )
}

function Info({ label, value, strong, warn }: { label: string; value: string; strong?: boolean; warn?: boolean }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 2 }}>
      <Typography.Text type="secondary">{label}</Typography.Text>
      <Typography.Text strong={strong} type={warn ? 'warning' : undefined} className="tabular" style={{ textAlign: 'right' }}>
        {value}
      </Typography.Text>
    </div>
  )
}
