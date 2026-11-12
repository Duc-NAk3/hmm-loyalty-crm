import { BarcodeOutlined } from '@ant-design/icons'
import { AutoComplete, Tag, Typography } from 'antd'
import type { GetRef } from 'antd'
import { forwardRef, useImperativeHandle, useRef, useState } from 'react'
import { api, errorMessage } from '../shared/api'
import { vnd } from '../shared/format'
import type { Product } from './types'

export interface ProductSearchHandle {
  focus: () => void
}

/**
 * Ô quét mã vạch / tìm hàng (FR-02). Máy quét mã vạch gõ mã rồi Enter: thêm ngay nếu khớp mã vạch.
 * Gõ tên hoặc mã hàng: hiện danh sách gợi ý để chọn.
 */
const ProductSearch = forwardRef<ProductSearchHandle, { onAdd: (p: Product) => void; onError: (msg: string) => void }>(
  function ProductSearch({ onAdd, onError }, ref) {
    const inputRef = useRef<GetRef<typeof AutoComplete>>(null)
    const [value, setValue] = useState('')
    const [options, setOptions] = useState<Product[]>([])
    const timer = useRef<number | undefined>(undefined)
    const seq = useRef(0)
    /** Người dùng đã dùng phím mũi tên để chọn gợi ý: Enter là chọn gợi ý, không phải quét mã. */
    const navigated = useRef(false)

    useImperativeHandle(ref, () => ({ focus: () => inputRef.current?.focus() }))

    const search = (q: string) => {
      window.clearTimeout(timer.current)
      if (q.trim().length < 2) return setOptions([])
      timer.current = window.setTimeout(async () => {
        const my = ++seq.current
        try {
          const res = await api<Product[]>('/api/pos/products?q=' + encodeURIComponent(q.trim()))
          if (my === seq.current) setOptions(res)
        } catch (e) {
          onError(errorMessage(e))
        }
      }, 200)
    }

    const add = (p: Product) => {
      onAdd(p)
      setValue('')
      setOptions([])
      seq.current++
      window.clearTimeout(timer.current)
      navigated.current = false
      inputRef.current?.focus()
    }

    /** Enter: thử khớp mã vạch trước, không có thì lấy kết quả tìm kiếm duy nhất. */
    const submit = async () => {
      const q = value.trim()
      if (!q) return
      window.clearTimeout(timer.current)
      seq.current++
      setOptions([])
      try {
        if (/^\d{8,14}$/.test(q)) {
          const byBarcode = await api<Product[]>('/api/pos/products?barcode=' + encodeURIComponent(q))
          if (byBarcode.length) return add(byBarcode[0])
        }
        const found = await api<Product[]>('/api/pos/products?q=' + encodeURIComponent(q))
        if (found.length === 1) return add(found[0])
        if (!found.length) {
          onError(`Không tìm thấy mặt hàng "${q}"`)
          setValue('')
        } else setOptions(found)
      } catch (e) {
        onError(errorMessage(e))
      }
    }

    return (
      <AutoComplete
        style={{ width: '100%' }}
        value={value}
        defaultActiveFirstOption={false}
        onChange={(v) => {
          setValue(v)
          navigated.current = false
          search(v)
        }}
        onSelect={(id) => {
          const p = options.find((o) => String(o.productId) === id)
          if (p) add(p)
        }}
        options={options.map((p) => ({
          value: String(p.productId),
          label: (
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
              <span>
                {p.name} <Typography.Text type="secondary" style={{ fontSize: 12 }}>{p.sku}</Typography.Text>
                {p.promoRestricted && <Tag color="red" style={{ marginLeft: 6 }}>Hạn chế KM</Tag>}
              </span>
              <span className="tabular">{p.price < 0 ? 'Chưa có giá' : vnd(p.price)}</span>
            </div>
          ),
        }))}
        ref={inputRef}
        size="large"
        prefix={<BarcodeOutlined />}
        placeholder="Quét mã vạch hoặc gõ tên / mã hàng rồi Enter  (F2)"
        autoFocus
        onKeyDown={(e) => {
          if (e.key === 'ArrowDown' || e.key === 'ArrowUp') navigated.current = true
          // Chọn gợi ý bằng phím mũi tên thì AutoComplete xử lý (onSelect); còn lại coi như quét mã / tìm kiếm
          else if (e.key === 'Enter' && !navigated.current) {
            e.preventDefault()
            submit()
          }
        }}
      />
    )
  },
)

export default ProductSearch
