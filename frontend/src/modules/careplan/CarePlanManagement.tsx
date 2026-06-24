import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { Card } from '../../components/Card'
import { LabelWithIcon } from '../../components/LabelWithIcon'
import { MetricCardIcon } from '../../components/MetricCardIcon'
import { SectionHeader } from '../../components/SectionHeader'
import { createCarePlan, listCarePlans, listPatients, updateCarePlan, type CarePlanResponse } from '../../services/platformApi'

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

function inferOwnerId(claims: Record<string, unknown> | null): string {
  if (!claims) {
    return ''
  }

  const candidates = [
    claims.ownerId,
    claims.owner_id,
    claims.preferred_username,
    claims.email,
    claims.name,
    claims.sub,
  ]

  const match = candidates.find((value) => typeof value === 'string' && value.length > 0)
  return match ? String(match) : ''
}

function formatTaskSummary(tasks: string[]) {
  if (tasks.length === 0) {
    return 'No tasks defined'
  }

  if (tasks.length === 1) {
    return tasks[0]
  }

  return `${tasks[0]} +${tasks.length - 1} more`
}

export function CarePlanManagement() {
  const { session } = useAuth()
  const [carePlans, setCarePlans] = useState<CarePlanResponse[]>([])
  const [carePlanPage, setCarePlanPage] = useState(0)
  const [selectedPlanId, setSelectedPlanId] = useState('')
  const [patientId, setPatientId] = useState('')
  const [patientOptions, setPatientOptions] = useState<string[]>([])
  const [goal, setGoal] = useState('Improve care coordination')
  const [ownerId, setOwnerId] = useState('')
  const [planStatus, setPlanStatus] = useState('ACTIVE')
  const [tasks, setTasks] = useState('Review patient intake\nConfirm appointment\nShare follow-up plan')
  const [expectedVersion, setExpectedVersion] = useState(1)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [patientFilter, setPatientFilter] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [isError, setIsError] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)

  const latestPlan = useMemo(() => carePlans[0] ?? null, [carePlans])
  const inferredOwnerId = useMemo(() => {
    if (!session) {
      return ''
    }

    const claims = decodeJwtPayload(session.idToken ?? session.accessToken)
    return inferOwnerId(claims)
  }, [session])
  const filteredCarePlans = useMemo(() => {
    return carePlans.filter((carePlan) => {
      const matchesStatus = statusFilter === 'ALL' || carePlan.planStatus === statusFilter
      const matchesPatient = !patientFilter.trim() || carePlan.patientId.toLowerCase().includes(patientFilter.trim().toLowerCase())
      return matchesStatus && matchesPatient
    })
  }, [carePlans, patientFilter, statusFilter])

  const carePlansPerPage = 4
  const totalCarePlanPages = Math.max(1, Math.ceil(filteredCarePlans.length / carePlansPerPage))
  const visibleCarePlans = filteredCarePlans.slice(carePlanPage * carePlansPerPage, (carePlanPage + 1) * carePlansPerPage)

  useEffect(() => {
    setCarePlanPage((current) => Math.min(current, Math.max(0, totalCarePlanPages - 1)))
  }, [totalCarePlanPages])

  const selectedPlan = useMemo(() => carePlans.find((carePlan) => carePlan.id === selectedPlanId) ?? null, [carePlans, selectedPlanId])
  const carePlanMetricStatus = (latestPlan?.planStatus ?? 'NONE').toLowerCase()

  useEffect(() => {
    if (!ownerId && inferredOwnerId) {
      setOwnerId(inferredOwnerId)
    }
  }, [inferredOwnerId, ownerId])

  useEffect(() => {
    let active = true

    async function loadData() {
      if (!session) {
        return
      }

      setLoading(true)
      try {
        const [returnedPlans, patients] = await Promise.all([
          listCarePlans(session.accessToken),
          listPatients(session.accessToken),
        ])
        if (active) {
          setCarePlans(returnedPlans)
          setPatientOptions(
            Array.from(
              new Set(
                patients
                  .map((patient) => String(patient.id ?? patient.externalReference ?? patient.patientId ?? ''))
                  .filter((value) => value.length > 0),
              ),
            ).sort((left, right) => left.localeCompare(right)),
          )
          setLoadError(null)
        }
      } catch (cause) {
        if (!active) {
          return
        }
        const message = cause instanceof Error ? cause.message : 'Care plans are temporarily unavailable. Please refresh in a moment.'
        setLoadError(message)
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

  useEffect(() => {
    if (!selectedPlan) {
      return
    }

    setGoal(selectedPlan.goal)
    setOwnerId(selectedPlan.ownerId)
    setPlanStatus(selectedPlan.planStatus)
    setTasks(selectedPlan.tasks.join('\n'))
    setExpectedVersion(selectedPlan.version)
    setPatientId(selectedPlan.patientId)
  }, [selectedPlan])

  async function handleCreateCarePlan() {
    if (!session) {
      setIsError(true)
      setActionMessage('Please sign in before creating a care plan.')
      return
    }

    if (!patientId.trim() || !goal.trim() || !ownerId.trim()) {
      setIsError(true)
      setActionMessage('Patient ID, goal, and owner ID are required.')
      return
    }

    const taskList = tasks
      .split('\n')
      .map((task) => task.trim())
      .filter(Boolean)

    setSubmitting(true)
    setIsError(false)
    setActionMessage(null)

    try {
      await createCarePlan(
        {
          patientId: patientId.trim(),
          goal: goal.trim(),
          planStatus,
          ownerId: ownerId.trim(),
          tasks: taskList,
        },
        session.accessToken,
      )

      const returnedPlans = await listCarePlans(session.accessToken)
      setCarePlans(returnedPlans)
      setActionMessage('Care plan created successfully.')
      setTasks('')
      setPatientId('')
      setGoal('Improve care coordination')
      setOwnerId(inferredOwnerId || '')
      setPlanStatus('ACTIVE')
      setCreateOpen(false)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'We could not create the care plan right now. Please review the details and try again.'
      setIsError(true)
      setActionMessage(message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleUpdateCarePlan() {
    if (!session) {
      setIsError(true)
      setActionMessage('Please sign in before updating a care plan.')
      return
    }

    if (!selectedPlan) {
      setIsError(true)
      setActionMessage('Select a care plan to update.')
      return
    }

    if (!goal.trim() || !ownerId.trim()) {
      setIsError(true)
      setActionMessage('Goal and owner ID are required.')
      return
    }

    const taskList = tasks
      .split('\n')
      .map((task) => task.trim())
      .filter(Boolean)

    setSubmitting(true)
    setIsError(false)
    setActionMessage(null)

    try {
      await updateCarePlan(
        selectedPlan.id,
        {
          goal: goal.trim(),
          planStatus,
          ownerId: ownerId.trim(),
          tasks: taskList,
          expectedVersion,
        },
        session.accessToken,
      )

      const returnedPlans = await listCarePlans(session.accessToken)
      setCarePlans(returnedPlans)
      setActionMessage('Care plan updated successfully.')
      setSelectedPlanId('')
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'We could not save changes to this care plan right now. Please try again.'
      setIsError(true)
      setActionMessage(message)
    } finally {
      setSubmitting(false)
    }
  }

  function loadPlanIntoEditor(carePlan: CarePlanResponse) {
    setSelectedPlanId(carePlan.id)
    setPatientId(carePlan.patientId)
    setGoal(carePlan.goal)
    setOwnerId(carePlan.ownerId)
    setPlanStatus(carePlan.planStatus)
    setTasks(carePlan.tasks.join('\n'))
    setExpectedVersion(carePlan.version)
  }

  return (
    <Card title="Care plan management" eyebrow="Coordinator workspace">
      <div className="careplan-summary-row">
        <div className="metric-card metric-card--volume">
          <MetricCardIcon variant="volume" />
          <span>Total plans</span>
          <strong>{carePlans.length}</strong>
        </div>
        <div className="metric-card metric-card--narrative">
          <MetricCardIcon variant="narrative" />
          <span>Latest plan</span>
          <strong>{latestPlan ? latestPlan.goal : 'None yet'}</strong>
        </div>
        <div className={`metric-card metric-card--status metric-card--status-${carePlanMetricStatus}`}>
          <MetricCardIcon variant="status" />
          <span>Status</span>
          <strong>{latestPlan ? latestPlan.planStatus : 'Not available'}</strong>
        </div>
      </div>

      <div className="field-grid careplan-filter-grid stacked-form">
        <label className="field-block">
          <LabelWithIcon icon="status">Filter by status</LabelWithIcon>
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="PAUSED">Paused</option>
            <option value="CLOSED">Closed</option>
          </select>
        </label>
        <label className="field-block">
          <LabelWithIcon icon="patient">Filter by patient</LabelWithIcon>
          <input value={patientFilter} placeholder="pat-1001" onChange={(event) => setPatientFilter(event.target.value)} />
        </label>
      </div>

      {loadError ? <p className="login-error">We could not load care plans right now: {loadError}</p> : null}
      {actionMessage ? (
        <p className={isError ? 'form-status form-status--error' : 'form-status form-status--success'} role={isError ? 'alert' : 'status'}>
          {actionMessage}
        </p>
      ) : null}

      <section className="careplan-list-panel">
        <SectionHeader
          title="Care plan list"
          subtitle="Active, paused, and closed plans for your current filters."
          action={
            <button
              type="button"
              className="primary-button careplan-create-button"
              onClick={() => {
                setCreateOpen((o) => !o)
                setSelectedPlanId('')
              }}
            >
              {createOpen ? 'Cancel' : 'Create plan'}
            </button>
          }
        />

        {createOpen ? (
          <section className="careplan-quick-create stacked-form">
            <div className="field-grid">
              <label className="field-block">
                <LabelWithIcon icon="patient">Patient ID</LabelWithIcon>
                {patientOptions.length > 0 ? (
                  <>
                    <input
                      list="careplan-patient-options"
                      value={patientId}
                      placeholder="pat-1001"
                      onChange={(event) => setPatientId(event.target.value)}
                    />
                    <datalist id="careplan-patient-options">
                      {patientOptions.map((option) => (
                        <option key={option} value={option} />
                      ))}
                    </datalist>
                  </>
                ) : (
                  <input value={patientId} placeholder="pat-1001" onChange={(event) => setPatientId(event.target.value)} />
                )}
              </label>
              <label className="field-block">
                <LabelWithIcon icon="owner">Care coordinator ID</LabelWithIcon>
                <input value={ownerId} placeholder="coordinator-01" onChange={(event) => setOwnerId(event.target.value)} />
              </label>
            </div>

            <div className="field-grid">
              <label className="field-block">
                <LabelWithIcon icon="goal">Goal</LabelWithIcon>
                <input value={goal} onChange={(event) => setGoal(event.target.value)} />
              </label>
              <label className="field-block">
                <LabelWithIcon icon="status">Care plan status</LabelWithIcon>
                <select value={planStatus} onChange={(event) => setPlanStatus(event.target.value)}>
                  <option value="ACTIVE">Active</option>
                  <option value="PAUSED">Paused</option>
                  <option value="CLOSED">Closed</option>
                </select>
              </label>
            </div>

            <label className="field-block">
              <LabelWithIcon icon="tasks">Tasks</LabelWithIcon>
              <textarea value={tasks} onChange={(event) => setTasks(event.target.value)} />
            </label>

            <div className="form-actions">
              <button type="button" className="secondary-button" onClick={() => setCreateOpen(false)}>
                Cancel
              </button>
              <button
                type="button"
                className="primary-button careplan-create-button"
                onClick={() => void handleCreateCarePlan()}
                disabled={submitting}
              >
                {submitting ? 'Saving...' : 'Create plan'}
              </button>
            </div>
          </section>
        ) : null}

        {selectedPlanId ? (
          <section className="careplan-quick-create careplan-edit-panel stacked-form">
            <h4>Editing care plan: {selectedPlan?.goal}</h4>
            <div className="field-grid">
              <label className="field-block">
                <LabelWithIcon icon="goal">Goal</LabelWithIcon>
                <input value={goal} onChange={(event) => setGoal(event.target.value)} />
              </label>
              <label className="field-block">
                <LabelWithIcon icon="owner">Care coordinator ID</LabelWithIcon>
                <input value={ownerId} onChange={(event) => setOwnerId(event.target.value)} />
              </label>
            </div>

            <div className="field-grid">
              <label className="field-block">
                <LabelWithIcon icon="status">Care plan status</LabelWithIcon>
                <select value={planStatus} onChange={(event) => setPlanStatus(event.target.value)}>
                  <option value="ACTIVE">Active</option>
                  <option value="PAUSED">Paused</option>
                  <option value="CLOSED">Closed</option>
                </select>
              </label>
              <label className="field-block">
                <LabelWithIcon icon="version">Version</LabelWithIcon>
                <input type="number" min="1" value={expectedVersion} onChange={(event) => setExpectedVersion(Number(event.target.value) || 1)} />
              </label>
            </div>

            <label className="field-block">
              <LabelWithIcon icon="tasks">Tasks</LabelWithIcon>
              <textarea value={tasks} onChange={(event) => setTasks(event.target.value)} />
            </label>

            <div className="form-actions">
              <button type="button" className="secondary-button" onClick={() => setSelectedPlanId('')}>
                Cancel
              </button>
              <button type="button" className="primary-button" onClick={() => void handleUpdateCarePlan()} disabled={submitting || !selectedPlan}>
                {submitting ? 'Updating...' : 'Save changes'}
              </button>
            </div>
          </section>
        ) : null}

        {loading ? <p>Loading care plans...</p> : null}
        {!loading && filteredCarePlans.length === 0 ? <p className="patient-appointments-empty">No care plans match your current filters.</p> : null}
        {!loading && filteredCarePlans.length > 0 && totalCarePlanPages > 1 ? (
          <div className="carousel-controls" aria-label="Care plan navigation">
            <button
              type="button"
              className={`${carePlanPage >= totalCarePlanPages - 1 ? 'primary-button' : 'secondary-button'} carousel-button`}
              onClick={() => setCarePlanPage((page) => Math.max(0, page - 1))}
              disabled={carePlanPage === 0}
            >
              <span aria-hidden="true">&lt;</span> Prev
            </button>
            <button
              type="button"
              className={`${carePlanPage < totalCarePlanPages - 1 ? 'primary-button' : 'secondary-button'} carousel-button`}
              onClick={() => setCarePlanPage((page) => Math.min(totalCarePlanPages - 1, page + 1))}
              disabled={carePlanPage >= totalCarePlanPages - 1}
            >
              Next <span aria-hidden="true">&gt;</span>
            </button>
          </div>
        ) : null}
        <div className="careplan-list careplan-list--paged">
          {visibleCarePlans.map((carePlan) => (
            <article key={carePlan.id} className="careplan-item careplan-item--management">
              <strong className="careplan-item-title">{carePlan.goal}</strong>
              <div className="careplan-item-body">
                <span>Plan ID: {carePlan.id}</span>
                <span>Patient: {carePlan.patientId}</span>
                <span>Owner: {carePlan.ownerId}</span>
                <span>Status: {carePlan.planStatus}</span>
                <span>Version: {carePlan.version}</span>
                <span>Tasks: {formatTaskSummary(carePlan.tasks)}</span>
              </div>
              <div className="button-row careplan-item-actions">
                <button
                  type="button"
                  className="secondary-button careplan-item-edit-button"
                  onClick={() => {
                    loadPlanIntoEditor(carePlan)
                    setCreateOpen(false)
                  }}
                >
                  Edit
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>
    </Card>
  )
}
