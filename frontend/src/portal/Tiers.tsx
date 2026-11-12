import { PlayCircleOutlined } from '@ant-design/icons'
import { Alert, App, Button, Card, Col, Descriptions, Popconfirm, Row, Table, Tag, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../shared/api'
import { TIER_COLORS, dateTime, num, vnd } from '../shared/format'
import { getSession } from '../shared/session'

interface TierView {
  tierCode: string
  name: string
  thresholdSpend: number
  multiplier: number
  benefits: string | null
  members: number
}
interface Policy {
  policyId: number
  earnUnitVnd: number
  pointValueVnd: number
  minRedeemPoints: number
  maxRedeemPct: number
  expiryMonths: number
  holdMinutes: number
}
interface ReviewResult {
  customersChecked: number
  spendRecalculated: number
  upgraded: number
  downgraded: number
  ranAt: string
}

/**
 * Hạng thành viên và chính sách điểm (FR-31): bảng hạng, số thành viên mỗi hạng, chính sách đang áp dụng.
 * ADMIN bấm "Chạy xét hạng" để chạy ngay tác vụ hằng ngày (tính lại chi tiêu 12 tháng, lên / xuống hạng).
 */
export default function Tiers() {
  const { message } = App.useApp()
  const isAdmin = getSession('portal')!.user.role === 'ADMIN'
  const [data, setData] = useState<{ tiers: TierView[]; policy: Policy } | null>(null)
  const [error, setError] = useState<string>()
  const [running, setRunning] = useState(false)
  const [result, setResult] = useState<ReviewResult | null>(null)

  const load = useCallback(() => {
    api<{ tiers: TierView[]; policy: Policy }>('/api/portal/loyalty/tiers').then(setData).catch((e) => setError(errorMessage(e)))
  }, [])
  useEffect(load, [load])

  const run = async () => {
    setRunning(true)
    try {
      const r = await api<ReviewResult>('/api/portal/loyalty/tier-review', { method: 'POST' })
      setResult(r)
      message.success('Đã chạy xét hạng')
      load()
    } catch (e) {
      message.error(errorMessage(e))
    } finally {
      setRunning(false)
    }
  }

  const p = data?.policy
  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>Hạng thành viên và chính sách điểm</Typography.Title>
      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
      <Row gutter={[16, 16]}>
        <Col xs={24} xl={15}>
          <Card title="Bảng hạng" style={{ height: '100%' }}
            extra={isAdmin && (
              <Popconfirm title="Chạy xét hạng ngay?" description="Tính lại chi tiêu 12 tháng, lên / xuống hạng; khách lên hạng nhận SMS." okText="Chạy" cancelText="Không" onConfirm={run}>
                <Button type="primary" icon={<PlayCircleOutlined />} loading={running}>Chạy xét hạng</Button>
              </Popconfirm>
            )}>
            <Table<TierView> size="middle" rowKey="tierCode" dataSource={data?.tiers ?? []} pagination={false} loading={!data && !error}
              columns={[
                { title: 'Hạng', dataIndex: 'name', render: (n, t) => <Tag color={TIER_COLORS[t.tierCode]}>{n}</Tag> },
                { title: 'Chi tiêu 12 tháng từ', dataIndex: 'thresholdSpend', align: 'right', render: (v: number) => <span className="tabular">{vnd(v)}</span> },
                { title: 'Hệ số tích điểm', dataIndex: 'multiplier', align: 'right', render: (v: number) => `×${v}` },
                { title: 'Thành viên', dataIndex: 'members', align: 'right', render: (v: number) => <span className="tabular">{num(v)}</span> },
                { title: 'Quyền lợi', dataIndex: 'benefits', render: (v) => v ?? '—' },
              ]}
            />
            {result && (
              <Alert type="success" showIcon style={{ marginTop: 12 }} title={`Xét hạng lúc ${dateTime(result.ranAt)}`}
                description={`Kiểm tra ${num(result.customersChecked)} khách · tính lại chi tiêu ${num(result.spendRecalculated)} · lên hạng ${num(result.upgraded)} · xuống hạng ${num(result.downgraded)}`} />
            )}
            <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0, fontSize: 12 }}>
              Lên hạng ngay khi hóa đơn làm chi tiêu 12 tháng đạt ngưỡng. Xuống hạng chỉ xét trong tác vụ hằng ngày
              (chạy tự động lúc 02:00; nút trên chạy ngay để demo).
            </Typography.Paragraph>
          </Card>
        </Col>
        <Col xs={24} xl={9}>
          <Card title={`Chính sách điểm${p ? ` (phiên bản ${p.policyId})` : ''}`} style={{ height: '100%' }}>
            {p && (
              <Descriptions column={1} size="small" items={[
                { label: 'Tích điểm', children: `${vnd(p.earnUnitVnd)} = 1 điểm × hệ số hạng` },
                { label: 'Giá trị điểm', children: `1 điểm = ${vnd(p.pointValueVnd)}` },
                { label: 'Đổi tối thiểu', children: `${num(p.minRedeemPoints)} điểm` },
                { label: 'Đổi tối đa', children: `${p.maxRedeemPct}% giá trị hàng được đổi điểm` },
                { label: 'Hạn điểm', children: `cuối tháng thứ ${p.expiryMonths} kể từ khi tích` },
                { label: 'Giữ điểm sau OTP', children: `${p.holdMinutes} phút` },
              ]} />
            )}
          </Card>
        </Col>
      </Row>
    </>
  )
}
