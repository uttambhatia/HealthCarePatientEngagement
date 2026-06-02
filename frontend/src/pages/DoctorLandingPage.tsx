import type { ReactNode } from 'react'
import { Card } from '../components/Card'
import { SectionHeader } from '../components/SectionHeader'
import { TeleconsultationManagement } from '../modules/teleconsultation/TeleconsultationManagement'
import { IotMonitoringDashboard } from '../modules/iot/IotMonitoringDashboard'
import { NotificationsPanel } from '../modules/notification/NotificationsPanel'
import type { Role } from '../utils/roleUtils'

type DoctorLandingPageProps = {
  role: Role
}

type MetricVariant = 'volume' | 'activity' | 'narrative' | 'status'

function DoctorMetric({ label, value, variant }: { label: string; value: string; variant: MetricVariant }) {
  return (
    <div className={`metric-card metric-card--${variant}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function DoctorModuleGrid({ children }: { children: ReactNode }) {
  return <div className="dashboard-grid dashboard-grid--primary">{children}</div>
}

export function DoctorLandingPage({ role }: DoctorLandingPageProps) {
  return (
    <div className="workspace-stack doctor-landing">
      <section className="role-spotlight card doctor-spotlight">
        <div className="role-spotlight-copy">
          <p className="eyebrow">{role} landing</p>
          <h2>Clinical command center</h2>
          <p>
            Manage patient consultations and provide remote care with integrated tele-consultation and real-time patient telemetry monitoring.
          </p>
          <div className="pill-row">
            <span className="pill">Tele-consultation sessions</span>
            <span className="pill">Patient telemetry oversight</span>
          </div>
        </div>
        <div className="role-spotlight-side">
          <DoctorMetric label="Primary workflow" value="Tele-consultation" variant="narrative" />
          <DoctorMetric label="Clinical focus" value="Patient care" variant="volume" />
          <DoctorMetric label="Remote monitoring" value="Telemetry live" variant="activity" />
        </div>
      </section>

      <section className="role-route-grid">
        <Card title="Doctor workflow" eyebrow="Landing page" centeredHeader>
          
          <DoctorModuleGrid>
            <div className="module-slot module-slot--featured">
              <TeleconsultationManagement />
            </div>
          </DoctorModuleGrid>
        </Card>

        <Card title="Supporting tools" eyebrow="Clinical support">
          <SectionHeader
            title="Operational context"
            subtitle="Telemetry and notifications support the primary tele-consultation workflow."
          />
          <div className="dashboard-grid dashboard-grid--support">
            <div className="module-slot">
              <IotMonitoringDashboard />
            </div>
            <div className="module-slot">
              <NotificationsPanel />
            </div>
          </div>
        </Card>
      </section>
    </div>
  )
}
