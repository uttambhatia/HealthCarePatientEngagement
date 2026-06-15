import { createContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import type { Role } from '../utils/roleUtils'

type AuthSession = {
  accessToken: string
  idToken?: string
  role: Role
  displayName: string
  expiresAt?: number
}

type AuthContextValue = {
  session: AuthSession | null
  authReady: boolean
  authError: string | null
  isOidcConfigured: boolean
  loginWithOidc: (loginHint?: string) => Promise<void>
  loginAs: (role: Role) => void
  logout: () => void
}

const STORAGE_KEY = 'care-coordination-session'
const OIDC_STATE_KEY = 'oidc_state'
const OIDC_VERIFIER_KEY = 'oidc_pkce_verifier'

const oidcConfig = {
  clientId: import.meta.env.VITE_OIDC_CLIENT_ID ?? '',
  authorizationEndpoint: import.meta.env.VITE_OIDC_AUTHORIZATION_ENDPOINT ?? '',
  tokenEndpoint: import.meta.env.VITE_OIDC_TOKEN_ENDPOINT ?? '',
  logoutEndpoint: import.meta.env.VITE_OIDC_LOGOUT_ENDPOINT ?? '',
  redirectUri: import.meta.env.VITE_OIDC_REDIRECT_URI ?? window.location.origin,
  scope: import.meta.env.VITE_OIDC_SCOPE ?? 'openid profile email',
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function randomString(size: number) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  const bytes = new Uint8Array(size)
  crypto.getRandomValues(bytes)
  return Array.from(bytes, (item) => chars[item % chars.length]).join('')
}

function base64UrlEncode(bytes: Uint8Array) {
  const value = btoa(String.fromCharCode(...bytes))
  return value.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

async function createPkceChallenge(verifier: string) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))
  return base64UrlEncode(new Uint8Array(digest))
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const [, payload] = token.split('.')
  if (!payload) {
    return null
  }

  try {
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decoded) as Record<string, unknown>
  } catch {
    return null
  }
}

function mapRoleFromClaims(claims: Record<string, unknown> | null): Role {
  const rolesClaim = claims?.roles
  if (Array.isArray(rolesClaim)) {
    const upper = rolesClaim.map((role) => String(role).toUpperCase())
    if (upper.includes('ADMIN')) {
      return 'ADMIN'
    }
    if (upper.includes('COORDINATOR')) {
      return 'COORDINATOR'
    }
    if (upper.includes('DOCTOR')) {
      return 'DOCTOR'
    }
    if (upper.includes('PATIENT')) {
      return 'PATIENT'
    }
  }

  const singleRole = claims?.role
  if (typeof singleRole === 'string') {
    const upperRole = singleRole.toUpperCase()
    if (upperRole === 'ADMIN' || upperRole === 'COORDINATOR' || upperRole === 'DOCTOR' || upperRole === 'PATIENT') {
      return upperRole
    }
  }

  return 'PATIENT'
}

function mapDisplayName(claims: Record<string, unknown> | null): string {
  const possible = [claims?.name, claims?.preferred_username, claims?.email, claims?.sub]
  const firstValue = possible.find((value) => typeof value === 'string' && value.length > 0)
  return firstValue ? String(firstValue) : 'unknown@healthcare.example'
}

function readSessionFromStorage() {
  const stored = window.localStorage.getItem(STORAGE_KEY)
  return stored ? (JSON.parse(stored) as AuthSession) : null
}

