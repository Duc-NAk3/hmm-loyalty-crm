import { UploadOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Col, Descriptions, Form, Modal, Row, Select, Statistic, Table, Tag, Typography, Upload } from 'antd'
import type { UploadFile } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../shared/api'
import { dateTime, num } from '../shared/format'

interface Vendor {
  code: string
  label: string
  delimiter: string
  dobPattern: string
  columns: string[]
}
interface ErrorView {
  rowNo: number
  message: string
  rawData: string
}
interface ImportResult {
  batchId: number
  totalRows: number
  created: number
  merged: number
  errorRows: number
  errors: ErrorView[]
}
interface Batch {
  batchId: number
  storeCode: string
  sourceVendor: string
  dataType: string
  status: string
  totalRows: number | null
  errorRows: number | null
  importedAt: string
  importedBy: string | null
  importedRows: number
}
interface Store {
  storeId: number
  storeCode: string
  name: string
}

/**
 * Chuyển đổi dữ liệu khách hàng từ 3 POS cũ (FR-58, FR-59, FR-60) – chỉ ADMIN.
 * Chọn cửa hàng + nhà cung cấp POS cũ (mỗi bên một định dạng cột) → tải CSV → báo cáo tạo mới / gộp / lỗi.
 * Khách tạo mới ở trạng thái Chưa xác thực, chưa có consent marketing; trùng SĐT thì gộp vào hồ sơ có sẵn.
 */
