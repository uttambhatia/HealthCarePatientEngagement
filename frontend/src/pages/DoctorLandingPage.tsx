import { useState, type ReactNode } from 'react'
import { Card } from '../components/Card'
import { MetricCardIcon, type MetricVariant } from '../components/MetricCardIcon'
import { TeleconsultationManagement } from '../modules/teleconsultation/TeleconsultationManagement'
import { IotMonitoringDashboard } from '../modules/iot/IotMonitoringDashboard'
import { NotificationsPanel } from '../modules/notification/NotificationsPanel'
import type { Role } from '../utils/roleUtils'

type DoctorLandingPageProps = {
  role: Role
}

function DoctorMetric({ label, value, variant }: { label: string; value: string; variant: MetricVariant }) {
  return (
    <div className={`metric-card metric-card--${variant}`}>
      <MetricCardIcon variant={variant} />
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function DoctorModuleGrid({ children }: { children: ReactNode }) {
  return <div className="dashboard-grid dashboard-grid--primary">{children}</div>
}

export function DoctorLandingPage({ role }: DoctorLandingPageProps) {
  const [showSupportPanel, setShowSupportPanel] = useState(false)
  const [isRightPaneMaximized, setIsRightPaneMaximized] = useState(false)
  const togglePanel = () => setShowSupportPanel((current) => !current)
  const toggleRightPaneSize = () => setIsRightPaneMaximized((current) => !current)

  return (
    <div className="workspace-stack doctor-landing">
      <section className={`doctor-workbench${isRightPaneMaximized ? ' doctor-workbench--right-maximized' : ''}`}>
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

        <Card
          title={showSupportPanel ? 'Clinical support' : 'Doctor workflow'}
          eyebrow={showSupportPanel ? 'Supporting tools' : 'Landing page'}
          subtitle={showSupportPanel ? 'Telemetry and notifications support the primary tele-consultation workflow.' : undefined}
          centeredHeader
          actions={(
            <div className="doctor-panel-actions">
              <button
                type="button"
                className="primary-button doctor-pane-size-toggle"
                onClick={toggleRightPaneSize}
                aria-label={isRightPaneMaximized ? 'Minimize right pane' : 'Maximize right pane'}
              >
                <span className="doctor-pane-size-toggle-icon" aria-hidden="true">
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
                <span className="doctor-pane-size-toggle-label">{isRightPaneMaximized ? 'Minimize' : 'Maximize'}</span>
              </button>
              <button
                type="button"
                className="primary-button doctor-panel-nav"
                onClick={togglePanel}
                aria-label={showSupportPanel ? 'Back to doctor workflow' : 'Go to clinical support'}
              >
                {showSupportPanel ? '← Back' : 'Next →'}
              </button>
            </div>
          )}
        >
          <button
            type="button"
            className="doctor-panel-stepper doctor-panel-stepper--centered"
            data-tooltip={showSupportPanel ? 'Clinical support' : 'Doctor workflow'}
            aria-label={showSupportPanel ? 'Back to doctor workflow' : 'Go to clinical support'}
            onClick={togglePanel}
          >
            <span className="doctor-panel-stepper-node doctor-panel-stepper-node--active" />
            <span className={`doctor-panel-stepper-node${showSupportPanel ? ' doctor-panel-stepper-node--active' : ''}`} />
          </button>

          {showSupportPanel ? (
            <>
              <div className="dashboard-grid dashboard-grid--support doctor-right-panel-grid">
                <div className="module-slot">
                  <IotMonitoringDashboard />
                </div>
                <div className="module-slot">
                  <NotificationsPanel />
                </div>
              </div>
            </>
          ) : (
            <DoctorModuleGrid>
              <div className="module-slot module-slot--featured doctor-right-panel-grid">
                <TeleconsultationManagement />
              </div>
            </DoctorModuleGrid>
          )}
        </Card>
      </section>
    </div>
  )
}
