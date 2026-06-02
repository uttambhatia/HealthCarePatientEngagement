import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { LabelWithIcon } from '../../components/LabelWithIcon'
import { createAppointment, listAppointments, listAvailableSlots } from '../../services/platformApi'

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const [, payload] = token.split('.')
  if (!payload) {
    return null
  }

  try {
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decoded) as Record<string, unknown>
  } catch {
    return null
  }
}

function inferPatientId(claims: Record<string, unknown> | null): string {
  if (!claims) {
    return ''
  }

  const candidates = [
    claims.patientId,
    claims.patient_id,
    claims['patient-id'],
    claims.externalReference,
    claims.external_reference,
    claims.sub,
  ]

  const match = candidates.find((value) => typeof value === 'string' && value.length > 0)
  return match ? String(match) : ''
}

function toDateInputValue(value: Date) {
  return value.toISOString().slice(0, 10)
}

function formatSlotLabel(value: string) {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }
  return parsed.toLocaleString()
}

type PatientAppointmentBookingProps = {
  onBooked?: () => Promise<void> | void
}

export function PatientAppointmentBooking({ onBooked }: PatientAppointmentBookingProps) {
  const { session } = useAuth()
  const [patientId, setPatientId] = useState('')
  const [providerId, setProviderId] = useState('')
  const [selectedProvider, setSelectedProvider] = useState('')
  const [providerOptions, setProviderOptions] = useState<string[]>([])
  const [loadingProviders, setLoadingProviders] = useState(false)
  const [appointmentDate, setAppointmentDate] = useState(() => toDateInputValue(new Date()))
  const [availableSlots, setAvailableSlots] = useState<string[]>([])
  const [selectedSlot, setSelectedSlot] = useState('')
  const [manualScheduledAt, setManualScheduledAt] = useState('')
  const [loadingSlots, setLoadingSlots] = useState(false)
  const [slotLoadError, setSlotLoadError] = useState<string | null>(null)
  const [channel, setChannel] = useState('VIDEO')
  const [submitting, setSubmitting] = useState(false)
  const [resultMessage, setResultMessage] = useState<string | null>(null)
  const [isError, setIsError] = useState(false)

  const inferredPatientId = useMemo(() => {
    if (!session) {
      return ''
    }

    const claims = decodeJwtPayload(session.idToken ?? session.accessToken)
    return inferPatientId(claims)
  }, [session])

  useEffect(() => {
    if (!patientId && inferredPatientId) {
      setPatientId(inferredPatientId)
    }
  }, [inferredPatientId, patientId])

  useEffect(() => {
    let active = true

    async function loadProviderOptions() {
      if (!session) {
        return
      }

      setLoadingProviders(true)
      try {
        const appointments = await listAppointments(session.accessToken)
        if (!active) {
          return
        }

        const providerIds = Array.from(new Set(
          appointments
            .map((item) => item.providerId)
            .filter((value): value is string => typeof value === 'string' && value.length > 0),
        )).sort((left, right) => left.localeCompare(right))

        setProviderOptions(providerIds)
        if (providerIds.length > 0) {
          setSelectedProvider(providerIds[0])
          if (!providerId) {
            setProviderId(providerIds[0])
          }
        } else {
          setSelectedProvider('CUSTOM')
        }
      } catch {
        if (!active) {
          return
        }
        setProviderOptions([])
        setSelectedProvider('CUSTOM')
      } finally {
        if (active) {
          setLoadingProviders(false)
        }
      }
    }

    void loadProviderOptions()

    return () => {
      active = false
    }
  }, [session])

  useEffect(() => {
    let active = true

    async function loadSlots() {
      if (!session || !providerId.trim() || !appointmentDate) {
        setAvailableSlots([])
        setSelectedSlot('')
        setSlotLoadError(null)
        return
      }

      setLoadingSlots(true)
      setSlotLoadError(null)
      try {
        const slots = await listAvailableSlots(providerId.trim(), appointmentDate, session.accessToken)
        if (!active) {
          return
        }

        setAvailableSlots(slots)
        setSelectedSlot((current) => (current && slots.includes(current) ? current : slots[0] ?? ''))
      } catch {
        if (!active) {
          return
        }
        setAvailableSlots([])
        setSelectedSlot('')
        setSlotLoadError('Unable to load available slots right now. Enter a date-time manually.')
      } finally {
        if (active) {
          setLoadingSlots(false)
        }
      }
    }

    void loadSlots()

    return () => {
      active = false
    }
  }, [appointmentDate, providerId, session])

  async function handleSubmit() {
    if (!session) {
      setIsError(true)
      setResultMessage('Please sign in before booking an appointment.')
      return
    }

    const selectedTime = selectedSlot || manualScheduledAt.trim()

    if (!patientId.trim() || !providerId.trim() || !selectedTime) {
      setIsError(true)
      setResultMessage('Patient ID, Provider ID and appointment time are required.')
      return
    }

    setSubmitting(true)
    setIsError(false)
    setResultMessage(null)

    try {
      const normalizedDate = Number.isNaN(Date.parse(selectedTime)) ? selectedTime : new Date(selectedTime).toISOString()
      await createAppointment(
        {
          patientId: patientId.trim(),
          providerId: providerId.trim(),
          scheduledAt: normalizedDate,
          channel,
        },
        session.accessToken,
      )
      await onBooked?.()
      setResultMessage('Appointment booked successfully. A confirmation event has been sent.')
      setProviderId('')
      setSelectedProvider(providerOptions[0] ?? 'CUSTOM')
      setSelectedSlot('')
      setManualScheduledAt('')
      setAvailableSlots([])
      setChannel('VIDEO')
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'We could not book this appointment right now. Please verify the details and try again.'
      setIsError(true)
      setResultMessage(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="patient-appointment-panel" aria-label="Book appointment">
      <h3>Book appointment</h3>
      <p>Schedule a visit directly from your landing page after sign-in.</p>

      <form className="stacked-form" onSubmit={(event) => { event.preventDefault(); void handleSubmit() }}>
        <div className="field-grid">
          <label className="field-block">
            <LabelWithIcon icon="patient">Patient ID</LabelWithIcon>
            <input
              value={patientId}
              placeholder="pat-1001"
              onChange={(event) => setPatientId(event.target.value)}
            />
            {inferredPatientId ? <small>Prefilled from your session token.</small> : null}
          </label>
          <label className="field-block">
            <LabelWithIcon icon="provider">Provider ID</LabelWithIcon>
            {providerOptions.length > 0 ? (
              <>
                <select
                  value={selectedProvider}
                  onChange={(event) => {
                    const next = event.target.value
                    setSelectedProvider(next)
                    if (next !== 'CUSTOM') {
                      setProviderId(next)
                      setSelectedSlot('')
                      setManualScheduledAt('')
                    }
                  }}
                >
                  {providerOptions.map((item) => (
                    <option key={item} value={item}>{item}</option>
                  ))}
                  <option value="CUSTOM">Custom provider ID</option>
                </select>
                {selectedProvider === 'CUSTOM' ? (
                  <input
                    value={providerId}
                    placeholder="prov-44"
                    onChange={(event) => {
                      setProviderId(event.target.value)
                      setSelectedSlot('')
                      setManualScheduledAt('')
                    }}
                  />
                ) : null}
              </>
            ) : (
              <input
                value={providerId}
                placeholder="prov-44"
                onChange={(event) => {
                  setProviderId(event.target.value)
                  setSelectedSlot('')
                  setManualScheduledAt('')
                }}
              />
            )}
            <small>
              {loadingProviders
                ? 'Loading providers from recent appointments...'
                : providerOptions.length > 0
                  ? 'Choose a provider or switch to custom entry.'
                  : 'No provider history found. Enter provider ID manually.'}
            </small>
          </label>
        </div>

        <div className="field-grid">
          <label className="field-block">
            <LabelWithIcon icon="calendar">Appointment date</LabelWithIcon>
            <input type="date" value={appointmentDate} onChange={(event) => setAppointmentDate(event.target.value)} />
            <small>Choose a date to fetch provider availability.</small>
          </label>

          <label className="field-block">
            <LabelWithIcon icon="clock">Available slot</LabelWithIcon>
            {loadingSlots ? <p>Loading available slots...</p> : null}
            {!loadingSlots && availableSlots.length > 0 ? (
              <select value={selectedSlot} onChange={(event) => setSelectedSlot(event.target.value)}>
                {availableSlots.map((slot) => (
                  <option key={slot} value={slot}>{formatSlotLabel(slot)}</option>
                ))}
              </select>
            ) : (
              <>
                <input
                  type="datetime-local"
                  value={manualScheduledAt}
                  onChange={(event) => setManualScheduledAt(event.target.value)}
                />
                <small>{slotLoadError ?? 'No slots returned for this provider/date. Enter time manually.'}</small>
              </>
            )}
          </label>
        </div>

        <div className="field-grid">
          <label className="field-block">
            <LabelWithIcon icon="channel">Channel</LabelWithIcon>
            <select value={channel} onChange={(event) => setChannel(event.target.value)}>
              <option value="VIDEO">Video consultation</option>
              <option value="IN_PERSON">In-person visit</option>
            </select>
          </label>
        </div>

        <div className="form-actions">
          <button type="submit" className="primary-button" disabled={submitting}>
            {submitting ? 'Booking...' : 'Book appointment'}
          </button>
        </div>

        {resultMessage ? (
          <p className={isError ? 'form-status form-status--error' : 'form-status form-status--success'} role={isError ? 'alert' : 'status'}>
            {resultMessage}
          </p>
        ) : null}
      </form>
    </section>
  )
}
