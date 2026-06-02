import type { PropsWithChildren, ReactNode } from 'react'
import { Card } from '../components/Card'

type HomePageProps = PropsWithChildren<{
  isAuthenticated?: boolean
  username?: string
  connectionStatus?: 'checking' | 'up' | 'auth-required' | 'down'
  headerActions?: ReactNode
}>

export function HomePage({
  children,
  isAuthenticated = false,
  username,
  connectionStatus = 'checking',
  headerActions,
}: HomePageProps) {
  const trustSignals = ['HIPAA-ready care workflows', 'FHIR-based care records', 'Reliable care delivery', 'Connected care teams']
  const breadcrumbCurrent = isAuthenticated ? 'Dashboard' : 'Login'
  const welcomeName = username?.split('@')[0] ?? 'Guest'
  const isConnected = connectionStatus === 'up' || connectionStatus === 'auth-required'
  const connectionStatusLabel = isConnected
    ? 'Connection status is healthy'
    : 'Connection status is unhealthy'

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
              <span className="site-user-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" focusable="false">
                  <circle cx="12" cy="8.2" r="3.2" />
                  <path d="M5.5 18.2a6.5 6.5 0 0 1 13 0" />
                </svg>
              </span>
              <span>Welcome {welcomeName}</span>
            </span>
          </div>
          {headerActions ? <div className="button-row site-top-actions">{headerActions}</div> : null}
        </div>
      </header>

      <section className="breadcrumb-strip" aria-label="Breadcrumb">
        <span>Home</span>
        <span className="breadcrumb-separator">/</span>
        <strong>{breadcrumbCurrent}</strong>
      </section>

      <section className="assurance-strip" aria-label="Platform trust signals">
        {trustSignals.map((item) => (
          <span key={item} className="assurance-pill">{item}</span>
        ))}
      </section>

      {children}

      {!isAuthenticated ? (
        <section className="experience-card-shell">
          <Card title="Experience highlights" eyebrow="Team feedback" centeredHeader>
            <section className="experience-band" aria-label="Experience highlights">
              <article className="experience-panel testimonial-panel">
                <p className="eyebrow">Team feedback</p>
                <h2>Care coordination designed for patients, clinicians, and families.</h2>
                <p>
                  Keep registration, appointments, care plans, and communication in one guided journey.
                  The experience supports smoother handoffs and clearer follow-up.
                </p>
                <p className="testimonial-signoff">Care Operations Leadership</p>
              </article>

              <article className="experience-panel mobile-panel">
                <p className="eyebrow">Mobile-first extension</p>
                <h3>Continue care updates from web to mobile</h3>
                <p>
                  Share reminders, care updates, and triage context in a mobile-friendly view so care teams and
                  families stay aligned away from the desktop.
                </p>
                <button type="button" className="secondary-button">Preview mobile experience</button>
              </article>
            </section>
          </Card>
        </section>
      ) : null}
    </main>
  )
}
