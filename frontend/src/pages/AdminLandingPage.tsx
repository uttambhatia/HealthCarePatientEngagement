import { useState, type ReactNode } from 'react'
import { Card } from '../components/Card'
import { MetricCardIcon, type MetricVariant } from '../components/MetricCardIcon'
import { IotMonitoringDashboard } from '../modules/iot/IotMonitoringDashboard'
import { NotificationsPanel } from '../modules/notification/NotificationsPanel'
import { PatientRegistrationReview } from '../modules/patient/PatientRegistrationReview'
import type { Role } from '../utils/roleUtils'

type AdminLandingPageProps = {
  role: Role
}

function AdminMetric({ label, value, variant }: { label: string; value: string; variant: MetricVariant }) {
  return (
    <div className={`metric-card metric-card--${variant}`}>
      <MetricCardIcon variant={variant} />
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function AdminModuleGrid({ children }: { children: ReactNode }) {
  return <div className="dashboard-grid dashboard-grid--primary">{children}</div>
}

export function AdminLandingPage({ role }: AdminLandingPageProps) {
  const [showSupportPanel, setShowSupportPanel] = useState(false)
  const [isRightPaneMaximized, setIsRightPaneMaximized] = useState(false)
  const togglePanel = () => setShowSupportPanel((current) => !current)
  const toggleRightPaneSize = () => setIsRightPaneMaximized((current) => !current)

  return (
    <div className="workspace-stack admin-landing">
      <section className={`admin-workbench${isRightPaneMaximized ? ' admin-workbench--right-maximized' : ''}`}>
        <section className="role-spotlight card admin-spotlight">
          <div className="role-spotlight-copy">
            <p className="eyebrow">{role} landing</p>
            <h2>Operational Governance Center</h2>
            <p>
              See service posture, policy visibility, and platform health from one control surface designed for governance workflows.
            </p>
            <div className="pill-row">
              <span className="pill">Service posture monitoring</span>
              <span className="pill">IoT and alert oversight</span>
            </div>
          </div>
          <div className="role-spotlight-side">
            <AdminMetric label="Platform lens" value="Governance" variant="narrative" />
            <AdminMetric label="Risk posture" value="Telemetry + alerts" variant="volume" />
            <AdminMetric label="Control surface" value="Policy and ops" variant="activity" />
          </div>
        </section>

        <Card
          title={showSupportPanel ? 'Operational support' : 'Patient registrations'}
          eyebrow={showSupportPanel ? 'Supporting tools' : undefined}
          subtitle={showSupportPanel ? 'Telemetry and notifications support the primary governance workflow.' : 'Review pending registrations and decide approval outcomes.'}
          centeredHeader
          actions={(
            <div className="admin-panel-actions">
              <button
                type="button"
                className="primary-button admin-panel-nav"
                onClick={togglePanel}
                aria-label={showSupportPanel ? 'Go to Patient Registrations' : 'Go to operational support'}
              >
                {showSupportPanel ? 'Patient Registrations' : 'Operational Support'}
              </button>
              <button
                type="button"
                className="secondary-button admin-pane-size-toggle"
                onClick={toggleRightPaneSize}
                aria-label={isRightPaneMaximized ? 'Minimize right pane' : 'Maximize right pane'}
              >
                <span className="admin-pane-size-toggle-icon" aria-hidden="true">
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
                <span className="admin-pane-size-toggle-label">{isRightPaneMaximized ? 'Minimize' : 'Maximize'}</span>
              </button>
            </div>
          )}
        >
          <button
            type="button"
            className="admin-panel-stepper admin-panel-stepper--centered"
            data-tooltip={showSupportPanel ? 'Operational Support' : 'Patient registrations'}
            aria-label={showSupportPanel ? 'Go to patient registrations' : 'Go to operational support'}
            onClick={togglePanel}
          >
            <span className="admin-panel-stepper-node admin-panel-stepper-node--active" />
            <span className={`admin-panel-stepper-node${showSupportPanel ? ' admin-panel-stepper-node--active' : ''}`} />
          </button>

          {showSupportPanel ? (
            <div className="dashboard-grid dashboard-grid--support admin-right-panel-grid">
              <div className="module-slot">
                <IotMonitoringDashboard />
              </div>
              <div className="module-slot">
                <NotificationsPanel />
              </div>
            </div>
          ) : (
            <AdminModuleGrid>
              <div className="module-slot module-slot--datatable admin-registration-module">
                <PatientRegistrationReview />
              </div>
            </AdminModuleGrid>
          )}
        </Card>
      </section>
    </div>
  )
}
