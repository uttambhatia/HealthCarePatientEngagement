import type { PropsWithChildren, ReactNode } from 'react'

type HomePageProps = PropsWithChildren<{
  headerActions?: ReactNode
}>

export function HomePage({ children, headerActions }: HomePageProps) {
  return (
    <main className="page-shell">
      <header className="hero">
        <div>
          <span className="chip">Healthcare Patient Engagement</span>
          <h1>Care coordination, telehealth and remote monitoring on Azure</h1>
          <p>
            Microservice-aligned experience with OAuth2 sign-in, role-aware dashboards,
            event-driven notifications and remote telemetry monitoring.
          </p>
        </div>
        {headerActions}
      </header>
      {children}
    </main>
  )
}
