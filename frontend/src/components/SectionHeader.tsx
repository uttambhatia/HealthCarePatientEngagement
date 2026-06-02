type SectionHeaderProps = {
  title: string
  subtitle: string
  action?: React.ReactNode
}

export function SectionHeader({ title, subtitle, action }: SectionHeaderProps) {
  return (
    <header className="section-header">
      <div>
        <h3>{title}</h3>
        <p>{subtitle}</p>
      </div>
      {action ? <div className="section-header-action">{action}</div> : null}
    </header>
  )
}
