import type { ReactNode } from 'react'
import { Card } from './Card'
import { MetricCardIcon, type MetricVariant } from './MetricCardIcon'
import { SectionHeader } from './SectionHeader'
import { AppointmentBooking } from '../modules/appointment/AppointmentBooking'
import { AdminDashboard } from '../modules/admin/AdminDashboard'
import { IotMonitoringDashboard } from '../modules/iot/IotMonitoringDashboard'
import { NotificationsPanel } from '../modules/notification/NotificationsPanel'
import { PatientDashboard } from '../modules/patient/PatientDashboard'
import { TeleconsultationWorkspace } from '../modules/teleconsultation/TeleconsultationWorkspace'
import type { Role } from '../utils/roleUtils'

type WorkspaceModule = {
  key: string
  node: ReactNode
}

type RoleBlueprint = {
  title: string
  summary: string
  focus: string[]
  metrics: Array<{ label: string; value: string; variant?: MetricVariant }>
  primary: WorkspaceModule[]
  supporting: WorkspaceModule[]
}

const roleBlueprints: Record<Role, RoleBlueprint> = {
  PATIENT: {
    title: 'Patient journey center',
    summary: 'Keep appointments, follow-ups, and care reminders in one clear path so patients always know the next step.',
    focus: ['Book the next visit', 'Review realtime updates', 'Stay engaged between care moments'],
    metrics: [
      { label: 'Next best action', value: 'Book follow-up', variant: 'status' },
      { label: 'Care continuity', value: 'Notifications live', variant: 'activity' },
      { label: 'Support channel', value: 'Teleconsultation ready', variant: 'narrative' },
    ],
    primary: [
      { key: 'patient', node: <PatientDashboard /> },
    ],
    supporting: [
      { key: 'notifications', node: <NotificationsPanel /> },
      { key: 'teleconsultation', node: <TeleconsultationWorkspace /> },
    ],
  },
  DOCTOR: {
    title: 'Clinical command center',
    summary: 'Track consultations and keep an eye on live telemetry without losing context.',
    focus: ['Review patient context', 'Handle teleconsultations', 'Escalate from telemetry'],
    metrics: [
      { label: 'Primary workflow', value: 'Patient care' },
      { label: 'Clinical control', value: 'Teleconsultation' },
      { label: 'Remote coverage', value: 'Telehealth + IoT' },
    ],
    primary: [
      { key: 'patient', node: <PatientDashboard /> },
    ],
    supporting: [
      { key: 'telemetry', node: <IotMonitoringDashboard /> },
      { key: 'notifications', node: <NotificationsPanel /> },
      { key: 'teleconsultation', node: <TeleconsultationWorkspace /> },
    ],
  },
  COORDINATOR: {
    title: 'Care coordination hub',
    summary: 'Orchestrate intake, bookings, and care-pathway handoffs while keeping all teams aligned to the same timeline.',
    focus: ['Coordinate appointments', 'Manage pathways', 'Monitor queue health'],
    metrics: [
      { label: 'Operations', value: 'Scheduling' },
      { label: 'Orchestration', value: 'Care plans' },
      { label: 'Visibility', value: 'Realtime feed' },
    ],
    primary: [
      { key: 'patient', node: <PatientDashboard /> },
      { key: 'appointments', node: <AppointmentBooking /> },
    ],
    supporting: [
      { key: 'notifications', node: <NotificationsPanel /> },
      { key: 'teleconsultation', node: <TeleconsultationWorkspace /> },
      { key: 'admin', node: <AdminDashboard /> },
    ],
  },
  ADMIN: {
    title: 'Operational governance center',
    summary: 'See service posture, policy visibility, and platform health from one control surface designed for governance work.',
    focus: ['Watch service posture', 'Inspect IoT health', 'Audit governance workflows'],
    metrics: [
      { label: 'Platform lens', value: 'Governance' },
      { label: 'Risk posture', value: 'Telemetry + alerts' },
      { label: 'Control surface', value: 'Policy and ops' },
    ],
    primary: [
      { key: 'admin', node: <AdminDashboard /> },
    ],
    supporting: [
      { key: 'telemetry', node: <IotMonitoringDashboard /> },
      { key: 'notifications', node: <NotificationsPanel /> },
    ],
  },
}

type RoleWorkspaceProps = {
  role: Role
}

function resolveMetricVariant(label: string): MetricVariant {
  const normalized = label.toLowerCase()
  if (normalized.includes('status') || normalized.includes('state')) {
    return 'status'
  }

  if (
    normalized.includes('total')
    || normalized.includes('count')
    || normalized.includes('appointments')
    || normalized.includes('plans')
    || normalized.includes('patients')
    || normalized.includes('volume')
  ) {
    return 'volume'
  }

  if (
    normalized.includes('active')
    || normalized.includes('monitor')
    || normalized.includes('telemetry')
    || normalized.includes('live')
    || normalized.includes('response')
    || normalized.includes('latency')
    || normalized.includes('uptime')
  ) {
    return 'activity'
  }

  return 'narrative'
}

export function RoleWorkspace({ role }: RoleWorkspaceProps) {
  const blueprint = roleBlueprints[role]

  return (
    <div className="workspace-stack">
      <section className="role-route-grid">
        <Card title="Primary workflow" eyebrow="Start here" centeredHeader>
          <SectionHeader
            title={blueprint.title}
            subtitle={blueprint.summary}
          />
          <div className="dashboard-grid dashboard-grid--primary">
            {blueprint.primary.map(({ key, node }) => (
              <div key={key} className="module-slot module-slot--featured">{node}</div>
            ))}
          </div>
        </Card>

        <Card title="Supporting tools" eyebrow="Secondary capabilities">
          <SectionHeader
            title="Workspace support"
            subtitle="Useful modules remain available, but they no longer compete visually with the main role-specific workflow."
          />
          <div className="dashboard-grid dashboard-grid--support">
            {blueprint.supporting.map(({ key, node }) => (
              <div key={key} className="module-slot">{node}</div>
            ))}
          </div>
        </Card>
      </section>

      <section className="role-spotlight card">
        <div className="role-spotlight-copy">
          <p className="eyebrow">At a glance</p>
          <h2>{blueprint.title}</h2>
          <p>{blueprint.summary}</p>
          <div className="pill-row">
            {blueprint.focus.map((item) => (
              <span key={item} className="pill">{item}</span>
            ))}
          </div>
        </div>
        <div className="role-spotlight-side">
          {blueprint.metrics.map((metric) => {
            const variant = metric.variant ?? resolveMetricVariant(metric.label)

            return (
              <div key={metric.label} className={`metric-card metric-card--${variant}`}>
                <MetricCardIcon variant={variant} />
                <span>{metric.label}</span>
                <strong>{metric.value}</strong>
              </div>
            )
          })}
        </div>
      </section>
    </div>
  )
}