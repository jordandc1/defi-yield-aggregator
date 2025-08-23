import { useEffect, useMemo, useState, useCallback, type ReactNode } from 'react'
import { useAccount, useConnect, useDisconnect } from 'wagmi'
import { injected } from 'wagmi/connectors'
import { addRecentAddress, getRecentAddresses } from './storage'
import { fetchPrices, fetchAlerts, fetchPortfolio } from './api'
import { Spinner } from './components/Spinner'
import { Alert } from './components/Alert'
import { useQuery } from '@tanstack/react-query'

export default function App() {
  // wallet state
  const { address: connectedAddr, isConnected } = useAccount()
  const { connect, connectors, isPending: isConnecting } = useConnect()
  const { disconnect } = useDisconnect()

  // ui state
  const [addr, setAddr] = useState<string>('')
  const [recent, setRecent] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<string | null>(null)

  const {
    data: portfolio,
    isFetching: fetchingPortfolio,
    refetch: refetchPortfolio,
    remove: removePortfolio,
  } = useQuery({
    queryKey: ['portfolio', addr],
    queryFn: () => fetchPortfolio(addr),
    enabled: false,
    retry: false,
  })

  const {
    data: alerts,
    isFetching: fetchingAlerts,
    refetch: refetchAlerts,
    remove: removeAlerts,
  } = useQuery({
    queryKey: ['alerts', addr],
    queryFn: () => fetchAlerts(addr),
    enabled: false,
    retry: false,
  })

  const {
    data: prices,
    isFetching: fetchingPrices,
    refetch: refetchPrices,
  } = useQuery({
    queryKey: ['prices'],
    queryFn: () => fetchPrices(['ETH', 'DAI', 'USDC']),
    retry: false,
  })

  const loading = fetchingPortfolio || fetchingAlerts || fetchingPrices

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

  const load = useCallback(async () => {
    if (!addr) return
    setError(null)
    try {
      const [p, a, pr] = await Promise.all([
        refetchPortfolio(),
        refetchAlerts(),
        refetchPrices(),
      ])
      if (p.error) throw p.error
      if (a.error) throw a.error
      if (pr.error) throw pr.error
      setLastUpdated(new Date().toISOString())
      addRecentAddress(addr)
      setRecent(getRecentAddresses())
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load data')
      removePortfolio()
      removeAlerts()
    }
  }, [addr, refetchPortfolio, refetchAlerts, refetchPrices, removePortfolio, removeAlerts])

  useEffect(() => {
    if (!portfolio) return
    const id = setInterval(() => {
      load()
    }, 5 * 60 * 1000)
    return () => clearInterval(id)
  }, [portfolio, load])

  function pickRecent(a: string) {
    setAddr(a)
    removePortfolio()
    removeAlerts()
  }

  return (
    <div className="max-w-screen-lg mx-auto my-8 p-4 text-sm sm:text-base">
      <header className="mb-4 flex items-center justify-between">
        <h1>DeFi Yield Aggregator (MVP)</h1>
        <div>
          {!isConnected ? (
            <button
              onClick={() => connect({ connector: metamask })}
              disabled={isConnecting}
              aria-busy={isConnecting}
              title="Connect MetaMask / Injected Wallet"
              className="flex items-center rounded bg-blue-500 px-4 py-2 text-white hover:bg-blue-600 disabled:opacity-50"
            >
              {isConnecting && <Spinner className="mr-2" />}
              Connect Wallet
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

      <section className="flex flex-col items-center gap-2 sm:flex-row sm:gap-4">
        <input
          placeholder="Enter EVM address (0x...) or connect wallet"
          value={addr}
          onChange={(e) => {
            setAddr(e.target.value.trim())
            // Reset existing data when user edits the address
            removePortfolio(); removeAlerts()
          }}
          className="w-full rounded border p-2 sm:flex-1"
        />
        <button
          onClick={load}
          disabled={!addr || loading}
          aria-busy={loading}
          className="flex w-full items-center justify-center rounded bg-blue-500 px-4 py-2 text-white hover:bg-blue-600 disabled:opacity-50 sm:w-auto"
        >
          {loading && <Spinner className="mr-2" />}
          Fetch
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
        <Alert message={error} onClose={() => setError(null)} onRetry={load} busy={loading} />
      )}

      <section className="mt-6" aria-busy={fetchingPortfolio}>
        <div className="flex items-center justify-between">
          <h2>Portfolio</h2>
          {portfolio && (
            <div className="text-xs">
              <button
                onClick={load}
                disabled={loading}
                aria-busy={loading}
                className="mr-2 flex items-center rounded bg-blue-500 px-3 py-1 text-white hover:bg-blue-600 disabled:opacity-50"
              >
                {loading && <Spinner className="mr-1" />}
                Refresh
              </button>
              {lastUpdated && <span>Last updated: {new Date(lastUpdated).toLocaleTimeString()}</span>}
            </div>
          )}
        </div>
        {!portfolio ? (
          fetchingPortfolio ? (
            <div className="flex justify-center p-4" role="status">
              <Spinner />
            </div>
          ) : (
            <EmptyTable />
          )
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
            <div className="overflow-x-auto">
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
                    const risk = riskClasses(p.riskStatus)
                    return (
                      <tr key={i} className={`border-t ${risk.row}`}>
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
            </div>
          </>
        )}
      </section>

      <section className="mt-6" aria-busy={fetchingPrices}>
        <h2>Live Prices</h2>
        {!prices ? (
          <div className="flex justify-center p-4" role="status">
            <Spinner />
          </div>
        ) : (
          <div className="text-sm">
            <span className="mr-3">ETH ${prices.ETH?.toFixed(2)}</span>
            <span className="mr-3">DAI ${prices.DAI?.toFixed(4)}</span>
            <span>USDC ${prices.USDC?.toFixed(4)}</span>
          </div>
        )}
      </section>

      <section className="mt-6" aria-busy={fetchingAlerts}>
        <h2>Alerts</h2>
        {!alerts ? (
          fetchingAlerts ? (
            <div className="flex justify-center p-4" role="status">
              <Spinner />
            </div>
          ) : (
            <p className="text-gray-500">No data yet.</p>
          )
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

function riskClasses(level?: string) {
  const lvl = level?.toUpperCase()
  if (lvl === 'CRITICAL' || lvl === 'DANGER' || lvl === 'HIGH') {
    return {
      tag: 'bg-red-600 text-white',
      row: 'bg-red-50 text-red-800 dark:bg-red-950 dark:text-red-200'
    }
  }
  if (lvl === 'WARN' || lvl === 'WARNING' || lvl === 'MEDIUM') {
    return {
      tag: 'bg-amber-600 text-white',
      row: 'bg-yellow-50 text-yellow-800 dark:bg-yellow-950 dark:text-yellow-200'
    }
  }
  return {
    tag: 'bg-green-600 text-white',
    row: ''
  }
}

function Th({ children, right }: { children: ReactNode; right?: boolean }) {
  return <th className={`${right ? 'text-right' : 'text-left'} text-sm font-semibold text-gray-700`}>{children}</th>
}
function Td({ children, right }: { children: ReactNode; right?: boolean }) {
  return <td className={`${right ? 'text-right' : 'text-left'} text-sm`}>{children}</td>
}
function RiskTag({ level }: { level: string }) {
  const { tag } = riskClasses(level)
  return <span className={`${tag} rounded px-2 py-0.5 text-xs`}>{level}</span>
}
function EmptyTable() {
  return (
    <div className="overflow-x-auto">
      <table width="100%" cellPadding={6} className="w-full border-collapse text-gray-500">
        <thead>
          <tr><th>Protocol</th><th>Network</th><th>Asset</th><th>Type</th><th>Amount</th><th>USD</th><th>APR</th><th>Risk</th></tr>
        </thead>
        <tbody><tr><td colSpan={8} className="p-5 text-center">No data yet. Enter an address or connect wallet.</td></tr></tbody>
      </table>
    </div>
  )
}
