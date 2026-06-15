import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { Card } from '../../components/Card'
import {
  getCarePlanResponsibility,
  listTelemetryMetricTypes,
  listTelemetryByPatient,
  type CarePlanResponsibilityResponse,
  type TelemetryResponse,
} from '../../services/platformApi'

function toIsoFromLocalDateTime(value: string) {
  if (!value) {
    return undefined
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return undefined
  }
  return date.toISOString()
}

function averageMetric(rows: TelemetryResponse[]) {
  const values = rows
    .map((row) => Number(row.metricValue))
    .filter((value) => Number.isFinite(value))
  if (values.length === 0) {
    return null
  }
  return (values.reduce((sum, value) => sum + value, 0) / values.length).toFixed(1)
}

function buildPolylinePoints(rows: TelemetryResponse[]) {
  const numeric = rows
    .slice()
    .sort((a, b) => a.recordedAt.localeCompare(b.recordedAt))
    .map((row) => Number(row.metricValue))
    .filter((value) => Number.isFinite(value))

  if (numeric.length < 2) {
    return ''
  }

  const max = Math.max(...numeric)
  const min = Math.min(...numeric)
  const range = Math.max(1, max - min)
  const width = 320
  const height = 96

  return numeric
    .map((value, index) => {
      const x = (index / (numeric.length - 1)) * width
      const y = height - ((value - min) / range) * height
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

export function IotMonitoringDashboard() {
  const { session } = useAuth()
  const [patientId, setPatientId] = useState('pat-seed-1001')
  const [metricType, setMetricType] = useState('')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [metricTypeOptions, setMetricTypeOptions] = useState<string[]>([])
  const [telemetryRows, setTelemetryRows] = useState<TelemetryResponse[]>([])
  const [monitoringOwner, setMonitoringOwner] = useState<string>('Unassigned')
  const [responsibility, setResponsibility] = useState<CarePlanResponsibilityResponse | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const trendPoints = useMemo(() => buildPolylinePoints(telemetryRows), [telemetryRows])
  const avgMetricValue = useMemo(() => averageMetric(telemetryRows), [telemetryRows])

  async function loadTelemetryAndOwner() {
    if (!session || !patientId.trim()) {
      return
    }

    setIsLoading(true)
    try {
      const [telemetry, apiMetricTypes] = await Promise.all([
        listTelemetryByPatient(patientId.trim(), {
          metricType: metricType.trim() || undefined,
          startTime: toIsoFromLocalDateTime(startTime),
          endTime: toIsoFromLocalDateTime(endTime),
          token: session.accessToken,
        }),
        listTelemetryMetricTypes(patientId.trim(), session.accessToken),
      ])

      let ownerId = 'Unassigned'
      let responsibilityData: CarePlanResponsibilityResponse | null = null
      try {
        const responsibility = await getCarePlanResponsibility(patientId.trim(), session.accessToken)
        responsibilityData = responsibility
        if (responsibility.ownerId) {
          ownerId = responsibility.ownerId
        }
      } catch {
        ownerId = 'Unassigned'
        responsibilityData = null
      }

      setTelemetryRows(telemetry)
      const discoveredFromRows = telemetry
        .map((row) => row.metricType.trim())
        .filter((value) => value.length > 0)
      const nextMetricTypeOptions = Array.from(new Set([...apiMetricTypes, ...discoveredFromRows]))
        .sort((a, b) => a.localeCompare(b))
      setMetricTypeOptions(nextMetricTypeOptions)
      setMonitoringOwner(ownerId)
      setResponsibility(responsibilityData)
      setLoadError(null)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Remote monitoring data is temporarily unavailable. Please try again shortly.'
      setLoadError(message)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    void loadTelemetryAndOwner()
    // Load defaults once session is available.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session])

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void loadTelemetryAndOwner()
  }

  return (
    <Card title="Remote monitoring" eyebrow="Vitals and alerts">
      <form className="stacked-form monitoring-filter-grid" onSubmit={handleSubmit}>
        <label>
          Patient ID
          <input value={patientId} onChange={(event) => setPatientId(event.target.value)} placeholder="pat-1001" />
        </label>
        <label>
          Metric type
          <select value={metricType} onChange={(event) => setMetricType(event.target.value)}>
            <option value="">All metrics</option>
            {metricTypeOptions.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
        <label>
          Start time
          <input type="datetime-local" value={startTime} onChange={(event) => setStartTime(event.target.value)} />
        </label>
        <label>
          End time
          <input type="datetime-local" value={endTime} onChange={(event) => setEndTime(event.target.value)} />
        </label>
        <div className="form-actions">
          <button type="submit" className="primary-button" disabled={isLoading}>
            {isLoading ? 'Loading...' : 'Load trend'}
          </button>
        </div>
        <small className="monitoring-metric-hint">
          Metric list is loaded from telemetry data in the backend for the selected patient.
        </small>
      </form>

      <p>Monitoring updates available: {telemetryRows.length}</p>
      <p>Responsible coordinator: <strong>{monitoringOwner}</strong></p>
      <div className="responsibility-meta-grid">
        <div>
          <strong>{responsibility?.carePlanId ?? '--'}</strong>
          <span>Care plan ID</span>
        </div>
        <div>
          <strong>{responsibility?.planStatus ?? '--'}</strong>
          <span>Plan status</span>
        </div>
        <div>
          <strong>{responsibility?.version ?? '--'}</strong>
          <span>Plan version</span>
        </div>
      </div>
      {avgMetricValue ? <p>Average {metricType || 'metric'} value: {avgMetricValue}</p> : null}

      {trendPoints ? (
        <div className="telemetry-trend-chart" role="img" aria-label="Telemetry trend chart">
          <svg viewBox="0 0 320 96" preserveAspectRatio="none">
            <polyline points={trendPoints} />
          </svg>
        </div>
      ) : (
        <p>No trendable telemetry values were found for the selected filters.</p>
      )}

      {loadError ? <p>We could not load remote monitoring data right now: {loadError}</p> : null}
      <div className="stats-grid">
        <div><strong>{telemetryRows.length}</strong><span>Telemetry points in range</span></div>
        <div><strong>{monitoringOwner === 'Unassigned' ? '0' : '1'}</strong><span>Assigned monitoring owner</span></div>
        <div><strong>{avgMetricValue ?? '--'}</strong><span>Average metric value</span></div>
      </div>
    </Card>
  )
}
