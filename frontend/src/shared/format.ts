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

/** Số tiền rút gọn cho trục và thẻ KPI: 764149500 → "764,1 tr", 1,2 tỷ. */
export const compactVnd = (n: number | null | undefined) => {
  if (n == null) return '—'
  const a = Math.abs(n)
  if (a >= 1e9) return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 }).format(n / 1e9) + ' tỷ'
  if (a >= 1e6) return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(n / 1e6) + ' tr'
  if (a >= 1e3) return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(n / 1e3) + ' k'
  return vndFormat.format(n)
}
export const pct = (n: number | null | undefined) => (n == null ? '—' : new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(n) + '%')
