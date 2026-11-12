import { InfoCircleOutlined } from '@ant-design/icons'
import { Alert, Card, Col, DatePicker, Empty, Row, Segmented, Select, Space, Spin, Table, Tooltip as AntTooltip, Typography } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { type ReactNode, useEffect, useState } from 'react'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api, errorMessage } from '../shared/api'
import { CHART, SERIES, axisTick } from '../shared/chart'
import { compactVnd, num, pct, vnd } from '../shared/format'
import { getSession } from '../shared/session'

interface Kpi {
  receipts: number
  revenue: number
  memberReceipts: number
  memberRevenue: number
  activeMembers: number
  pointsEarned: number
  pointsRedeemed: number
  identificationRate: number
  memberRevenueShare: number
  avgBasket: number
  returningRate: number
  totalMembers: number
  newMembers: number
  verifiedMembers: number
}
interface DashboardView {
  from: string
  to: string
  storeId: number | null
  kpi: Kpi
  tiers: { tierCode: string; name: string; members: number }[]
  byStore: { storeCode: string; name: string; receipts: number; revenue: number; memberRevenue: number; memberReceipts: number }[]
  byDay: { day: string; memberRevenue: number; guestRevenue: number; receipts: number; memberReceipts: number }[]
  categories: { name: string; revenue: number }[]
}
interface Store {
  storeId: number
  storeCode: string
  name: string
}

const PRESETS: { label: string; value: [Dayjs, Dayjs] }[] = [
  { label: '7 ngày qua', value: [dayjs().subtract(6, 'day'), dayjs()] },
  { label: '30 ngày qua', value: [dayjs().subtract(29, 'day'), dayjs()] },
  { label: '90 ngày qua', value: [dayjs().subtract(89, 'day'), dayjs()] },
  { label: 'Tháng này', value: [dayjs().startOf('month'), dayjs()] },
]

/**
 * Dashboard KPI (FR-51): tỷ lệ hóa đơn có định danh, tỷ trọng doanh thu thành viên, tỷ lệ quay lại,
 * phân bố hạng, doanh thu theo ngày / cửa hàng / ngành hàng. Lọc theo khoảng ngày và cửa hàng.
 * Quản lý cửa hàng chỉ xem được cửa hàng của mình (backend tự giới hạn).
 */
