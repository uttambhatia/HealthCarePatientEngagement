import './App.css'
import { LoginPanel } from './modules/auth/LoginPanel'
import { PatientDashboard } from './modules/patient/PatientDashboard'
import { AppointmentBooking } from './modules/appointment/AppointmentBooking'
import { CarePlanManagement } from './modules/careplan/CarePlanManagement'
import { NotificationsPanel } from './modules/notification/NotificationsPanel'
import { TeleconsultationWorkspace } from './modules/teleconsultation/TeleconsultationWorkspace'
import { IotMonitoringDashboard } from './modules/iot/IotMonitoringDashboard'
import { AdminDashboard } from './modules/admin/AdminDashboard'
import { HomePage } from './pages/HomePage'
import { useAuth } from './auth/useAuth'

function App() {
  const { session, loginAs, logout } = useAuth()

  return (
    <HomePage
      headerActions={
        session ? (
          <button className="primary-button" onClick={logout}>Sign out</button>
        ) : undefined
      }
    >
      {!session ? (
        <LoginPanel onLogin={loginAs} />
      ) : (
        <div className="dashboard-grid">
          <PatientDashboard />
          <AppointmentBooking />
          <CarePlanManagement />
          <NotificationsPanel />
          <TeleconsultationWorkspace />
          <IotMonitoringDashboard />
          <AdminDashboard />
        </div>
      )}
    </HomePage>
  )
}

export default App
