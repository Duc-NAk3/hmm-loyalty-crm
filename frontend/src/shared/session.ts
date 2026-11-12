// Phiên đăng nhập lưu ở trình duyệt. POS và Portal có phiên riêng (một máy quầy không dùng chung phiên với Portal).

export type Area = 'pos' | 'portal'

export interface User {
  id: number
  username: string
  fullName: string
  role: string
  storeId: number | null
  storeCode: string | null
  storeName: string | null
}

export interface Terminal {
  terminalId: number
  terminalCode: string
  storeId: number
  storeCode: string
  storeName: string
}

export interface Session {
  token: string
  user: User
}

const key = (area: Area) => `hmm.${area}.session`
const TERMINAL_KEY = 'hmm.pos.terminal'

function read<T>(k: string): T | null {
  try {
    const raw = localStorage.getItem(k)
    return raw ? (JSON.parse(raw) as T) : null
  } catch {
    return null
  }
}

function write(k: string, v: unknown) {
  try {
    if (v == null) localStorage.removeItem(k)
    else localStorage.setItem(k, JSON.stringify(v))
  } catch {
    /* trình duyệt chặn lưu trữ: phiên chỉ còn trong bộ nhớ */
  }
}

const memory: Partial<Record<Area, Session | null>> = {}

export function getSession(area: Area): Session | null {
  return memory[area] ?? read<Session>(key(area))
}

export function setSession(area: Area, s: Session | null) {
  memory[area] = s
  write(key(area), s)
}

/** Quầy đã "đăng ký" cho máy này (giả lập đăng ký thiết bị POS). */
export function getTerminal(): Terminal | null {
  const t = read<Terminal>(TERMINAL_KEY)
  // Bỏ qua dữ liệu lưu theo định dạng cũ (snake_case) → máy POS chọn lại quầy
  return t && typeof t.terminalId === 'number' ? t : null
}

export function setTerminal(t: Terminal | null) {
  write(TERMINAL_KEY, t)
}
