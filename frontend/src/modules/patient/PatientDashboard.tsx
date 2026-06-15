import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { Card } from '../../components/Card'
import { SectionHeader } from '../../components/SectionHeader'
import {
  joinTeleconsultation,
  listAppointments,
  listPatients,
  type AppointmentResponse,
  type TeleconsultationResponse,
} from '../../services/platformApi'
import { PatientAppointmentBooking } from './PatientAppointmentBooking'
import { PatientConsentManagement } from './PatientConsentManagement'

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

export function PatientDashboard() {
  const { session } = useAuth()
  const [, setPatientCount] = useState<number | null>(null)
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([])
  const [teleconsultations, setTeleconsultations] = useState<Record<string, TeleconsultationResponse>>({})
  const [joiningAppointmentId, setJoiningAppointmentId] = useState<string | null>(null)
  const [teleconsultActionMessage, setTeleconsultActionMessage] = useState<string | null>(null)
  const [teleconsultActionError, setTeleconsultActionError] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [bookingOpen, setBookingOpen] = useState(false)
  const [consentFormOpen, setConsentFormOpen] = useState(false)
  const [appointmentPage, setAppointmentPage] = useState(0)
  const appointmentsPerPage = 4

  async function refreshBookedAppointments() {
    if (!session) {
      return
    }

    const appointmentRecords = await listAppointments(session.accessToken)
    setAppointments(
      appointmentRecords.filter((appointment) => appointment.status === 'BOOKED'
        && (!inferredPatientId || appointment.patientId === inferredPatientId)),
    )
  }

  async function handleJoinTeleconsultation(appointment: AppointmentResponse) {
    if (!session) {
      return
    }

    setJoiningAppointmentId(appointment.id)
    setTeleconsultActionMessage(null)
    setTeleconsultActionError(false)

    try {
      const teleconsultation = await joinTeleconsultation(appointment.id, session.accessToken)
      setTeleconsultations((previous) => ({
        ...previous,
        [appointment.id]: teleconsultation,
      }))
      setTeleconsultActionError(false)
      setTeleconsultActionMessage('Teleconsultation is ready. Use the join link to enter the secure session.')
    } catch (cause) {
      const message = cause instanceof Error
        ? cause.message
        : 'Teleconsultation is not available for this appointment yet. Please try again in a moment.'
      setTeleconsultActionError(true)
      setTeleconsultActionMessage(message)
    } finally {
      setJoiningAppointmentId(null)
    }
  }

  const inferredPatientId = useMemo(() => {
    if (!session) {
      return ''
    }

    const claims = decodeJwtPayload(session.idToken ?? session.accessToken)
    return inferPatientId(claims)
  }, [session])

  const totalAppointmentPages = Math.max(1, Math.ceil(appointments.length / appointmentsPerPage))
  const visibleAppointments = appointments.slice(
    appointmentPage * appointmentsPerPage,
    (appointmentPage + 1) * appointmentsPerPage,
  )

  useEffect(() => {
    setAppointmentPage((current) => Math.min(current, Math.max(0, totalAppointmentPages - 1)))
  }, [totalAppointmentPages])

  useEffect(() => {
    let active = true

    async function loadDashboardData() {
      if (!session) {
        return
      }

      try {
        const [patients, appointmentRecords] = await Promise.all([
          listPatients(session.accessToken),
          listAppointments(session.accessToken),
        ])

        if (active) {
          setPatientCount(patients.length)
          setAppointments(
            appointmentRecords.filter((appointment) => appointment.status === 'BOOKED'
              && (!inferredPatientId || appointment.patientId === inferredPatientId)),
          )
          setLoadError(null)
        }
      } catch (cause) {
        if (!active) {
          return
        }
        const message = cause instanceof Error ? cause.message : 'Patient dashboard data is temporarily unavailable. Please refresh shortly.'
        setLoadError(message)
      }
    }

    void loadDashboardData()

    return () => {
      active = false
    }
  }, [inferredPatientId, session])

  return (
    <Card
      title="Patient dashboard"
      eyebrow="Care journey"
      subtitle="Registration, consent, and appointment progress"
      centeredHeader
    >
      {loadError ? <p>We could not load patient details right now: {loadError}</p> : null}
      
      <section className="patient-consent-section" aria-label="Consent management">
        <SectionHeader
          title="Consent management"
          subtitle="Grant or revoke care data permissions and review your consent records."
          action={
            <button type="button" className="primary-button" onClick={() => setConsentFormOpen((open) => !open)}>
              {consentFormOpen ? 'Cancel' : 'Create consent'}
            </button>
          }
        />
        <PatientConsentManagement showConsentForm={consentFormOpen} />
      </section>
      {teleconsultActionMessage ? (
        <p className={teleconsultActionError ? 'form-status form-status--error' : 'form-status form-status--success'} role={teleconsultActionError ? 'alert' : 'status'}>
          {teleconsultActionMessage}
        </p>
      ) : null}
      <section className="patient-appointments-panel" aria-label="Booked appointments">
        <SectionHeader
          title="Booked appointments"
          subtitle="Appointments already scheduled through the platform."
          action={
            <button type="button" className="primary-button" onClick={() => setBookingOpen((o) => !o)}>
              {bookingOpen ? 'Cancel' : 'Book appointment'}
            </button>
          }
        />
        {bookingOpen ? (
          <PatientAppointmentBooking
            onBooked={async () => {
              await refreshBookedAppointments()
              setBookingOpen(false)
            }}
          />
        ) : null}
        {appointments.length > 0 ? (
          <>
            <div className="carousel-controls" aria-label="Booked appointments navigation">
              <button
                type="button"
                className={`${appointmentPage >= totalAppointmentPages - 1 ? 'primary-button' : 'secondary-button'} carousel-button`}
                onClick={() => setAppointmentPage((page) => Math.max(0, page - 1))}
                disabled={appointmentPage === 0}
              >
                <span aria-hidden="true">&lt;</span> Prev
              </button>
              <button
                type="button"
                className={`${appointmentPage < totalAppointmentPages - 1 ? 'primary-button' : 'secondary-button'} carousel-button`}
                onClick={() => setAppointmentPage((page) => Math.min(totalAppointmentPages - 1, page + 1))}
                disabled={appointmentPage >= totalAppointmentPages - 1}
              >
                Next <span aria-hidden="true">&gt;</span>
              </button>
            </div>
            <div className="patient-appointment-list patient-appointment-list--paged">
            {visibleAppointments.map((appointment) => (
              <article key={appointment.id} className="patient-appointment-item">
                <strong>{new Date(appointment.scheduledAt).toLocaleString()}</strong>
                <span>Appointment ID: {appointment.id}</span>
                <span>Provider: {appointment.providerId}</span>
                <span>Patient: {appointment.patientId}</span>
                <span>Channel: {appointment.channel}</span>
                <span>Status: {appointment.status}</span>
                {teleconsultations[appointment.id] ? (
                  <span className="patient-teleconsult-status">
                    Teleconsultation status: {teleconsultations[appointment.id].status}
                  </span>
                ) : null}
                <div className="patient-appointment-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => void handleJoinTeleconsultation(appointment)}
                    disabled={joiningAppointmentId === appointment.id}
                  >
                    {joiningAppointmentId === appointment.id ? 'Checking session...' : 'Join teleconsultation'}
                  </button>
                  {teleconsultations[appointment.id]?.patientJoinUrl ? (
                    <a
                      href={teleconsultations[appointment.id].patientJoinUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="primary-button"
                    >
                      Open secure session
                    </a>
                  ) : null}
                </div>
              </article>
            ))}
            </div>
          </>
        ) : (
          <p className="patient-appointments-empty">
            {inferredPatientId ? 'No booked appointments found for your patient profile yet.' : 'No booked appointments yet.'}
          </p>
        )}
      </section>
    </Card>
  )
}
