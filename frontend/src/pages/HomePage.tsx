import type { PropsWithChildren, ReactNode } from 'react'

type HomePageProps = PropsWithChildren<{
  isAuthenticated?: boolean
  username?: string
  profilePhotoUrl?: string
  connectionStatus?: 'checking' | 'up' | 'auth-required' | 'down'
  headerActions?: ReactNode
  onPhotoUpload?: (file: File) => void
}>

export function HomePage({
  children,
  isAuthenticated = false,
  username,
  profilePhotoUrl,
  connectionStatus = 'checking',
  headerActions,
  onPhotoUpload,
}: HomePageProps) {
  const trustSignals = [
    { label: 'HIPAA-ready security', tone: 'safe' },
    { label: 'FHIR interoperability', tone: 'interop' },
    { label: '24x7 care continuity', tone: 'care' },
    { label: 'Coordinated team workflows', tone: 'team' },
  ] as const
  const carePaths = [
    {
      title: 'For patients',
      tone: 'patient',
      badge: 'PT',
      description: 'Register quickly, track appointments, and receive follow-up guidance in one place.',
    },
    {
      title: 'For clinicians',
      tone: 'clinician',
      badge: 'MD',
      description: 'Review patient context, update care plans, and coordinate secure handoffs without friction.',
    },
    {
      title: 'For coordinators and admins',
      tone: 'operations',
      badge: 'OPS',
      description: 'Monitor operations, prioritize escalations, and maintain policy-aligned care delivery.',
    },
  ] as const
  const breadcrumbCurrent = isAuthenticated ? 'Dashboard' : 'Login'
  const welcomeName = username?.split('@')[0] ?? 'Guest'
  const isConnected = connectionStatus === 'up' || connectionStatus === 'auth-required'
  const connectionStatusLabel = isConnected
    ? 'Connection status is healthy'
    : 'Connection status is unhealthy'
  const statusBanner = !isAuthenticated && connectionStatus === 'auth-required'
    ? {
        tone: 'auth',
        title: 'Secure access required',
        message: 'Complete secure sign-in to access protected backend services.',
        action: 'Recommended action: Sign in securely from the right panel.',
      }
    : !isAuthenticated && connectionStatus === 'down'
      ? {
          tone: 'down',
          title: 'Backend is temporarily unavailable',
          message: 'Some live platform capabilities may be delayed right now.',
          action: 'Recommended action: Use role-based preview and retry secure sign-in shortly.',
        }
      : null

  function renderTrustIcon(tone: (typeof trustSignals)[number]['tone']) {
    if (tone === 'safe') {
      return (
        <svg viewBox="0 0 24 24" focusable="false" aria-hidden="true">
          <path d="M12 3.2 5.3 6.1v5.7c0 4.1 2.5 7.8 6.7 9.2 4.2-1.4 6.7-5.1 6.7-9.2V6.1L12 3.2z" />
          <path d="m9.2 12.3 2 2 3.7-3.8" />
        </svg>
      )
    }

    if (tone === 'interop') {
      return (
        <svg viewBox="0 0 24 24" focusable="false" aria-hidden="true">
          <rect x="3.5" y="6" width="7.5" height="5.6" rx="1.2" />
          <rect x="13" y="12.4" width="7.5" height="5.6" rx="1.2" />
          <path d="M10.2 8.8h3.5c1.5 0 2.7 1.2 2.7 2.7v0.5" />
          <path d="m14.1 14.3 2.1-2.1 2.1 2.1" />
        </svg>
      )
    }

    if (tone === 'care') {
      return (
        <svg viewBox="0 0 24 24" focusable="false" aria-hidden="true">
          <path d="M4 12h3.1l2.1-3.7 2.4 7.1 2.1-4.1H20" />
          <path d="M21 8.8c0-1.8-1.4-3.3-3.2-3.3-1.3 0-2.4.7-3 1.8-.6-1.1-1.7-1.8-3-1.8-1.8 0-3.2 1.5-3.2 3.3 0 3.9 6.2 7.2 6.2 7.2S21 12.7 21 8.8Z" />
        </svg>
      )
    }

    return (
      <svg viewBox="0 0 24 24" focusable="false" aria-hidden="true">
        <circle cx="8" cy="9" r="2.3" />
        <circle cx="16" cy="9" r="2.3" />
        <path d="M3.8 17.3c.7-2 2.4-3.2 4.2-3.2s3.5 1.2 4.2 3.2" />
        <path d="M11.8 17.3c.7-2 2.4-3.2 4.2-3.2s3.5 1.2 4.2 3.2" />
      </svg>
    )
  }

  return (
    <main className="page-shell">
      <header className="site-top-header" aria-label="Top navigation">
        <div className="site-top-brand">
          <span className="site-brand-mark" aria-hidden="true">CC</span>
          <div>
            <strong>Care Coordination</strong>
            <small>Healthcare Collaboration Platform</small>
          </div>
        </div>
        <div className="site-top-tools">
          <div className="site-top-nav" aria-label="User welcome">
            <span
              className={`site-connection-dot ${isConnected ? 'site-connection-dot--up' : 'site-connection-dot--down'}`}
              aria-label={isConnected ? 'Connected' : 'Disconnected'}
              title={connectionStatusLabel}
              data-status-label={connectionStatusLabel}
            />
            <span className="site-user-welcome" title={`Welcome ${welcomeName}`}>
              {onPhotoUpload ? (
                <label className="site-user-icon site-user-icon--uploadable" aria-label="Change profile photo">
                  {profilePhotoUrl ? (
                    <img src={profilePhotoUrl} alt="Profile" className="site-user-photo" />
                  ) : (
                    <svg viewBox="0 0 24 24" focusable="false" className="site-user-avatar-svg">
                      <circle cx="12" cy="8.2" r="3.2" />
                      <path d="M5.5 18.2a6.5 6.5 0 0 1 13 0" />
                    </svg>
                  )}
                  <span className="site-user-camera-overlay" aria-hidden="true">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
                      <circle cx="12" cy="13" r="4" />
                    </svg>
                  </span>
                  <input
                    type="file"
                    accept="image/*"
                    className="site-user-upload-input"
                    onChange={(e) => { const f = e.target.files?.[0]; if (f) onPhotoUpload(f) }}
                  />
                </label>
              ) : (
                <span className="site-user-icon" aria-hidden="true">
                  {profilePhotoUrl ? (
                    <img src={profilePhotoUrl} alt="Profile" className="site-user-photo" />
                  ) : (
                    <svg viewBox="0 0 24 24" focusable="false">
                      <circle cx="12" cy="8.2" r="3.2" />
                      <path d="M5.5 18.2a6.5 6.5 0 0 1 13 0" />
                    </svg>
                  )}
                </span>
              )}
              <span>Welcome {welcomeName}</span>
            </span>
          </div>
          {headerActions ? <div className="button-row site-top-actions">{headerActions}</div> : null}
        </div>
      </header>

      {!isAuthenticated ? (
        <section className="home-clinical-brief" aria-label="Clinical support notice">
          <strong>Clinical continuity:</strong> For urgent follow-ups, complete secure sign-in first so care teams can review the latest patient context.
        </section>
      ) : null}

      {statusBanner ? (
        <section className={`home-status-banner home-status-banner--${statusBanner.tone}`} role="status" aria-live="polite">
          <strong>{statusBanner.title}</strong>
          <span>{statusBanner.message}</span>
          <small>{statusBanner.action}</small>
        </section>
      ) : null}

      <section className="breadcrumb-strip" aria-label="Breadcrumb">
        <span>Home</span>
        <span className="breadcrumb-separator">/</span>
        <strong>{breadcrumbCurrent}</strong>
      </section>

      <ul className="assurance-strip" aria-label="Platform trust signals">
        {trustSignals.map((item) => (
          <li key={item.label} className={`assurance-pill assurance-pill--${item.tone}`}>
            <span className="assurance-pill-icon" aria-hidden="true">{renderTrustIcon(item.tone)}</span>
            <span>{item.label}</span>
          </li>
        ))}
      </ul>

      {!isAuthenticated ? (
        <section className="home-guidance-strip" aria-label="Role guidance">
          {carePaths.map((path) => (
            <article key={path.title} className="home-guidance-card" aria-label={`${path.title} guidance`}>
              <p className="eyebrow">Care pathway</p>
              <h3>
                <span className={`home-guidance-icon home-guidance-icon--${path.tone}`} aria-hidden="true">{path.badge}</span>
                <span>{path.title}</span>
              </h3>
              <p>{path.description}</p>
            </article>
          ))}
        </section>
      ) : null}

      {children}
    </main>
  )
}
