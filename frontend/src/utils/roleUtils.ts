export type Role = 'PATIENT' | 'DOCTOR' | 'ADMIN' | 'COORDINATOR'

export function canViewAdmin(role: Role) {
  return role === 'ADMIN' || role === 'COORDINATOR'
}

export function canViewPatientDashboard(role: Role) {
  return role === 'PATIENT' || role === 'DOCTOR' || role === 'ADMIN' || role === 'COORDINATOR'
}

export function canBookAppointments(role: Role) {
  return role === 'PATIENT' || role === 'DOCTOR' || role === 'COORDINATOR'
}

export function canManageCarePlans(role: Role) {
  return role === 'COORDINATOR'
}

export function canViewNotifications(role: Role) {
  return role === 'PATIENT' || role === 'DOCTOR' || role === 'COORDINATOR' || role === 'ADMIN'
}

export function canUseTeleconsultation(role: Role) {
  return role === 'PATIENT' || role === 'DOCTOR' || role === 'COORDINATOR'
}

export function canViewIotMonitoring(role: Role) {
  return role === 'DOCTOR' || role === 'COORDINATOR' || role === 'ADMIN'
}
