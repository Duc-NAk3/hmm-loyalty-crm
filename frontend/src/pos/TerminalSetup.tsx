import { Alert, Button, Card, Select, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, errorMessage } from '../shared/api'
import { Brand } from '../shared/Brand'
import { type Terminal, getTerminal, setTerminal } from '../shared/session'

/** Cài đặt máy POS lần đầu: chọn quầy đã đăng ký với cửa hàng (giả lập đăng ký thiết bị). */
export default function TerminalSetup() {
  const nav = useNavigate()
  const [terminals, setTerminals] = useState<Terminal[]>([])
  const [selected, setSelected] = useState<string | undefined>(getTerminal()?.terminal_code)
  const [error, setError] = useState<string>()

  useEffect(() => {
    api<Terminal[]>('/api/pos/terminals').then(setTerminals).catch((e) => setError(errorMessage(e)))
  }, [])

  const save = () => {
    const t = terminals.find((x) => x.terminal_code === selected)
    if (!t) return
    setTerminal(t)
    nav('/pos/login')
  }

  return (
    <div className="center-page">
      <Card style={{ width: '100%', maxWidth: 420 }}>
        <Brand subtitle="Cài đặt máy POS" />
        <Typography.Paragraph type="secondary" style={{ marginTop: 16 }}>
          Chọn quầy cho máy này. Trong hệ thống thật, quầy được đăng ký một lần bằng khóa thiết bị; bản demo lưu lựa chọn trên trình duyệt.
        </Typography.Paragraph>
        {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 12 }} />}
        <Select
          style={{ width: '100%' }}
          size="large"
          placeholder="Chọn quầy"
          value={selected}
          onChange={setSelected}
          options={terminals.map((t) => ({ value: t.terminal_code, label: `${t.terminal_code} · ${t.store_name}` }))}
        />
        <Button type="primary" size="large" block style={{ marginTop: 16 }} disabled={!selected} onClick={save}>
          Lưu quầy
        </Button>
      </Card>
    </div>
  )
}
