import { useCallback, useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { Card } from '../../components/Card'
import { LabelWithIcon } from '../../components/LabelWithIcon'
import { MetricCardIcon } from '../../components/MetricCardIcon'
import { SectionHeader } from '../../components/SectionHeader'
import {
  listAlerts,
  listAppointments,
  listCarePlans,
  updateCarePlan,
  type AlertResponse,
  type AppointmentResponse,
  type CarePlanResponse,
} from '../../services/platformApi'

// ---------------------------------------------------------------------------
// Task parsing helpers
// ---------------------------------------------------------------------------

type ParsedTask = {
  raw: string
  type: 'FOLLOW_UP' | 'FOLLOW_UP_APPOINTMENT' | 'OTHER'
  label: string
  meta: string
}

function parseTask(raw: string): ParsedTask {
  const sep = raw.indexOf(' | ')
  if (sep === -1) {
    return { raw, type: 'OTHER', label: raw, meta: '' }
  }

  const prefix = raw.slice(0, sep).trim().toUpperCase()
  const body = raw.slice(sep + 3).trim()

  if (prefix === 'FOLLOW_UP_APPOINTMENT') {
    return { raw, type: 'FOLLOW_UP_APPOINTMENT', label: body, meta: 'Book follow-up appointment' }
  }

  if (prefix === 'FOLLOW_UP') {
    return { raw, type: 'FOLLOW_UP', label: body, meta: 'Post-teleconsult follow-up' }
  }

  return { raw, type: 'OTHER', label: raw, meta: '' }
}

function isFollowUpTask(parsed: ParsedTask) {
  return parsed.type === 'FOLLOW_UP' || parsed.type === 'FOLLOW_UP_APPOINTMENT'
}

// ---------------------------------------------------------------------------
// Alert helpers
// ---------------------------------------------------------------------------

function alertBadgeClass(triggerType: string) {
  if (triggerType === 'TELECONSULT_CRITICAL_FINDINGS') return 'badge badge--high'
  if (triggerType === 'TELECONSULT_INCOMPLETE_FOLLOWUP') return 'badge badge--warning'
  return 'badge badge--info'
}

function alertLabel(triggerType: string) {
  if (triggerType === 'TELECONSULT_CRITICAL_FINDINGS') return 'Critical findings'
  if (triggerType === 'TELECONSULT_INCOMPLETE_FOLLOWUP') return 'Incomplete follow-up'
  return triggerType.replace(/_/g, ' ').toLowerCase()
}

// ---------------------------------------------------------------------------
// Main component
// ---------------------------------------------------------------------------

export function FollowUpTaskboard() {
  const { session } = useAuth()

  const [carePlans, setCarePlans] = useState<CarePlanResponse[]>([])
  const [alerts, setAlerts] = useState<AlertResponse[]>([])
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [isError, setIsError] = useState(false)
  const [completing, setCompleting] = useState<string | null>(null) // planId being saved
  const [alertFilter, setAlertFilter] = useState<'ALL' | 'OPEN'>('OPEN')
  const [taskFilter, setTaskFilter] = useState<'ALL' | 'FOLLOW_UP' | 'FOLLOW_UP_APPOINTMENT'>('ALL')

  // ---------------------------------------------------------------------------
  // Derived data
  // ---------------------------------------------------------------------------

  const followUpPlans = useMemo(() => {
    return carePlans
      .map((plan) => ({
        plan,
        followUpTasks: plan.tasks.map(parseTask).filter(isFollowUpTask),
      }))
      .filter(({ followUpTasks }) => followUpTasks.length > 0)
  }, [carePlans])

  const visibleFollowUpPlans = useMemo(() => {
    if (taskFilter === 'ALL') return followUpPlans
    return followUpPlans
      .map(({ plan, followUpTasks }) => ({
        plan,
        followUpTasks: followUpTasks.filter((t) => t.type === taskFilter),
      }))
      .filter(({ followUpTasks }) => followUpTasks.length > 0)
  }, [followUpPlans, taskFilter])

  const teleconsultAlerts = useMemo(() => {
    return alerts.filter(
      (alert) =>
        alert.triggerType === 'TELECONSULT_CRITICAL_FINDINGS' ||
        alert.triggerType === 'TELECONSULT_INCOMPLETE_FOLLOWUP',
    )
  }, [alerts])

  const visibleAlerts = useMemo(() => {
    if (alertFilter === 'ALL') return teleconsultAlerts
    return teleconsultAlerts.filter((alert) => alert.status === 'OPEN')
  }, [teleconsultAlerts, alertFilter])

  const recentFollowUpAppointments = useMemo(() => {
    const today = new Date().toISOString().slice(0, 10)
    return appointments
      .filter((appt) => appt.status === 'BOOKED' && appt.scheduledAt >= today)
      .sort((a, b) => a.scheduledAt.localeCompare(b.scheduledAt))
      .slice(0, 10)
  }, [appointments])

  const totalFollowUpTasks = useMemo(
    () => followUpPlans.reduce((sum, { followUpTasks }) => sum + followUpTasks.length, 0),
    [followUpPlans],
  )

  const openAlertCount = useMemo(
    () => teleconsultAlerts.filter((a) => a.status === 'OPEN').length,
    [teleconsultAlerts],
  )

  // ---------------------------------------------------------------------------
  // Data loading
  // ---------------------------------------------------------------------------

  const loadData = useCallback(
    async (active: { value: boolean }) => {
      if (!session) return
      setLoading(true)
      setLoadError(null)
      try {
        const [plans, fetchedAlerts, appts] = await Promise.all([
          listCarePlans(session.accessToken),
          listAlerts(session.accessToken).catch(() => [] as AlertResponse[]),
          listAppointments(session.accessToken).catch(() => [] as AppointmentResponse[]),
        ])
        if (!active.value) return
        setCarePlans(plans)
        setAlerts(fetchedAlerts)
        setAppointments(appts)
      } catch (cause) {
        if (!active.value) return
        setLoadError(cause instanceof Error ? cause.message : 'Unable to load taskboard data.')
      } finally {
        if (active.value) setLoading(false)
      }
    },
    [session],
  )

  useEffect(() => {
    const active = { value: true }
    void loadData(active)
    return () => { active.value = false }
  }, [loadData])

  // ---------------------------------------------------------------------------
  // Mark task complete
  // ---------------------------------------------------------------------------

  async function handleMarkTaskDone(plan: CarePlanResponse, taskRaw: string) {
    if (!session) {
      setIsError(true)
      setActionMessage('Please sign in.')
      return
    }

    const updatedTasks = plan.tasks.filter((t) => t !== taskRaw)
    if (updatedTasks.length === 0) {
      setIsError(true)
      setActionMessage(
        `Cannot mark the last task done — a care plan must have at least one task. Edit the plan instead.`,
      )
      return
    }

    setCompleting(plan.id + taskRaw)
    setIsError(false)
    setActionMessage(null)

    try {
      await updateCarePlan(
        plan.id,
        {
          goal: plan.goal,
          planStatus: plan.planStatus,
          ownerId: plan.ownerId,
          tasks: updatedTasks,
          expectedVersion: plan.version,
        },
        session.accessToken,
      )
      const active = { value: true }
      await loadData(active)
      setActionMessage(`Task completed and removed from plan "${plan.goal}".`)
    } catch (cause) {
      setIsError(true)
      setActionMessage(
        cause instanceof Error ? cause.message : 'Could not update care plan. Please try again.',
      )
    } finally {
      setCompleting(null)
    }
  }

  // ---------------------------------------------------------------------------
  // Render
  // ---------------------------------------------------------------------------

  return (
    <Card title="Follow-up taskboard" eyebrow="Care coordination">
      {/* ── summary metrics ── */}
      <div className="careplan-summary-row">
        <div className="metric-card metric-card--volume">
          <MetricCardIcon variant="volume" />
          <span>Open follow-up tasks</span>
          <strong>{totalFollowUpTasks}</strong>
        </div>
        <div className={`metric-card metric-card--status metric-card--status-${openAlertCount > 0 ? 'active' : 'closed'}`}>
          <MetricCardIcon variant="status" />
          <span>Open teleconsult alerts</span>
          <strong>{openAlertCount}</strong>
        </div>
        <div className="metric-card metric-card--activity">
          <MetricCardIcon variant="activity" />
          <span>Upcoming follow-up appointments</span>
          <strong>{recentFollowUpAppointments.length}</strong>
        </div>
      </div>

      {actionMessage ? (
        <p
          className={isError ? 'form-status form-status--error' : 'form-status form-status--success'}
          role={isError ? 'alert' : 'status'}
        >
          {actionMessage}
        </p>
      ) : null}

      {loadError ? <p className="login-error">Unable to load taskboard: {loadError}</p> : null}

      {/* ── follow-up tasks from care plans ── */}
      <section className="careplan-list-panel">
        <SectionHeader
          title="Follow-up tasks"
          subtitle="Tasks generated from teleconsultation completions requiring coordinator action."
          action={
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <LabelWithIcon icon="status">
                <span style={{ fontSize: '0.8rem' }}>Filter</span>
              </LabelWithIcon>
              <select
                style={{ fontSize: '0.85rem', padding: '0.25rem 0.5rem' }}
                value={taskFilter}
                onChange={(e) => setTaskFilter(e.target.value as typeof taskFilter)}
              >
                <option value="ALL">All types</option>
                <option value="FOLLOW_UP">Review tasks</option>
                <option value="FOLLOW_UP_APPOINTMENT">Appointment tasks</option>
              </select>
              <button
                type="button"
                className="secondary-button"
                style={{ fontSize: '0.8rem', padding: '0.25rem 0.75rem' }}
                onClick={() => void loadData({ value: true })}
              >
                Refresh
              </button>
            </div>
          }
        />

        {loading ? <p>Loading tasks…</p> : null}

        {!loading && visibleFollowUpPlans.length === 0 ? (
          <p className="patient-appointments-empty">
            No follow-up tasks found.{' '}
            {taskFilter !== 'ALL' ? 'Try changing the filter.' : 'Tasks appear here after teleconsultation sessions are completed.'}
          </p>
        ) : null}

        <div className="careplan-list careplan-list--paged">
          {visibleFollowUpPlans.map(({ plan, followUpTasks }) => (
            <article key={plan.id} className="careplan-item careplan-item--tasks">
              <div className="careplan-item-header">
                <strong>{plan.goal}</strong>
                <span className="careplan-item-meta">
                  Patient: {plan.patientId} · Owner: {plan.ownerId} · v{plan.version}
                </span>
              </div>

              <ul className="followup-task-list">
                {followUpTasks.map((task) => {
                  const key = plan.id + task.raw
                  const isLoading = completing === key
                  return (
                    <li key={task.raw} className={`followup-task-item followup-task-item--${task.type.toLowerCase()}`}>
                      <div className="followup-task-content">
                        <span className={`badge ${task.type === 'FOLLOW_UP_APPOINTMENT' ? 'badge--appointment' : 'badge--review'}`}>
                          {task.type === 'FOLLOW_UP_APPOINTMENT' ? 'Appointment' : 'Review'}
                        </span>
                        <span className="followup-task-label">{task.label}</span>
                      </div>
                      <button
                        type="button"
                        className="secondary-button followup-task-done-btn"
                        disabled={isLoading || !!completing}
                        onClick={() => void handleMarkTaskDone(plan, task.raw)}
                      >
                        {isLoading ? 'Saving…' : 'Mark done'}
                      </button>
                    </li>
                  )
                })}
              </ul>
            </article>
          ))}
        </div>
      </section>

      {/* ── teleconsult alerts ── */}
      <section className="careplan-list-panel" style={{ marginTop: '1.5rem' }}>
        <SectionHeader
          title="Teleconsult alerts"
          subtitle="Active alerts raised by critical findings or missing follow-up dates."
          action={
            <select
              style={{ fontSize: '0.85rem', padding: '0.25rem 0.5rem' }}
              value={alertFilter}
              onChange={(e) => setAlertFilter(e.target.value as typeof alertFilter)}
            >
              <option value="OPEN">Open only</option>
              <option value="ALL">All statuses</option>
            </select>
          }
        />

        {!loading && visibleAlerts.length === 0 ? (
          <p className="patient-appointments-empty">
            {alertFilter === 'OPEN' ? 'No open teleconsult alerts.' : 'No teleconsult alerts found.'}
          </p>
        ) : null}

        <div className="careplan-list careplan-list--paged">
          {visibleAlerts.map((alert) => (
            <article key={alert.id} className="careplan-item careplan-item--alert">
              <div className="careplan-item-header">
                <span className={alertBadgeClass(alert.triggerType)}>{alertLabel(alert.triggerType)}</span>
                <span className="careplan-item-meta">
                  Patient: {alert.patientId} · Status: {alert.status} · Severity: {alert.severity}
                </span>
              </div>
              <p className="followup-task-label">{alert.summary}</p>
            </article>
          ))}
        </div>
      </section>

      {/* ── upcoming follow-up appointments ── */}
      <section className="careplan-list-panel" style={{ marginTop: '1.5rem' }}>
        <SectionHeader
          title="Upcoming follow-up appointments"
          subtitle="Booked appointments scheduled from today onward."
        />

        {!loading && recentFollowUpAppointments.length === 0 ? (
          <p className="patient-appointments-empty">No upcoming follow-up appointments found.</p>
        ) : null}

        <div className="careplan-list careplan-list--paged">
          {recentFollowUpAppointments.map((appt) => (
            <article key={appt.id} className="careplan-item careplan-item--appointment">
              <div className="careplan-item-header">
                <strong>{new Date(appt.scheduledAt).toLocaleString()}</strong>
                <span className="badge badge--appointment">{appt.channel}</span>
              </div>
              <span className="careplan-item-meta">
                Patient: {appt.patientId} · Provider: {appt.providerId} · Status: {appt.status}
              </span>
            </article>
          ))}
        </div>
      </section>
    </Card>
  )
}
