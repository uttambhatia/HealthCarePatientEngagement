import { Card } from '../../components/Card'

export function CarePlanManagement() {
  return (
    <Card title="Care plan management" eyebrow="Care plan service">
      <div className="pill-row">
        <span className="pill">Medication adherence</span>
        <span className="pill">Remote monitoring</span>
        <span className="pill">Teleconsultation</span>
      </div>
      <p>Configure reusable care pathways and orchestrated care-plan lifecycle events.</p>
    </Card>
  )
}
