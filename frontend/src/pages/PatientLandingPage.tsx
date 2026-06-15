import { useState, type ReactNode } from 'react'
import { Card } from '../components/Card'
import { MetricCardIcon, type MetricVariant } from '../components/MetricCardIcon'
import { NotificationsPanel } from '../modules/notification/NotificationsPanel'
import { PatientDashboard } from '../modules/patient/PatientDashboard'
import { TeleconsultationWorkspace } from '../modules/teleconsultation/TeleconsultationWorkspace'
import type { Role } from '../utils/roleUtils'

type PatientLandingPageProps = {
  role: Role
}

function PatientMetric({ label, value, variant }: { label: string; value: string; variant: MetricVariant }) {
  return (
    <div className={`metric-card metric-card--${variant}`}>
      <MetricCardIcon variant={variant} />
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function PatientModuleGrid({ children }: { children: ReactNode }) {
  return <div className="dashboard-grid dashboard-grid--primary">{children}</div>
}

export function PatientLandingPage({ role }: PatientLandingPageProps) {
  const [showSupportPanel, setShowSupportPanel] = useState(false)
  const [isRightPaneMaximized, setIsRightPaneMaximized] = useState(false)
  const togglePanel = () => setShowSupportPanel((current) => !current)
  const toggleRightPaneSize = () => setIsRightPaneMaximized((current) => !current)

  return (
    <div className="workspace-stack patient-landing">
      <section className={`patient-workbench${isRightPaneMaximized ? ' patient-workbench--right-maximized' : ''}`}>
        <section className="role-spotlight card patient-spotlight">
          <div className="role-spotlight-copy">
            <p className="eyebrow">{role} landing</p>
            <h2>Patient engagement center</h2>
            <p>
              Stay connected to care plans, appointments, and remote updates with one clear patient-first workspace.
            </p>
            <div className="pill-row">
              <span className="pill">Appointments and follow-up</span>
              <span className="pill">Teleconsultation readiness</span>
            </div>
          </div>
          <div className="role-spotlight-side">
            <PatientMetric label="Primary workflow" value="Care plan and visits" variant="narrative" />
            <PatientMetric label="Wellness focus" value="Daily engagement" variant="volume" />
            <PatientMetric label="Support channel" value="Notifications live" variant="activity" />
          </div>
        </section>

        <Card
          title={showSupportPanel ? 'Care support' : 'Patient workflow'}
          eyebrow={showSupportPanel ? 'Supporting tools' : 'Landing page'}
          subtitle={showSupportPanel ? 'Notifications and teleconsultation support the primary patient workflow.' : undefined}
          centeredHeader
          actions={(
            <div className="patient-panel-actions">
              <button
                type="button"
                className="primary-button patient-pane-size-toggle"
                onClick={toggleRightPaneSize}
                aria-label={isRightPaneMaximized ? 'Minimize right pane' : 'Maximize right pane'}
              >
                <span className="patient-pane-size-toggle-icon" aria-hidden="true">
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
                <span className="patient-pane-size-toggle-label">{isRightPaneMaximized ? 'Minimize' : 'Maximize'}</span>
              </button>
              <button
                type="button"
                className="primary-button patient-panel-nav"
                onClick={togglePanel}
                aria-label={showSupportPanel ? 'Back to patient workflow' : 'Go to care support'}
              >
                {showSupportPanel ? '← Back' : 'Next →'}
              </button>
            </div>
          )}
        >
          <button
            type="button"
            className="patient-panel-stepper patient-panel-stepper--centered"
            data-tooltip={showSupportPanel ? 'Care support' : 'Patient workflow'}
            aria-label={showSupportPanel ? 'Back to patient workflow' : 'Go to care support'}
            onClick={togglePanel}
          >
            <span className="patient-panel-stepper-node patient-panel-stepper-node--active" />
            <span className={`patient-panel-stepper-node${showSupportPanel ? ' patient-panel-stepper-node--active' : ''}`} />
          </button>

          {showSupportPanel ? (
            <div className="dashboard-grid dashboard-grid--support patient-right-panel-grid">
              <div className="module-slot">
                <NotificationsPanel />
              </div>
              <div className="module-slot">
                <TeleconsultationWorkspace />
              </div>
            </div>
          ) : (
            <PatientModuleGrid>
              <div className="module-slot module-slot--featured patient-right-panel-grid">
                <PatientDashboard />
              </div>
            </PatientModuleGrid>
          )}
        </Card>
      </section>
    </div>
  )
}
