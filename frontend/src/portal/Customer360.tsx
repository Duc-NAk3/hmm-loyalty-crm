import { ArrowLeftOutlined, CheckCircleFilled, EyeOutlined, WarningFilled } from '@ant-design/icons'
import { Alert, App, Button, Card, Col, Descriptions, Empty, Progress, Row, Spin, Table, Tabs, Tag, Typography } from 'antd'
import { type ReactNode, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, errorMessage } from '../shared/api'
import { CUSTOMER_STATUS, TIER_COLORS, compactVnd, date, dateTime, num, vnd } from '../shared/format'
import { getSession } from '../shared/session'

interface Profile {
  customerId: number
  memberCode: string
  fullName: string
  phone: string
  gender: string | null
  dateOfBirth: string | null
  email: string | null
  status: string
  verifiedAt: string | null
  tierCode: string
  tierName: string
  multiplier: number
  pointBalance: number
  spend12m: number
  redeemLocked: boolean
  employee: boolean
  registeredStore: string | null
  registeredStoreName: string | null
  createdAt: string
  mergedIntoId: number | null
  nextTier: string | null
  nextThreshold: number | null
  thresholdSpend: number
}
interface Stats {
  receipts: number
  lifetimeSpend: number
  lastPurchase: string | null
  storesVisited: number
  pointsEarnedTotal: number
  pointsRedeemedTotal: number
  ledgerSum: number
  expiringPoints: number
  nextExpiry: string | null
}
interface TxnRow {
  transactionId: number
  receiptNo: string
  txnTime: string
  storeCode: string
  storeName: string
  txnType: string
  netAmount: number
  pointsEarned: number | null
  pointsRedeemed: number | null
  source: string
  lineCount: number
}
interface LedgerRow {
  ledgerId: number
  createdAt: string
  entryType: string
  points: number
  balanceAfter: number
  receiptNo: string | null
  redemptionId: number | null
  expiresAt: string | null
}
interface View360 {
  profile: Profile
  stats: Stats
  transactions: TxnRow[]
  ledger: LedgerRow[]
  consents: { purpose: string; status: string; channel: string; termsVersion: string; capturedAt: string; capturedBy: string | null }[]
  tierHistory: { changedAt: string; fromTier: string | null; toTier: string; reason: string }[]
  sources: { sourceVendor: string; sourceCustomerRef: string; batchId: number; importedAt: string; storeCode: string }[]
}
interface LineRow {
  sku: string
  name: string
  category: string
  quantity: number
  unitPrice: number
  lineAmount: number
  pointEligible: boolean
}

const GENDER: Record<string, string> = { FEMALE: 'Nữ', MALE: 'Nam', OTHER: 'Khác' }
const ENTRY: Record<string, string> = {
  EARN: 'Tích điểm', REDEEM: 'Đổi điểm', HOLD: 'Giữ điểm (OTP đúng)', RELEASE: 'Trả điểm giữ', EXPIRE: 'Hết hạn',
  ADJUST: 'Điều chỉnh', REVERSE: 'Hoàn điểm (trả hàng)', MERGE: 'Gộp hồ sơ',
}
const PURPOSE: Record<string, string> = { MEMBERSHIP: 'Tham gia chương trình thành viên', MARKETING_SMS: 'Nhận tin khuyến mại qua SMS' }
const REASON: Record<string, string> = { UPGRADE: 'Lên hạng', DOWNGRADE: 'Xuống hạng', MIGRATION: 'Chuyển đổi dữ liệu' }
const SOURCE: Record<string, string> = { POS: 'POS', OFFLINE_SYNC: 'POS (đồng bộ sau)', MIGRATION: 'Chuyển đổi' }

/**
 * Customer 360 (FR-22): hồ sơ, hạng và tiến độ lên hạng, số dư (đối chiếu tổng sổ cái), hóa đơn ở mọi cửa hàng
 * xem tới từng dòng hàng, sổ cái điểm, consent, lịch sử hạng, nguồn dữ liệu chuyển đổi.
 * "Xem SĐT đầy đủ" chỉ dành cho ADMIN / CSKH và được ghi audit_log.
 */
