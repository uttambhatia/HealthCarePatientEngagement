import './App.css'
import { useEffect, useState } from 'react'
import { LoginPanel } from './modules/auth/LoginPanel'
import { RoleWorkspace } from './components/RoleWorkspace'
import { AdminLandingPage } from './pages/AdminLandingPage'
import { CoordinatorLandingPage } from './pages/CoordinatorLandingPage'
import { DoctorLandingPage } from './pages/DoctorLandingPage'
import { PatientLandingPage } from './pages/PatientLandingPage'
import { HomePage } from './pages/HomePage'
import { TeleconsultCallClientPage } from './pages/TeleconsultCallClientPage'
import { useAuth } from './auth/useAuth'
import { setApiAuthTokenGetter, setApiErrorInterceptor } from './services/apiClient'
import { PatientRegistrationForm } from './modules/patient/PatientRegistrationForm'
import { getProfilePhotoUrl, uploadProfilePhoto } from './services/platformApi'

type BackendStatus = 'checking' | 'up' | 'auth-required' | 'down'

function App() {
  const { session, authReady, authError, isOidcConfigured, loginWithOidc, loginAs, logout } = useAuth()
  const currentPath = typeof window !== 'undefined' ? window.location.pathname : ''

  const [showPatientRegistration, setShowPatientRegistration] = useState(false)
  const [registrationNotice, setRegistrationNotice] = useState<string | null>(null)
  const [backendStatus, setBackendStatus] = useState<BackendStatus>('checking')
  const [profilePhotoUrl, setProfilePhotoUrl] = useState<string | null>(null)
  const [photoStatus, setPhotoStatus] = useState<string | null>(null)
  const [photoError, setPhotoError] = useState<string | null>(null)

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
    let loadedUrl: string | null = null

    async function loadPhoto() {
      if (!session?.accessToken) {
        if (active) {
          setProfilePhotoUrl(null)
        }
        return
      }

      try {
        const objectUrl = await getProfilePhotoUrl(session.accessToken)
        if (active) {
          loadedUrl = objectUrl
          setProfilePhotoUrl(objectUrl)
        }
      } catch {
        if (active) {
          setProfilePhotoUrl(null)
        }
      }
    }

    void loadPhoto()

    return () => {
      active = false
      if (loadedUrl) {
        URL.revokeObjectURL(loadedUrl)
      }
    }
  }, [session?.accessToken])

  async function handleProfilePhotoUpload(file: File | null) {
    if (!file || !session?.accessToken) {
      return
    }
    setPhotoError(null)
    setPhotoStatus('Uploading profile photo...')

    try {
      await uploadProfilePhoto(file, session.accessToken)
      const objectUrl = await getProfilePhotoUrl(session.accessToken)
      setProfilePhotoUrl((current) => {
        if (current) {
          URL.revokeObjectURL(current)
        }
        return objectUrl
      })
      setPhotoStatus('Profile photo updated.')
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Unable to upload profile photo.'
      setPhotoError(message)
      setPhotoStatus(null)
    }
  }

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

  if (currentPath === '/teleconsult/call') {
    const params = new URLSearchParams(typeof window !== 'undefined' ? window.location.search : '')
    const joinUrl = params.get('joinUrl') ?? ''
    const role = params.get('role') ?? ''
    return <TeleconsultCallClientPage joinUrl={joinUrl} role={role} />
  }

  return (
    <HomePage
      isAuthenticated={Boolean(session)}
      username={session?.displayName}
      profilePhotoUrl={profilePhotoUrl ?? undefined}
      connectionStatus={backendStatus}
      headerActions={session ? (
        <>
          <label className="secondary-button upload-button">
            Upload photo
            <input
              type="file"
              accept="image/*"
              onChange={(event) => void handleProfilePhotoUpload(event.target.files?.[0] ?? null)}
            />
          </label>
          <button className="primary-button" onClick={logout}>Sign out</button>
        </>
      ) : undefined}
    >
      {photoStatus ? <p className="form-status form-status--success">{photoStatus}</p> : null}
      {photoError ? <p className="form-status form-status--error">{photoError}</p> : null}
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
