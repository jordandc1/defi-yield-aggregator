import { useState } from "react";
import { fetchPortfolio, fetchAlerts } from "./api";

export default function App() {
  const [addr, setAddr] = useState("");
  const [portfolio, setPortfolio] = useState<any>(null);
  const [alerts, setAlerts] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const [p, a] = await Promise.all([
        fetchPortfolio(addr),
        fetchAlerts(addr),
      ]);
      setPortfolio(p);
      setAlerts(a);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 900, margin: "40px auto", padding: 16 }}>
      <h1>DeFi Yield Aggregator (MVP)</h1>
      <div style={{ display: "flex", gap: 8 }}>
        <input
          placeholder="Enter EVM address"
          value={addr}
          onChange={(e) => setAddr(e.target.value)}
          style={{ flex: 1, padding: 8 }}
        />
        <button onClick={load} disabled={!addr || loading}>
          {loading ? "Loading..." : "Fetch"}
        </button>
      </div>

      {portfolio && (
        <>
          <h2 style={{ marginTop: 24 }}>Portfolio</h2>
          <p>Total USD: {portfolio.totalUsd}</p>
          <table width="100%" cellPadding={6}>
            <thead>
              <tr>
                <th>Protocol</th><th>Asset</th><th>Amount</th>
                <th>USD</th><th>APR</th><th>Risk</th>
              </tr>
            </thead>
            <tbody>
              {portfolio.positions?.map((p: any, i: number) => (
                <tr key={i}>
                  <td>{p.protocol}</td>
                  <td>{p.asset}</td>
                  <td>{p.amount}</td>
                  <td>{p.usdValue}</td>
                  <td>{(p.apr * 100).toFixed(2)}%</td>
                  <td>{p.riskStatus}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {alerts && (
        <>
          <h2 style={{ marginTop: 24 }}>Alerts</h2>
          <ul>
            {alerts.alerts?.map((a: any, i: number) => (
              <li key={i}>
                [{a.type}] {a.message} ({a.protocol}) – {a.createdIso}
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
