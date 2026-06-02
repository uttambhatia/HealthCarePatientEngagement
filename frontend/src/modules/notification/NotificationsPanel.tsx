import { useEffect, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { Card } from '../../components/Card'
import { listNotifications } from '../../services/platformApi'
import { useAppState } from '../../store/useAppState'

export function NotificationsPanel() {
  const { session } = useAuth()
  const { liveUpdates, liveTransport } = useAppState()
  const [notificationCount, setNotificationCount] = useState<number | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    let active = true

    async function loadNotifications() {
      if (!session) {
        return
      }

      try {
        const notifications = await listNotifications(session.accessToken)
        if (active) {
          setNotificationCount(notifications.length)
          setLoadError(null)
        }
      } catch (cause) {
        if (!active) {
          return
        }
        const message = cause instanceof Error ? cause.message : 'Notifications are temporarily unavailable. Please refresh shortly.'
        setLoadError(message)
      }
    }

    void loadNotifications()

    return () => {
      active = false
    }
  }, [session])

  return (
    <Card title="Notifications" eyebrow="Care communication">
      <p>Live update channel: {liveTransport.toUpperCase()}</p>
      {notificationCount !== null ? <p>Messages available: {notificationCount}</p> : null}
      {loadError ? <p>We could not load notifications right now: {loadError}</p> : null}
      <ul>
        {liveUpdates.map((update) => (
          <li key={update}>{update}</li>
        ))}
      </ul>
    </Card>
  )
}
