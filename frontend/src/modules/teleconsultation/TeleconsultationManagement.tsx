import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { ApiError } from '../../services/apiClient'
import { Card } from '../../components/Card'
import { LabelWithIcon } from '../../components/LabelWithIcon'
import { MetricCardIcon } from '../../components/MetricCardIcon'
import { completeTeleconsultation, joinTeleconsultation, listAppointments, startTeleconsultation, type AppointmentResponse, type TeleconsultationResponse } from '../../services/platformApi'

function buildTeleconsultCallClientUrl(joinUrl: string, role: string) {
  const encodedJoinUrl = encodeURIComponent(joinUrl)
  const encodedRole = encodeURIComponent(role || 'UNKNOWN')
  return `/teleconsult/call?role=${encodedRole}&joinUrl=${encodedJoinUrl}`
}

function resolveDoctorJoinUrl(url: string) {
  try {
    const parsed = new URL(url)
    if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') {
      return {
        resolvedUrl: '',
        error: 'Teleconsultation session URL is invalid. Contact support to update teleconsultation configuration.',
      }
    }

    return { resolvedUrl: parsed.toString(), error: null }
  } catch {
    return {
      resolvedUrl: '',
      error: 'Teleconsultation session URL is invalid. Contact support to update teleconsultation configuration.',
    }
  }
}

function isTeleconsultSessionNotStarted(cause: unknown) {
  if (!(cause instanceof ApiError) || cause.status !== 404) {
    return false
  }

  if (cause.body && typeof cause.body === 'object' && 'code' in cause.body && cause.body.code === 'TELECONSULTATION_NOT_FOUND') {
    return true
  }

  return cause.message.toUpperCase().includes('TELECONSULTATION_NOT_FOUND')
    || cause.message.toUpperCase().includes('SESSION NOT FOUND')
}

