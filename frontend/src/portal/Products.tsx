import { CheckOutlined, CloseOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons'
import {
  Alert, App, Badge, Button, Card, Checkbox, DatePicker, Divider, Drawer, Form, Input, InputNumber, Modal, Popconfirm, Radio, Select,
  Space, Table, Tabs, Tag, Typography, Upload,
} from 'antd'
import type { Dayjs } from 'dayjs'
import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../shared/api'
import { dateTime, num } from '../shared/format'
import { getSession } from '../shared/session'

interface Category {
  categoryId: number
  code: string
  name: string
  promoRestricted: boolean
}
interface ProductView {
  productId: number
  sku: string
  name: string
  unit: string
  categoryId: number
  categoryName: string
  vatRate: number
  promoRestricted: boolean
  categoryPromoRestricted: boolean
  pointExcluded: boolean
  status: string
  barcodes: string
  currentPrice: number | null
  draftPrices: number
}
interface PriceView {
  priceId: number
  storeId: number | null
  storeCode: string | null
  price: number
  effectiveFrom: string
  effectiveTo: string | null
  status: string
  approvedByName: string | null
}
interface PendingPrice {
  priceId: number
  productId: number
  sku: string
  name: string
  storeId: number | null
  storeCode: string | null
  price: number
  effectiveFrom: string
}
interface Store {
  storeId: number
  storeCode: string
  name: string
}

/**
 * Mặt hàng và giá (FR-11, FR-12).
 * - Mọi vai trò Portal xem được; CATALOG / ADMIN tạo, sửa, nhập CSV, tạo bản giá mới (DRAFT).
 * - Chỉ ADMIN duyệt bản giá. Bản giá đã duyệt không sửa – muốn đổi giá thì tạo bản mới.
 */
export default function Products() {
  const role = getSession('portal')!.user.role
  const canEdit = role === 'CATALOG' || role === 'ADMIN'
  const canApprove = role === 'ADMIN'
  const [pendingCount, setPendingCount] = useState(0)
  const [reload, setReload] = useState(0)
  const refresh = () => setReload((n) => n + 1)

  useEffect(() => {
    api<PendingPrice[]>('/api/portal/prices/pending').then((p) => setPendingCount(p.length)).catch(() => {})
  }, [reload])

  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>Mặt hàng và giá</Typography.Title>
      <Card>
        <Tabs items={[
          { key: 'list', label: 'Danh sách mặt hàng', children: <ProductList canEdit={canEdit} reload={reload} onChanged={refresh} /> },
          {
            key: 'pending',
            label: <Badge count={pendingCount} size="small" offset={[10, -2]}>Giá chờ duyệt</Badge>,
            children: <PendingPrices canApprove={canApprove} reload={reload} onChanged={refresh} />,
          },
        ]} />
      </Card>
    </>
  )
}

