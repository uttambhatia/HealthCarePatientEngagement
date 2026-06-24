import { useEffect, useState, type ReactNode } from 'react'
import { Card } from '../../components/Card'
import { trackUiEvent } from '../../services/uiTelemetry'

type Role = 'PATIENT' | 'DOCTOR' | 'ADMIN' | 'COORDINATOR'

const roles: Role[] = ['PATIENT', 'DOCTOR', 'ADMIN', 'COORDINATOR']

const rolePrompts: Record<Role, string> = {
  PATIENT: 'Book care, review updates, and stay on track with follow-ups.',
  DOCTOR: 'Review the patient context, adjust care plans, and manage escalations.',
  ADMIN: 'Review operations, access policies, and overall platform health.',
  COORDINATOR: 'Coordinate scheduling, handoffs, and care-pathway alignment.',
}

const rolePreviewStats: Record<Role, Array<{ value: string; label: string; icon: 'journey' | 'clock' | 'signal' | 'profile' | 'sync' | 'pulse' | 'settings' | 'shield' | 'ops' | 'handoff' | 'path' | 'calendar' }>> = {
  PATIENT: [
    { value: '3 steps', label: 'Intake to follow-up', icon: 'journey' },
    { value: '<2 min', label: 'Quick registration', icon: 'clock' },
    { value: '24x7', label: 'Care visibility', icon: 'signal' },
  ],
  DOCTOR: [
    { value: '1 view', label: 'Patient context', icon: 'profile' },
    { value: 'Real-time', label: 'Clinical updates', icon: 'sync' },
    { value: 'Fast', label: 'Decision support', icon: 'pulse' },
  ],
  ADMIN: [
    { value: 'Unified', label: 'Platform controls', icon: 'settings' },
    { value: 'Policy', label: 'Access governance', icon: 'shield' },
    { value: 'Ops', label: 'Health overview', icon: 'ops' },
  ],
  COORDINATOR: [
    { value: 'Aligned', label: 'Team handoffs', icon: 'handoff' },
    { value: 'Guided', label: 'Care pathways', icon: 'path' },
    { value: 'Faster', label: 'Scheduling flow', icon: 'calendar' },
  ],
}

type LoginPanelProps = {
  oidcEnabled: boolean
  authError: string | null
  onOidcLogin: (loginHint?: string) => void
  onLogin: (role: Role) => void
  onRegisterPatient: () => void
  showPatientRegistration: boolean
  registrationPanel?: ReactNode
}

const loginCarouselImages = [
  {
    src: '/PE1.png',
    alt: 'Secure healthcare access overview',
  },
  {
    src: '/patient-engagement_2.png',
    alt: 'Safer handoffs and coordinated care continuity',
  },
]