export function TeleconsultationManagement() {
  const { session } = useAuth()
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([])
  const [activeSessions, setActiveSessions] = useState<TeleconsultationResponse[]>([])
  const [selectedAppointmentId, setSelectedAppointmentId] = useState('')
  const [consultationNotes, setConsultationNotes] = useState('')
  const [followUpRequired, setFollowUpRequired] = useState(false)
  const [nextFollowUpDate, setNextFollowUpDate] = useState('')
  const [loading, setLoading] = useState(false)
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [isError, setIsError] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [historyPage, setHistoryPage] = useState(0)
  const [awaitingProviderStart, setAwaitingProviderStart] = useState<Record<string, boolean>>({})

  const historyPerPage = 4

  const currentSession = useMemo(() => activeSessions[0] ?? null, [activeSessions])
  const activeSession = useMemo(
    () => (currentSession && (currentSession.status === 'INITIATED' || currentSession.status === 'IN_PROGRESS') ? currentSession : null),
    [currentSession],
  )
  const completedSession = useMemo(
    () => (currentSession && currentSession.status === 'COMPLETED' ? currentSession : null),
    [currentSession],
  )
  const isPatientSession = session?.role === 'PATIENT'
  const activeAppointmentId = activeSession?.appointmentId || completedSession?.appointmentId || selectedAppointmentId
  const waitingForProviderToStart = Boolean(activeAppointmentId && awaitingProviderStart[activeAppointmentId])
  const teleconsultMetricStatus = (currentSession?.status ?? 'NONE').toLowerCase()
  const totalHistoryPages = Math.max(1, Math.ceil(appointments.length / historyPerPage))
  const visibleHistory = appointments.slice(historyPage * historyPerPage, (historyPage + 1) * historyPerPage)

  useEffect(() => {
    setHistoryPage((current) => Math.min(current, Math.max(0, totalHistoryPages - 1)))
  }, [totalHistoryPages])

  useEffect(() => {
    let active = true

    async function loadData() {
      if (!session) {
        return
      }

      setLoading(true)
      try {
        const returnedAppointments = await listAppointments(session.accessToken)
        if (active) {
          setAppointments(returnedAppointments)
        }
      } catch (cause) {
        if (!active) {
          return
        }
        const message = cause instanceof Error ? cause.message : 'We could not load appointments for tele-consultation right now. Please try again.'
        setIsError(true)
        setActionMessage(message)
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    void loadData()

    return () => {
      active = false
    }
  }, [session])

  async function handleStartTeleconsultation() {
    if (!session) {
      setIsError(true)
      setActionMessage('Please sign in before starting a teleconsultation.')
      return
    }

    if (!selectedAppointmentId) {
      setIsError(true)
      setActionMessage('Select an appointment to start.')
      return
    }

    setSubmitting(true)
    setIsError(false)
    setActionMessage(null)

    try {
      const response = await startTeleconsultation(selectedAppointmentId, session.accessToken)
      if (active) {
        setActiveSessions([response])
        setAwaitingProviderStart((previous) => {
          if (!previous[selectedAppointmentId]) {
            return previous
          }
          const next = { ...previous }
          delete next[selectedAppointmentId]
          return next
        })
        setActionMessage('Teleconsultation session started successfully.')
      }
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'We could not start the tele-consultation session right now. Please retry.'
      setIsError(true)
      setActionMessage(message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleJoinTeleconsultation() {
    if (!session || !activeSession) {
      setIsError(true)
      setActionMessage('No active session to join.')
      return
    }

    setSubmitting(true)
    setIsError(false)
    setActionMessage(null)

    try {
      const response = await joinTeleconsultation(activeSession.appointmentId, session.accessToken)
      if (active) {
        setActiveSessions([response])
        setAwaitingProviderStart((previous) => {
          if (!previous[activeSession.appointmentId]) {
            return previous
          }
          const next = { ...previous }
          delete next[activeSession.appointmentId]
          return next
        })
        setActionMessage('Joined teleconsultation session.')
      }
    } catch (cause) {
      if (isTeleconsultSessionNotStarted(cause)) {
        setAwaitingProviderStart((previous) => ({
          ...previous,
          [activeSession.appointmentId]: true,
        }))
        setIsError(false)
        setActionMessage('Teleconsultation has not started yet. Ask your provider to start the session, then click Recheck session.')
        return
      }

      const message = cause instanceof Error ? cause.message : 'We could not join the tele-consultation session right now. Please retry.'
      setIsError(true)
      setActionMessage(message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleJoinAndOpenSession() {
    if (!session || !activeSession) {
      setIsError(true)
      setActionMessage('No active session to join.')
      return
    }

    setSubmitting(true)
    setIsError(false)
    setActionMessage(null)

    try {
      const response = await joinTeleconsultation(activeSession.appointmentId, session.accessToken)
      const { resolvedUrl, error } = resolveDoctorJoinUrl(response.doctorJoinUrl)
      if (error) {
        setIsError(true)
        setActionMessage(error)
        return
      }

      if (active) {
        setActiveSessions([response])
        setAwaitingProviderStart((previous) => {
          if (!previous[activeSession.appointmentId]) {
            return previous
          }
          const next = { ...previous }
          delete next[activeSession.appointmentId]
          return next
        })
        setActionMessage('Joined teleconsultation session.')
      }

      const callClientUrl = buildTeleconsultCallClientUrl(resolvedUrl, 'DOCTOR')
      // Use same-tab navigation to avoid popup blockers after async join API calls.
      window.location.assign(callClientUrl)
    } catch (cause) {
      if (isTeleconsultSessionNotStarted(cause)) {
        setAwaitingProviderStart((previous) => ({
          ...previous,
          [activeSession.appointmentId]: true,
        }))
        setIsError(false)
        setActionMessage('Teleconsultation has not started yet. Ask your provider to start the session, then click Recheck session.')
        return
      }

      const message = cause instanceof Error ? cause.message : 'We could not join the tele-consultation session right now. Please retry.'
      setIsError(true)
      setActionMessage(message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCompleteTeleconsultation() {
    if (!session || !activeSession) {
      setIsError(true)
      setActionMessage('No active session to complete.')
      return
    }

    if (!consultationNotes.trim()) {
      setIsError(true)
      setActionMessage('Consultation notes are required.')
      return
    }

    if (followUpRequired && !nextFollowUpDate.trim()) {
      setIsError(true)
      setActionMessage('Follow-up date is required when follow-up is selected.')
      return
    }

    setSubmitting(true)
    setIsError(false)
    setActionMessage(null)

    try {
      const response = await completeTeleconsultation(
        activeSession.appointmentId,
        {
          consultationNotes: consultationNotes.trim(),
          followUpRequired,
          nextFollowUpDate: followUpRequired ? nextFollowUpDate : undefined,
        },
        session.accessToken,
      )

      if (active) {
        setActiveSessions([response])
        setActionMessage('Teleconsultation completed and notes recorded. Review the post-session summary below.')
        setConsultationNotes('')
        setFollowUpRequired(false)
        setNextFollowUpDate('')
      }
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'We could not complete the tele-consultation session right now. Please try again.'
      setIsError(true)
      setActionMessage(message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCopyCompletionSummary() {
    if (!completedSession || typeof navigator === 'undefined' || !navigator.clipboard) {
      return
    }

    const lines = [
      `Session ID: ${completedSession.sessionId}`,
      `Appointment ID: ${completedSession.appointmentId}`,
      `Status: ${completedSession.status}`,
      `Started: ${new Date(completedSession.startedAt).toLocaleString()}`,
      `Joined: ${completedSession.joinedAt ? new Date(completedSession.joinedAt).toLocaleString() : 'Not recorded'}`,
      `Completed: ${completedSession.completedAt ? new Date(completedSession.completedAt).toLocaleString() : 'Not recorded'}`,
      `Follow-up required: ${completedSession.followUpRequired ? 'Yes' : 'No'}`,
      `Next follow-up: ${completedSession.nextFollowUpDate ?? 'None'}`,
      '',
      'Consultation notes:',
      completedSession.consultationNotes ?? '(none)',
    ]

    await navigator.clipboard.writeText(lines.join('\n'))
    setIsError(false)
    setActionMessage('Post-session summary copied to clipboard.')
  }

  let active = true

  return (
    <Card title="Tele-consultation management" eyebrow="Remote care delivery">
      <div className="teleconsult-summary-row">
        <div className="metric-card metric-card--volume">
          <MetricCardIcon variant="volume" />
          <span>Upcoming appointments</span>
          <strong>{appointments.length}</strong>
        </div>
        <div className="metric-card metric-card--activity">
          <MetricCardIcon variant="activity" />
          <span>Active sessions</span>
          <strong>{activeSessions.length}</strong>
        </div>
        <div className={`metric-card metric-card--status metric-card--status-${teleconsultMetricStatus}`}>
          <MetricCardIcon variant="status" />
          <span>Session status</span>
          <strong>{currentSession ? currentSession.status : 'None'}</strong>
        </div>
      </div>

      {actionMessage ? (
        <p className={isError ? 'form-status form-status--error' : 'form-status form-status--success'} role={isError ? 'alert' : 'status'}>
          {actionMessage}
        </p>
      ) : null}

      <section className="teleconsult-quick-start">
        <h3>Start teleconsultation</h3>
        <div className="field-grid">
          <label className="field-block">
            <LabelWithIcon icon="calendar">Select appointment</LabelWithIcon>
            <select value={selectedAppointmentId} onChange={(event) => setSelectedAppointmentId(event.target.value)}>
              <option value="">Choose an appointment</option>
              {appointments.map((appt) => (
                <option key={appt.id} value={appt.id}>
                  {appt.patientId} - {appt.scheduledAt} ({appt.channel})
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="form-actions">
          <button type="button" className="primary-button" onClick={() => void handleStartTeleconsultation()} disabled={submitting || !selectedAppointmentId}>
            {submitting ? 'Starting...' : 'Start session'}
          </button>
        </div>
      </section>

      {activeSession ? (
        <section className="teleconsult-active-panel">
          <h3>Active session</h3>
          <div className="active-session-info">
            <p>
              <strong>Session ID:</strong> {activeSession.sessionId}
            </p>
            <p>
              <strong>Status:</strong> {activeSession.status}
            </p>
            <p>
              <strong>Started:</strong> {new Date(activeSession.startedAt).toLocaleString()}
            </p>
            {activeSession.joinedAt ? <p><strong>Joined:</strong> {new Date(activeSession.joinedAt).toLocaleString()}</p> : null}
          </div>

          <div className="form-actions teleconsult-quick-actions">
            <button
              type="button"
              className="primary-button"
              onClick={() => void handleJoinAndOpenSession()}
              disabled={submitting || (isPatientSession && waitingForProviderToStart)}
            >
              {submitting
                ? 'Joining...'
                : (isPatientSession && waitingForProviderToStart ? 'Awaiting provider start' : 'Join session')}
            </button>
            <button type="button" className="secondary-button" onClick={() => void handleJoinTeleconsultation()} disabled={submitting}>
              {submitting ? 'Updating...' : (isPatientSession && waitingForProviderToStart ? 'Recheck session' : 'Refresh session')}
            </button>
          </div>

          {isPatientSession && waitingForProviderToStart ? (
            <small>Provider must start teleconsultation before patients can join this appointment.</small>
          ) : null}

          {activeSession.status === 'IN_PROGRESS' ? (
            <section className="teleconsult-completion-panel">
              <h3>Complete session</h3>
              <label className="field-block">
                <LabelWithIcon icon="notes">Consultation notes</LabelWithIcon>
                <textarea value={consultationNotes} onChange={(event) => setConsultationNotes(event.target.value)} />
              </label>

              <label className="field-block teleconsult-followup-toggle">
                <input type="checkbox" checked={followUpRequired} onChange={(event) => setFollowUpRequired(event.target.checked)} />
                <LabelWithIcon icon="followup">Schedule follow-up</LabelWithIcon>
              </label>

              {followUpRequired ? (
                <label className="field-block">
                  <LabelWithIcon icon="clock">Follow-up date</LabelWithIcon>
                  <input type="datetime-local" value={nextFollowUpDate} onChange={(event) => setNextFollowUpDate(event.target.value)} />
                </label>
              ) : null}

              <div className="form-actions">
                <button type="button" className="primary-button" onClick={() => void handleCompleteTeleconsultation()} disabled={submitting}>
                  {submitting ? 'Completing...' : 'Complete and save notes'}
                </button>
              </div>
            </section>
          ) : null}
        </section>
      ) : null}

      {completedSession ? (
        <section className="teleconsult-completion-panel">
          <h3>Post-session summary</h3>
          <div className="active-session-info">
            <p>
              <strong>Session ID:</strong> {completedSession.sessionId}
            </p>
            <p>
              <strong>Appointment ID:</strong> {completedSession.appointmentId}
            </p>
            <p>
              <strong>Status:</strong> {completedSession.status}
            </p>
            <p>
              <strong>Completed:</strong> {completedSession.completedAt ? new Date(completedSession.completedAt).toLocaleString() : 'Not recorded'}
            </p>
            <p>
              <strong>Follow-up:</strong> {completedSession.followUpRequired ? `Required${completedSession.nextFollowUpDate ? `, ${new Date(completedSession.nextFollowUpDate).toLocaleString()}` : ''}` : 'Not required'}
            </p>
          </div>

          <div className="teleconsult-post-session-notes">
            <h4>What the platform completed</h4>
            <ul>
              <li>Consultation notes were saved to the appointment workflow.</li>
              <li>The appointment was marked completed and the downstream completion event was emitted.</li>
              <li>{completedSession.followUpRequired ? 'A follow-up reminder has been captured for the requested date.' : 'No follow-up was requested for this session.'}</li>
            </ul>
          </div>

          {completedSession.consultationNotes ? (
            <div className="teleconsult-post-session-notes">
              <h4>Recorded consultation notes</h4>
              <p>{completedSession.consultationNotes}</p>
            </div>
          ) : null}

          {completedSession.interactionLogs.length > 0 ? (
            <div className="teleconsult-post-session-notes">
              <h4>Session timeline</h4>
              <ul>
                {completedSession.interactionLogs.map((entry) => (
                  <li key={entry}>{entry}</li>
                ))}
              </ul>
            </div>
          ) : null}

          <div className="form-actions teleconsult-quick-actions">
            <button type="button" className="primary-button" onClick={() => void handleCopyCompletionSummary()}>
              Copy post-session summary
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => {
                setActiveSessions([])
                setActionMessage('Completed session cleared from view. You can now start the next appointment.')
                setIsError(false)
              }}
            >
              Dismiss summary
            </button>
          </div>
        </section>
      ) : null}

      <section className="teleconsult-history-panel">
        <h3>Session history</h3>
        {loading ? <p>Loading appointments...</p> : null}
        {!loading && appointments.length === 0 ? <p className="patient-appointments-empty">No appointments available for teleconsultation.</p> : null}
        {!loading && appointments.length > 0 ? (
          <div className="carousel-controls" aria-label="Session history navigation">
            <button
              type="button"
              className={`${historyPage >= totalHistoryPages - 1 ? 'primary-button' : 'secondary-button'} carousel-button`}
              onClick={() => setHistoryPage((page) => Math.max(0, page - 1))}
              disabled={historyPage === 0}
            >
              <span aria-hidden="true">&lt;</span> Prev
            </button>
            <button
              type="button"
              className={`${historyPage < totalHistoryPages - 1 ? 'primary-button' : 'secondary-button'} carousel-button`}
              onClick={() => setHistoryPage((page) => Math.min(totalHistoryPages - 1, page + 1))}
              disabled={historyPage >= totalHistoryPages - 1}
            >
              Next <span aria-hidden="true">&gt;</span>
            </button>
          </div>
        ) : null}
        <div className="teleconsult-history-list teleconsult-history-list--paged">
          {visibleHistory.map((appt) => (
            <article key={appt.id} className="teleconsult-history-item">
              <strong>Appointment ID: {appt.id}</strong>
              <span>Patient: {appt.patientId}</span>
              <span>Scheduled: {new Date(appt.scheduledAt).toLocaleString()}</span>
              <span>Channel: {appt.channel}</span>
              <span>Status: {appt.status}</span>
            </article>
          ))}
        </div>
      </section>
    </Card>
  )
}

