import { Spinner } from './Spinner'

interface AlertProps {
  message: string
  onClose: () => void
  onRetry: () => void
  busy?: boolean
}

export function Alert({ message, onClose, onRetry, busy }: AlertProps) {
  return (
    <div
      role="alert"
      className="mb-4 flex items-start justify-between rounded border border-red-300 bg-red-50 p-3 text-red-800"
    >
      <div className="flex flex-1 items-center">
        {busy && <Spinner className="mr-2" />}
        <span>{message}</span>
      </div>
      <div className="ml-4 flex items-center gap-2">
        <button
          onClick={onRetry}
          className="rounded bg-red-200 px-2 py-1 text-xs hover:bg-red-300"
        >
          Retry
        </button>
        <button
          onClick={onClose}
          aria-label="Dismiss"
          className="rounded bg-red-200 px-2 py-1 text-xs hover:bg-red-300"
        >
          ✕
        </button>
      </div>
    </div>
  )
}
