/**
 * Màu và kiểu chung cho biểu đồ (theo skill dataviz – bảng màu tham chiếu, đã chạy validate_palette trên nền #ffffff).
 * - Thứ tự màu cố định theo thực thể: Thành viên luôn là slot 1 (xanh), Vãng lai luôn là slot 2 (cam).
 * - Biểu đồ một chuỗi dùng một màu (slot 1). Lưới và trục là đường mảnh, nhạt.
 */
export const SERIES = {
  member: '#2a78d6',
  guest: '#eb6834',
} as const

export const CHART = {
  surface: '#ffffff',
  grid: '#e1e0d9',
  axis: '#c3c2b7',
  muted: '#898781',
  textSecondary: '#52514e',
  /** Thanh tối đa 24px, góc bo 4px ở đầu dữ liệu. */
  barSize: 24,
} as const

/** Kiểu chữ trục dùng chung. */
export const axisTick = { fill: CHART.muted, fontSize: 12 }
