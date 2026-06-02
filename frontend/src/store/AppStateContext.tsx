import { createContext, useMemo, type PropsWithChildren } from 'react'
import { useRealtimeFeed } from '../hooks/useRealtimeFeed'

type AppStateValue = {
  liveUpdates: string[]
  liveTransport: 'websocket' | 'sse' | 'polling'
}

const AppStateContext = createContext<AppStateValue | undefined>(undefined)

export function AppStateProvider({ children }: PropsWithChildren) {
  const feed = useRealtimeFeed([
    'Patient onboarding saga ready',
    'Appointment notification queue healthy',
    'Telemetry ingestion within SLA',
  ])

  const { items: liveUpdates, transport: liveTransport } = feed

  const value = useMemo(() => ({ liveUpdates, liveTransport }), [liveTransport, liveUpdates])

  return <AppStateContext.Provider value={value}>{children}</AppStateContext.Provider>
}

export { AppStateContext }
