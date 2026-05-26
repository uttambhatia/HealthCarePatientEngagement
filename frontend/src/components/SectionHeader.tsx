type SectionHeaderProps = {
  title: string
  subtitle: string
}

export function SectionHeader({ title, subtitle }: SectionHeaderProps) {
  return (
    <header className="section-header">
      <h3>{title}</h3>
      <p>{subtitle}</p>
    </header>
  )
}