export function LoginPanel({
  oidcEnabled,
  authError,
  onOidcLogin,
  onLogin,
  onRegisterPatient,
  showPatientRegistration,
  registrationPanel,
}: LoginPanelProps) {
  const [selectedRole, setSelectedRole] = useState<Role>('PATIENT')
  const [activeSlide, setActiveSlide] = useState(0)
  const [loginHint, setLoginHint] = useState('')

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setActiveSlide((current) => (current + 1) % 2)
    }, 4800)

    return () => {
      window.clearInterval(intervalId)
    }
  }, [])

  function handleSecureSignIn() {
    const normalizedLoginHint = loginHint.trim()

    trackUiEvent({
      name: 'login.sign_in_securely_clicked',
      context: {
        oidcEnabled,
        hasLoginHint: normalizedLoginHint.length > 0,
        source: 'home_auth_panel',
      },
    })
    onOidcLogin(normalizedLoginHint || undefined)
  }

  function handleRegisterPatient() {
    trackUiEvent({
      name: 'login.register_patient_clicked',
      context: {
        source: 'home_auth_panel',
      },
    })
    onRegisterPatient()
  }

  function handleOpenRoleView() {
    trackUiEvent({
      name: 'login.open_role_view_clicked',
      context: {
        selectedRole,
        source: 'home_auth_panel',
      },
    })
    onLogin(selectedRole)
  }

  function renderRoleStatIcon(icon: (typeof rolePreviewStats)[Role][number]['icon']) {
    if (icon === 'clock') {
      return <path d="M12 6.5v5l3.3 1.8M20 12a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z" />
    }
    if (icon === 'signal') {
      return <path d="M4.8 16.8 8 13.6l2.2 2.2 4-4 1.8 1.8M6 7.6h.01M11 7.6h.01M16 7.6h.01" />
    }
    if (icon === 'profile') {
      return <path d="M12 8.5a2.9 2.9 0 1 0 0 5.8 2.9 2.9 0 0 0 0-5.8Zm-5.6 10c.6-1.9 2.2-3 4-3h3.2c1.8 0 3.4 1.1 4 3" />
    }
    if (icon === 'sync') {
      return <path d="M6.4 8.8A5.9 5.9 0 0 1 17 8l1.5 1.2M17.6 15.2A5.9 5.9 0 0 1 7 16l-1.5-1.2M18.7 9.2h-3.2M5.3 14.8h3.2" />
    }
    if (icon === 'pulse') {
      return <path d="M4 12h3l2-3.4 2.4 6.2 2.2-4H20" />
    }
    if (icon === 'settings') {
      return <path d="m12 8.2 1 .4 1-.6 1.2 1.2-.6 1 .4 1 .9.5v1.7l-.9.5-.4 1 .6 1L14 18.1l-1-.6-1 .4-.5.9h-1.7l-.5-.9-1-.4-1 .6-1.2-1.2.6-1-.4-1-.9-.5v-1.7l.9-.5.4-1-.6-1L8 8l1 .6 1-.4.5-.9h1.7l.5.9Z" />
    }
    if (icon === 'shield') {
      return <path d="M12 4.2 6 6.8v4.8c0 3.4 2.1 6.4 6 7.8 3.9-1.4 6-4.4 6-7.8V6.8L12 4.2Zm-2.2 7.5 1.7 1.7 3-3.1" />
    }
    if (icon === 'ops') {
      return <path d="M5.8 18.2h12.4M7.2 16.8V10M11.2 16.8V7.8M15.2 16.8V12.2M19.2 16.8V9.4" />
    }
    if (icon === 'handoff') {
      return <path d="M7.2 10.5h4.1a2 2 0 1 1 0 4H9.9M16.8 13.5h-4.1a2 2 0 1 1 0-4h1.4M6 10.5l1.2 1.2L6 12.9M18 13.5l-1.2-1.2 1.2-1.2" />
    }
    if (icon === 'path') {
      return <path d="M5.2 7.8A1.8 1.8 0 1 0 5.2 11.4 1.8 1.8 0 0 0 5.2 7.8Zm13.6 4.2a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6ZM7 9.6h6.8a2.4 2.4 0 0 1 0 4.8h-2.6" />
    }
    if (icon === 'calendar') {
      return <path d="M7.2 5.8v2.1M16.8 5.8v2.1M5.8 9.2h12.4M6.8 7.2h10.4a1 1 0 0 1 1 1v9.2a1 1 0 0 1-1 1H6.8a1 1 0 0 1-1-1V8.2a1 1 0 0 1 1-1Z" />
    }

    return <path d="M5.8 10.2h3.1l1.8-2.6 1.9 4 2-2.7h3.6M5.8 16h12.4" />
  }

  return (
    <Card title="Sign-in" eyebrow="Account access" centeredHeader>
      <div className="login-page-grid home-workbench">
        <section className="login-carousel-shell home-spotlight" aria-label="Welcome and experience highlights">
          <div className="login-carousel-track" style={{ transform: `translateX(-${activeSlide * 50}%)` }}>
            <article className="login-marketing-panel login-carousel-slide" aria-hidden={activeSlide !== 0}>
              <p className="eyebrow">Care starts here</p>
              <h3>Secure access for patients, clinicians, and care coordinators.</h3>
              <p>
                Sign in to continue appointments, care plans, and follow-up communication in one coordinated workflow.
              </p>
              <ul className="login-feature-list">
                <li>Patient journeys from intake to follow-up with clear milestones</li>
                <li>Clinical-ready identity and access controls for secure sessions</li>
                <li>Real-time collaboration across providers and operations teams</li>
              </ul>
              <div className="login-trust-row">
                <span className="pill">HIPAA-focused</span>
                <span className="pill">FHIR-aligned</span>
                <span className="pill">Cloud-native</span>
              </div>
            </article>

            <article className="login-marketing-panel login-carousel-slide" aria-hidden={activeSlide !== 1}>
              <p className="eyebrow">Clinical support, simplified</p>
              <h3>Designed for safer handoffs, faster decisions, and better continuity.</h3>
              <p>
                Keep registration, triage notes, virtual touchpoints, and patient updates connected across every care moment.
              </p>
              <ul className="login-feature-list">
                <li>Guided experiences for both first-time and returning patients</li>
                <li>Rapid coordination across clinician and coordinator workbenches</li>
                <li>Mobile-ready visibility for care updates beyond the desktop</li>
              </ul>
              <div className="login-trust-row">
                <span className="pill">Patient-first</span>
                <span className="pill">Team-aligned</span>
                <span className="pill">Mobile-ready</span>
              </div>
            </article>
          </div>

          <img
            className="login-carousel-image"
            src={loginCarouselImages[activeSlide]?.src ?? loginCarouselImages[0].src}
            alt={loginCarouselImages[activeSlide]?.alt ?? loginCarouselImages[0].alt}
          />

          <div className="login-carousel-stepper" aria-label="Login highlights auto rotation">
            <span className={`login-carousel-stepper-node${activeSlide === 0 ? ' login-carousel-stepper-node--active' : ''}`} />
            <span className={`login-carousel-stepper-node${activeSlide === 1 ? ' login-carousel-stepper-node--active' : ''}`} />
          </div>
        </section>

        <section className="login-auth-panel home-auth-panel" aria-label={showPatientRegistration ? 'Patient registration' : 'Sign in options'}>
          {showPatientRegistration ? (
            <div className="login-registration-panel">{registrationPanel}</div>
          ) : (
            <>
              <div className="login-panel-copy">
                <h3>Login</h3>
                <p>Use secure sign-in to continue care delivery, or preview the platform by role.</p>
                <label className="login-hint-field" htmlFor="oidc-login-hint">
                  <span>Account email (optional)</span>
                  <input
                    id="oidc-login-hint"
                    className="login-hint-input"
                    type="email"
                    inputMode="email"
                    autoComplete="username"
                    placeholder="user@organization.com"
                    value={loginHint}
                    onChange={(event) => setLoginHint(event.target.value)}
                  />
                </label>
                <small className="login-hint-help">Credentials are entered on Microsoft sign-in page, not in this app.</small>
                {!oidcEnabled ? (
                  <p className="login-note">
                    Secure sign-in is not configured yet in this environment. Verify
                    {' '}
                    <strong>VITE_OIDC_CLIENT_ID</strong>
                    {' '}
                    and the other
                    {' '}
                    <strong>VITE_OIDC_*</strong>
                    {' '}
                    values before retrying, or continue with local role access.
                  </p>
                ) : null}
                {authError ? <p className="login-error">Authentication error: {authError}</p> : null}
                <div className="button-row login-panel-cta-row">
                  <button type="button" className="primary-button" onClick={handleSecureSignIn} disabled={!oidcEnabled}>
                    Sign in securely
                  </button>
                  <button type="button" className="secondary-button" onClick={handleRegisterPatient}>
                    Register patient
                  </button>
                </div>
              </div>

              <div className="role-launchpad">
                <p className="eyebrow">Role-based preview</p>
                <select
                  className="role-launch-select"
                  value={selectedRole}
                  onChange={(event) => setSelectedRole(event.target.value as Role)}
                  aria-label="Quick role access"
                >
                  {roles.map((role) => (
                    <option key={role} value={role}>
                      {role}
                    </option>
                  ))}
                </select>
                <small className="role-launch-help">{rolePrompts[selectedRole]}</small>
                <small className="role-launch-help">Role-based preview is UI-only. Use secure sign-in for backend operations.</small>
                <div className={`role-preview-grid role-preview-grid--${selectedRole.toLowerCase()}`} aria-label={`${selectedRole} role highlights`}>
                  {rolePreviewStats[selectedRole].map((item) => (
                    <article key={`${selectedRole}-${item.label}`} className="role-preview-tile">
                      <div className="role-preview-title">
                        <span className="role-preview-icon" aria-hidden="true">
                          <svg viewBox="0 0 24 24" focusable="false">{renderRoleStatIcon(item.icon)}</svg>
                        </span>
                        <strong>{item.value}</strong>
                      </div>
                      <span>{item.label}</span>
                    </article>
                  ))}
                </div>
                <button type="button" className="secondary-button" onClick={handleOpenRoleView}>
                  Open {selectedRole} view
                </button>
              </div>
            </>
          )}
        </section>
      </div>
    </Card>
  )
}
