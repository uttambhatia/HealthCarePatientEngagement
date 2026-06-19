import { apiClient } from './apiClient'

export const platformRoutes = {
  patients: '/api/patients',
  appointments: '/api/appointments',
  alerts: '/api/alerts',
  careplans: '/api/careplans',
  consents: '/api/consents',
  identityAssertions: '/api/identity/assertions',
  deviceEvents: '/api/devices/events',
  medicalRecords: '/api/medical-records',
  notifications: '/api/notifications',
  serviceBusMessages: '/api/servicebus/messages',
  telemetry: '/api/telemetry',
  profilePhoto: '/api/identity/assertions/profile-photo',
}

type Envelope<T> = {
  correlationId?: string
  data: T
}

export type AppointmentRequest = {
  patientId: string
  providerId: string
  scheduledAt: string
  channel: string
}

export type AppointmentResponse = {
  id: string
  status: string
  patientId: string
  providerId: string
  scheduledAt: string
  channel: string
}

function readStringField(source: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    const value = source[key]
    if (typeof value === 'string' && value.trim().length > 0) {
      return value.trim()
    }
  }
  return ''
}

function normalizeAppointmentRecord(record: Record<string, unknown>): AppointmentResponse | null {
  const id = readStringField(record, ['id'])
  const status = readStringField(record, ['status'])
  const patientId = readStringField(record, ['patientId', 'patient_id', 'patient-id'])
  const providerId = readStringField(record, ['providerId', 'provider_id', 'provider-id'])
  const scheduledAt = readStringField(record, ['scheduledAt', 'scheduled_at', 'scheduled-at'])
  const channel = readStringField(record, ['channel'])

  if (!id || !status || !patientId || !providerId || !scheduledAt || !channel) {
    return null
  }

  return {
    id,
    status,
    patientId,
    providerId,
    scheduledAt,
    channel,
  }
}

export type AvailableSlotsResponse = {
  providerId: string
  date: string
  availableSlots: string[]
}

export type CarePlanRequest = {
  patientId: string
  goal: string
  planStatus: string
  ownerId: string
  tasks: string[]
}

export type CarePlanUpdateRequest = {
  goal: string
  planStatus: string
  ownerId: string
  tasks: string[]
  expectedVersion: number
}

export type TeleconsultationResponse = {
  sessionId: string
  appointmentId: string
  status: string
  doctorJoinUrl: string
  patientJoinUrl: string
  startedAt: string
  joinedAt?: string
  completedAt?: string
  consultationNotes?: string
  followUpRequired: boolean
  nextFollowUpDate?: string
  interactionLogs: string[]
}

export type TeleconsultationCompleteRequest = {
  consultationNotes: string
  followUpRequired: boolean
  nextFollowUpDate?: string
}

export type TeleconsultTokenRequest = {
  sessionId: string
  role: 'DOCTOR' | 'PATIENT'
}

export type TeleconsultTokenResponse = {
  sessionId: string
  role: string
  accessToken: string
  tokenType: string
  expiresAt: string
  joinUrl: string
  tokenProvider: string
}

export type CarePlanResponse = {
  id: string
  status: string
  patientId: string
  goal: string
  planStatus: string
  ownerId: string
  tasks: string[]
  version: number
}

export type CarePlanResponsibilityResponse = {
  patientId: string
  ownerId: string
  carePlanId: string
  planStatus: string
  version: number
}

export type ConsentRequest = {
  patientId: string
  consentType: string
  granted: boolean
  effectiveFrom: string
}

export type ConsentResponse = {
  id: string
  status: string
  patientId: string
  consentType: string
  granted: string
  effectiveFrom: string
}

export type TelemetryResponse = {
  id: string
  status: string
  deviceId: string
  metricType: string
  metricValue: string
  recordedAt: string
}

export type CreatePatientRequest = {
  externalReference: string
  givenName: string
  familyName: string
  birthDate: string
  email: string
  phone: string
  demographics: string
}

export type PatientResponse = CreatePatientRequest & {
  id: string
  status: string
  decisionAudit?: string
  idProofUploaded?: boolean
  idProofFileName?: string
}

export type PatientRegistrationResponse = PatientResponse

function extractData<T>(payload: Envelope<T> | T): T {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return (payload as Envelope<T>).data
  }
  return payload as T
}

function normalizeArray(payload: unknown): Record<string, unknown>[] {
  const data = extractData(payload as Envelope<unknown> | unknown)
  if (Array.isArray(data)) {
    return data.filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === 'object')
  }
  return []
}

