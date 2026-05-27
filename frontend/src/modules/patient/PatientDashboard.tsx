import { Card } from '../../components/Card'
import { SectionHeader } from '../../components/SectionHeader'

export function PatientDashboard() {
  return (
    <Card title="Patient dashboard" eyebrow="Patient service">
      <SectionHeader title="Onboarding" subtitle="Timeline, consent and outreach status" />
      <ul>
        <li>Zero-trust onboarding with correlation ID tracking</li>
        <li>Consent and care-gap snapshot aligned to care plans</li>
        <li>FHIR-linked profile summary for coordinators</li>
      </ul>
    </Card>
  )
}
