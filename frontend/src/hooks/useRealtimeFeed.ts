import { useEffect, useState } from 'react'

type FeedTransport = 'websocket' | 'sse' | 'polling'

type RealtimeFeedState = {
  items: string[]
  transport: FeedTransport
}

function toMessage(value: unknown) {
  if (typeof value === 'string') {
    return value
  }
  return `Realtime update @ ${new Date().toLocaleTimeString()}`
}

export function useRealtimeFeed(seed: string[]) {
  const initialTransport: FeedTransport = import.meta.env.VITE_EVENTS_WS_URL
    ? 'websocket'
    : import.meta.env.VITE_EVENTS_SSE_URL
      ? 'sse'
      : 'polling'

  const [state, setState] = useState<RealtimeFeedState>({
    items: seed,
    transport: initialTransport,
  })

  useEffect(() => {
    const wsUrl = import.meta.env.VITE_EVENTS_WS_URL
    if (wsUrl) {
      const socket = new WebSocket(wsUrl)
      socket.onmessage = (event) => {
        setState((current) => ({
          ...current,
          items: [toMessage(event.data), ...current.items].slice(0, 5),
        }))
      }
      socket.onerror = () => {
        setState((current) => ({ ...current, transport: 'polling' }))
      }
      return () => socket.close()
    }

    const sseUrl = import.meta.env.VITE_EVENTS_SSE_URL
    if (sseUrl) {
      const source = new EventSource(sseUrl)
      source.onmessage = (event) => {
        setState((current) => ({
          ...current,
          items: [toMessage(event.data), ...current.items].slice(0, 5),
        }))
      }
      source.onerror = () => {
        setState((current) => ({ ...current, transport: 'polling' }))
      }
      return () => source.close()
    }

    const interval = window.setInterval(() => {
      setState((current) => ({
        ...current,
        items: [toMessage(undefined), ...current.items].slice(0, 5),
      }))
    }, 5000)

    return () => {
      window.clearInterval(interval)
    }
  }, [])

  return state
}
