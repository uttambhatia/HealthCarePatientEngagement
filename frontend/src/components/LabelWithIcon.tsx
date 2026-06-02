import type { ReactNode } from 'react'

type LabelIconName =
  | 'patient'
  | 'provider'
  | 'calendar'
  | 'clock'
  | 'channel'
  | 'status'
  | 'owner'
  | 'goal'
  | 'tasks'
  | 'notes'
  | 'followup'
  | 'version'

type LabelWithIconProps = {
  icon: LabelIconName
  children: ReactNode
}

function IconPath({ icon }: { icon: LabelIconName }) {
  switch (icon) {
    case 'patient':
      return (
        <>
          <path d="M12 12a4 4 0 1 0-0.001-8.001A4 4 0 0 0 12 12z" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M5 20a7 7 0 0 1 14 0" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
        </>
      )
    case 'provider':
      return (
        <>
          <path d="M12 4v16M4 12h16" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M7 7h10v10H7z" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
        </>
      )
    case 'calendar':
      return (
        <path d="M7 3v2M17 3v2M4 8h16M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
      )
    case 'clock':
      return (
        <>
          <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" strokeWidth="1.9" />
          <path d="M12 7.8v4.6l3 1.8" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
        </>
      )
    case 'channel':
      return (
        <>
          <rect x="3.5" y="6.5" width="12" height="11" rx="2" fill="none" stroke="currentColor" strokeWidth="1.9" />
          <path d="m15.5 10 5-2.5v9l-5-2.5" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
        </>
      )
    case 'status':
      return (
        <>
          <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" strokeWidth="1.9" />
          <path d="m8.5 12 2.2 2.3 4.8-4.8" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
        </>
      )
    case 'owner':
      return (
        <>
          <circle cx="10" cy="9" r="3" fill="none" stroke="currentColor" strokeWidth="1.9" />
          <path d="M4.8 18a5.3 5.3 0 0 1 10.4 0" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
          <path d="M17 7v6M14 10h6" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
        </>
      )
    case 'goal':
      return (
        <>
          <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" strokeWidth="1.9" />
          <circle cx="12" cy="12" r="4.5" fill="none" stroke="currentColor" strokeWidth="1.9" />
          <circle cx="12" cy="12" r="1.5" fill="currentColor" />
        </>
      )
    case 'tasks':
      return (
        <>
          <path d="M8.5 7h10M8.5 12h10M8.5 17h10" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
          <circle cx="5.3" cy="7" r="1" fill="currentColor" />
          <circle cx="5.3" cy="12" r="1" fill="currentColor" />
          <circle cx="5.3" cy="17" r="1" fill="currentColor" />
        </>
      )
    case 'notes':
      return (
        <>
          <path d="M7 4h10a2 2 0 0 1 2 2v12l-3-2-3 2-3-2-3 2V6a2 2 0 0 1 2-2z" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M9 8h6M9 11h6" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
        </>
      )
    case 'followup':
      return (
        <>
          <path d="M12 5v7l4 2" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M3 12a9 9 0 1 0 3-6.7M3 4v4h4" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
        </>
      )
    case 'version':
      return <path d="M8.5 5h7M8.5 12h7M8.5 19h7M6 5h.01M6 12h.01M6 19h.01" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
    default:
      return null
  }
}

export function LabelWithIcon({ icon, children }: LabelWithIconProps) {
  return (
    <span className={`label-with-icon label-with-icon--${icon}`}>
      <svg className="label-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <IconPath icon={icon} />
      </svg>
      {children}
    </span>
  )
}
