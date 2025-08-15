import axios from "axios";

const BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

export type PositionDTO = {
  protocol: string
  network: string
  asset: string
  amount: number
  usdValue: number
  apr: number
  riskStatus: 'OK' | 'WARN' | 'CRITICAL' | string
}
export type PortfolioDTO = {
  address: string
  totalUsd: number
  positions: PositionDTO[]
  lastUpdatedIso: string
}
export type AlertsResponse = {
  address: string
  alerts: { type:string; message:string; protocol:string; createdIso:string }[]
}

export async function fetchPortfolio(address: string) {
  const { data } = await axios.get<PortfolioDTO>(`${BASE}/portfolio/${address}`);
  return data;
}

export async function fetchAlerts(address: string) {
  const { data } = await axios.get<AlertsResponse>(`${BASE}/alerts/${address}`);
  return data;
}
