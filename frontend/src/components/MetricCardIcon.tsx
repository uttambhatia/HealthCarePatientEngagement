type MetricVariant = 'volume' | 'activity' | 'narrative' | 'status'

type MetricCardIconProps = {
  variant: MetricVariant
}

function VolumeIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M4 20V10" />
      <path d="M10 20V6" />
      <path d="M16 20V13" />
      <path d="M22 20V4" />
    </svg>
  )
}

function ActivityIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M3 12h4l2-4 4 8 2-4h6" />
    </svg>
  )
}

function NarrativeIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="4" y="4" width="16" height="16" rx="2" />
      <path d="M8 9h8" />
      <path d="M8 13h8" />
      <path d="M8 17h5" />
    </svg>
  )
}

function StatusIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="8" />
      <path d="M8.5 12l2.5 2.5L16 9.5" />
    </svg>
  )
}

export function MetricCardIcon({ variant }: MetricCardIconProps) {
  return (
    <span className="metric-card-icon" aria-hidden="true">
      {variant === 'volume' ? <VolumeIcon /> : null}
      {variant === 'activity' ? <ActivityIcon /> : null}
      {variant === 'narrative' ? <NarrativeIcon /> : null}
      {variant === 'status' ? <StatusIcon /> : null}
    </span>
  )
}

export type { MetricVariant }
