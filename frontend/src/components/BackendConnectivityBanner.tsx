import { useEffect, useState } from 'react'

type BackendStatus = 'checking' | 'up' | 'auth-required' | 'down'
const BYPASS_AUTH = import.meta.env.VITE_BYPASS_AUTH === 'true'

function getStatusMessage(status: BackendStatus) {
  if (status === 'checking') {
    return 'Checking connection...'
  }
  if (status === 'up') {
    return 'Connected. Services are available.'
  }
  if (status === 'auth-required') {
    if (BYPASS_AUTH) {
      return 'Connected. Demo sign-in mode is currently enabled.'
    }
    return 'Connected. Please sign in to continue.'
  }
  return 'Connection is unavailable right now. Please try again in a moment.'
}

export function BackendConnectivityBanner() {
  const [status, setStatus] = useState<BackendStatus>('checking')

  useEffect(() => {
    let active = true

    async function checkBackend() {
      try {
        const response = await fetch('/actuator/health')
        if (!active) {
          return
        }

        if (response.status === 401 || response.status === 403) {
          setStatus('auth-required')
          return
        }

        if (response.ok) {
          setStatus('up')
          return
        }

        setStatus('down')
      } catch {
        if (active) {
          setStatus('down')
        }
      }
    }

    const onAuthFailure = () => {
      if (active) {
        setStatus('auth-required')
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
    <div className={`connectivity-banner connectivity-${status}`}>
      <strong>Connection status</strong>
      <span>{getStatusMessage(status)}</span>
    </div>
  )
}
