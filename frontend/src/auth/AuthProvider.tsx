import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react'

type Role = 'PATIENT' | 'DOCTOR' | 'ADMIN' | 'COORDINATOR'

type AuthSession = {
  accessToken: string
  role: Role
  displayName: string
}

type AuthContextValue = {
  session: AuthSession | null
  loginAs: (role: Role) => void
  logout: () => void
}

const STORAGE_KEY = 'care-coordination-session'
const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(null)

  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY)
    if (stored) {
      setSession(JSON.parse(stored) as AuthSession)
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    session,
    loginAs: (role) => {
      const nextSession = {
        accessToken: `mock-jwt-${role.toLowerCase()}`,
        role,
        displayName: `${role.toLowerCase()}@healthcare.example`,
      }
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(nextSession))
      setSession(nextSession)
    },
    logout: () => {
      window.localStorage.removeItem(STORAGE_KEY)
      setSession(null)
    },
  }), [session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
