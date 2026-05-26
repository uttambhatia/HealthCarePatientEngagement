export type Role = 'PATIENT' | 'DOCTOR' | 'ADMIN' | 'COORDINATOR'

export function canViewAdmin(role: Role) {
  return role === 'ADMIN' || role === 'COORDINATOR'
}