export default function Dashboard() {
  const user = getSession('portal')!.user
  const storeLocked = user.role === 'STORE_MANAGER'
  const [range, setRange] = useState<[Dayjs, Dayjs]>(PRESETS[1].value)
  const [storeId, setStoreId] = useState<number | undefined>(storeLocked ? user.storeId ?? undefined : undefined)
  const [stores, setStores] = useState<Store[]>([])
  const [data, setData] = useState<DashboardView | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const [dayView, setDayView] = useState<'chart' | 'table'>('chart')

  useEffect(() => {
    api<Store[]>('/api/portal/stores').then(setStores).catch(() => setStores([]))
  }, [])

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(undefined)
    const qs = new URLSearchParams({ from: range[0].format('YYYY-MM-DD'), to: range[1].format('YYYY-MM-DD') })
    if (storeId) qs.set('storeId', String(storeId))
    api<DashboardView>('/api/portal/dashboard?' + qs)
      .then((d) => alive && setData(d))
      .catch((e) => alive && setError(errorMessage(e)))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [range, storeId])

  const k = data?.kpi
  return (
    <>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>Dashboard</Typography.Title>
        {/* Bộ lọc: một hàng phía trên các biểu đồ */}
        <Space wrap>
          <DatePicker.RangePicker
            value={range}
            onChange={(v) => v?.[0] && v[1] && setRange([v[0], v[1]])}
            presets={PRESETS}
            format="DD/MM/YYYY"
            allowClear={false}
            disabledDate={(d) => d.isAfter(dayjs(), 'day')}
          />
          <Select
            style={{ width: 220 }}
            value={storeId ?? 0}
            disabled={storeLocked}
            onChange={(v) => setStoreId(v || undefined)}
            options={[{ value: 0, label: 'Toàn chuỗi (3 cửa hàng)' }, ...stores.map((s) => ({ value: s.storeId, label: `${s.storeCode} · ${s.name}` }))]}
          />
        </Space>
      </div>

      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
      <Spin spinning={loading}>
        {!k ? (
          <Card><Empty description={loading ? 'Đang tải…' : 'Không có dữ liệu'} /></Card>
        ) : (
          <Space orientation="vertical" size={16} style={{ width: '100%' }}>
            {/* Hàng KPI */}
            <Row gutter={[16, 16]}>
              <Tile label="Doanh thu" value={compactVnd(k.revenue)} sub={`${num(k.receipts)} hóa đơn · giỏ TB ${compactVnd(k.avgBasket)}`} title={vnd(k.revenue)} />
              <Tile label="Hóa đơn có định danh" value={pct(k.identificationRate)} sub={`${num(k.memberReceipts)} / ${num(k.receipts)} hóa đơn gắn thành viên`}
                hint="Tỷ lệ hóa đơn có gắn thành viên – chỉ số chính của chương trình Loyalty" />
              <Tile label="Doanh thu từ thành viên" value={pct(k.memberRevenueShare)} sub={compactVnd(k.memberRevenue)} />
              <Tile label="Thành viên quay lại" value={pct(k.returningRate)} sub={`${num(k.activeMembers)} thành viên mua trong kỳ`}
                hint="Tỷ lệ thành viên có từ 2 hóa đơn trở lên trong kỳ, trên số thành viên có mua" />
              <Tile label="Tổng thành viên" value={num(k.totalMembers)} sub={`+${num(k.newMembers)} mới trong kỳ · ${num(k.verifiedMembers)} đã xác thực`} />
              <Tile label="Điểm trong kỳ" value={`+${num(k.pointsEarned)}`} sub={`Đã đổi ${num(k.pointsRedeemed)} điểm (${compactVnd(k.pointsRedeemed * 100)})`} />
            </Row>

            <Card
              title="Doanh thu theo ngày"
              extra={<Segmented size="small" value={dayView} onChange={(v) => setDayView(v as 'chart' | 'table')}
                options={[{ label: 'Biểu đồ', value: 'chart' }, { label: 'Bảng', value: 'table' }]} />}
            >
              {dayView === 'chart' ? <DayChart data={data!.byDay} /> : <DayTable data={data!.byDay} />}
            </Card>

            <Row gutter={[16, 16]}>
              {/* Một cửa hàng thì không vẽ biểu đồ một thanh – số liệu đã có ở hàng KPI */}
              {data!.byStore.length > 1 && (
                <Col xs={24} xl={12}>
                  <Card title="Doanh thu theo cửa hàng" style={{ height: '100%' }}>
                    <StoreChart data={data!.byStore} />
                  </Card>
                </Col>
              )}
              <Col xs={24} xl={data!.byStore.length > 1 ? 12 : 24}>
                <Card title="Phân bố hạng thành viên" style={{ height: '100%' }}
                  extra={<Typography.Text type="secondary">{data!.storeId ? 'khách đăng ký tại cửa hàng' : 'toàn chuỗi'}, hạng hiện tại</Typography.Text>}>
                  <RankedBars data={data!.tiers.map((t) => ({ name: t.name, value: t.members }))} format={num} unit="thành viên" keepOrder />
                </Card>
              </Col>
            </Row>

            <Card title="Ngành hàng doanh thu cao nhất">
              <RankedBars data={data!.categories.map((c) => ({ name: c.name, value: c.revenue }))} format={compactVnd} full={vnd} />
            </Card>
          </Space>
        )}
      </Spin>
    </>
  )
}

/** Thẻ KPI: số lớn + một dòng phụ. */
function Tile({ label, value, sub, hint, title }: { label: string; value: string; sub: ReactNode; hint?: string; title?: string }) {
  return (
    <Col xs={12} md={8} xl={4}>
      <Card size="small" style={{ height: '100%' }} styles={{ body: { padding: '14px 16px' } }}>
        <Typography.Text type="secondary" style={{ fontSize: 13 }}>
          {label}{hint && <AntTooltip title={hint}><InfoCircleOutlined style={{ marginLeft: 6 }} /></AntTooltip>}
        </Typography.Text>
        <div style={{ fontSize: 28, fontWeight: 600, lineHeight: 1.25, margin: '4px 0' }} title={title}>{value}</div>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>{sub}</Typography.Text>
      </Card>
    </Col>
  )
}

