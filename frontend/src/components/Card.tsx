import type { PropsWithChildren, ReactNode } from 'react'

type CardProps = PropsWithChildren<{
  title: string
  eyebrow?: string
  actions?: ReactNode
}>

export function Card({ title, eyebrow, actions, children }: CardProps) {
  return (
    <section className="card">
      <div className="card-header">
        <div>
          {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
          <h2>{title}</h2>
        </div>
        {actions}
      </div>
      {children}
    </section>
  )
}
