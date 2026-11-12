import { type Area, getSession, setSession } from './session'

/** Lỗi trả về từ backend: { code, message } với thông báo tiếng Việt. */
export class ApiError extends Error {
  status: number
  code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

type Options = {
  method?: string
  body?: unknown
  /** Phiên dùng để gửi token; mặc định suy ra từ đường dẫn /api/pos hoặc /api/portal. */
  area?: Area
}

export async function api<T = unknown>(path: string, opts: Options = {}): Promise<T> {
  const area: Area = opts.area ?? (path.startsWith('/api/pos') ? 'pos' : 'portal')
  const session = getSession(area)
  const headers: Record<string, string> = {}
  if (session) headers.Authorization = `Bearer ${session.token}`
  let body: BodyInit | undefined
  if (opts.body instanceof FormData) body = opts.body
  else if (opts.body !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(opts.body)
  }

  let res: Response
  try {
    res = await fetch(path, { method: opts.method ?? (body ? 'POST' : 'GET'), headers, body })
  } catch {
    throw new ApiError(0, 'NETWORK', 'Không kết nối được máy chủ. Kiểm tra backend đã chạy chưa.')
  }

  if (res.status === 401 && session) {
    // Hết phiên: xóa phiên và quay về trang đăng nhập của khu vực
    setSession(area, null)
    window.location.assign(area === 'pos' ? '/pos/login' : '/portal/login')
  }
  const text = await res.text()
  const data = text ? safeJson(text) : null
  if (!res.ok) {
    const d = (data ?? {}) as { code?: string; message?: string }
    throw new ApiError(res.status, d.code ?? 'HTTP_' + res.status,
      d.message ?? (res.status === 403 ? 'Bạn không có quyền thực hiện thao tác này' : `Lỗi ${res.status}`))
  }
  return data as T
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

export function errorMessage(e: unknown): string {
  return e instanceof Error ? e.message : String(e)
}