/** Tooltip chung: nền trắng, chữ màu chữ (không dùng màu chuỗi cho chữ), ô màu nhỏ nhận diện chuỗi. */
function ChartTip({ active, label, rows }: { active?: boolean; label: ReactNode; rows: { color?: string; name: string; value: string }[] }) {
  if (!active) return null
  return (
    <div style={{ background: '#fff', border: '1px solid rgba(11,11,11,0.10)', borderRadius: 8, padding: '8px 10px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)', fontSize: 12 }}>
      <div style={{ fontWeight: 600, marginBottom: 4 }}>{label}</div>
      {rows.map((r) => (
        <div key={r.name} style={{ display: 'flex', alignItems: 'center', gap: 6, justifyContent: 'space-between', minWidth: 180 }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: 6, color: CHART.textSecondary }}>
            {r.color && <span style={{ width: 10, height: 10, borderRadius: 2, background: r.color }} />}{r.name}
          </span>
          <span className="tabular">{r.value}</span>
        </div>
      ))}
    </div>
  )
}

const legendFmt = (v: string) => <span style={{ color: CHART.textSecondary }}>{v}</span>

/** Cột chồng theo ngày: thành viên (dưới, xanh) + vãng lai (trên, cam). Đầu cột bo 4px, khe 2px màu nền giữa hai đoạn. */
function DayChart({ data }: { data: DashboardView['byDay'] }) {
  if (!data.length) return <Empty description="Không có hóa đơn trong kỳ" />
  const rows = data.map((d) => ({ ...d, label: dayjs(d.day).format('DD/MM') }))
  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={rows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }} barCategoryGap="20%">
        <CartesianGrid vertical={false} stroke={CHART.grid} />
        <XAxis dataKey="label" tick={axisTick} tickLine={false} axisLine={{ stroke: CHART.axis }} minTickGap={16} />
        <YAxis tick={axisTick} tickLine={false} axisLine={false} tickFormatter={compactVnd} width={64} />
        <Tooltip
          cursor={{ fill: 'rgba(11,11,11,0.04)' }}
          content={({ active, payload }) => {
            const d = payload?.[0]?.payload as (typeof rows)[number] | undefined
            if (!d) return null
            const total = d.memberRevenue + d.guestRevenue
            return (
              <ChartTip active={active} label={dayjs(d.day).format('dddd DD/MM/YYYY')} rows={[
                { color: SERIES.member, name: 'Thành viên', value: vnd(d.memberRevenue) },
                { color: SERIES.guest, name: 'Vãng lai', value: vnd(d.guestRevenue) },
                { name: 'Tổng', value: vnd(total) },
                { name: 'HĐ có định danh', value: `${d.memberReceipts}/${d.receipts} (${pct(d.receipts ? (d.memberReceipts * 100) / d.receipts : 0)})` },
              ]} />
            )
          }}
        />
        <Legend formatter={legendFmt} iconType="square" iconSize={10} wrapperStyle={{ fontSize: 12 }} />
        <Bar dataKey="memberRevenue" name="Thành viên" stackId="r" fill={SERIES.member} maxBarSize={CHART.barSize}
          stroke={CHART.surface} strokeWidth={1} />
        <Bar dataKey="guestRevenue" name="Vãng lai" stackId="r" fill={SERIES.guest} maxBarSize={CHART.barSize}
          stroke={CHART.surface} strokeWidth={1} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}

/** Bảng số liệu thay cho biểu đồ (đọc chính xác, hỗ trợ người không phân biệt màu). */
function DayTable({ data }: { data: DashboardView['byDay'] }) {
  return (
    <Table size="small" rowKey="day" dataSource={data} pagination={{ pageSize: 10, size: 'small' }}
      columns={[
        { title: 'Ngày', dataIndex: 'day', render: (d: string) => dayjs(d).format('DD/MM/YYYY') },
        { title: 'Doanh thu thành viên', dataIndex: 'memberRevenue', align: 'right', render: (v: number) => <span className="tabular">{num(v)}</span> },
        { title: 'Doanh thu vãng lai', dataIndex: 'guestRevenue', align: 'right', render: (v: number) => <span className="tabular">{num(v)}</span> },
        { title: 'Tổng', align: 'right', render: (_, d) => <b className="tabular">{num(d.memberRevenue + d.guestRevenue)}</b> },
        { title: 'Hóa đơn', dataIndex: 'receipts', align: 'right' },
        { title: 'Có định danh', align: 'right', render: (_, d) => `${d.memberReceipts} (${pct(d.receipts ? (d.memberReceipts * 100) / d.receipts : 0)})` },
      ]}
    />
  )
}

