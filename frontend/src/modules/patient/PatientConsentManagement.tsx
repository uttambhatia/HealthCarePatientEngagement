import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { createConsent, listConsents, type ConsentResponse } from '../../services/platformApi'

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

function inferPatientId(claims: Record<string, unknown> | null): string {
  if (!claims) {
    return ''
  }

  const candidates = [
    claims.patientId,
    claims.patient_id,
    claims['patient-id'],
    claims.externalReference,
    claims.external_reference,
    claims.sub,
  ]

  const match = candidates.find((value) => typeof value === 'string' && value.length > 0)
  return match ? String(match) : ''
}

function toDateTimeInputValue(value: Date) {
  const offset = value.getTimezoneOffset()
  const local = new Date(value.getTime() - offset * 60_000)
  return local.toISOString().slice(0, 16)
}

function toConsentFlag(value: unknown) {
  if (typeof value === 'boolean') {
    return value ? 'Granted' : 'Denied'
  }
  if (typeof value === 'string') {
    return value.toLowerCase() === 'true' ? 'Granted' : 'Denied'
  }
  return 'Unknown'
}

function formatEffectiveFrom(value: unknown) {
  if (typeof value !== 'string' || value.length === 0) {
    return 'Not set'
  }
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString()
}

function getConsentCacheKey(scope: string) {
  return `patient-consent-cache:${scope}`
}

function readConsentCache(scope: string) {
  try {
    const raw = window.localStorage.getItem(getConsentCacheKey(scope))
    if (!raw) {
      return [] as ConsentResponse[]
    }

    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) {
      return []
    }

    return parsed.filter((item): item is ConsentResponse => Boolean(item) && typeof item === 'object')
  } catch {
    return []
  }
}

function writeConsentCache(scope: string, records: ConsentResponse[]) {
  try {
    window.localStorage.setItem(getConsentCacheKey(scope), JSON.stringify(records))
  } catch {
    // Ignore cache write failures to keep form UX unaffected.
  }
}

function resolveConsentPatientId(item: ConsentResponse) {
  const fallback = item as ConsentResponse & {
    patient_id?: string
    ['patient-id']?: string
    patient?: string
  }

  return item.patientId || fallback.patient_id || fallback['patient-id'] || fallback.patient || ''
}

type PatientConsentManagementProps = {
  showConsentForm: boolean
}

