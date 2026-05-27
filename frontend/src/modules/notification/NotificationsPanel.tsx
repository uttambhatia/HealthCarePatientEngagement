import { Card } from '../../components/Card'
import { useAppState } from '../../store/useAppState'

export function NotificationsPanel() {
  const { liveUpdates } = useAppState()

  return (
    <Card title="Notifications" eyebrow="ACS + Service Bus">
      <ul>
        {liveUpdates.map((update) => (
          <li key={update}>{update}</li>
        ))}
      </ul>
    </Card>
  )
}
