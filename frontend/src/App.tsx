import { useEffect, useMemo, useState, useCallback, type ReactNode } from 'react'
import { useAccount, useConnect, useDisconnect } from 'wagmi'
import { injected } from 'wagmi/connectors'
import { addRecentAddress, getRecentAddresses } from './storage'
import { fetchPrices, fetchAlerts, fetchPortfolio, type PortfolioDTO } from './api'

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
  const [prices, setPrices] = useState<Record<string, number> | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<string | null>(null)

  useEffect(() => {
    setRecent(getRecentAddresses())
  }, [])

  // autofill when wallet connects
  useEffect(() => {
    if (isConnected && connectedAddr) {
      setAddr(connectedAddr)
    }
  }, [isConnected, connectedAddr])

  useEffect(() => {
    fetchPrices(['ETH','DAI','USDC']).then(setPrices).catch(console.error)
  }, [])

  const metamask = useMemo(
    () => connectors.find(c => c.id === 'injected') ?? injected(),
    [connectors]
  )

  const load = useCallback(async () => {
    if (!addr) return
    setLoading(true); setError(null)
    try {
      const [p, a, pr] = await Promise.all([
        fetchPortfolio(addr),
        fetchAlerts(addr),
        fetchPrices(['ETH','DAI','USDC'])
      ])
      setPortfolio(p); setAlerts(a); setPrices(pr)
      setLastUpdated(new Date().toISOString())
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
  }, [addr])

  useEffect(() => {
    if (!portfolio) return
    const id = setInterval(() => { load() }, 5 * 60 * 1000)
    return () => clearInterval(id)
  }, [portfolio, load])

  function pickRecent(a: string) {
    setAddr(a)
    setPortfolio(null); setAlerts(null)
  }

  return (
    <div className="max-w-[980px] mx-auto my-8 p-4">
      <header className="mb-4 flex items-center justify-between">
        <h1>DeFi Yield Aggregator (MVP)</h1>
        <div>
          {!isConnected ? (
            <button
              onClick={() => connect({ connector: metamask })}
              disabled={isConnecting}
              title="Connect MetaMask / Injected Wallet"
              className="rounded bg-blue-500 px-4 py-2 text-white hover:bg-blue-600 disabled:opacity-50"
            >
              {isConnecting ? 'Connecting…' : 'Connect Wallet'}
            </button>
          ) : (
            <button
              onClick={() => disconnect()}
              className="rounded bg-gray-500 px-4 py-2 text-white hover:bg-gray-600"
            >
              Disconnect {short(connectedAddr)}
            </button>
          )}
        </div>
      </header>

      <section className="grid grid-cols-[1fr_auto] items-center gap-2">
        <input
          placeholder="Enter EVM address (0x...) or connect wallet"
          value={addr}
          onChange={(e) => {
            setAddr(e.target.value.trim())
            // Reset existing data when user edits the address
            setPortfolio(null); setAlerts(null)
          }}
          className="rounded border p-2 text-sm"
        />
        <button
          onClick={load}
          disabled={!addr || loading}
          className="rounded bg-blue-500 px-4 py-2 text-white hover:bg-blue-600 disabled:opacity-50"
        >
          {loading ? 'Loading…' : 'Fetch'}
        </button>
      </section>

      {recent.length > 0 && (
        <section className="mt-3">
          <small>Recent:</small>{' '}
          {recent.map((a, i) => (
            <button
              key={i}
              onClick={() => pickRecent(a)}
              className="mr-1.5 rounded bg-gray-200 px-2 py-1 text-sm hover:bg-gray-300"
            >
              {short(a)}
            </button>
          ))}
        </section>
      )}

      {error && (
        <div className="mt-4 text-red-700">
          <strong>Error:</strong> {error}
        </div>
      )}

      <section className="mt-6">
        <div className="flex items-center justify-between">
          <h2>Portfolio</h2>
          {portfolio && (
            <div className="text-xs">
              <button
                onClick={load}
                disabled={loading}
                className="mr-2 rounded bg-blue-500 px-3 py-1 text-white hover:bg-blue-600 disabled:opacity-50"
              >
                Refresh
              </button>
              {lastUpdated && <span>Last updated: {new Date(lastUpdated).toLocaleTimeString()}</span>}
            </div>
          )}
        </div>
        {!portfolio ? (
          <EmptyTable />
        ) : (
          <>
            <div className="mb-3">
              <span className="mr-3">Total USD: <b>{fmtUsd(portfolio.totalUsd)}</b></span>
              <span>Daily Yield USD: <b>{fmtUsd(portfolio.dailyYieldUsd)}</b></span>
            </div>
            <p>
              Address: {portfolio.address} ·
              Health Factor: {portfolio.healthFactor != null ? portfolio.healthFactor.toFixed(2) : 'N/A'} · Updated: {portfolio.lastUpdatedIso}
            </p>
            <table width="100%" cellPadding={6} className="w-full border-collapse">
              <thead>
                <tr>
                  <Th>Protocol</Th><Th>Network</Th><Th>Asset</Th><Th>Type</Th><Th right>Amount</Th><Th right>USD</Th><Th right>APR</Th><Th>Risk</Th>
                </tr>
              </thead>
              <tbody>
                {portfolio.positions?.map((p, i) => {
                  const amount = p.positionType === 'BORROW' ? -p.amount : p.amount
                  const usd = p.positionType === 'BORROW' ? -p.usdValue : p.usdValue
                  return (
                    <tr key={i} className="border-t">
                      <Td>{p.protocol}</Td>
                      <Td>{p.network}</Td>
                      <Td>{p.asset}</Td>
                      <Td>{p.positionType}</Td>
                      <Td right>{num(amount)}</Td>
                      <Td right>{fmtUsd(usd)}</Td>
                      <Td right>{(p.apr * 100).toFixed(2)}%</Td>
                      <Td><RiskTag level={p.riskStatus} /></Td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </>
        )}
      </section>

      {prices && (
        <section className="mt-6">
          <h2>Live Prices</h2>
          <div className="text-sm">
            <span className="mr-3">ETH ${prices.ETH?.toFixed(2)}</span>
            <span className="mr-3">DAI ${prices.DAI?.toFixed(4)}</span>
            <span>USDC ${prices.USDC?.toFixed(4)}</span>
          </div>
        </section>
      )}

      <section className="mt-6">
        <h2>Alerts</h2>
        {!alerts ? (
          <p className="text-gray-500">No data yet.</p>
        ) : alerts.alerts?.length ? (
          <ul>
            {alerts.alerts.map((a, i) => (
              <li key={i}>[{a.type}] {a.message} ({a.protocol}) – {a.createdIso}</li>
            ))}
          </ul>
        ) : (
          <p className="text-gray-500">No alerts.</p>
        )}
      </section>
    </div>
  )
}

function short(a?: string) { return a ? `${a.slice(0,6)}…${a.slice(-4)}` : '' }
function num(n: number) { return Number(n).toLocaleString(undefined, { maximumFractionDigits: 6 }) }
function fmtUsd(n: number) { return n.toLocaleString(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }) }

function Th({ children, right }: { children: ReactNode; right?: boolean }) {
  return <th className={`${right ? 'text-right' : 'text-left'} text-sm font-semibold text-gray-700`}>{children}</th>
}
function Td({ children, right }: { children: ReactNode; right?: boolean }) {
  return <td className={`${right ? 'text-right' : 'text-left'} text-sm`}>{children}</td>
}
function RiskTag({ level }: { level: string }) {
  const lvl = level?.toUpperCase()
  let color = 'bg-green-600'
  if (lvl === 'CRITICAL' || lvl === 'DANGER' || lvl === 'HIGH') color = 'bg-red-600'
  else if (lvl === 'WARN' || lvl === 'WARNING' || lvl === 'MEDIUM') color = 'bg-amber-600'
  return <span className={`${color} rounded px-2 py-0.5 text-xs text-white`}>{level}</span>
}
function EmptyTable() {
  return (
    <table width="100%" cellPadding={6} className="w-full border-collapse text-gray-500">
      <thead>
        <tr><th>Protocol</th><th>Network</th><th>Asset</th><th>Type</th><th>Amount</th><th>USD</th><th>APR</th><th>Risk</th></tr>
      </thead>
      <tbody><tr><td colSpan={8} className="p-5 text-center">No data yet. Enter an address or connect wallet.</td></tr></tbody>
    </table>
  )
}
