import { DesktopOutlined, ShopOutlined } from '@ant-design/icons'
import { Card, Col, Row, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { Brand } from './shared/Brand'

/** Trang chọn khu vực: POS tại quầy hoặc Web Portal. */
export default function Home() {
  return (
    <div className="center-page">
      <div style={{ width: '100%', maxWidth: 760 }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 8 }}>
          <Brand subtitle="Loyalty CRM – bản demo SofiTech" />
        </div>
        <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 28 }}>
          Chọn khu vực làm việc
        </Typography.Paragraph>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12}>
            <Link to="/pos">
              <Card hoverable>
                <ShopOutlined style={{ fontSize: 32, color: '#b45309' }} />
                <Typography.Title level={4} style={{ marginTop: 12 }}>POS tại quầy</Typography.Title>
                <Typography.Text type="secondary">
                  Thu ngân: mở ca, bán hàng, gắn thành viên, tích và đổi điểm.
                </Typography.Text>
              </Card>
            </Link>
          </Col>
          <Col xs={24} sm={12}>
            <Link to="/portal">
              <Card hoverable>
                <DesktopOutlined style={{ fontSize: 32, color: '#b45309' }} />
                <Typography.Title level={4} style={{ marginTop: 12 }}>Web Portal</Typography.Title>
                <Typography.Text type="secondary">
                  Trung tâm và quản lý cửa hàng: dashboard, khách hàng, hàng hóa, chuyển đổi dữ liệu.
                </Typography.Text>
              </Card>
            </Link>
          </Col>
        </Row>
      </div>
    </div>
  )
}
