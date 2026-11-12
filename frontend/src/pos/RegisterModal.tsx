import { Alert, Checkbox, DatePicker, Form, Input, Modal, Select } from 'antd'
import type { Dayjs } from 'dayjs'
import { useEffect, useState } from 'react'
import { api, errorMessage } from '../shared/api'
import type { MemberSummary } from './types'

type Values = { phone: string; fullName: string; gender?: string; dateOfBirth?: Dayjs; marketingConsent: boolean }

/**
 * Đăng ký thành viên nhanh tại quầy (FR-19): SĐT và họ tên bắt buộc, không cần OTP.
 * Hồ sơ ở trạng thái Chưa xác thực; khách nhận SMS chào mừng (giả lập). Hóa đơn đang bán được tích điểm.
 */
export default function RegisterModal({ phone, onCancel, onRegistered }: {
  phone: string | null
  onCancel: () => void
  onRegistered: (m: MemberSummary) => void
}) {
  const [form] = Form.useForm<Values>()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()

  useEffect(() => {
    if (phone != null) {
      setError(undefined)
      form.setFieldsValue({ phone, fullName: '', gender: undefined, dateOfBirth: undefined, marketingConsent: false })
    }
  }, [phone, form])

  const submit = async (v: Values) => {
    setLoading(true)
    setError(undefined)
    try {
      const m = await api<MemberSummary>('/api/pos/members', {
        body: {
          phone: v.phone,
          fullName: v.fullName,
          gender: v.gender ?? null,
          dateOfBirth: v.dateOfBirth ? v.dateOfBirth.format('YYYY-MM-DD') : null,
          marketingConsent: v.marketingConsent,
        },
      })
      onRegistered(m)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      title="Đăng ký thành viên nhanh"
      open={phone != null}
      onCancel={onCancel}
      onOk={() => form.submit()}
      okText="Đăng ký và gắn vào hóa đơn"
      cancelText="Không đăng ký"
      confirmLoading={loading}
      width={460}
      forceRender
    >
      <Alert type="info" showIcon style={{ marginBottom: 12 }}
        title="SĐT này chưa là thành viên. Hỏi khách có muốn đăng ký để tích điểm cho hóa đơn này không." />
      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 12 }} />}
      <Form form={form} layout="vertical" onFinish={submit} requiredMark>
        <Form.Item name="phone" label="Số điện thoại" rules={[{ required: true, message: 'Nhập SĐT' }]}>
          <Input inputMode="tel" />
        </Form.Item>
        <Form.Item name="fullName" label="Họ tên" rules={[{ required: true, whitespace: true, message: 'Nhập họ tên khách' }]}>
          <Input autoFocus placeholder="Ví dụ: Nguyễn Văn An" />
        </Form.Item>
        <div style={{ display: 'flex', gap: 12 }}>
          <Form.Item name="gender" label="Giới tính" style={{ flex: 1 }}>
            <Select allowClear placeholder="Không bắt buộc" options={[
              { value: 'FEMALE', label: 'Nữ' },
              { value: 'MALE', label: 'Nam' },
              { value: 'OTHER', label: 'Khác' },
            ]} />
          </Form.Item>
          <Form.Item name="dateOfBirth" label="Ngày sinh" style={{ flex: 1 }}>
            <DatePicker format="DD/MM/YYYY" placeholder="Không bắt buộc" style={{ width: '100%' }} />
          </Form.Item>
        </div>
        <Form.Item name="marketingConsent" valuePropName="checked" style={{ marginBottom: 0 }}>
          <Checkbox>Khách đồng ý nhận tin khuyến mại qua SMS</Checkbox>
        </Form.Item>
      </Form>
    </Modal>
  )
}
