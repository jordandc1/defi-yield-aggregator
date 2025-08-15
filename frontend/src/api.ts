import axios from "axios";

const BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

export async function fetchPortfolio(address: string) {
  const { data } = await axios.get(`${BASE}/portfolio/${address}`);
  return data;
}

export async function fetchAlerts(address: string) {
  const { data } = await axios.get(`${BASE}/alerts/${address}`);
  return data;
}
