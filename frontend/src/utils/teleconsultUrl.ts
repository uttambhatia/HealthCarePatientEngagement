export function isSupportedJoinUrl(value: string) {
  try {
    const parsed = new URL(value)
    return parsed.protocol === 'https:' || parsed.protocol === 'http:'
  } catch {
    return false
  }
}

export function buildTeleconsultCallClientUrl(joinUrl: string, role: string) {
  const encodedJoinUrl = encodeURIComponent(joinUrl)
  const encodedRole = encodeURIComponent(role || 'UNKNOWN')
  return `/teleconsult/call?role=${encodedRole}&joinUrl=${encodedJoinUrl}`
}