function ProductList({ canEdit, reload, onChanged }: { canEdit: boolean; reload: number; onChanged: () => void }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [q, setQ] = useState('')
  const [categoryId, setCategoryId] = useState<number>()
  const [page, setPage] = useState(1)
  const [data, setData] = useState<{ total: number; items: ProductView[] }>({ total: 0, items: [] })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const [editing, setEditing] = useState<ProductView | 'new' | null>(null)
  const [importOpen, setImportOpen] = useState(false)

  useEffect(() => {
    api<Category[]>('/api/portal/categories').then(setCategories).catch(() => {})
  }, [])

  useEffect(() => {
    let alive = true
    setLoading(true)
    const qs = new URLSearchParams({ page: String(page - 1), size: '20' })
    if (q) qs.set('q', q)
    if (categoryId) qs.set('categoryId', String(categoryId))
    api<{ total: number; items: ProductView[] }>('/api/portal/products?' + qs)
      .then((d) => {
        if (!alive) return
        setData(d)
        setError(undefined)
        // Cập nhật lại mặt hàng đang mở trong drawer (giá hiện hành, số bản nháp)
        setEditing((e) => (e && e !== 'new' ? d.items.find((x) => x.productId === e.productId) ?? e : e))
      })
      .catch((e) => alive && setError(errorMessage(e)))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [q, categoryId, page, reload])

  return (
    <>
      <Space wrap style={{ marginBottom: 16 }}>
        <Input.Search style={{ width: 300 }} placeholder="Mã hàng, tên hoặc mã vạch" allowClear
          onSearch={(v) => { setQ(v.trim()); setPage(1) }} />
        <Select style={{ width: 240 }} placeholder="Ngành hàng" allowClear value={categoryId}
          options={categories.map((c) => ({ value: c.categoryId, label: c.name }))}
          onChange={(v) => { setCategoryId(v); setPage(1) }} />
        <Typography.Text type="secondary">{num(data.total)} mặt hàng</Typography.Text>
        {canEdit && (
          <>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setEditing('new')}>Thêm mặt hàng</Button>
            <Button icon={<UploadOutlined />} onClick={() => setImportOpen(true)}>Nhập CSV</Button>
          </>
        )}
      </Space>
      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 12 }} />}
      <Table<ProductView>
        size="middle"
        rowKey="productId"
        loading={loading}
        dataSource={data.items}
        onRow={(r) => ({ onClick: () => setEditing(r), style: { cursor: 'pointer' } })}
        pagination={{ current: page, pageSize: 20, total: data.total, showSizeChanger: false, onChange: setPage }}
        scroll={{ x: 900 }}
        columns={[
          { title: 'Mã hàng', dataIndex: 'sku', width: 100 },
          {
            title: 'Tên hàng', dataIndex: 'name',
            render: (n, p) => (
              <>
                {n}
                {(p.promoRestricted || p.categoryPromoRestricted) && <Tag color="red" style={{ marginLeft: 6 }}>Hạn chế KM</Tag>}
                {p.pointExcluded && <Tag style={{ marginLeft: 6 }}>Không tích điểm</Tag>}
                {p.status !== 'ACTIVE' && <Tag style={{ marginLeft: 6 }}>Ngừng bán</Tag>}
              </>
            ),
          },
          { title: 'Ngành hàng', dataIndex: 'categoryName', width: 180 },
          { title: 'ĐVT', dataIndex: 'unit', width: 70 },
          { title: 'Mã vạch', dataIndex: 'barcodes', width: 150, ellipsis: true },
          {
            title: 'Giá chuỗi', dataIndex: 'currentPrice', align: 'right', width: 110,
            render: (v: number | null) => (v == null ? <Typography.Text type="secondary">chưa có</Typography.Text> : <span className="tabular">{num(v)}</span>),
          },
          { title: 'Chờ duyệt', dataIndex: 'draftPrices', align: 'center', width: 90, render: (v: number) => (v ? <Tag color="gold">{v}</Tag> : '') },
        ]}
      />
      <ProductDrawer product={editing} categories={categories} canEdit={canEdit} onClose={() => setEditing(null)}
        onSaved={(created) => { onChanged(); if (created) setEditing(null) }} />
      <ImportModal open={importOpen} onClose={() => setImportOpen(false)} onDone={onChanged} />
    </>
  )
}

type ProductForm = {
  sku: string
  name: string
  unit: string
  categoryId: number
  vatRate: number
  barcodes: string
  promoRestricted: boolean
  pointExcluded: boolean
  active: boolean
}

