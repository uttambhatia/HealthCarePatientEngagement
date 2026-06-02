import type { ReactNode } from 'react'
import { Card } from '../components/Card'
import { SectionHeader } from '../components/SectionHeader'
import { CarePlanManagement } from '../modules/careplan/CarePlanManagement'
import { NotificationsPanel } from '../modules/notification/NotificationsPanel'
import { TeleconsultationWorkspace } from '../modules/teleconsultation/TeleconsultationWorkspace'
import type { Role } from '../utils/roleUtils'

type CoordinatorLandingPageProps = {
  role: Role
}

type MetricVariant = 'volume' | 'activity' | 'narrative' | 'status'

function CoordinatorMetric({ label, value, variant }: { label: string; value: string; variant: MetricVariant }) {
  return (
    <div className={`metric-card metric-card--${variant}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function CoordinatorModuleGrid({ children }: { children: ReactNode }) {
  return <div className="dashboard-grid dashboard-grid--primary">{children}</div>
}

export function CoordinatorLandingPage({ role }: CoordinatorLandingPageProps) {
  return (
    <div className="workspace-stack coordinator-landing">
      <section className="role-spotlight card coordinator-spotlight">
        <div className="role-spotlight-copy">
          <p className="eyebrow">{role} landing</p>
          <h2>Care coordination command center</h2>
          <p>
            Coordinate patient flow, activate care plans, and keep the team aligned on the same operational timeline.
          </p>
          <div className="pill-row">
            <span className="pill">Care plan orchestration</span>
            <span className="pill">Appointment handoffs</span>
            <span className="pill">Realtime follow-up visibility</span>
          </div>
        </div>
        <div className="role-spotlight-side">
          <CoordinatorMetric label="Primary workflow" value="Care plans" variant="narrative" />
          <CoordinatorMetric label="Operating focus" value="Scheduling + follow-up" variant="volume" />
          <CoordinatorMetric label="Support channel" value="Notifications live" variant="activity" />
        </div>
      </section>

      <section className="role-route-grid">
        <Card title="Coordinator overview" eyebrow="Landing page" centeredHeader>
          <SectionHeader
            title="What the coordinator sees first"
            subtitle="This landing page keeps care-plan management at the top and keeps secondary tools one level below it."
          />
          <CoordinatorModuleGrid>
            <div className="module-slot module-slot--featured">
              <CarePlanManagement />
            </div>
          </CoordinatorModuleGrid>
        </Card>

        <Card title="Supporting workspace" eyebrow="Secondary coordination tools">
          <SectionHeader
            title="Operational support"
            subtitle="Notifications and teleconsultation remain available, but they no longer compete with the coordinator landing page workflow."
          />
          <div className="dashboard-grid dashboard-grid--support">
            <div className="module-slot">
              <NotificationsPanel />
            </div>
            <div className="module-slot">
              <TeleconsultationWorkspace />
            </div>
          </div>
        </Card>
      </section>
    </div>
  )
}
