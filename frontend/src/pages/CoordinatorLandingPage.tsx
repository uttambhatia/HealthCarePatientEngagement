import { useState, type ReactNode } from 'react'
import { Card } from '../components/Card'
import { MetricCardIcon, type MetricVariant } from '../components/MetricCardIcon'
import { CarePlanManagement } from '../modules/careplan/CarePlanManagement'
import { FollowUpTaskboard } from '../modules/careplan/FollowUpTaskboard'
import { NotificationsPanel } from '../modules/notification/NotificationsPanel'
import { TeleconsultationWorkspace } from '../modules/teleconsultation/TeleconsultationWorkspace'
import type { Role } from '../utils/roleUtils'

type CoordinatorPanel = 'careplan' | 'followup' | 'support'

type CoordinatorLandingPageProps = {
  role: Role
}

function CoordinatorMetric({ label, value, variant }: { label: string; value: string; variant: MetricVariant }) {
  return (
    <div className={`metric-card metric-card--${variant}`}>
      <MetricCardIcon variant={variant} />
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function CoordinatorModuleGrid({ children }: { children: ReactNode }) {
  return <div className="dashboard-grid dashboard-grid--primary">{children}</div>
}

export function CoordinatorLandingPage({ role }: CoordinatorLandingPageProps) {
  const [activePanel, setActivePanel] = useState<CoordinatorPanel>('careplan')
  const [isRightPaneMaximized, setIsRightPaneMaximized] = useState(false)
  const toggleRightPaneSize = () => setIsRightPaneMaximized((current) => !current)

  const panelOrder: CoordinatorPanel[] = ['careplan', 'followup', 'support']
  const panelLabels: Record<CoordinatorPanel, string> = {
    careplan: 'Care plan management',
    followup: 'Follow-up taskboard',
    support: 'Operational support',
  }

  const currentIndex = panelOrder.indexOf(activePanel)
  const nextPanel = panelOrder[(currentIndex + 1) % panelOrder.length]
  const prevPanel = panelOrder[(currentIndex - 1 + panelOrder.length) % panelOrder.length]

  return (
    <div className="workspace-stack coordinator-landing">
      <section className={`coordinator-workbench${isRightPaneMaximized ? ' coordinator-workbench--right-maximized' : ''}`}>
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

        <Card
          title={panelLabels[activePanel]}
          eyebrow={activePanel === 'support' ? 'Supporting tools' : 'Landing page'}
          subtitle={activePanel === 'support' ? 'Notifications and teleconsultation support the primary care-plan workflow.' : undefined}
          centeredHeader
          actions={(
            <div className="coordinator-panel-actions">
              <button
                type="button"
                className="primary-button coordinator-pane-size-toggle"
                onClick={toggleRightPaneSize}
                aria-label={isRightPaneMaximized ? 'Minimize right pane' : 'Maximize right pane'}
              >
                <span className="coordinator-pane-size-toggle-icon" aria-hidden="true">
                  {isRightPaneMaximized ? (
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-minimize-icon lucide-minimize">
                      <path d="M8 3v3a2 2 0 0 1-2 2H3" />
                      <path d="M21 8h-3a2 2 0 0 1-2-2V3" />
                      <path d="M3 16h3a2 2 0 0 1 2 2v3" />
                      <path d="M16 21v-3a2 2 0 0 1 2-2h3" />
                    </svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-maximize-icon lucide-maximize">
                      <path d="M8 3H5a2 2 0 0 0-2 2v3" />
                      <path d="M21 8V5a2 2 0 0 0-2-2h-3" />
                      <path d="M3 16v3a2 2 0 0 0 2 2h3" />
                      <path d="M16 21h3a2 2 0 0 0 2-2v-3" />
                    </svg>
                  )}
                </span>
                <span className="coordinator-pane-size-toggle-label">{isRightPaneMaximized ? 'Minimize' : 'Maximize'}</span>
              </button>
              <button
                type="button"
                className="secondary-button coordinator-panel-nav"
                onClick={() => setActivePanel(prevPanel)}
                aria-label={`Go to ${panelLabels[prevPanel]}`}
              >
                ← {panelLabels[prevPanel]}
              </button>
              <button
                type="button"
                className="primary-button coordinator-panel-nav"
                onClick={() => setActivePanel(nextPanel)}
                aria-label={`Go to ${panelLabels[nextPanel]}`}
              >
                {panelLabels[nextPanel]} →
              </button>
            </div>
          )}
        >
          {/* 3-dot stepper */}
          <div className="coordinator-panel-stepper coordinator-panel-stepper--centered">
            {panelOrder.map((panel) => (
              <button
                key={panel}
                type="button"
                className={`coordinator-panel-stepper-node${activePanel === panel ? ' coordinator-panel-stepper-node--active' : ''}`}
                aria-label={`Go to ${panelLabels[panel]}`}
                title={panelLabels[panel]}
                onClick={() => setActivePanel(panel)}
              />
            ))}
          </div>

          {activePanel === 'careplan' ? (
            <CoordinatorModuleGrid>
              <div className="module-slot module-slot--featured coordinator-right-panel-grid">
                <CarePlanManagement />
              </div>
            </CoordinatorModuleGrid>
          ) : activePanel === 'followup' ? (
            <CoordinatorModuleGrid>
              <div className="module-slot module-slot--featured coordinator-right-panel-grid">
                <FollowUpTaskboard />
              </div>
            </CoordinatorModuleGrid>
          ) : (
            <div className="dashboard-grid dashboard-grid--support coordinator-right-panel-grid">
              <div className="module-slot">
                <NotificationsPanel />
              </div>
              <div className="module-slot">
                <TeleconsultationWorkspace />
              </div>
            </div>
          )}
        </Card>
      </section>
    </div>
  )
}