export async function listPatients(token?: string) {
  const payload = await apiClient<Envelope<Record<string, unknown>[]> | Record<string, unknown>[]>(platformRoutes.patients, { token })
  return normalizeArray(payload)
}

export async function listAppointments(token?: string) {
  const payload = await apiClient<Envelope<AppointmentResponse[]> | AppointmentResponse[]>(platformRoutes.appointments, { token })
  return normalizeArray(payload)
    .map(normalizeAppointmentRecord)
    .filter((item): item is AppointmentResponse => Boolean(item))
}

export async function listAvailableSlots(providerId: string, date: string, token?: string) {
  const endpoint = `${platformRoutes.appointments}/available-slots?providerId=${encodeURIComponent(providerId)}&date=${encodeURIComponent(date)}`
  const payload = await apiClient<Envelope<AvailableSlotsResponse> | AvailableSlotsResponse>(endpoint, { token })
  const data = extractData(payload)

  if (!data || typeof data !== 'object') {
    return []
  }

  return Array.isArray(data.availableSlots)
    ? data.availableSlots.filter((value): value is string => typeof value === 'string' && value.length > 0)
    : []
}

export async function listCarePlans(token?: string) {
  const payload = await apiClient<Envelope<CarePlanResponse[]> | CarePlanResponse[]>(platformRoutes.careplans, { token })
  return normalizeArray(payload) as CarePlanResponse[]
}

export async function getCarePlanResponsibility(patientId: string, token?: string) {
  const payload = await apiClient<Envelope<CarePlanResponsibilityResponse> | CarePlanResponsibilityResponse>(
    `${platformRoutes.careplans}/responsibility/${encodeURIComponent(patientId)}`,
    { token },
  )
  return extractData(payload)
}

export async function listConsents(token?: string) {
  const payload = await apiClient<Envelope<ConsentResponse[]> | ConsentResponse[]>(platformRoutes.consents, { token })
  return normalizeArray(payload) as ConsentResponse[]
}

export async function listNotifications(token?: string) {
  const payload = await apiClient<Envelope<Record<string, unknown>[]> | Record<string, unknown>[]>(platformRoutes.notifications, { token })
  return normalizeArray(payload)
}

export async function listTelemetry(token?: string) {
  const payload = await apiClient<Envelope<TelemetryResponse[]> | TelemetryResponse[]>(platformRoutes.telemetry, { token })
  return normalizeArray(payload) as TelemetryResponse[]
}

export async function listTelemetryByPatient(
  patientId: string,
  options?: {
    metricType?: string
    startTime?: string
    endTime?: string
    token?: string
  },
) {
  const query = new URLSearchParams()
  if (options?.metricType) {
    query.set('metricType', options.metricType)
  }
  if (options?.startTime) {
    query.set('startTime', options.startTime)
  }
  if (options?.endTime) {
    query.set('endTime', options.endTime)
  }

  const endpoint = `${platformRoutes.telemetry}/by-patient/${encodeURIComponent(patientId)}${query.toString() ? `?${query.toString()}` : ''}`
  const payload = await apiClient<Envelope<TelemetryResponse[]> | TelemetryResponse[]>(endpoint, { token: options?.token })
  return normalizeArray(payload) as TelemetryResponse[]
}

export async function listTelemetryMetricTypes(patientId?: string, token?: string) {
  const query = new URLSearchParams()
  if (patientId && patientId.trim()) {
    query.set('patientId', patientId.trim())
  }
  const endpoint = `${platformRoutes.telemetry}/metric-types${query.toString() ? `?${query.toString()}` : ''}`
  const payload = await apiClient<Envelope<string[]> | string[]>(endpoint, { token })
  const data = extractData(payload)
  if (!Array.isArray(data)) {
    return []
  }
  return data
    .filter((value): value is string => typeof value === 'string')
    .map((value) => value.trim())
    .filter((value) => value.length > 0)
}

export async function createAppointment(request: AppointmentRequest, token?: string) {
  const payload = await apiClient<Envelope<Record<string, unknown>> | Record<string, unknown>>(platformRoutes.appointments, {
    method: 'POST',
    body: JSON.stringify(request),
    token,
  })
  return extractData(payload)
}

export async function createCarePlan(request: CarePlanRequest, token?: string) {
  const payload = await apiClient<Envelope<CarePlanResponse> | CarePlanResponse>(platformRoutes.careplans, {
    method: 'POST',
    body: JSON.stringify(request),
    token,
  })
  return extractData(payload)
}

