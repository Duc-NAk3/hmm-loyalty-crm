import {
  CloudUploadOutlined,
  DashboardOutlined,
  CrownOutlined,
  MessageOutlined,
  ShoppingOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'

export interface MenuEntry {
  key: string
  label: string
  icon: ReactNode
  roles: string[]
  /** Bước làm chức năng (để hiện trên trang chờ). */
  step: string
}

/** Menu Web Portal theo vai trò (FR-65). Backend vẫn kiểm tra quyền ở từng API. */
export const PORTAL_MENU: MenuEntry[] = [
  { key: 'dashboard', label: 'Dashboard', icon: <DashboardOutlined />, roles: ['ADMIN', 'STORE_MANAGER', 'MARKETING'], step: 'B8' },
  { key: 'customers', label: 'Khách hàng', icon: <TeamOutlined />, roles: ['ADMIN', 'STORE_MANAGER', 'CSKH', 'MARKETING'], step: 'B8' },
  { key: 'products', label: 'Mặt hàng và giá', icon: <ShoppingOutlined />, roles: ['ADMIN', 'STORE_MANAGER', 'CSKH', 'MARKETING', 'CATALOG'], step: 'B9' },
  { key: 'tiers', label: 'Hạng thành viên', icon: <CrownOutlined />, roles: ['ADMIN', 'STORE_MANAGER', 'CSKH', 'MARKETING'], step: 'B9' },
  { key: 'migration', label: 'Chuyển đổi dữ liệu', icon: <CloudUploadOutlined />, roles: ['ADMIN'], step: 'B9' },
  { key: 'users', label: 'Người dùng', icon: <UserOutlined />, roles: ['ADMIN'], step: 'B9' },
  { key: 'sms', label: 'Hộp thư SMS giả lập', icon: <MessageOutlined />, roles: ['ADMIN', 'STORE_MANAGER', 'CSKH', 'MARKETING', 'CATALOG'], step: 'B9' },
]

export const menuFor = (role: string) => PORTAL_MENU.filter((m) => m.roles.includes(role))
