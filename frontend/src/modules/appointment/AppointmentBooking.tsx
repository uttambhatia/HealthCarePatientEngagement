import { Card } from '../../components/Card'

export function AppointmentBooking() {
  return (
    <Card title="Appointment booking" eyebrow="Appointment service">
      <form className="stacked-form">
        <input placeholder="Patient ID" />
        <input placeholder="Provider ID" />
        <input placeholder="2026-05-26T09:00:00Z" />
        <select defaultValue="video">
          <option value="video">Video consultation</option>
          <option value="in-person">In-person visit</option>
        </select>
        <button type="button" className="primary-button">Book appointment</button>
      </form>
    </Card>
  )
}