function hasQueryOidcParams(search: string) {
  const params = new URLSearchParams(search)
  return params.has('code') || params.has('error')
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(() => readSessionFromStorage())
  const [authReady, setAuthReady] = useState(false)
  const [authError, setAuthError] = useState<string | null>(null)

  const isOidcConfigured = Boolean(
    oidcConfig.clientId && oidcConfig.authorizationEndpoint && oidcConfig.tokenEndpoint && oidcConfig.redirectUri,
  )

  useEffect(() => {
    let active = true

    async function runLoginFinalization() {
      if (!isOidcConfigured || !hasQueryOidcParams(window.location.search)) {
        if (active) {
          setAuthReady(true)
        }
        return
      }

      const params = new URLSearchParams(window.location.search)
      const code = params.get('code')
      const error = params.get('error')
      const returnedState = params.get('state')

      if (error) {
        if (active) {
          setAuthError(`OIDC sign-in failed: ${error}`)
          setAuthReady(true)
        }
        window.history.replaceState({}, '', window.location.pathname)
        return
      }

      if (!code || !returnedState) {
        if (active) {
          setAuthReady(true)
        }
        return
      }

      const expectedState = window.sessionStorage.getItem(OIDC_STATE_KEY)
      const verifier = window.sessionStorage.getItem(OIDC_VERIFIER_KEY)

      if (!expectedState || !verifier || returnedState !== expectedState) {
        if (active) {
          setAuthError('OIDC sign-in failed due to invalid state.')
          setAuthReady(true)
        }
        window.history.replaceState({}, '', window.location.pathname)
        return
      }

      try {
        const body = new URLSearchParams({
          grant_type: 'authorization_code',
          code,
          client_id: oidcConfig.clientId,
          redirect_uri: oidcConfig.redirectUri,
          code_verifier: verifier,
        })

        const response = await fetch(oidcConfig.tokenEndpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: body.toString(),
        })

        if (!response.ok) {
          throw new Error(`Token exchange failed with status ${response.status}`)
        }

        const payload = (await response.json()) as {
          access_token: string
          id_token?: string
          expires_in?: number
        }

        // Access token carries API app-role claims; ID token carries profile/display claims.
        const roleClaims = decodeJwtPayload(payload.access_token)
        const identityClaims = decodeJwtPayload(payload.id_token ?? payload.access_token)
        const nextSession: AuthSession = {
          accessToken: payload.access_token,
          idToken: payload.id_token,
          role: mapRoleFromClaims(roleClaims),
          displayName: mapDisplayName(identityClaims),
          expiresAt: payload.expires_in ? Date.now() + payload.expires_in * 1000 : undefined,
        }

        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(nextSession))
        if (active) {
          setSession(nextSession)
        }
      } catch (cause) {
        const message = cause instanceof Error ? cause.message : 'Unknown OIDC callback error'
        if (active) {
          setAuthError(message)
        }
      } finally {
        window.sessionStorage.removeItem(OIDC_STATE_KEY)
        window.sessionStorage.removeItem(OIDC_VERIFIER_KEY)
        window.history.replaceState({}, '', window.location.pathname)
        if (active) {
          setAuthReady(true)
        }
      }
    }

    void runLoginFinalization()

    return () => {
      active = false
    }
  }, [isOidcConfigured])

  const value = useMemo<AuthContextValue>(() => ({
    session,
    authReady,
    authError,
    isOidcConfigured,
    loginWithOidc: async (loginHint?: string) => {
      if (!isOidcConfigured) {
        setAuthError(null)
        return
      }

      const state = randomString(48)
      const verifier = randomString(64)
      const challenge = await createPkceChallenge(verifier)

      window.sessionStorage.setItem(OIDC_STATE_KEY, state)
      window.sessionStorage.setItem(OIDC_VERIFIER_KEY, verifier)

      const authUrl = new URL(oidcConfig.authorizationEndpoint)
      authUrl.searchParams.set('response_type', 'code')
      authUrl.searchParams.set('client_id', oidcConfig.clientId)
      authUrl.searchParams.set('redirect_uri', oidcConfig.redirectUri)
      authUrl.searchParams.set('scope', oidcConfig.scope)
      authUrl.searchParams.set('state', state)
      authUrl.searchParams.set('code_challenge', challenge)
      authUrl.searchParams.set('code_challenge_method', 'S256')

      const normalizedLoginHint = loginHint?.trim()
      if (normalizedLoginHint) {
        authUrl.searchParams.set('login_hint', normalizedLoginHint)
      }

      window.location.assign(authUrl.toString())
    },
    loginAs: (role) => {
      const nextSession = {
        accessToken: `mock-jwt-${role.toLowerCase()}`,
        role,
        displayName: `${role.toLowerCase()}@healthcare.example`,
      }
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(nextSession))
      setAuthError(null)
      setSession(nextSession)
    },
    logout: () => {
      window.localStorage.removeItem(STORAGE_KEY)
      setSession(null)
      setAuthError(null)
      if (isOidcConfigured && oidcConfig.logoutEndpoint) {
        const logoutUrl = new URL(oidcConfig.logoutEndpoint)
        logoutUrl.searchParams.set('post_logout_redirect_uri', oidcConfig.redirectUri)
        window.location.assign(logoutUrl.toString())
      }
    },
  }), [authError, authReady, isOidcConfigured, session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export { AuthContext }
