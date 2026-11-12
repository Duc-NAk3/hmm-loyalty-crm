import { Card, Empty, Typography } from 'antd'

/** Trang chờ cho chức năng sẽ làm ở các bước sau. */
export default function Placeholder({ title, step }: { title: string; step: string }) {
  return (
    <>
      <Typography.Title level={3} style={{ marginTop: 0 }}>{title}</Typography.Title>
      <Card>
        <Empty description={`Chức năng này sẽ có ở bước ${step}`} />
      </Card>
    </>
  )
}
