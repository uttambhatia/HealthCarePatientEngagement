import { useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { Card } from '../../components/Card'
import { createAppointment } from '../../services/platformApi'

export function AppointmentBooking() {
  const { session } = useAuth()
  const [patientId, setPatientId] = useState('')
  const [providerId, setProviderId] = useState('')
  const [scheduledAt, setScheduledAt] = useState('2026-05-26T09:00:00Z')
  const [channel, setChannel] = useState('VIDEO')
  const [result, setResult] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleBookAppointment() {
    if (!session) {
      setResult('Please sign in before booking an appointment.')
      return
    }

    if (!patientId || !providerId || !scheduledAt) {
      setResult('Patient ID, Provider ID and time are required.')
      return
    }

    setSubmitting(true)
    try {
      await createAppointment(
        {
          patientId,
          providerId,
          scheduledAt,
          channel,
        },
        session.accessToken,
      )
      setResult('Appointment request submitted successfully.')
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'We could not submit the appointment request right now. Please try again.'
      setResult(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card title="Appointment booking" eyebrow="Appointment service">
      <form className="stacked-form">
        <input value={patientId} placeholder="Patient ID" onChange={(event) => setPatientId(event.target.value)} />
        <input value={providerId} placeholder="Provider ID" onChange={(event) => setProviderId(event.target.value)} />
        <input value={scheduledAt} placeholder="2026-05-26T09:00:00Z" onChange={(event) => setScheduledAt(event.target.value)} />
        <select value={channel} onChange={(event) => setChannel(event.target.value)}>
          <option value="VIDEO">Video consultation</option>
          <option value="IN_PERSON">In-person visit</option>
        </select>
        <button type="button" className="primary-button" disabled={submitting} onClick={() => void handleBookAppointment()}>
          {submitting ? 'Submitting...' : 'Book appointment'}
        </button>
        {result ? <p>{result}</p> : null}
      </form>
    </Card>
  )
}
