import { Button, Modal, Result } from 'antd'
import { dateTime, num, vnd } from '../shared/format'
import type { Receipt } from './types'

const METHOD: Record<string, string> = { CASH: 'Tiền mặt', POINTS: 'Điểm', CARD: 'Thẻ', BANK_QR: 'Chuyển khoản / QR', VOUCHER: 'Voucher' }

/** Xem trước hóa đơn in nhiệt (FR-05 ở mức màn hình): dòng hàng, thanh toán, điểm cộng, số dư, hạng. */
export default function ReceiptView({ receipt, onNew }: { receipt: Receipt | null; onNew: () => void }) {
  if (!receipt) return null
  const r = receipt
  return (
    <Modal open footer={null} closable={false} width={440} styles={{ body: { padding: 0 } }} onCancel={onNew}>
      <Result status="success" title="Hoàn tất hóa đơn" style={{ padding: '20px 16px 8px' }}
        subTitle={r.change != null ? `Tiền thừa trả khách: ${vnd(r.change)}` : undefined} />
      <div style={{ margin: '0 20px', padding: 16, background: '#fffdf7', border: '1px dashed #d9c9a3', fontFamily: 'Consolas, "Courier New", monospace', fontSize: 12.5 }}>
        <div style={{ textAlign: 'center', fontWeight: 700 }}>{r.store_name}</div>
        <div style={{ textAlign: 'center' }}>{r.store_address}</div>
        <Hr />
        <Line l={`HĐ: ${r.receipt_no}`} />
        <Line l={dateTime(r.txn_time)} r={`Quầy ${r.terminal_code}`} />
        <Line l={`Thu ngân: ${r.cashier_name}`} />
        <Hr />
        {r.lines.map((l, i) => (
          <div key={i} style={{ marginBottom: 4 }}>
            <div>{l.name}{!l.point_eligible && ' *'}</div>
            <Line l={`  ${num(l.quantity)} x ${num(l.unit_price)}`} r={num(l.line_amount)} />
          </div>
        ))}
        <Hr />
        <Line l="TỔNG CỘNG" r={vnd(r.net_amount)} bold />
        {r.payments.map((p, i) => <Line key={i} l={METHOD[p.method] ?? p.method} r={vnd(p.amount)} />)}
        {r.cashTendered != null && <Line l="Khách đưa" r={vnd(r.cashTendered)} />}
        {r.change != null && <Line l="Tiền thừa" r={vnd(r.change)} />}
        {r.member_code && (
          <>
            <Hr />
            <Line l={`Thành viên: ${r.member_name}`} />
            <Line l={`${r.member_code} · ${r.memberPhone ?? ''}`} r={r.tier_name ?? ''} />
            {!!r.points_redeemed && <Line l="Điểm đã dùng" r={'-' + num(r.points_redeemed)} />}
            <Line l="Điểm cộng" r={'+' + num(r.points_earned ?? 0)} />
            <Line l="Số dư điểm" r={num(r.point_balance)} bold />
            {r.upgradedTo && <div style={{ textAlign: 'center', marginTop: 4 }}>Chúc mừng lên hạng {r.upgradedTo}!</div>}
          </>
        )}
        {r.lines.some((l) => !l.point_eligible) && (
          <div style={{ marginTop: 6 }}>(*) Mặt hàng không tích điểm</div>
        )}
        <Hr />
        <div style={{ textAlign: 'center' }}>Cảm ơn quý khách!</div>
      </div>
      <div style={{ padding: 20 }}>
        <Button type="primary" size="large" block onClick={onNew} autoFocus>Hóa đơn mới (Enter)</Button>
      </div>
    </Modal>
  )
}

const Hr = () => <div style={{ borderTop: '1px dashed #bfae86', margin: '8px 0' }} />

function Line({ l, r, bold }: { l: string; r?: string; bold?: boolean }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, fontWeight: bold ? 700 : undefined }}>
      <span>{l}</span>
      {r != null && <span>{r}</span>}
    </div>
  )
}
