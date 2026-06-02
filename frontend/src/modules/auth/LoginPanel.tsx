import { useState, type ReactNode } from 'react'
import { Card } from '../../components/Card'

type Role = 'PATIENT' | 'DOCTOR' | 'ADMIN' | 'COORDINATOR'

const roles: Role[] = ['PATIENT', 'DOCTOR', 'ADMIN', 'COORDINATOR']

const rolePrompts: Record<Role, string> = {
  PATIENT: 'Book care, review updates, and stay on track with follow-ups.',
  DOCTOR: 'Review the patient context, adjust care plans, and manage escalations.',
  ADMIN: 'Review operations, access policies, and overall platform health.',
  COORDINATOR: 'Coordinate scheduling, handoffs, and care-pathway alignment.',
}

type LoginPanelProps = {
  oidcEnabled: boolean
  authError: string | null
  onOidcLogin: () => void
  onLogin: (role: Role) => void
  onRegisterPatient: () => void
  showPatientRegistration: boolean
  registrationPanel?: ReactNode
}

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

  return (
    <Card title="Sign-in" eyebrow="Account access" centeredHeader>
      <div className="login-page-grid">
        <section className="login-marketing-panel" aria-label="Login information">
          <p className="eyebrow">Welcome back</p>
          <h3>Secure access for care teams, patients, and operations leaders.</h3>
          <p>
            Sign in with your secure account for normal use. For local demos, you can continue with a role-based view.
          </p>
          <ul className="login-feature-list">
            <li>Role-based journeys across appointments, care plans, and remote monitoring</li>
            <li>Secure sign-in with modern identity standards</li>
            <li>Real-time updates for care teams and operations</li>
          </ul>
          <div className="login-trust-row">
            <span className="pill">HIPAA-focused</span>
            <span className="pill">FHIR-aligned</span>
            <span className="pill">Cloud-native</span>
          </div>
        </section>

        <section className="login-auth-panel" aria-label={showPatientRegistration ? 'Patient registration' : 'Sign in options'}>
          {showPatientRegistration ? (
            <div className="login-registration-panel">{registrationPanel}</div>
          ) : (
            <>
              <div className="login-panel-copy">
                <h3>Login</h3>
                <p>Use secure sign-in, or choose a local role to preview the experience.</p>
                {!oidcEnabled ? (
                  <p className="login-note">Secure sign-in is not configured yet in this environment. You can still continue with local role access.</p>
                ) : null}
                {authError ? <p className="login-error">Authentication error: {authError}</p> : null}
                <div className="button-row">
                  <button type="button" className="primary-button" onClick={onOidcLogin} disabled={!oidcEnabled}>
                    Sign in
                  </button>
                  <button type="button" className="secondary-button" onClick={onRegisterPatient}>
                    Register
                  </button>
                </div>
              </div>

              <div className="role-launchpad">
                <p className="eyebrow">Quick role access</p>
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
                <button type="button" className="secondary-button" onClick={() => onLogin(selectedRole)}>
                  Continue as {selectedRole}
                </button>
              </div>
            </>
          )}
        </section>
      </div>
    </Card>
  )
}
