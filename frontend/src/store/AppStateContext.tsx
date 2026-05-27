import { createContext, useMemo, type PropsWithChildren } from 'react'
import { useRealtimeFeed } from '../hooks/useRealtimeFeed'

type AppStateValue = {
  liveUpdates: string[]
}

const AppStateContext = createContext<AppStateValue | undefined>(undefined)

export function AppStateProvider({ children }: PropsWithChildren) {
  const liveUpdates = useRealtimeFeed([
    'Patient onboarding saga ready',
    'Appointment notification queue healthy',
    'Telemetry ingestion within SLA',
  ])

  const value = useMemo(() => ({ liveUpdates }), [liveUpdates])

  return <AppStateContext.Provider value={value}>{children}</AppStateContext.Provider>
}

export { AppStateContext }
