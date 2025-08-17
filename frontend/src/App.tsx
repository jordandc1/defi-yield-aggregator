import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useAccount, useConnect, useDisconnect } from 'wagmi'
import { injected } from 'wagmi/connectors'
import { addRecentAddress, getRecentAddresses } from './storage'
import { fetchAlerts, fetchPortfolio, type PortfolioDTO } from './api'

export default function App() {
  // wallet state
  const { address: connectedAddr, isConnected } = useAccount()
  const { connect, connectors, isPending: isConnecting } = useConnect()
  const { disconnect } = useDisconnect()

  // ui state
  const [addr, setAddr] = useState<string>('')
  const [recent, setRecent] = useState<string[]>([])
  const [portfolio, setPortfolio] = useState<PortfolioDTO | null>(null)
  const [alerts, setAlerts] = useState<Awaited<ReturnType<typeof fetchAlerts>> | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setRecent(getRecentAddresses())
  }, [])

  // autofill when wallet connects
  useEffect(() => {
    if (isConnected && connectedAddr) {
      setAddr(connectedAddr)
    }
  }, [isConnected, connectedAddr])

  const metamask = useMemo(
    () => connectors.find(c => c.id === 'injected') ?? injected(),
    [connectors]
  )

  async function load() {
    if (!addr) return
    setLoading(true); setError(null)
    try {
      const [p, a] = await Promise.all([fetchPortfolio(addr), fetchAlerts(addr)])
      setPortfolio(p as PortfolioDTO); setAlerts(a)
      addRecentAddress(addr)
      setRecent(getRecentAddresses())
    } catch (e: unknown) {
      if(e instanceof Error) {
        setError(e?.message ?? 'Failed to load data')
      }
      // Clear stale data on error to avoid showing old results
      setPortfolio(null); setAlerts(null)
    } finally {
      setLoading(false)
    }
  }

  function pickRecent(a: string) {
    setAddr(a)
    setPortfolio(null); setAlerts(null)
  }

  return (
    <div style={{ maxWidth: 980, margin: '32px auto', padding: 16 }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h1>DeFi Yield Aggregator (MVP)</h1>
        <div>
          {!isConnected ? (
            <button
              onClick={() => connect({ connector: metamask })}
              disabled={isConnecting}
              title="Connect MetaMask / Injected Wallet"
            >
              {isConnecting ? 'Connecting…' : 'Connect Wallet'}
            </button>
          ) : (
            <button onClick={() => disconnect()}>Disconnect {short(connectedAddr)}</button>
          )}
        </div>
      </header>

      <section style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 8, alignItems: 'center' }}>
        <input
          placeholder="Enter EVM address (0x...) or connect wallet"
          value={addr}
          onChange={(e) => {
            setAddr(e.target.value.trim())
            // Reset existing data when user edits the address
            setPortfolio(null); setAlerts(null)
          }}
          style={{ padding: 10, fontSize: 14 }}
        />
        <button onClick={load} disabled={!addr || loading}>
          {loading ? 'Loading…' : 'Fetch'}
        </button>
      </section>

      {recent.length > 0 && (
        <section style={{ marginTop: 12 }}>
          <small>Recent:</small>{' '}
          {recent.map((a, i) => (
            <button key={i} onClick={() => pickRecent(a)} style={{ marginRight: 6 }}>
              {short(a)}
            </button>
          ))}
        </section>
      )}

      {error && (
        <div style={{ marginTop: 16, color: 'crimson' }}>
          <strong>Error:</strong> {error}
        </div>
      )}

      <section style={{ marginTop: 24 }}>
        <h2>Portfolio</h2>
        {!portfolio ? (
          <EmptyTable />
        ) : (
          <>
            <p>Address: {portfolio.address} · Total USD: <b>{fmtUsd(portfolio.totalUsd)}</b> · Updated: {portfolio.lastUpdatedIso}</p>
            <table width="100%" cellPadding={6} style={{ borderCollapse: 'collapse' }}>
              <thead>
                <tr>
                  <Th>Protocol</Th><Th>Network</Th><Th>Asset</Th><Th right>Amount</Th><Th right>USD</Th><Th right>APR</Th><Th>Risk</Th>
                </tr>
              </thead>
              <tbody>
                {portfolio.positions?.map((p, i) => (
                  <tr key={i} style={{ borderTop: '1px solid #eee' }}>
                    <Td>{p.protocol}</Td>
                    <Td>{p.network}</Td>
                    <Td>{p.asset}</Td>
                    <Td right>{num(p.amount)}</Td>
                    <Td right>{fmtUsd(p.usdValue)}</Td>
                    <Td right>{(p.apr * 100).toFixed(2)}%</Td>
                    <Td><RiskTag level={p.riskStatus} /></Td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Alerts</h2>
        {!alerts ? (
          <p style={{ color: '#777' }}>No data yet.</p>
        ) : alerts.alerts?.length ? (
          <ul>
            {alerts.alerts.map((a, i) => (
              <li key={i}>[{a.type}] {a.message} ({a.protocol}) – {a.createdIso}</li>
            ))}
          </ul>
        ) : (
          <p style={{ color: '#777' }}>No alerts.</p>
        )}
      </section>
    </div>
  )
}

function short(a?: string) { return a ? `${a.slice(0,6)}…${a.slice(-4)}` : '' }
function num(n: number) { return Number(n).toLocaleString(undefined, { maximumFractionDigits: 6 }) }
function fmtUsd(n: number) { return n.toLocaleString(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }) }

function Th({ children, right }: { children: ReactNode; right?: boolean }) {
  return <th style={{ textAlign: right ? 'right' as const : 'left', fontWeight: 600, fontSize: 13, color: '#444' }}>{children}</th>
}
function Td({ children, right }: { children: ReactNode; right?: boolean }) {
  return <td style={{ textAlign: right ? 'right' as const : 'left', fontSize: 13 }}>{children}</td>
}
function RiskTag({ level }: { level: string }) {
  const color = level === 'CRITICAL' ? '#dc2626' : level === 'WARN' ? '#d97706' : '#16a34a'
  return <span style={{ background: color, color: 'white', padding: '2px 6px', borderRadius: 6, fontSize: 12 }}>{level}</span>
}
function EmptyTable() {
  return (
    <table width="100%" cellPadding={6} style={{ borderCollapse: 'collapse', color: '#777' }}>
      <thead>
        <tr><th>Protocol</th><th>Network</th><th>Asset</th><th>Amount</th><th>USD</th><th>APR</th><th>Risk</th></tr>
      </thead>
      <tbody><tr><td colSpan={7} style={{ textAlign: 'center', padding: 20 }}>No data yet. Enter an address or connect wallet.</td></tr></tbody>
    </table>
  )
}
