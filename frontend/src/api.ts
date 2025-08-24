import axios from "axios";

const BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

export type PositionDTO = {
  protocol: string
  network: string
  asset: string
  amount: number
  usdValue: number
  apr: number
  borrowAmount: number
  borrowApr: number
  riskStatus: 'OK' | 'WARN' | 'CRITICAL' | string
  positionType: 'DEPOSIT' | 'BORROW' | string
}
export type PortfolioDTO = {
  address: string
  totalUsd: number
  dailyYieldUsd: number
  netWorthUsd?: number
  healthFactor: number | null
  positions: PositionDTO[]
  lastUpdatedIso: string
}
export type AlertsResponse = {
  address: string
  alerts: { type:string; message:string; protocol:string; createdIso:string }[]
}

export async function fetchPortfolio(address: string) {
  try {
    const { data } = await axios.get<PortfolioDTO>(`${BASE}/portfolio/${address}`)
    return data
  } catch {
    throw new Error('Failed to fetch portfolio')
  }
}

export async function fetchAlerts(address: string) {
  try {
    const { data } = await axios.get<AlertsResponse>(`${BASE}/alerts/${address}`)
    return data
  } catch {
    throw new Error('Failed to fetch alerts')
  }
}

export async function fetchPrices(symbols: string[]) {
  const qs = encodeURIComponent(symbols.join(','))
  try {
    const { data } = await axios.get<Record<string, number>>(`${BASE}/prices?symbols=${qs}`)
    return data
  } catch {
    throw new Error('Failed to fetch prices')
  }
}

export async function subscribeEmail(address: string, email: string) {
  try {
    await axios.post(`${BASE}/alerts/subscribe`, { address, email })
  } catch {
    // ignore errors in MVP
  }
}

