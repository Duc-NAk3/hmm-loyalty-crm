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
  terminal_id: number
  terminal_code: string
  store_id: number
  store_code: string
  store_name: string
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
  return read<Terminal>(TERMINAL_KEY)
}

export function setTerminal(t: Terminal | null) {
  write(TERMINAL_KEY, t)
}