/** Drawer mặt hàng: thông tin (tạo / sửa) và lịch sử giá + tạo bản giá mới. */
function ProductDrawer({ product, categories, canEdit, onClose, onSaved }: {
  product: ProductView | 'new' | null
  categories: Category[]
  canEdit: boolean
  onClose: () => void
  onSaved: (created: boolean) => void
}) {
  const { message } = App.useApp()
  const [form] = Form.useForm<ProductForm>()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string>()
  const isNew = product === 'new'
  const p = product && product !== 'new' ? product : null

  useEffect(() => {
    if (!product) return
    setError(undefined)
    form.setFieldsValue(p
      ? {
          sku: p.sku, name: p.name, unit: p.unit, categoryId: p.categoryId, vatRate: p.vatRate,
          barcodes: (p.barcodes ?? '').split(',').map((s) => s.trim()).filter(Boolean).join('\n'),
          promoRestricted: p.promoRestricted, pointExcluded: p.pointExcluded, active: p.status === 'ACTIVE',
        }
      : { sku: '', name: '', unit: '', categoryId: undefined, vatRate: 8, barcodes: '', promoRestricted: false, pointExcluded: false, active: true })
    // chỉ nạp lại form khi đổi mặt hàng
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [product === 'new' ? 'new' : p?.productId])

  const save = async (v: ProductForm) => {
    setSaving(true)
    setError(undefined)
    try {
      const body = {
        sku: v.sku, name: v.name, unit: v.unit, categoryId: v.categoryId, vatRate: v.vatRate,
        promoRestricted: v.promoRestricted, pointExcluded: v.pointExcluded, status: v.active ? 'ACTIVE' : 'INACTIVE',
        barcodes: v.barcodes.split(/[\s,;|]+/).map((s) => s.trim()).filter(Boolean),
      }
      if (isNew) await api('/api/portal/products', { body })
      else await api(`/api/portal/products/${p!.productId}`, { method: 'PUT', body })
      message.success(isNew ? 'Đã tạo mặt hàng. Mở lại để tạo giá bán.' : 'Đã lưu mặt hàng')
      onSaved(isNew)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Drawer open={!!product} onClose={onClose} size={560} destroyOnHidden
      title={isNew ? 'Thêm mặt hàng' : `${p?.sku} · ${p?.name}`}
      extra={canEdit && <Button type="primary" loading={saving} onClick={() => form.submit()}>Lưu</Button>}>
      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 12 }} />}
      <Form form={form} layout="vertical" onFinish={save} disabled={!canEdit}>
        <div style={{ display: 'flex', gap: 12 }}>
          <Form.Item name="sku" label="Mã hàng" rules={[{ required: true, whitespace: true }]} style={{ width: 140 }}><Input /></Form.Item>
          <Form.Item name="name" label="Tên hàng" rules={[{ required: true, whitespace: true }]} style={{ flex: 1 }}><Input /></Form.Item>
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <Form.Item name="categoryId" label="Ngành hàng" rules={[{ required: true }]} style={{ flex: 1 }}>
            <Select options={categories.map((c) => ({ value: c.categoryId, label: c.name + (c.promoRestricted ? ' (hạn chế KM)' : '') }))} />
          </Form.Item>
          <Form.Item name="unit" label="ĐVT" rules={[{ required: true, whitespace: true }]} style={{ width: 100 }}><Input /></Form.Item>
          <Form.Item name="vatRate" label="VAT %" rules={[{ required: true }]} style={{ width: 90 }}>
            <Select options={[0, 5, 8, 10].map((x) => ({ value: x, label: x + '%' }))} />
          </Form.Item>
        </div>
        <Form.Item name="barcodes" label="Mã vạch (mỗi dòng một mã)" extra="Một mặt hàng có thể có nhiều mã vạch (nhiều nhà cung cấp, quy cách).">
          <Input.TextArea autoSize={{ minRows: 2, maxRows: 5 }} />
        </Form.Item>
        <Space size="large" wrap>
          <Form.Item name="promoRestricted" valuePropName="checked" noStyle><Checkbox>Hạn chế khuyến mại (không tích/đổi điểm)</Checkbox></Form.Item>
          <Form.Item name="pointExcluded" valuePropName="checked" noStyle><Checkbox>Không tích điểm</Checkbox></Form.Item>
          <Form.Item name="active" valuePropName="checked" noStyle><Checkbox>Đang bán</Checkbox></Form.Item>
        </Space>
        {p?.categoryPromoRestricted && (
          <Alert type="warning" showIcon style={{ marginTop: 12 }} title="Ngành hàng này bị hạn chế khuyến mại nên mọi mặt hàng trong ngành đều không tích/đổi điểm." />
        )}
      </Form>
      {p && (
        <>
          <Divider />
          <PriceHistory product={p} canEdit={canEdit} onChanged={() => onSaved(false)} />
        </>
      )}
    </Drawer>
  )
}

