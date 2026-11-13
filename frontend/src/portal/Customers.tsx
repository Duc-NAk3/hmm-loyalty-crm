import { SearchOutlined } from '@ant-design/icons'
import { Alert, Card, Input, Select, Space, Table, Tag, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api, errorMessage } from '../shared/api'
import { CUSTOMER_STATUS, TIER_COLORS, date, num, vnd } from '../shared/format'

export interface CustomerRow {
  customerId: number
  memberCode: string
  fullName: string
  phone: string
  tierCode: string
  tierName: string
  status: string
  pointBalance: number
  spend12m: number
  registeredStore: string | null
  createdAt: string
}

const TIERS = [
  { value: 'MEMBER', label: 'Thành viên' },
  { value: 'SILVER', label: 'Bạc' },
  { value: 'GOLD', label: 'Vàng' },
  { value: 'DIAMOND', label: 'Kim cương' },
]

/**
 * Tìm khách hàng (FR-18 trên Portal): theo SĐT, mã thành viên hoặc họ tên; lọc hạng, trạng thái; phân trang.
 * Bấm một dòng để mở Customer 360. Bộ lọc lưu trên URL để quay lại không mất.
 */
export default function Customers() {
  const nav = useNavigate()
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const tier = params.get('tier') ?? undefined
  const status = params.get('status') ?? undefined
  const page = Number(params.get('page') ?? '1')
  const [text, setText] = useState(q)
  const [data, setData] = useState<{ total: number; items: CustomerRow[] }>({ total: 0, items: [] })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()

  const update = (patch: Record<string, string | undefined>) => {
    const next = new URLSearchParams(params)
    Object.entries(patch).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)))
    if (!('page' in patch)) next.delete('page')
    setParams(next, { replace: true })
  }

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(undefined)
    const qs = new URLSearchParams({ page: String(page - 1), size: '20' })
    if (q) qs.set('q', q)
    if (tier) qs.set('tier', tier)
    if (status) qs.set('status', status)
    api<{ total: number; items: CustomerRow[] }>('/api/portal/customers?' + qs)
      .then((d) => alive && setData(d))
      .catch((e) => alive && setError(errorMessage(e)))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [q, tier, status, page])

  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>Khách hàng</Typography.Title>
      <Card>
        <Space wrap style={{ marginBottom: 16 }}>
          <Input.Search
            style={{ width: 320 }}
            placeholder="SĐT, mã thành viên hoặc họ tên"
            enterButton={<SearchOutlined />}
            allowClear
            value={text}
            onChange={(e) => setText(e.target.value)}
            onSearch={(v) => update({ q: v.trim() || undefined })}
          />
          <Select style={{ width: 150 }} placeholder="Hạng" allowClear value={tier} options={TIERS} onChange={(v) => update({ tier: v })} />
          <Select style={{ width: 170 }} placeholder="Trạng thái" allowClear value={status}
            options={Object.entries(CUSTOMER_STATUS).map(([value, s]) => ({ value, label: s.label }))}
            onChange={(v) => update({ status: v })} />
          <Typography.Text type="secondary">{num(data.total)} khách</Typography.Text>
        </Space>
        {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 12 }} />}
        <Table<CustomerRow>
          size="middle"
          rowKey="customerId"
          loading={loading}
          dataSource={data.items}
          onRow={(r) => ({ onClick: () => nav(`/portal/customers/${r.customerId}`), style: { cursor: 'pointer' } })}
          pagination={{
            current: page, pageSize: 20, total: data.total, showSizeChanger: false,
            onChange: (p) => update({ page: p > 1 ? String(p) : undefined }),
          }}
          scroll={{ x: 900 }}
          columns={[
            { title: 'Mã', dataIndex: 'memberCode', width: 120 },
            { title: 'Họ tên', dataIndex: 'fullName', width: 180 },
            { title: 'SĐT', dataIndex: 'phone', width: 120 },
            { title: 'Hạng', dataIndex: 'tierName', width: 110, render: (v, r) => <Tag color={TIER_COLORS[r.tierCode]}>{v}</Tag> },
            {
              title: 'Trạng thái', dataIndex: 'status', width: 130,
              render: (v: string) => <Tag color={CUSTOMER_STATUS[v]?.color}>{CUSTOMER_STATUS[v]?.label ?? v}</Tag>,
            },
            { title: 'Điểm', dataIndex: 'pointBalance', align: 'right', width: 90, render: (v: number) => <span className="tabular">{num(v)}</span> },
            { title: 'Chi tiêu 12 tháng', dataIndex: 'spend12m', align: 'right', width: 150, render: (v: number) => <span className="tabular">{vnd(v)}</span> },
            { title: 'Cửa hàng ĐK', dataIndex: 'registeredStore', width: 110 },
            { title: 'Ngày tham gia', dataIndex: 'createdAt', width: 120, render: date },
          ]}
        />
      </Card>
    </>
  )
}
