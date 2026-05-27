import { Card } from '../../components/Card'

export function IotMonitoringDashboard() {
  return (
    <Card title="IoT monitoring" eyebrow="Telemetry + alerts">
      <div className="stats-grid">
        <div><strong>98.7%</strong><span>Device ingestion success</span></div>
        <div><strong>23</strong><span>Open patient alerts</span></div>
        <div><strong>7 sec</strong><span>Median event latency</span></div>
      </div>
    </Card>
  )
}