/** Lịch sử giá + tạo bản giá mới (DRAFT), toàn chuỗi hoặc riêng một cửa hàng. */
function PriceHistory({ product, canEdit, onChanged }: { product: ProductView; canEdit: boolean; onChanged: () => void }) {
  const { message } = App.useApp()
  const [rows, setRows] = useState<PriceView[]>([])
  const [stores, setStores] = useState<Store[]>([])
  const [scope, setScope] = useState<'chain' | 'store'>('chain')
  const [storeId, setStoreId] = useState<number>()
  const [price, setPrice] = useState<number | null>(null)
  const [from, setFrom] = useState<Dayjs | null>(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(() => {
    api<PriceView[]>(`/api/portal/products/${product.productId}/prices`).then(setRows).catch(() => setRows([]))
  }, [product.productId])
  useEffect(load, [load])
  useEffect(() => {
    api<Store[]>('/api/portal/stores').then(setStores).catch(() => {})
  }, [])

  const create = async () => {
    if (!price || price <= 0) return message.warning('Nhập giá bán')
    if (scope === 'store' && !storeId) return message.warning('Chọn cửa hàng')
    setSaving(true)
    try {
      await api(`/api/portal/products/${product.productId}/prices`, {
        body: { storeId: scope === 'store' ? storeId : null, price, effectiveFrom: from ? from.format() : null },
      })
      message.success('Đã tạo bản giá nháp, chờ ADMIN duyệt')
      setPrice(null)
      setFrom(null)
      load()
      onChanged()
    } catch (e) {
      message.error(errorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <Typography.Title level={5} style={{ marginTop: 0 }}>Giá bán</Typography.Title>
      {canEdit && (
        <Card size="small" style={{ marginBottom: 12, background: '#fafafa' }}>
          <Space wrap>
            <Radio.Group value={scope} onChange={(e) => setScope(e.target.value)} optionType="button" size="small"
              options={[{ value: 'chain', label: 'Toàn chuỗi' }, { value: 'store', label: 'Một cửa hàng' }]} />
            {scope === 'store' && (
              <Select size="small" style={{ width: 120 }} placeholder="Cửa hàng" value={storeId} onChange={setStoreId}
                options={stores.map((s) => ({ value: s.storeId, label: s.storeCode }))} />
            )}
          </Space>
          <Space wrap style={{ marginTop: 8 }}>
            <InputNumber<number> placeholder="Giá bán (đ)" min={0} step={500} value={price} onChange={setPrice} style={{ width: 150 }}
              formatter={(v) => `${v ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')} parser={(v) => Number((v ?? '').replace(/\./g, ''))} />
            <DatePicker showTime={{ format: 'HH:mm' }} format="DD/MM/YYYY HH:mm" placeholder="Hiệu lực từ (trống = ngay)" value={from} onChange={setFrom} />
            <Button type="primary" loading={saving} onClick={create}>Tạo bản giá</Button>
          </Space>
          <Typography.Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 6 }}>
            Bản giá mới ở trạng thái Nháp, chỉ có hiệu lực ở POS sau khi ADMIN duyệt. Giá riêng cửa hàng được ưu tiên hơn giá toàn chuỗi.
          </Typography.Text>
        </Card>
      )}
      <Table<PriceView> size="small" rowKey="priceId" dataSource={rows} pagination={false}
        columns={[
          { title: 'Phạm vi', dataIndex: 'storeCode', render: (s) => s ?? 'Toàn chuỗi' },
          { title: 'Giá', dataIndex: 'price', align: 'right', render: (v: number) => <span className="tabular">{num(v)}</span> },
          { title: 'Hiệu lực từ', dataIndex: 'effectiveFrom', render: dateTime },
          { title: 'Đến', dataIndex: 'effectiveTo', render: (v) => (v ? dateTime(v) : '—') },
          {
            title: 'Trạng thái', dataIndex: 'status',
            render: (v: string, r) => (v === 'APPROVED'
              ? <span><Tag color="green">Đã duyệt</Tag>{r.approvedByName && <Typography.Text type="secondary" style={{ fontSize: 12 }}>{r.approvedByName}</Typography.Text>}</span>
              : <Tag color="gold">Nháp</Tag>),
          },
        ]}
      />
    </>
  )
}

function PendingPrices({ canApprove, reload, onChanged }: { canApprove: boolean; reload: number; onChanged: () => void }) {
  const { message } = App.useApp()
  const [rows, setRows] = useState<PendingPrice[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    api<PendingPrice[]>('/api/portal/prices/pending').then(setRows).catch(() => setRows([])).finally(() => setLoading(false))
  }, [reload])

  const act = async (r: PendingPrice, action: 'approve' | 'reject') => {
    try {
      await api(`/api/portal/prices/${r.priceId}/${action}`, { method: 'POST' })
      message.success(action === 'approve' ? `Đã duyệt giá ${num(r.price)}đ cho ${r.sku}` : 'Đã hủy bản giá nháp')
      onChanged()
    } catch (e) {
      message.error(errorMessage(e))
    }
  }

  return (
    <>
      {!canApprove && <Alert type="info" showIcon style={{ marginBottom: 12 }} title="Chỉ ADMIN được duyệt giá. Bạn chỉ xem được danh sách chờ duyệt." />}
      <Table<PendingPrice> size="middle" rowKey="priceId" loading={loading} dataSource={rows} pagination={false}
        locale={{ emptyText: 'Không có bản giá nào chờ duyệt' }}
        columns={[
          { title: 'Mã hàng', dataIndex: 'sku', width: 100 },
          { title: 'Tên hàng', dataIndex: 'name' },
          { title: 'Phạm vi', dataIndex: 'storeCode', width: 110, render: (s) => s ?? 'Toàn chuỗi' },
          { title: 'Giá mới', dataIndex: 'price', align: 'right', width: 110, render: (v: number) => <b className="tabular">{num(v)}</b> },
          { title: 'Hiệu lực từ', dataIndex: 'effectiveFrom', width: 150, render: dateTime },
          ...(canApprove ? [{
            title: '', width: 190,
            render: (_: unknown, r: PendingPrice) => (
              <Space>
                <Popconfirm title={`Duyệt giá ${num(r.price)}đ cho ${r.sku}?`} okText="Duyệt" cancelText="Không" onConfirm={() => act(r, 'approve')}>
                  <Button size="small" type="primary" icon={<CheckOutlined />}>Duyệt</Button>
                </Popconfirm>
                <Popconfirm title="Hủy bản giá nháp này?" okText="Hủy bản giá" cancelText="Không" onConfirm={() => act(r, 'reject')}>
                  <Button size="small" danger icon={<CloseOutlined />}>Hủy</Button>
                </Popconfirm>
              </Space>
            ),
          }] : []),
        ]}
      />
    </>
  )
}

/** Nhập mặt hàng từ CSV (sample-data/products_import.csv). */
function ImportModal({ open, onClose, onDone }: { open: boolean; onClose: () => void; onDone: () => void }) {
  const [result, setResult] = useState<{ created: number; updated: number; errors: string[] } | null>(null)
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)

  const upload = async (file: File) => {
    setLoading(true)
    setError(undefined)
    setResult(null)
    const fd = new FormData()
    fd.append('file', file)
    try {
      setResult(await api('/api/portal/products/import', { body: fd }))
      onDone()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
    return false
  }

  return (
    <Modal title="Nhập mặt hàng từ CSV" open={open} onCancel={() => { setResult(null); setError(undefined); onClose() }}
      footer={null} width={560} destroyOnHidden>
      <Typography.Paragraph type="secondary">
        Cột: <code>sku, name, unit, category_code, vat_rate, barcodes, promo_restricted, point_excluded, price</code>.
        Nhiều mã vạch cách nhau bằng <code>|</code>. Cột <code>price</code> tạo giá toàn chuỗi đã duyệt, hiệu lực ngay.
        Mặt hàng đã có (trùng mã hàng) được cập nhật, giữ mã vạch cũ.
      </Typography.Paragraph>
      <Upload.Dragger accept=".csv" maxCount={1} showUploadList={false} beforeUpload={upload} disabled={loading}>
        <p className="ant-upload-drag-icon"><UploadOutlined /></p>
        <p>{loading ? 'Đang nhập…' : 'Bấm hoặc kéo file CSV vào đây'}</p>
      </Upload.Dragger>
      {error && <Alert type="error" showIcon title={error} style={{ marginTop: 12 }} />}
      {result && (
        <Alert style={{ marginTop: 12 }} type={result.errors.length ? 'warning' : 'success'} showIcon
          title={`Tạo mới ${result.created} · Cập nhật ${result.updated} · Lỗi ${result.errors.length}`}
          description={result.errors.length ? <ul style={{ margin: 0, paddingLeft: 18 }}>{result.errors.map((e) => <li key={e}>{e}</li>)}</ul> : undefined} />
      )}
    </Modal>
  )
}
