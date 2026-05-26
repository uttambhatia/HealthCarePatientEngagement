import { Card } from '../../components/Card'
import { useAuth } from '../../auth/AuthProvider'
import { canViewAdmin } from '../../utils/roleUtils'

export function AdminDashboard() {
  const { session } = useAuth()
  const visible = session ? canViewAdmin(session.role) : false

  return (
    <Card title="Admin operations" eyebrow="Governance">
      {visible ? (
        <ul>
          <li>Monitor AKS rollout health and APIM gateway posture</li>
          <li>Rotate Key Vault backed secrets and policy mappings</li>
          <li>Audit HIPAA/GDPR retention workflows</li>
        </ul>
      ) : (
        <p>Admin insights are hidden until a coordinator or administrator signs in.</p>
      )}
    </Card>
  )
}