export async function updateCarePlan(id: string, request: CarePlanUpdateRequest, token?: string) {
  const payload = await apiClient<Envelope<CarePlanResponse> | CarePlanResponse>(`${platformRoutes.careplans}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(request),
    token,
  })
  return extractData(payload)
}

export async function createConsent(request: ConsentRequest, token?: string) {
  const payload = await apiClient<Envelope<ConsentResponse> | ConsentResponse>(platformRoutes.consents, {
    method: 'POST',
    body: JSON.stringify(request),
    token,
  })
  return extractData(payload)
}

export async function createPatient(request: CreatePatientRequest, token?: string) {
  const payload = await apiClient<Envelope<PatientResponse> | PatientResponse>(platformRoutes.patients, {
    method: 'POST',
    body: JSON.stringify(request),
    token,
  })
  return extractData(payload)
}

export async function uploadPatientIdProof(id: string, file: File, token?: string) {
  const formData = new FormData()
  formData.append('file', file)

  const payload = await apiClient<Envelope<PatientResponse> | PatientResponse>(
    `${platformRoutes.patients}/${encodeURIComponent(id)}/id-proof`,
    {
      method: 'POST',
      body: formData,
      token,
    },
  )
  return extractData(payload)
}

export async function downloadPatientIdProof(id: string, token?: string) {
  const response = await fetch(`${platformRoutes.patients}/${encodeURIComponent(id)}/id-proof`, {
    method: 'GET',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    const body = await response.text()
    throw new Error(body || `Unable to download ID proof. Status ${response.status}`)
  }

  return response.blob()
}

export async function uploadProfilePhoto(file: File, token?: string) {
  const formData = new FormData()
  formData.append('file', file)

  await apiClient<void>(platformRoutes.profilePhoto, {
    method: 'POST',
    body: formData,
    token,
  })
}

export async function getProfilePhotoUrl(token?: string) {
  const response = await fetch(platformRoutes.profilePhoto, {
    method: 'GET',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (response.status === 404) {
    return null
  }

  if (!response.ok) {
    const body = await response.text()
    throw new Error(body || `Unable to load profile photo. Status ${response.status}`)
  }

  const blob = await response.blob()
  return URL.createObjectURL(blob)
}

export async function approvePatientRegistration(id: string, token?: string) {
  const payload = await apiClient<Envelope<PatientRegistrationResponse> | PatientRegistrationResponse>(
    `${platformRoutes.patients}/${encodeURIComponent(id)}/approval/approve`,
    {
      method: 'PATCH',
      token,
    },
  )
  return extractData(payload)
}

export async function rejectPatientRegistration(id: string, token?: string) {
  const payload = await apiClient<Envelope<PatientRegistrationResponse> | PatientRegistrationResponse>(
    `${platformRoutes.patients}/${encodeURIComponent(id)}/approval/reject`,
    {
      method: 'PATCH',
      token,
    },
  )
  return extractData(payload)
}

export async function resendPatientRegistrationNotification(id: string, token?: string) {
  const payload = await apiClient<Envelope<PatientRegistrationResponse> | PatientRegistrationResponse>(
    `${platformRoutes.patients}/${encodeURIComponent(id)}/notifications/resend`,
    {
      method: 'POST',
      token,
    },
  )
  return extractData(payload)
}

export async function startTeleconsultation(appointmentId: string, token?: string) {
  const payload = await apiClient<Envelope<TeleconsultationResponse> | TeleconsultationResponse>(
    `/api/appointments/${encodeURIComponent(appointmentId)}/teleconsult/start`,
    { method: 'POST', token },
  )
  return extractData(payload) as TeleconsultationResponse
}

export async function joinTeleconsultation(appointmentId: string, token?: string) {
  const payload = await apiClient<Envelope<TeleconsultationResponse> | TeleconsultationResponse>(
    `/api/appointments/${encodeURIComponent(appointmentId)}/teleconsult/join`,
    { method: 'POST', token },
  )
  return extractData(payload) as TeleconsultationResponse
}

export async function completeTeleconsultation(appointmentId: string, request: TeleconsultationCompleteRequest, token?: string) {
  const payload = await apiClient<Envelope<TeleconsultationResponse> | TeleconsultationResponse>(
    `/api/appointments/${encodeURIComponent(appointmentId)}/teleconsult/complete`,
    { method: 'POST', body: JSON.stringify(request), token },
  )
  return extractData(payload) as TeleconsultationResponse
}

export async function requestTeleconsultToken(request: TeleconsultTokenRequest, token?: string) {
  const payload = await apiClient<Envelope<TeleconsultTokenResponse> | TeleconsultTokenResponse>(
    '/api/acs/teleconsult/token',
    { method: 'POST', body: JSON.stringify(request), token },
  )
  return extractData(payload) as TeleconsultTokenResponse
}