export default function Migration() {
  const [vendors, setVendors] = useState<Vendor[]>([])
  const [stores, setStores] = useState<Store[]>([])
  const [batches, setBatches] = useState<Batch[]>([])
  const [storeId, setStoreId] = useState<number>()
  const [vendor, setVendor] = useState<string>()
  const [file, setFile] = useState<UploadFile | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const [result, setResult] = useState<ImportResult | null>(null)
  const [errorsOf, setErrorsOf] = useState<{ batch: Batch; rows: ErrorView[] } | null>(null)

  const loadBatches = useCallback(() => {
    api<Batch[]>('/api/portal/migration/batches').then(setBatches).catch(() => {})
  }, [])
  useEffect(() => {
    api<Vendor[]>('/api/portal/migration/vendors').then(setVendors).catch((e) => setError(errorMessage(e)))
    api<Store[]>('/api/portal/stores').then(setStores).catch(() => {})
    loadBatches()
  }, [loadBatches])

  const v = vendors.find((x) => x.code === vendor)

  const submit = async () => {
    if (!storeId || !vendor || !file?.originFileObj) return
    setLoading(true)
    setError(undefined)
    setResult(null)
    const fd = new FormData()
    fd.append('file', file.originFileObj)
    try {
      setResult(await api<ImportResult>(`/api/portal/migration/customers?storeId=${storeId}&vendor=${vendor}`, { body: fd }))
      setFile(null)
      loadBatches()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  const showErrors = async (b: Batch) => {
    try {
      setErrorsOf({ batch: b, rows: await api<ErrorView[]>(`/api/portal/migration/batches/${b.batchId}/errors`) })
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>Chuyển đổi dữ liệu khách hàng</Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} xl={10}>
          <Card title="Nhập file từ POS cũ" style={{ height: '100%' }}>
            <Form layout="vertical" onFinish={submit}>
              <Form.Item label="Cửa hàng" required>
                <Select placeholder="Chọn cửa hàng" value={storeId} onChange={setStoreId}
                  options={stores.map((s) => ({ value: s.storeId, label: `${s.storeCode} · ${s.name}` }))} />
              </Form.Item>
              <Form.Item label="POS cũ (nhà cung cấp)" required>
                <Select placeholder="Chọn định dạng" value={vendor} onChange={setVendor}
                  options={vendors.map((x) => ({ value: x.code, label: x.label }))} />
              </Form.Item>
              {v && (
                <Descriptions size="small" column={1} bordered style={{ marginBottom: 16 }} items={[
                  { label: 'Dấu phân cách', children: <code>{v.delimiter}</code> },
                  { label: 'Ngày sinh', children: <code>{v.dobPattern}</code> },
                  { label: 'Cột', children: <span style={{ fontSize: 12 }}>{v.columns.join(', ')}</span> },
                ]} />
              )}
              <Form.Item label="File CSV" required extra="File mẫu ở thư mục sample-data/ (customers_POS_ALPHA_HM001.csv …)">
                <Upload accept=".csv" maxCount={1} fileList={file ? [file] : []} beforeUpload={() => false}
                  onChange={({ fileList }) => setFile(fileList[0] ?? null)} onRemove={() => setFile(null)}>
                  <Button icon={<UploadOutlined />}>Chọn file</Button>
                </Upload>
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={loading} disabled={!storeId || !vendor || !file}>Nhập dữ liệu</Button>
            </Form>
            {error && <Alert type="error" showIcon title={error} style={{ marginTop: 12 }} />}
          </Card>
        </Col>
        <Col xs={24} xl={14}>
          <Card title={result ? `Kết quả lần nhập #${result.batchId}` : 'Kết quả'} style={{ height: '100%' }}>
            {!result ? (
              <Typography.Text type="secondary">
                Chọn cửa hàng, định dạng và file rồi bấm "Nhập dữ liệu". Hệ thống chuẩn hóa SĐT (+84/84 → 0), họ tên, ngày sinh;
                trùng SĐT thì gộp vào hồ sơ có sẵn; dòng lỗi được ghi lại kèm dữ liệu gốc.
              </Typography.Text>
            ) : (
              <>
                <Row gutter={16}>
                  <Col span={6}><Statistic title="Tổng dòng" value={result.totalRows} /></Col>
                  <Col span={6}><Statistic title="Tạo mới" value={result.created} /></Col>
                  <Col span={6}><Statistic title="Gộp hồ sơ" value={result.merged} /></Col>
                  <Col span={6}><Statistic title="Dòng lỗi" value={result.errorRows} /></Col>
                </Row>
                {result.errors.length > 0 && <ErrorTable rows={result.errors} />}
              </>
            )}
          </Card>
        </Col>
      </Row>

      <Card title="Các lần nhập" style={{ marginTop: 16 }}>
        <Table<Batch> size="small" rowKey="batchId" dataSource={batches} pagination={{ pageSize: 10, size: 'small' }} scroll={{ x: 760 }}
          locale={{ emptyText: 'Chưa có lần nhập nào' }}
          columns={[
            { title: '#', dataIndex: 'batchId', width: 60 },
            { title: 'Thời điểm', dataIndex: 'importedAt', width: 140, render: dateTime },
            { title: 'Cửa hàng', dataIndex: 'storeCode', width: 90 },
            { title: 'POS cũ', dataIndex: 'sourceVendor', width: 110 },
            { title: 'Người nhập', dataIndex: 'importedBy', render: (x) => x ?? '—' },
            { title: 'Tổng dòng', dataIndex: 'totalRows', align: 'right', render: (x) => num(x) },
            { title: 'Đã nhập', dataIndex: 'importedRows', align: 'right', render: (x) => num(x) },
            {
              title: 'Lỗi', dataIndex: 'errorRows', align: 'right',
              render: (x: number | null, b) => (x ? <Button size="small" type="link" onClick={() => showErrors(b)}>{x} dòng</Button> : '0'),
            },
            { title: 'Trạng thái', dataIndex: 'status', render: (s: string) => (s === 'IMPORTED' ? <Tag color="green">Đã nhập</Tag> : <Tag color="gold">{s === 'UPLOADED' ? 'Đang xử lý' : s}</Tag>) },
          ]}
        />
      </Card>

      <Modal open={!!errorsOf} onCancel={() => setErrorsOf(null)} footer={null} width={760}
        title={errorsOf && `Dòng lỗi – lần nhập #${errorsOf.batch.batchId} (${errorsOf.batch.storeCode}, ${errorsOf.batch.sourceVendor})`}>
        {errorsOf && <ErrorTable rows={errorsOf.rows} />}
      </Modal>
    </>
  )
}

function ErrorTable({ rows }: { rows: ErrorView[] }) {
  return (
    <Table<ErrorView> size="small" style={{ marginTop: 16 }} rowKey={(r) => r.rowNo + r.message} dataSource={rows} pagination={false}
      columns={[
        { title: 'Dòng', dataIndex: 'rowNo', width: 60 },
        { title: 'Lỗi', dataIndex: 'message', width: 240 },
        { title: 'Dữ liệu gốc', dataIndex: 'rawData', render: (x: string) => <code style={{ fontSize: 12, wordBreak: 'break-all' }}>{x}</code> },
      ]}
    />
  )
}