export function PatientConsentManagement({ showConsentForm }: PatientConsentManagementProps) {
  const { session } = useAuth()
  const [patientId, setPatientId] = useState('')
  const [consentType, setConsentType] = useState('GENERAL_CARE')
  const [granted, setGranted] = useState(true)
  const [effectiveFrom, setEffectiveFrom] = useState(() => toDateTimeInputValue(new Date()))
  const [consents, setConsents] = useState<ConsentResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [resultMessage, setResultMessage] = useState<string | null>(null)
  const [isError, setIsError] = useState(false)
  const [consentPage, setConsentPage] = useState(0)
  const consentsPerPage = 3

  const inferredPatientId = useMemo(() => {
    if (!session) {
      return ''
    }

    // Consent API enforces patient scope from the access token claims.
    const accessClaims = decodeJwtPayload(session.accessToken)
    const accessScopedPatientId = inferPatientId(accessClaims)
    if (accessScopedPatientId) {
      return accessScopedPatientId
    }

    const idClaims = decodeJwtPayload(session.idToken ?? '')
    return inferPatientId(idClaims)
  }, [session])

  const cacheScope = inferredPatientId || 'all'
  const totalConsentPages = Math.max(1, Math.ceil(consents.length / consentsPerPage))
  const visibleConsents = consents.slice(consentPage * consentsPerPage, (consentPage + 1) * consentsPerPage)

  useEffect(() => {
    setConsentPage((current) => Math.min(current, Math.max(0, totalConsentPages - 1)))
  }, [totalConsentPages])

  useEffect(() => {
    if (!patientId && inferredPatientId) {
      setPatientId(inferredPatientId)
    }
  }, [inferredPatientId, patientId])

  useEffect(() => {
    let active = true

    async function loadConsents() {
      if (!session) {
        return
      }

      const cached = readConsentCache(cacheScope)
      if (cached.length > 0) {
        setConsents(cached)
      }

      setLoading(true)
      try {
        const records = await listConsents(session.accessToken)
        if (!active) {
          return
        }

        const filtered = records.filter((item) => !inferredPatientId || resolveConsentPatientId(item) === inferredPatientId)
        const visibleRecords = filtered.length > 0 || !inferredPatientId ? filtered : records
        setConsents(visibleRecords)
        writeConsentCache(cacheScope, visibleRecords)
      } catch {
        if (!active) {
          return
        }

        if (cached.length === 0) {
          setConsents([])
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    void loadConsents()

    return () => {
      active = false
    }
  }, [cacheScope, inferredPatientId, session])

  async function handleSubmit() {
    if (!session) {
      setIsError(true)
      setResultMessage('Please sign in before managing consent.')
      return
    }

    if (!patientId.trim() || !consentType.trim() || !effectiveFrom.trim()) {
      setIsError(true)
      setResultMessage('Patient ID, consent type and effective time are required.')
      return
    }

    setSubmitting(true)
    setIsError(false)
    setResultMessage(null)

    try {
      const effectiveAt = Number.isNaN(Date.parse(effectiveFrom)) ? effectiveFrom : new Date(effectiveFrom).toISOString()
      const created = await createConsent(
        {
          patientId: patientId.trim(),
          consentType,
          granted,
          effectiveFrom: effectiveAt,
        },
        session.accessToken,
      )

      const optimisticRecord: ConsentResponse = {
        id: created.id,
        status: created.status || 'RECORDED',
        patientId: created.patientId || patientId.trim(),
        consentType: created.consentType || consentType,
        granted: typeof created.granted === 'string' ? created.granted : String(granted),
        effectiveFrom: created.effectiveFrom || effectiveAt,
      }

      setConsents((current) => {
        const withoutDuplicate = current.filter((item) => item.id !== optimisticRecord.id)
        const next = [optimisticRecord, ...withoutDuplicate]
        writeConsentCache(cacheScope, next)
        return next
      })

      const records = await listConsents(session.accessToken)
      const filtered = records.filter((item) => !inferredPatientId || resolveConsentPatientId(item) === inferredPatientId)
      const visibleRecords = filtered.length > 0 || !inferredPatientId ? filtered : records
      setConsents(visibleRecords)
      writeConsentCache(cacheScope, visibleRecords)
      setResultMessage('Consent preference updated successfully.')
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'We could not update consent preferences right now. Please try again.'
      setIsError(true)
      setResultMessage(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      {showConsentForm ? (
        <section className="patient-consent-panel" aria-label="Consent management form">
          <h3>Consent management</h3>
          <p>Grant or revoke care data permissions.</p>

          <form className="stacked-form" onSubmit={(event) => { event.preventDefault(); void handleSubmit() }}>
            <div className="field-grid">
              <label className="field-block">
                <span>Patient ID</span>
                <input value={patientId} placeholder="pat-1001" onChange={(event) => setPatientId(event.target.value)} />
                {inferredPatientId ? <small>Prefilled from your session token.</small> : null}
              </label>

              <label className="field-block">
                <span>Consent type</span>
                <select value={consentType} onChange={(event) => setConsentType(event.target.value)}>
                  <option value="GENERAL_CARE">General care</option>
                  <option value="RESEARCH">Research</option>
                  <option value="DATA_SHARING">Data sharing</option>
                  <option value="TELECONSULT">Teleconsultation</option>
                </select>
              </label>
            </div>

            <div className="field-grid">
              <label className="field-block">
                <span>Effective from</span>
                <input type="datetime-local" value={effectiveFrom} onChange={(event) => setEffectiveFrom(event.target.value)} />
              </label>

              <label className="field-block">
                <span>Permission</span>
                <select value={granted ? 'GRANTED' : 'DENIED'} onChange={(event) => setGranted(event.target.value === 'GRANTED')}>
                  <option value="GRANTED">Grant consent</option>
                  <option value="DENIED">Revoke consent</option>
                </select>
              </label>
            </div>

            <div className="form-actions">
              <button type="submit" className="primary-button" disabled={submitting}>
                {submitting ? 'Updating...' : 'Save consent'}
              </button>
            </div>

            {resultMessage ? (
              <p className={isError ? 'form-status form-status--error' : 'form-status form-status--success'} role={isError ? 'alert' : 'status'}>
                {resultMessage}
              </p>
            ) : null}
          </form>
        </section>
      ) : null}

      <section className="patient-consent-history" aria-label="Consent history">
        
        {loading ? <p>Loading consent records...</p> : null}
        {!loading && consents.length > 0 ? (
          <>
            <div className="carousel-controls" aria-label="Consent records navigation">
                <button
                  type="button"
                  className={`${consentPage >= totalConsentPages - 1 ? 'primary-button' : 'secondary-button'} carousel-button`}
                  onClick={() => setConsentPage((page) => Math.max(0, page - 1))}
                  disabled={consentPage === 0}
                >
                <span aria-hidden="true">&lt;</span> Prev
              </button>
                <button
                  type="button"
                  className={`${consentPage < totalConsentPages - 1 ? 'primary-button' : 'secondary-button'} carousel-button`}
                  onClick={() => setConsentPage((page) => Math.min(totalConsentPages - 1, page + 1))}
                  disabled={consentPage >= totalConsentPages - 1}
                >
                Next <span aria-hidden="true">&gt;</span>
              </button>
            </div>
              <div className="patient-consent-list patient-consent-list--paged">
                {visibleConsents.map((item) => (
                <article key={item.id} className="patient-consent-item">
                  <strong>{item.consentType}</strong>
                  <span>Consent ID: {item.id}</span>
                  <span>Status: {item.status}</span>
                  <span>Permission: {toConsentFlag(item.granted)}</span>
                  <span>Effective from: {formatEffectiveFrom(item.effectiveFrom)}</span>
                </article>
              ))}
            </div>
          </>
        ) : null}
        {!loading && consents.length === 0 ? <p className="patient-consent-empty">No consent records found for this patient.</p> : null}
      </section>
    </>
  )
}
