import { useEffect, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { Card } from '../../components/Card'
import { listAppointments, listAlerts, type AppointmentResponse, type AlertResponse } from '../../services/platformApi'

export function TeleconsultationWorkspace() {
  const { session } = useAuth()
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([])
  const [escalations, setEscalations] = useState<AlertResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function loadData() {
      if (!session) {
        setLoading(false)
        return
      }

      try {
        const [appts, alerts] = await Promise.all([
          listAppointments(session.accessToken),
          listAlerts(session.accessToken),
        ])

        const bookedAppts = appts.filter((a) => a.status === 'BOOKED')
        setAppointments(bookedAppts)

        const teleconsultAlerts = alerts.filter(
          (a) =>
            a.status === 'OPEN' &&
            (a.triggerType === 'TELECONSULT_CRITICAL_FINDINGS' || a.triggerType === 'TELECONSULT_INCOMPLETE_FOLLOWUP'),
        )
        setEscalations(teleconsultAlerts)
        setError(null)
      } catch (cause) {
        const message = cause instanceof Error ? cause.message : 'Failed to load teleconsultation data'
        setError(message)
      } finally {
        setLoading(false)
      }
    }

    void loadData()
  }, [session])

  return (
    <Card title="Teleconsultation" eyebrow="Realtime care delivery">
      <p>Prepare secure video sessions, care notes and post-visit follow-up reminders.</p>
      {error ? (
        <p style={{ color: 'var(--color-error)' }}>We could not load teleconsultation data: {error}</p>
      ) : !loading ? (
        <div className="stats-grid">
          <div>
            <strong>{appointments.length}</strong>
            <span>Upcoming virtual consults</span>
          </div>
          <div>
            <strong>{escalations.length}</strong>
            <span>Escalations awaiting review</span>
          </div>
        </div>
      ) : (
        <p>Loading teleconsultation data...</p>
      )}
    </Card>
  )
}
