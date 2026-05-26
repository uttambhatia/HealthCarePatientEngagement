import { Card } from '../../components/Card'

type Role = 'PATIENT' | 'DOCTOR' | 'ADMIN' | 'COORDINATOR'

const roles: Role[] = ['PATIENT', 'DOCTOR', 'ADMIN', 'COORDINATOR']

type LoginPanelProps = {
  onLogin: (role: Role) => void
}

export function LoginPanel({ onLogin }: LoginPanelProps) {
  return (
    <Card title="OAuth2 login" eyebrow="Identity">
      <p>Simulate Azure AD role-aware sign-in for the BFF and microservice APIs.</p>
      <div className="button-row">
        {roles.map((role) => (
          <button key={role} className="primary-button" onClick={() => onLogin(role)}>
            Continue as {role}
          </button>
        ))}
      </div>
    </Card>
  )
}
