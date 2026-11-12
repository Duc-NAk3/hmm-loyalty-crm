import { DeleteOutlined, ShoppingCartOutlined } from '@ant-design/icons'
import { App, Button, Card, Col, Empty, InputNumber, Popconfirm, Row, Table, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import { useCallback, useEffect, useRef, useState } from 'react'
import { api, errorMessage } from '../shared/api'
import { num, vnd } from '../shared/format'
import type { Terminal } from '../shared/session'
import PaymentModal from './PaymentModal'
import ProductSearch, { type ProductSearchHandle } from './ProductSearch'
import ReceiptView from './ReceiptView'
import { type CartLine, type Product, type Receipt, type Shift, cartTotal, lineAmount } from './types'

/** Mã hóa đơn do quầy sinh – là khóa chống trùng khi gửi lại (receiptNo). */
const newReceiptNo = (terminalCode: string) => `${terminalCode}-${dayjs().format('YYMMDD-HHmmssSSS')}`

/** Màn hình bán hàng: quét mã, giỏ hàng, thanh toán tiền mặt, hóa đơn (FR-02, FR-04). */
export default function SaleScreen({ terminal, shift, onSold }: { terminal: Terminal; shift: Shift; onSold: () => void }) {
  const { message } = App.useApp()
  const [lines, setLines] = useState<CartLine[]>([])
  const [paying, setPaying] = useState(false)
  const [payLoading, setPayLoading] = useState(false)
  const [payError, setPayError] = useState<string>()
  const [receipt, setReceipt] = useState<Receipt | null>(null)
  const receiptNo = useRef(newReceiptNo(terminal.terminalCode))
  const searchRef = useRef<ProductSearchHandle>(null)

  const total = cartTotal(lines)
  const restrictedCount = lines.filter((l) => l.product.promoRestricted || l.product.pointExcluded).length

  const add = (p: Product) => {
    if (p.price < 0) return message.error(`${p.name} chưa có giá bán`)
    setLines((ls) => {
      const i = ls.findIndex((l) => l.product.productId === p.productId)
      if (i >= 0) return ls.map((l, j) => (j === i ? { ...l, quantity: l.quantity + 1 } : l))
      return [...ls, { product: p, quantity: 1 }]
    })
  }

  const setQty = (productId: number, q: number | null) =>
    setLines((ls) => ls.map((l) => (l.product.productId === productId ? { ...l, quantity: Math.max(1, q ?? 1) } : l)))
  const remove = (productId: number) => setLines((ls) => ls.filter((l) => l.product.productId !== productId))

  const openPayment = useCallback(() => {
    if (!lines.length) return message.warning('Hóa đơn chưa có mặt hàng')
    setPayError(undefined)
    setPaying(true)
  }, [lines.length, message])

  const checkout = async (cashTendered: number) => {
    setPayLoading(true)
    setPayError(undefined)
    try {
      const r = await api<Receipt>('/api/pos/sales', {
        body: {
          terminalId: terminal.terminalId,
          shiftId: shift.shiftId,
          receiptNo: receiptNo.current,
          lines: lines.map((l) => ({ productId: l.product.productId, quantity: l.quantity })),
          cashTendered,
        },
      })
      setPaying(false)
      setReceipt(r)
      onSold()
    } catch (e) {
      setPayError(errorMessage(e))
    } finally {
      setPayLoading(false)
    }
  }

  const newSale = () => {
    setReceipt(null)
    setLines([])
    receiptNo.current = newReceiptNo(terminal.terminalCode)
    setTimeout(() => searchRef.current?.focus(), 50)
  }

  // Phím tắt: F2 – ô quét mã, F9 – thanh toán
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'F2') {
        e.preventDefault()
        searchRef.current?.focus()
      } else if (e.key === 'F9' && !paying && !receipt) {
        e.preventDefault()
        openPayment()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [openPayment, paying, receipt])

  return (
    <Row gutter={[16, 16]}>
      <Col xs={24} lg={16}>
        <Card styles={{ body: { padding: 16 } }}>
          <ProductSearch ref={searchRef} onAdd={add} onError={(m) => message.error(m)} />
          <Table<CartLine>
            style={{ marginTop: 12 }}
            size="middle"
            rowKey={(l) => l.product.productId}
            dataSource={lines}
            pagination={false}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Quét mã vạch để thêm mặt hàng" /> }}
            columns={[
              { title: '#', width: 44, render: (_, __, i) => i + 1 },
              {
                title: 'Mặt hàng',
                render: (_, l) => (
                  <>
                    <div>{l.product.name}</div>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>{l.product.sku} · {l.product.unit}</Typography.Text>
                    {l.product.promoRestricted && <Tag color="red" style={{ marginLeft: 6 }}>Hạn chế KM – không tích/đổi điểm</Tag>}
                    {l.product.pointExcluded && !l.product.promoRestricted && <Tag style={{ marginLeft: 6 }}>Không tích điểm</Tag>}
                  </>
                ),
              },
              {
                title: 'SL', width: 96,
                render: (_, l) => <InputNumber min={1} max={999} value={l.quantity} onChange={(v) => setQty(l.product.productId, v)} style={{ width: 76 }} />,
              },
              { title: 'Đơn giá', align: 'right', width: 110, render: (_, l) => <span className="tabular">{num(l.product.price)}</span> },
              { title: 'Thành tiền', align: 'right', width: 120, render: (_, l) => <b className="tabular">{num(lineAmount(l))}</b> },
              {
                width: 48,
                render: (_, l) => <Button type="text" danger icon={<DeleteOutlined />} aria-label="Xóa dòng" onClick={() => remove(l.product.productId)} />,
              },
            ]}
          />
        </Card>
      </Col>

      <Col xs={24} lg={8}>
        <Card title="Hóa đơn" extra={<Typography.Text type="secondary" style={{ fontSize: 12 }}>{receiptNo.current}</Typography.Text>}>
          <Summary label="Số mặt hàng" value={`${lines.length} (${num(lines.reduce((s, l) => s + l.quantity, 0))} sp)`} />
          {restrictedCount > 0 && <Summary label="Không tích điểm" value={`${restrictedCount} dòng`} />}
          <div style={{ borderTop: '1px solid #f0f0f0', margin: '12px 0' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
            <Typography.Text strong>Tổng cộng</Typography.Text>
            <Typography.Text strong className="tabular" style={{ fontSize: 28, color: '#b45309' }}>{vnd(total)}</Typography.Text>
          </div>
          <Card size="small" style={{ marginTop: 16, background: '#fafafa' }}>
            <Typography.Text type="secondary">Thành viên: tra cứu SĐT, đăng ký và đổi điểm sẽ có ở bước B7.</Typography.Text>
          </Card>
          <Button type="primary" size="large" block icon={<ShoppingCartOutlined />} style={{ marginTop: 16, height: 52, fontSize: 18 }}
            onClick={openPayment} disabled={!lines.length}>
            Thanh toán (F9)
          </Button>
          <Popconfirm title="Xóa toàn bộ hóa đơn đang bán?" okText="Xóa" cancelText="Không" onConfirm={newSale} disabled={!lines.length}>
            <Button block style={{ marginTop: 8 }} disabled={!lines.length}>Hủy hóa đơn đang bán</Button>
          </Popconfirm>
        </Card>
      </Col>

      <PaymentModal open={paying} total={total} loading={payLoading} error={payError}
        onCancel={() => setPaying(false)} onConfirm={checkout} />
      <ReceiptView receipt={receipt} onNew={newSale} />
    </Row>
  )
}

function Summary({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
      <Typography.Text type="secondary">{label}</Typography.Text>
      <Typography.Text className="tabular">{value}</Typography.Text>
    </div>
  )
}
