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
  shiftId: number
  terminalId: number
  cashierId: number
  cashierName: string
  openedAt: string
  openingCash: number
  receiptCount: number
  cashSales: number
}

export interface ReceiptLine {
  sku: string
  name: string
  unit: string
  quantity: number
  unitPrice: number
  lineAmount: number
  pointEligible: boolean
}

/** Dữ liệu hóa đơn sau khi hoàn tất (POST /api/pos/sales). */
export interface Receipt {
  transactionId: number
  receiptNo: string
  txnTime: string
  netAmount: number
  eligibleAmount: number | null
  pointsEarned: number | null
  pointsRedeemed: number | null
  storeCode: string
  storeName: string
  storeAddress: string
  terminalCode: string
  cashierName: string
  memberCode: string | null
  memberName: string | null
  memberPhone?: string
  pointBalance: number | null
  tierName: string | null
  lines: ReceiptLine[]
  payments: { method: string; amount: number }[]
  cashTendered: number | null
  change: number | null
  upgradedTo?: string | null
}

export const lineAmount = (l: CartLine) => l.product.price * l.quantity
export const cartTotal = (lines: CartLine[]) => lines.reduce((s, l) => s + lineAmount(l), 0)