/** Thanh ngang chồng theo cửa hàng (cùng hai màu thành viên / vãng lai), một trục chung. */
function StoreChart({ data }: { data: DashboardView['byStore'] }) {
  if (!data.length) return <Empty />
  const rows = data.map((s) => ({ ...s, guestRevenue: s.revenue - s.memberRevenue, label: s.storeCode }))
  return (
    <ResponsiveContainer width="100%" height={Math.max(140, rows.length * 56 + 48)}>
      <BarChart data={rows} layout="vertical" margin={{ top: 0, right: 16, left: 0, bottom: 0 }} barCategoryGap="30%">
        <CartesianGrid horizontal={false} stroke={CHART.grid} />
        <XAxis type="number" tick={axisTick} tickLine={false} axisLine={false} tickFormatter={compactVnd} />
        <YAxis type="category" dataKey="label" tick={{ ...axisTick, fill: CHART.textSecondary }} tickLine={false} axisLine={{ stroke: CHART.axis }} width={56} />
        <Tooltip
          cursor={{ fill: 'rgba(11,11,11,0.04)' }}
          content={({ active, payload }) => {
            const s = payload?.[0]?.payload as (typeof rows)[number] | undefined
            if (!s) return null
            return (
              <ChartTip active={active} label={s.name} rows={[
                { color: SERIES.member, name: 'Thành viên', value: `${vnd(s.memberRevenue)} (${pct((s.memberRevenue * 100) / (s.revenue || 1))})` },
                { color: SERIES.guest, name: 'Vãng lai', value: vnd(s.guestRevenue) },
                { name: 'Tổng', value: vnd(s.revenue) },
                { name: 'Hóa đơn có định danh', value: `${num(s.memberReceipts)}/${num(s.receipts)} (${pct((s.memberReceipts * 100) / (s.receipts || 1))})` },
              ]} />
            )
          }}
        />
        <Legend formatter={legendFmt} iconType="square" iconSize={10} wrapperStyle={{ fontSize: 12 }} />
        <Bar dataKey="memberRevenue" name="Thành viên" stackId="s" fill={SERIES.member} maxBarSize={CHART.barSize} stroke={CHART.surface} strokeWidth={1} />
        <Bar dataKey="guestRevenue" name="Vãng lai" stackId="s" fill={SERIES.guest} maxBarSize={CHART.barSize} stroke={CHART.surface} strokeWidth={1}
          radius={[0, 4, 4, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}

/**
 * Danh sách thanh ngang một màu (một chuỗi → một màu): tên, thanh, số liệu trên mỗi hàng.
 * Mặc định xếp từ lớn đến nhỏ; `keepOrder` giữ thứ tự gốc (ví dụ thứ tự hạng).
 */
function RankedBars({ data, format, full, unit, keepOrder }: {
  data: { name: string; value: number }[]
  format: (n: number) => string
  full?: (n: number) => string
  unit?: string
  keepOrder?: boolean
}) {
  if (!data.length) return <Empty />
  const rows = keepOrder ? data : [...data].sort((a, b) => b.value - a.value)
  const max = Math.max(...rows.map((r) => r.value), 1)
  const total = rows.reduce((s, r) => s + r.value, 0)
  return (
    <div role="table">
      {rows.map((r) => (
        <div key={r.name} role="row" title={`${r.name}: ${full ? full(r.value) : format(r.value)}${unit ? ' ' + unit : ''} (${pct((r.value * 100) / (total || 1))})`}
          style={{ display: 'grid', gridTemplateColumns: 'minmax(110px, 38%) 1fr auto', alignItems: 'center', gap: 12, padding: '6px 0' }}>
          <span role="cell" style={{ color: CHART.textSecondary, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.name}</span>
          <span role="cell" style={{ height: 12, background: '#f3f2ee', borderRadius: 4 }}>
            <span style={{ display: 'block', height: '100%', width: `${(r.value * 100) / max}%`, minWidth: r.value ? 4 : 0, background: SERIES.member, borderRadius: 4 }} />
          </span>
          <span role="cell" className="tabular" style={{ minWidth: 92, textAlign: 'right' }}>
            {format(r.value)} <Typography.Text type="secondary" style={{ fontSize: 12 }}>{pct((r.value * 100) / (total || 1))}</Typography.Text>
          </span>
        </div>
      ))}
    </div>
  )
}
