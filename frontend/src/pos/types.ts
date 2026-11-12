/** Mặt hàng kèm giá đang hiệu lực tại cửa hàng (GET /api/pos/products). */
export interface Product {
  productId: number
  sku: string
  name: string
  unit: string
  categoryName: string
  vatRate: number
  promoRestricted: boolean
  pointExcluded: boolean
  price: number
  barcode: string | null
}

export interface CartLine {
  product: Product
  quantity: number
}

export interface Shift {
  shift_id: number
  terminal_id: number
  cashier_id: number
  cashier_name: string
  opened_at: string
  opening_cash: number
  receipt_count: number
  cash_sales: number
}

export interface ReceiptLine {
  sku: string
  name: string
  unit: string
  quantity: number
  unit_price: number
  line_amount: number
  point_eligible: boolean
}

/** Dữ liệu hóa đơn sau khi hoàn tất (POST /api/pos/sales). */
export interface Receipt {
  transaction_id: number
  receipt_no: string
  txn_time: string
  net_amount: number
  eligible_amount: number | null
  points_earned: number | null
  points_redeemed: number | null
  store_code: string
  store_name: string
  store_address: string
  terminal_code: string
  cashier_name: string
  member_code: string | null
  member_name: string | null
  memberPhone?: string
  point_balance: number | null
  tier_name: string | null
  lines: ReceiptLine[]
  payments: { method: string; amount: number }[]
  cashTendered: number | null
  change: number | null
  upgradedTo?: string | null
}

export const lineAmount = (l: CartLine) => l.product.price * l.quantity
export const cartTotal = (lines: CartLine[]) => lines.reduce((s, l) => s + lineAmount(l), 0)
