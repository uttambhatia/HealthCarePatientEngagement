import type { PropsWithChildren, ReactNode } from 'react'

type CardProps = PropsWithChildren<{
  title: string
  eyebrow?: string
  subtitle?: string
  actions?: ReactNode
  centeredHeader?: boolean
}>

export function Card({ title, eyebrow, subtitle, actions, centeredHeader = false, children }: CardProps) {
  return (
    <section className="card">
      <div className={`card-header${centeredHeader ? ' card-header--centered' : ''}`}>
        <div>
          {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
          <h2>{title}</h2>
          {subtitle ? <p className="card-header-subtitle">{subtitle}</p> : null}
        </div>
        {actions}
      </div>
      {children}
    </section>
  )
}
