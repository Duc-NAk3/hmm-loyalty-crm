import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input } from 'antd'
import { useState } from 'react'

export interface LoginValues {
  username: string
  password: string
}

/** Form đăng nhập dùng chung cho POS và Portal. */
export function LoginForm({ onSubmit, submitText = 'Đăng nhập' }: {
  onSubmit: (v: LoginValues) => Promise<void>
  submitText?: string
}) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()

  const finish = async (v: LoginValues) => {
    setLoading(true)
    setError(undefined)
    try {
      await onSubmit(v)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Form layout="vertical" onFinish={finish} requiredMark={false} size="large">
      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
      <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
        <Input prefix={<UserOutlined />} autoFocus autoComplete="username" />
      </Form.Item>
      <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
        <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
      </Form.Item>
      <Button type="primary" htmlType="submit" block loading={loading}>
        {submitText}
      </Button>
    </Form>
  )
}
