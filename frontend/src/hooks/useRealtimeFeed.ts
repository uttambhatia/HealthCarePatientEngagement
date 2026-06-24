import { useEffect, useRef, useState } from 'react'

type FeedTransport = 'websocket' | 'sse' | 'polling'

type RealtimeFeedState = {
  items: string[]
  transport: FeedTransport
}

interface NotificationMessage {
  id?: string
  message?: string
  timestamp?: string
}

function toMessage(value: unknown) {
  if (typeof value === 'string') {
    return value
  }
  return `Realtime update @ ${new Date().toLocaleString()}`
}

export function useRealtimeFeed(
  seed: string[],
  fetchNotifications?: (token?: string) => Promise<NotificationMessage[] | Array<{ id?: string; message?: string; timestamp?: string }>>,
  token?: string,
) {
  const initialTransport: FeedTransport = import.meta.env.VITE_EVENTS_WS_URL
    ? 'websocket'
    : import.meta.env.VITE_EVENTS_SSE_URL
      ? 'sse'
      : 'polling'

  const [state, setState] = useState<RealtimeFeedState>({
    items: seed,
    transport: initialTransport,
  })

  const seenNotificationIdsRef = useRef<Set<string>>(new Set())

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

    const interval = window.setInterval(async () => {
      try {
        if (fetchNotifications) {
          const notifications = await fetchNotifications(token)
          const newMessages: string[] = []
          
          for (const notif of notifications) {
            const notifId = notif.id || notif.message || ''
            if (!seenNotificationIdsRef.current.has(notifId)) {
              seenNotificationIdsRef.current.add(notifId)
              newMessages.push(notif.message || `Notification @ ${new Date().toLocaleString()}`)
              if (newMessages.length >= 1) break
            }
          }
          
          if (newMessages.length > 0) {
            setState((current) => ({
              ...current,
              items: [...newMessages, ...current.items].slice(0, 5),
            }))
          }
        }
      } catch {
        // Silently handle fetch errors
      }
    }, 5000)

    return () => {
      window.clearInterval(interval)
    }
  }, [fetchNotifications, token])

  return state
}
