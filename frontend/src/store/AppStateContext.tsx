import { createContext, useMemo, type PropsWithChildren } from 'react'
import { useAuth } from '../auth/useAuth'
import { listNotifications } from '../services/platformApi'
import { useRealtimeFeed } from '../hooks/useRealtimeFeed'

type AppStateValue = {
  liveUpdates: string[]
  liveTransport: 'websocket' | 'sse' | 'polling'
}

const AppStateContext = createContext<AppStateValue | undefined>(undefined)

export function AppStateProvider({ children }: PropsWithChildren) {
  const { session } = useAuth()

  const feed = useRealtimeFeed([], listNotifications, session?.accessToken)

  const { items: liveUpdates, transport: liveTransport } = feed

  const value = useMemo(() => ({ liveUpdates, liveTransport }), [liveTransport, liveUpdates])

  return <AppStateContext.Provider value={value}>{children}</AppStateContext.Provider>
}

export { AppStateContext }
