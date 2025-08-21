import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { describe, it, expect, vi } from 'vitest'
import App from './App'
import type { PortfolioDTO } from './api'

vi.mock('wagmi', () => ({
  useAccount: () => ({ address: '0xabc', isConnected: true }),
  useConnect: () => ({ connect: vi.fn(), connectors: [], isPending: false }),
  useDisconnect: () => ({ disconnect: vi.fn() }),
}))
vi.mock('wagmi/connectors', () => ({ injected: vi.fn() }))
vi.mock('./storage', () => ({ addRecentAddress: vi.fn(), getRecentAddresses: () => [] }))

const mockPortfolio: PortfolioDTO = {
  address: '0xabc',
  totalUsd: 1000,
  dailyYieldUsd: 10,
  netWorthUsd: 1000,
  healthFactor: 1.2,
  positions: [],
  lastUpdatedIso: '2024-01-01',
}

vi.mock('./api', () => ({
  fetchPrices: vi.fn(() => Promise.resolve({})),
  fetchAlerts: vi.fn(() => Promise.resolve({ address: '0xabc', alerts: [] })),
  fetchPortfolio: vi.fn(() => Promise.resolve(mockPortfolio)),
}))

describe('App summary', () => {
  it('renders total and daily yield', async () => {
    render(<App />)
    const btn = await screen.findByText('Fetch')
    fireEvent.click(btn)
    await waitFor(() => {
      expect(screen.getByText(/Total USD:/)).toBeInTheDocument()
    })
    expect(screen.getByText(/Total USD:/).textContent).toContain('$1,000.00')
    expect(screen.getByText(/Daily Yield USD:/).textContent).toContain('$10.00')
  })
})
