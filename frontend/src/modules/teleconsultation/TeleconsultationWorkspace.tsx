import { Card } from '../../components/Card'

export function TeleconsultationWorkspace() {
  return (
    <Card title="Teleconsultation" eyebrow="Realtime care delivery">
      <p>Prepare secure video sessions, care notes and post-visit follow-up reminders.</p>
      <div className="stats-grid">
        <div><strong>12</strong><span>Upcoming virtual consults</span></div>
        <div><strong>4</strong><span>Escalations awaiting review</span></div>
      </div>
    </Card>
  )
}
