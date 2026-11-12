import dayjs from 'dayjs'

const vndFormat = new Intl.NumberFormat('vi-VN')

/** 181500 → "181.500đ" */
export const vnd = (n: number | null | undefined) => (n == null ? '—' : vndFormat.format(n) + 'đ')
export const num = (n: number | null | undefined) => (n == null ? '—' : vndFormat.format(n))
export const dateTime = (s: string | null | undefined) => (s ? dayjs(s).format('DD/MM/YYYY HH:mm') : '—')
export const date = (s: string | null | undefined) => (s ? dayjs(s).format('DD/MM/YYYY') : '—')

export const ROLE_NAMES: Record<string, string> = {
  ADMIN: 'Quản trị hệ thống',
  STORE_MANAGER: 'Quản lý cửa hàng',
  CASHIER: 'Thu ngân',
  CSKH: 'Chăm sóc khách hàng',
  MARKETING: 'Marketing',
  CATALOG: 'Danh mục và giá',
}

export const TIER_COLORS: Record<string, string> = {
  MEMBER: 'default',
  SILVER: '#8c8c8c',
  GOLD: '#d48806',
  DIAMOND: '#1d39c4',
}

export const CUSTOMER_STATUS: Record<string, { label: string; color: string }> = {
  UNVERIFIED: { label: 'Chưa xác thực', color: 'orange' },
  VERIFIED: { label: 'Đã xác thực', color: 'green' },
  LOCKED: { label: 'Đã khóa', color: 'red' },
  MERGED: { label: 'Đã gộp', color: 'default' },
  ANONYMIZED: { label: 'Đã ẩn danh', color: 'default' },
}