export default function Customer360() {
  const { id } = useParams()
  const nav = useNavigate()
  const { message } = App.useApp()
  const role = getSession('portal')!.user.role
  const [v, setV] = useState<View360 | null>(null)
  const [error, setError] = useState<string>()
  const [phone, setPhone] = useState<string | null>(null)

  useEffect(() => {
    setV(null)
    setPhone(null)
    setError(undefined)
    api<View360>(`/api/portal/customers/${id}/360`).then(setV).catch((e) => setError(errorMessage(e)))
  }, [id])

  const reveal = async () => {
    try {
      setPhone((await api<{ phone: string }>(`/api/portal/customers/${id}/reveal-phone`, { method: 'POST' })).phone)
      message.info('Đã ghi nhật ký xem SĐT đầy đủ')
    } catch (e) {
      message.error(errorMessage(e))
    }
  }

  const back = (
    <Button type="link" icon={<ArrowLeftOutlined />} onClick={() => nav(-1)} style={{ paddingLeft: 0 }}>Danh sách khách hàng</Button>
  )
  if (error) return <>{back}<Alert type="error" showIcon title={error} /></>
  if (!v) return <>{back}<Card><div style={{ textAlign: 'center', padding: 48 }}><Spin /></div></Card></>

  const p = v.profile
  const s = v.stats
  const st = CUSTOMER_STATUS[p.status]
  const ledgerOk = s.ledgerSum === p.pointBalance
  const toNext = p.nextThreshold != null ? Math.max(0, p.nextThreshold - p.spend12m) : 0
  const progress = p.nextThreshold ? Math.min(100, (p.spend12m * 100) / p.nextThreshold) : 100

  return (
    <>
      {back}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 10, marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>{p.fullName}</Typography.Title>
        <Typography.Text type="secondary">{p.memberCode}</Typography.Text>
        <Tag color={TIER_COLORS[p.tierCode]}>{p.tierName} ×{p.multiplier}</Tag>
        {st && <Tag color={st.color}>{st.label}</Tag>}
        {p.redeemLocked && <Tag color="red">Tạm khóa đổi điểm</Tag>}
        {p.employee && <Tag>Nhân viên</Tag>}
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={10}>
          <Card title="Hồ sơ" style={{ height: '100%' }}>
            <Descriptions column={1} size="small" styles={{ label: { width: 140 } }} items={[
              {
                label: 'Số điện thoại',
                children: (
                  <span>
                    <span className="tabular">{phone ?? p.phone}</span>
                    {!phone && ['ADMIN', 'CSKH'].includes(role) && (
                      <Button size="small" type="link" icon={<EyeOutlined />} onClick={reveal}>Xem SĐT đầy đủ</Button>
                    )}
                  </span>
                ),
              },
              { label: 'Giới tính', children: p.gender ? GENDER[p.gender] ?? p.gender : '—' },
              { label: 'Ngày sinh', children: date(p.dateOfBirth) },
              { label: 'Email', children: p.email ?? '—' },
              { label: 'Cửa hàng đăng ký', children: p.registeredStore ? `${p.registeredStore} · ${p.registeredStoreName}` : '—' },
              { label: 'Ngày tham gia', children: date(p.createdAt) },
              { label: 'Xác thực SĐT', children: p.verifiedAt ? dateTime(p.verifiedAt) : 'Chưa (sẽ xác thực bằng OTP lần đổi điểm đầu)' },
              ...(p.mergedIntoId ? [{ label: 'Đã gộp vào', children: <a onClick={() => nav(`/portal/customers/${p.mergedIntoId}`)}>Hồ sơ #{p.mergedIntoId}</a> }] : []),
            ]} />
          </Card>
        </Col>
        <Col xs={24} lg={14}>
          <Card title="Điểm và hạng" style={{ height: '100%' }}>
            <Row gutter={[16, 16]}>
              <Fig label="Số dư điểm" value={num(p.pointBalance)} sub={
                <span>
                  ≈ {vnd(p.pointBalance * 100)} ·{' '}
                  {ledgerOk
                    ? <Typography.Text type="success"><CheckCircleFilled /> khớp tổng sổ cái</Typography.Text>
                    : <Typography.Text type="danger"><WarningFilled /> lệch sổ cái ({num(s.ledgerSum)})</Typography.Text>}
                </span>
              } />
              <Fig label="Sắp hết hạn" value={num(s.expiringPoints)} sub={s.nextExpiry ? `lô gần nhất hết hạn ${date(s.nextExpiry)}` : 'không có'} />
              <Fig label="Tổng đã tích / đã đổi" value={`${num(s.pointsEarnedTotal)} / ${num(s.pointsRedeemedTotal)}`} sub="điểm" />
              <Fig label="Hóa đơn" value={num(s.receipts)} sub={`${s.storesVisited} cửa hàng · lần cuối ${date(s.lastPurchase)}`} />
              <Fig label="Chi tiêu trọn đời" value={compactVnd(s.lifetimeSpend)} sub={vnd(s.lifetimeSpend)} />
            </Row>
            <div style={{ marginTop: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                <Typography.Text>Chi tiêu 12 tháng: <b className="tabular">{vnd(p.spend12m)}</b></Typography.Text>
                <Typography.Text type="secondary">
                  {p.nextTier ? `còn ${vnd(toNext)} để lên ${p.nextTier}` : 'Đang ở hạng cao nhất'}
                </Typography.Text>
              </div>
              <Progress percent={Math.round(progress)} showInfo={false} strokeColor="#2a78d6" railColor="#f3f2ee" />
              {p.nextThreshold != null && (
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>Ngưỡng {p.nextTier}: {vnd(p.nextThreshold)}</Typography.Text>
              )}
            </div>
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 16 }}>
        <Tabs items={[
          { key: 'txn', label: `Hóa đơn (${num(s.receipts)})`, children: <Transactions customerId={p.customerId} rows={v.transactions} total={s.receipts} /> },
          { key: 'ledger', label: 'Sổ cái điểm', children: <Ledger rows={v.ledger} ledgerSum={s.ledgerSum} balance={p.pointBalance} /> },
          {
            key: 'consent', label: `Consent (${v.consents.length})`,
            children: (
              <Table size="small" rowKey={(r) => r.purpose + r.capturedAt} dataSource={v.consents} pagination={false} columns={[
                { title: 'Mục đích', dataIndex: 'purpose', render: (x: string) => PURPOSE[x] ?? x },
                { title: 'Trạng thái', dataIndex: 'status', render: (x: string) => <Tag color={x === 'GRANTED' ? 'green' : 'default'}>{x === 'GRANTED' ? 'Đồng ý' : 'Đã rút'}</Tag> },
                { title: 'Kênh', dataIndex: 'channel' },
                { title: 'Điều khoản', dataIndex: 'termsVersion' },
                { title: 'Thời điểm', dataIndex: 'capturedAt', render: dateTime },
                { title: 'Người ghi nhận', dataIndex: 'capturedBy', render: (x) => x ?? '—' },
              ]} />
            ),
          },
          {
            key: 'tier', label: `Lịch sử hạng (${v.tierHistory.length})`,
            children: (
              <Table size="small" rowKey={(r) => r.changedAt + r.toTier} dataSource={v.tierHistory} pagination={false} columns={[
                { title: 'Thời điểm', dataIndex: 'changedAt', render: dateTime },
                { title: 'Từ hạng', dataIndex: 'fromTier', render: (x) => x ?? '—' },
                { title: 'Sang hạng', dataIndex: 'toTier', render: (x: string) => <Tag color={TIER_COLORS[x]}>{x}</Tag> },
                { title: 'Lý do', dataIndex: 'reason', render: (x: string) => REASON[x] ?? x },
              ]} />
            ),
          },
          {
            key: 'src', label: `Nguồn dữ liệu (${v.sources.length})`,
            children: v.sources.length ? (
              <Table size="small" rowKey={(r) => r.sourceVendor + r.sourceCustomerRef} dataSource={v.sources} pagination={false} columns={[
                { title: 'POS cũ', dataIndex: 'sourceVendor' },
                { title: 'Mã khách ở POS cũ', dataIndex: 'sourceCustomerRef' },
                { title: 'Cửa hàng', dataIndex: 'storeCode' },
                { title: 'Lô nhập', dataIndex: 'batchId', render: (x) => '#' + x },
                { title: 'Thời điểm nhập', dataIndex: 'importedAt', render: dateTime },
              ]} />
            ) : <Empty description="Hồ sơ tạo trực tiếp trên hệ thống mới, không có bản ghi từ POS cũ" />,
          },
        ]} />
      </Card>
    </>
  )
}

function Fig({ label, value, sub }: { label: string; value: string; sub: ReactNode }) {
  return (
    <Col xs={12} md={8}>
      <Typography.Text type="secondary" style={{ fontSize: 13 }}>{label}</Typography.Text>
      <div style={{ fontSize: 24, fontWeight: 600, lineHeight: 1.3 }}>{value}</div>
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>{sub}</Typography.Text>
    </Col>
  )
}

/** Hóa đơn ở mọi cửa hàng; mở một dòng để xem từng mặt hàng (tải khi mở). */
function Transactions({ customerId, rows, total }: { customerId: number; rows: TxnRow[]; total: number }) {
  const [lines, setLines] = useState<Record<number, LineRow[] | 'error'>>({})
  const load = (txnId: number) => {
    if (lines[txnId]) return
    api<LineRow[]>(`/api/portal/customers/${customerId}/transactions/${txnId}/lines`)
      .then((l) => setLines((m) => ({ ...m, [txnId]: l })))
      .catch(() => setLines((m) => ({ ...m, [txnId]: 'error' })))
  }
  return (
    <>
      {total > rows.length && (
        <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>Hiện {rows.length} hóa đơn gần nhất / {num(total)}.</Typography.Text>
      )}
      <Table<TxnRow>
        size="small"
        rowKey="transactionId"
        dataSource={rows}
        pagination={{ pageSize: 10, size: 'small', showSizeChanger: false }}
        scroll={{ x: 760 }}
        expandable={{
          onExpand: (open, r) => open && load(r.transactionId),
          expandedRowRender: (r) => {
            const l = lines[r.transactionId]
            if (l === 'error') return <Alert type="error" title="Không tải được dòng hàng" />
            if (!l) return <Spin size="small" />
            return (
              <Table<LineRow> size="small" rowKey={(x) => x.sku} dataSource={l} pagination={false} columns={[
                { title: 'Mã hàng', dataIndex: 'sku', width: 100 },
                { title: 'Tên hàng', dataIndex: 'name', render: (n, x) => <>{n}{!x.pointEligible && <Tag style={{ marginLeft: 6 }}>Không tích điểm</Tag>}</> },
                { title: 'Ngành hàng', dataIndex: 'category' },
                { title: 'SL', dataIndex: 'quantity', align: 'right', width: 60 },
                { title: 'Đơn giá', dataIndex: 'unitPrice', align: 'right', render: (x: number) => <span className="tabular">{num(x)}</span> },
                { title: 'Thành tiền', dataIndex: 'lineAmount', align: 'right', render: (x: number) => <b className="tabular">{num(x)}</b> },
              ]} />
            )
          },
        }}
        columns={[
          { title: 'Thời gian', dataIndex: 'txnTime', width: 140, render: dateTime },
          { title: 'Số hóa đơn', dataIndex: 'receiptNo' },
          { title: 'Cửa hàng', dataIndex: 'storeCode', width: 90, render: (x, r) => <span title={r.storeName}>{x}</span> },
          { title: 'Nguồn', dataIndex: 'source', width: 90, render: (x: string) => SOURCE[x] ?? x },
          { title: 'Số dòng', dataIndex: 'lineCount', align: 'right', width: 80 },
          { title: 'Tổng tiền', dataIndex: 'netAmount', align: 'right', render: (x: number) => <span className="tabular">{num(x)}</span> },
          { title: 'Điểm cộng', dataIndex: 'pointsEarned', align: 'right', width: 90, render: (x) => (x ? '+' + num(x) : '—') },
          { title: 'Điểm đổi', dataIndex: 'pointsRedeemed', align: 'right', width: 90, render: (x) => (x ? '−' + num(x) : '—') },
        ]}
      />
    </>
  )
}

/** Sổ cái điểm chỉ ghi thêm: mỗi biến động một dòng, kèm số dư sau bút toán. */
function Ledger({ rows, ledgerSum, balance }: { rows: LedgerRow[]; ledgerSum: number; balance: number }) {
  return (
    <>
      <Alert
        type={ledgerSum === balance ? 'success' : 'error'}
        showIcon
        style={{ marginBottom: 12 }}
        title={`Tổng sổ cái = ${num(ledgerSum)} điểm; số dư trên hồ sơ = ${num(balance)} điểm${ledgerSum === balance ? ' – khớp' : ' – LỆCH'}`}
        description="Sổ cái chỉ ghi thêm (không sửa, không xóa). Đổi điểm: HOLD khi OTP đúng → RELEASE + REDEEM theo từng lô (cũ trước) khi hoàn tất hóa đơn."
      />
      <Table<LedgerRow> size="small" rowKey="ledgerId" dataSource={rows} pagination={{ pageSize: 15, size: 'small', showSizeChanger: false }}
        scroll={{ x: 700 }}
        columns={[
          { title: 'Thời điểm', dataIndex: 'createdAt', width: 140, render: dateTime },
          { title: 'Loại', dataIndex: 'entryType', render: (x: string) => <span title={x}>{ENTRY[x] ?? x}</span> },
          { title: 'Điểm', dataIndex: 'points', align: 'right', width: 90, render: (x: number) => <b className="tabular">{x > 0 ? '+' : '−'}{num(Math.abs(x))}</b> },
          { title: 'Số dư sau', dataIndex: 'balanceAfter', align: 'right', width: 100, render: (x: number) => <span className="tabular">{num(x)}</span> },
          { title: 'Hóa đơn', dataIndex: 'receiptNo', render: (x) => x ?? '—' },
          { title: 'Hết hạn lô', dataIndex: 'expiresAt', width: 110, render: (x) => (x ? date(x) : '—') },
        ]}
      />
    </>
  )
}
