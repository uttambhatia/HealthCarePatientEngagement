import './App.css'
import { useEffect, useState } from 'react'
import { LoginPanel } from './modules/auth/LoginPanel'
import { RoleWorkspace } from './components/RoleWorkspace'
import { AdminLandingPage } from './pages/AdminLandingPage'
import { CoordinatorLandingPage } from './pages/CoordinatorLandingPage'
import { DoctorLandingPage } from './pages/DoctorLandingPage'
import { PatientLandingPage } from './pages/PatientLandingPage'
import { HomePage } from './pages/HomePage'
import { useAuth } from './auth/useAuth'
import { setApiAuthTokenGetter, setApiErrorInterceptor } from './services/apiClient'
import { PatientRegistrationForm } from './modules/patient/PatientRegistrationForm'

type BackendStatus = 'checking' | 'up' | 'auth-required' | 'down'

function App() {
  const { session, authReady, authError, isOidcConfigured, loginWithOidc, loginAs, logout } = useAuth()
  const [showPatientRegistration, setShowPatientRegistration] = useState(false)
  const [registrationNotice, setRegistrationNotice] = useState<string | null>(null)
  const [backendStatus, setBackendStatus] = useState<BackendStatus>('checking')

  useEffect(() => {
    setApiAuthTokenGetter(() => session?.accessToken ?? null)
    setApiErrorInterceptor((error) => {
      if (error.status === 401 || error.status === 403) {
        window.dispatchEvent(new CustomEvent('app:api-auth-failure', { detail: error.status }))
      }
    })

    return () => {
      setApiAuthTokenGetter(null)
      setApiErrorInterceptor(null)
    }
  }, [session])

  useEffect(() => {
    let active = true

    async function checkBackend() {
      try {
        const response = await fetch('/actuator/health')
        if (!active) {
          return
        }

        if (response.status === 401 || response.status === 403) {
          setBackendStatus('auth-required')
          return
        }

        setBackendStatus(response.ok ? 'up' : 'down')
      } catch {
        if (active) {
          setBackendStatus('down')
        }
      }
    }

    const onAuthFailure = () => {
      if (active) {
        setBackendStatus('auth-required')
      }
    }

    void checkBackend()
    window.addEventListener('app:api-auth-failure', onAuthFailure)

    return () => {
      active = false
      window.removeEventListener('app:api-auth-failure', onAuthFailure)
    }
  }, [])

  return (
    <HomePage
      isAuthenticated={Boolean(session)}
      username={session?.displayName}
      connectionStatus={backendStatus}
      headerActions={session ? <button className="primary-button" onClick={logout}>Sign out</button> : undefined}
    >
      {!authReady ? <p>Completing secure sign-in...</p> : null}
      {!session ? (
        <>
          {registrationNotice ? <p className="form-status form-status--success">{registrationNotice}</p> : null}
          <LoginPanel
            oidcEnabled={isOidcConfigured}
            authError={authError}
            onOidcLogin={(loginHint) => void loginWithOidc(loginHint)}
            onLogin={loginAs}
            onRegisterPatient={() => setShowPatientRegistration(true)}
            showPatientRegistration={showPatientRegistration}
            registrationPanel={showPatientRegistration ? (
              <PatientRegistrationForm
                onBack={() => setShowPatientRegistration(false)}
                onSaved={(patient) => {
                  setRegistrationNotice(`Patient ${patient.givenName} ${patient.familyName} was registered successfully. A confirmation update has been sent.`)
                  setShowPatientRegistration(false)
                }}
              />
            ) : undefined}
          />
        </>
      ) : (
        session.role === 'PATIENT' ? (
          <PatientLandingPage role={session.role} />
        ) : session.role === 'DOCTOR' ? (
          <DoctorLandingPage role={session.role} />
        ) : session.role === 'ADMIN' ? (
          <AdminLandingPage role={session.role} />
        ) : session.role === 'COORDINATOR' ? (
          <CoordinatorLandingPage role={session.role} />
        ) : (
          <RoleWorkspace role={session.role} />
        )
      )}
    </HomePage>
  )
}

export default App
