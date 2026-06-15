export type ApiOptions = RequestInit & { token?: string }

type ErrorInterceptor = (error: ApiError) => void
type AuthTokenGetter = () => string | null

const rawApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
const API_BASE_URL = rawApiBaseUrl ? rawApiBaseUrl.replace(/\/$/, '') : ''
const BYPASS_AUTH = import.meta.env.VITE_BYPASS_AUTH === 'true'

let authTokenGetter: AuthTokenGetter | null = null
let errorInterceptor: ErrorInterceptor | null = null

export class ApiError extends Error {
  status: number
  body: unknown

  constructor(message: string, status: number, body: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

export function setApiAuthTokenGetter(getter: AuthTokenGetter | null) {
  authTokenGetter = getter
}

export function setApiErrorInterceptor(interceptor: ErrorInterceptor | null) {
  errorInterceptor = interceptor
}

function buildUrl(input: string) {
  if (/^https?:\/\//i.test(input)) {
    return input
  }

  if (!API_BASE_URL) {
    return input
  }

  return `${API_BASE_URL}${input.startsWith('/') ? input : `/${input}`}`
}

function createCorrelationId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `corr-${Date.now()}`
}

async function parseResponseBody(response: Response) {
  const text = await response.text()
  if (!text) {
    return null
  }

  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function extractErrorFields(body: unknown) {
  if (typeof body === 'string') {
    return { code: '', message: body }
  }

  if (!body || typeof body !== 'object') {
    return { code: '', message: '' }
  }

  const directCode = 'code' in body && typeof body.code === 'string' ? body.code : ''
  const directMessage = 'message' in body && typeof body.message === 'string' ? body.message : ''
  const directError = 'error' in body && typeof body.error === 'string' ? body.error : ''
  const directDetail = 'detail' in body && typeof body.detail === 'string' ? body.detail : ''
  const directDescription = 'error_description' in body && typeof body.error_description === 'string'
    ? body.error_description
    : ''

  if (directCode || directMessage || directError || directDetail || directDescription) {
    return { code: directCode, message: directMessage || directError || directDetail || directDescription }
  }

  if ('data' in body && body.data && typeof body.data === 'object') {
    const nestedCode = 'code' in body.data && typeof body.data.code === 'string' ? body.data.code : ''
    const nestedMessage = 'message' in body.data && typeof body.data.message === 'string' ? body.data.message : ''
    return { code: nestedCode, message: nestedMessage }
  }

  return { code: '', message: '' }
}

function toUserFriendlyApiMessage(status: number, body: unknown) {
  const { code, message } = extractErrorFields(body)
  const normalizedMessage = message.toUpperCase()
  const normalizedCode = code.toUpperCase()

  if (
    normalizedCode === 'CONSENT_REQUIRED'
    || normalizedMessage.includes('CONSENT_REQUIRED')
    || normalizedMessage.includes('CONSENT POLICY')
  ) {
    return message || 'Access denied by consent policy: CONSENT_REQUIRED'
  }

  if (code === 'SLOT_ALREADY_BOOKED') {
    return 'This time slot was just booked by someone else. Please select another available slot.'
  }

  if (code === 'VALIDATION_ERROR') {
    return message || 'Please review the form values and try again.'
  }

  if (status === 401 || status === 403) {
    if (message) {
      return message
    }
    return 'Authentication required. Sign in with OAuth2 to access backend APIs.'
  }

  if (status === 404) {
    return message || 'The requested record was not found.'
  }

  if (status === 409) {
    return message || 'This request conflicts with existing data. Please refresh and try again.'
  }

  if (status === 422 || status === 400) {
    return message || 'Some input values are invalid. Please correct them and try again.'
  }

  if (status >= 500) {
    return 'Backend service is unavailable. Try again shortly.'
  }

  return message || 'Request failed. Please try again.'
}

function getPathname(url: string) {
  if (/^https?:\/\//i.test(url)) {
    return new URL(url).pathname
  }
  return url
}

function createBypassPayload(url: string, method: string) {
  const path = getPathname(url)

  if (method === 'GET') {
    if (
      path === '/api/patients'
      || path === '/api/appointments'
      || path === '/api/consents'
      || path === '/api/careplans'
      || path === '/api/notifications'
      || path === '/api/telemetry'
      || path.startsWith('/api/telemetry/by-patient/')
    ) {
      if (path === '/api/appointments') {
        return {
          correlationId: 'dev-bypass',
          data: [
            {
              id: 'apt-dev-0001',
              status: 'BOOKED',
              patientId: 'pat-dev-0001',
              providerId: 'prov-44',
              scheduledAt: '2026-06-01T09:30:00Z',
              channel: 'VIDEO',
            },
            {
              id: 'apt-dev-0002',
              status: 'BOOKED',
              patientId: 'pat-dev-0002',
              providerId: 'prov-66',
              scheduledAt: '2026-06-02T10:00:00Z',
              channel: 'IN_PERSON',
            },
          ],
        }
      }

      if (path === '/api/consents') {
        return {
          correlationId: 'dev-bypass',
          data: [
            {
              id: 'con-dev-0001',
              status: 'RECORDED',
              patientId: 'pat-dev-0001',
              consentType: 'GENERAL_CARE',
              granted: 'true',
              effectiveFrom: '2026-05-31T10:00:00Z',
            },
          ],
        }
      }

      if (path.startsWith('/api/careplans/responsibility/')) {
        return {
          correlationId: 'dev-bypass',
          data: {
            patientId: path.split('/').pop() ?? 'pat-dev-0001',
            ownerId: 'coordinator-dev',
            carePlanId: 'cp-dev-0001',
            planStatus: 'ACTIVE',
            version: 2,
          },
        }
      }

      if (path.startsWith('/api/telemetry/by-patient/')) {
        return {
          correlationId: 'dev-bypass',
          data: [
            {
              id: 'tel-dev-0001',
              status: 'RECORDED',
              deviceId: 'dev-seed-1001',
              metricType: 'HEART_RATE',
              metricValue: '82',
              recordedAt: '2026-06-01T08:00:00Z',
            },
            {
              id: 'tel-dev-0002',
              status: 'RECORDED',
              deviceId: 'dev-seed-1002',
              metricType: 'HEART_RATE',
              metricValue: '88',
              recordedAt: '2026-06-01T08:10:00Z',
            },
            {
              id: 'tel-dev-0003',
              status: 'RECORDED',
              deviceId: 'dev-seed-1001',
              metricType: 'HEART_RATE',
              metricValue: '79',
              recordedAt: '2026-06-01T08:20:00Z',
            },
          ],
        }
      }

      if (path.startsWith('/api/telemetry/metric-types')) {
        return {
          correlationId: 'dev-bypass',
          data: ['HEART_RATE', 'SPO2', 'GLUCOSE'],
        }
      }

      return {
        correlationId: 'dev-bypass',
        data: [],
      }
    }

    if (path === '/api/appointments/available-slots') {
      return {
        correlationId: 'dev-bypass',
        data: {
          providerId: 'prov-44',
          date: new Date().toISOString().slice(0, 10),
          availableSlots: [
            '2026-06-01T09:00:00Z',
            '2026-06-01T09:30:00Z',
            '2026-06-01T10:00:00Z',
          ],
        },
      }
    }
  }

  if (method === 'POST' && path === '/api/appointments') {
    return {
      correlationId: 'dev-bypass',
      data: {
        id: 'apt-dev-0001',
        status: 'BOOKED',
      },
    }
  }

  if (method === 'POST' && path === '/api/patients') {
    return {
      correlationId: 'dev-bypass',
      data: {
        id: 'pat-dev-0001',
        status: 'ACTIVE',
        externalReference: 'EXT-DEV-0001',
        givenName: 'Demo',
        familyName: 'Patient',
        birthDate: '1988-06-21',
        email: 'demo.patient@example.com',
        phone: '+1-555-0100',
        demographics: 'BYPASS',
      },
    }
  }

  if (method === 'POST' && path === '/api/consents') {
    return {
      correlationId: 'dev-bypass',
      data: {
        id: 'con-dev-0002',
        status: 'RECORDED',
      },
    }
  }

  if (method === 'POST' && path === '/api/careplans') {
    return {
      correlationId: 'dev-bypass',
      data: {
        id: 'cp-dev-0001',
        status: 'MANAGED',
        patientId: 'pat-dev-0001',
        goal: 'Improve care coordination',
        planStatus: 'ACTIVE',
        ownerId: 'coordinator-dev',
        tasks: ['Review intake', 'Schedule follow-up'],
        version: 1,
      },
    }
  }

  if (method === 'PUT' && path.startsWith('/api/careplans/')) {
    const id = path.split('/').pop() ?? 'cp-dev-0001'
    return {
      correlationId: 'dev-bypass',
      data: {
        id,
        status: 'MANAGED',
        patientId: 'pat-dev-0001',
        goal: 'Updated care coordination',
        planStatus: 'ACTIVE',
        ownerId: 'coordinator-dev',
        tasks: ['Review intake', 'Confirm appointment', 'Share updated plan'],
        version: 2,
      },
    }
  }

  if (method === 'POST' && path.includes('/teleconsult/start')) {
    return {
      correlationId: 'dev-bypass',
      data: {
        sessionId: 'tcs-dev-0001',
        appointmentId: 'apt-dev-0001',
        status: 'INITIATED',
        doctorJoinUrl: 'https://teleconsult.healthcare.local/session/tcs-dev-0001?role=DOCTOR',
        patientJoinUrl: 'https://teleconsult.healthcare.local/session/tcs-dev-0001?role=PATIENT',
        startedAt: new Date().toISOString(),
        followUpRequired: false,
        interactionLogs: ['Teleconsultation session initiated'],
      },
    }
  }

  if (method === 'POST' && path.includes('/teleconsult/join')) {
    return {
      correlationId: 'dev-bypass',
      data: {
        sessionId: 'tcs-dev-0001',
        appointmentId: 'apt-dev-0001',
        status: 'IN_PROGRESS',
        doctorJoinUrl: 'https://teleconsult.healthcare.local/session/tcs-dev-0001?role=DOCTOR',
        patientJoinUrl: 'https://teleconsult.healthcare.local/session/tcs-dev-0001?role=PATIENT',
        startedAt: new Date().toISOString(),
        joinedAt: new Date().toISOString(),
        followUpRequired: false,
        interactionLogs: ['Teleconsultation initiated', 'Patient joined session'],
      },
    }
  }

  if (method === 'POST' && path.includes('/teleconsult/complete')) {
    return {
      correlationId: 'dev-bypass',
      data: {
        sessionId: 'tcs-dev-0001',
        appointmentId: 'apt-dev-0001',
        status: 'COMPLETED',
        doctorJoinUrl: 'https://teleconsult.healthcare.local/session/tcs-dev-0001?role=DOCTOR',
        patientJoinUrl: 'https://teleconsult.healthcare.local/session/tcs-dev-0001?role=PATIENT',
        startedAt: new Date().toISOString(),
        joinedAt: new Date().toISOString(),
        completedAt: new Date().toISOString(),
        consultationNotes: 'Patient presented with follow-up on prescribed treatment.',
        followUpRequired: true,
        nextFollowUpDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
        interactionLogs: ['Teleconsultation initiated', 'Patient joined', 'Consultation completed'],
      },
    }
  }

  return null
}

export async function apiClient<T>(input: string, options: ApiOptions = {}): Promise<T> {
  const requestUrl = buildUrl(input)
  const method = (options.method ?? 'GET').toUpperCase()
  const path = getPathname(requestUrl)
  const token = options.token ?? authTokenGetter?.() ?? undefined
  const isMockToken = typeof token === 'string' && token.startsWith('mock-jwt-')
  const effectiveToken = isMockToken ? undefined : token

  if (!BYPASS_AUTH && isMockToken && path.startsWith('/api/')) {
    const apiError = new ApiError(
      'Role preview is UI-only. Use "Sign in securely" to call protected backend APIs.',
      401,
      null,
    )
    errorInterceptor?.(apiError)
    throw apiError
  }

  const response = await fetch(requestUrl, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Correlation-Id': createCorrelationId(),
      ...(effectiveToken ? { Authorization: `Bearer ${effectiveToken}` } : {}),
      ...(options.headers ?? {}),
    },
  })

  const parsedBody = await parseResponseBody(response)

  if (!response.ok) {
    if ((BYPASS_AUTH || isMockToken) && !effectiveToken && (response.status === 401 || response.status === 403)) {
      const bypassPayload = createBypassPayload(requestUrl, method)
      if (bypassPayload !== null) {
        return bypassPayload as T
      }
    }

    const apiError = new ApiError(toUserFriendlyApiMessage(response.status, parsedBody), response.status, parsedBody)
    errorInterceptor?.(apiError)
    throw apiError
  }

  return parsedBody as T
}
