const KEY = 'dya_recent_addresses'
const MAX = 5

export function getRecentAddresses(): string[] {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return []
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

export function addRecentAddress(addr: string) {
  const norm = addr.trim()
  if (!norm) return
  const list = getRecentAddresses().filter(a => a.toLowerCase() !== norm.toLowerCase())
  list.unshift(norm)
  localStorage.setItem(KEY, JSON.stringify(list.slice(0, MAX)))
}
