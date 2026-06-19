import { useState } from 'react'
import {
  createPatient,
  uploadPatientIdProof,
  type CreatePatientRequest,
  type PatientResponse,
} from '../../services/platformApi'

type PatientRegistrationFormProps = {
  onBack: () => void
  onSaved: (patient: PatientResponse) => void
}

const initialForm: CreatePatientRequest = {
  externalReference: '',
  givenName: '',
  familyName: '',
  birthDate: '',
  email: '',
  phone: '',
  demographics: '',
}

type FormField = keyof CreatePatientRequest
type FormErrors = Partial<Record<FormField, string>>

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const phonePattern = /^[+]?[-()\d\s]{7,20}$/

function validateField(field: FormField, value: string): string | null {
  const trimmedValue = value.trim()

  if (!trimmedValue) {
    return 'This field is required.'
  }

  if (field === 'externalReference' && !/^[A-Za-z0-9_-]{3,40}$/.test(trimmedValue)) {
    return 'Use 3-40 letters, numbers, hyphen, or underscore.'
  }

  if ((field === 'givenName' || field === 'familyName') && !/^[A-Za-z][A-Za-z\s'-]{1,59}$/.test(trimmedValue)) {
    return 'Use letters with optional spaces, apostrophes, or hyphens.'
  }

  if (field === 'email' && !emailPattern.test(trimmedValue)) {
    return 'Enter a valid email address.'
  }

  if (field === 'phone' && !phonePattern.test(trimmedValue)) {
    return 'Enter a valid phone number (7-20 characters).'
  }

  if (field === 'birthDate') {
    const parsedDate = new Date(trimmedValue)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    if (Number.isNaN(parsedDate.getTime())) {
      return 'Enter a valid birth date.'
    }
    if (parsedDate > today) {
      return 'Birth date cannot be in the future.'
    }

    const oldestAllowed = new Date('1900-01-01')
    if (parsedDate < oldestAllowed) {
      return 'Birth date must be on or after 1900-01-01.'
    }
  }

  if (field === 'demographics' && trimmedValue.length < 10) {
    return 'Provide at least 10 characters for demographics.'
  }

  return null
}

function validateForm(form: CreatePatientRequest): FormErrors {
  const errors: FormErrors = {}
  ;(Object.keys(form) as FormField[]).forEach((field) => {
    const maybeError = validateField(field, form[field])
    if (maybeError) {
      errors[field] = maybeError
    }
  })
  return errors
}

export function PatientRegistrationForm({ onBack, onSaved }: PatientRegistrationFormProps) {
  const [form, setForm] = useState<CreatePatientRequest>(initialForm)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [statusMessage, setStatusMessage] = useState<string | null>(null)
  const [isError, setIsError] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FormErrors>({})
  const [touchedFields, setTouchedFields] = useState<Partial<Record<FormField, boolean>>>({})
  const [idProofFile, setIdProofFile] = useState<File | null>(null)

  async function handleSave() {
    const errors = validateForm(form)
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      setTouchedFields({
        externalReference: true,
        givenName: true,
        familyName: true,
        birthDate: true,
        email: true,
        phone: true,
        demographics: true,
      })
      setIsError(true)
      setStatusMessage('Please correct the highlighted registration fields.')
      return
    }

    setIsSubmitting(true)
    setIsError(false)
    setStatusMessage(null)

    try {
      const patient = await createPatient(form)
      if (idProofFile) {
        await uploadPatientIdProof(patient.id, idProofFile)
      }
      onSaved(patient)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'We could not complete patient registration right now. Please check the details and try again.'
      setIsError(true)
      setStatusMessage(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  function updateField<K extends keyof CreatePatientRequest>(field: K, value: CreatePatientRequest[K]) {
    setForm((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => {
      if (!current[field]) {
        return current
      }
      const next = { ...current }
      delete next[field]
      return next
    })
  }

  function markFieldTouched(field: FormField) {
    setTouchedFields((current) => ({ ...current, [field]: true }))
    const maybeError = validateField(field, form[field])
    setFieldErrors((current) => {
      const next = { ...current }
      if (maybeError) {
        next[field] = maybeError
      } else {
        delete next[field]
      }
      return next
    })
  }

  function fieldState(field: FormField) {
    const showError = Boolean(touchedFields[field] && fieldErrors[field])
    return {
      showError,
      errorText: showError ? fieldErrors[field] : null,
      errorId: `${field}-error`,
    }
  }

  return (
    <div className="patient-registration-shell">
      <div className="patient-registration-topbar">
        <button type="button" className="secondary-button patient-back-button" onClick={onBack}>
          ← Back
        </button>
        <div>
          <p className="eyebrow">Patient onboarding</p>
          <h2>Register new patient</h2>
          <p>Capture the UC-01 intake fields, create the profile, and queue the confirmation notification.</p>
        </div>
      </div>

      <form className="stacked-form patient-registration-form" onSubmit={(event) => { event.preventDefault(); void handleSave() }}>
        <div className="field-grid">
          <label className="field-block">
            <span>External reference</span>
            <input
              value={form.externalReference}
              onChange={(event) => updateField('externalReference', event.target.value)}
              onBlur={() => markFieldTouched('externalReference')}
              placeholder="EXT-1001"
              required
              aria-invalid={fieldState('externalReference').showError}
              aria-describedby={fieldState('externalReference').showError ? fieldState('externalReference').errorId : undefined}
              className={fieldState('externalReference').showError ? 'input-error' : undefined}
            />
            {fieldState('externalReference').showError ? (
              <small id={fieldState('externalReference').errorId} className="field-error-text">
                {fieldState('externalReference').errorText}
              </small>
            ) : null}
          </label>
          <label className="field-block">
            <span>Birth date</span>
            <input
              type="date"
              value={form.birthDate}
              onChange={(event) => updateField('birthDate', event.target.value)}
              onBlur={() => markFieldTouched('birthDate')}
              required
              aria-invalid={fieldState('birthDate').showError}
              aria-describedby={fieldState('birthDate').showError ? fieldState('birthDate').errorId : undefined}
              className={fieldState('birthDate').showError ? 'input-error' : undefined}
            />
            {fieldState('birthDate').showError ? (
              <small id={fieldState('birthDate').errorId} className="field-error-text">
                {fieldState('birthDate').errorText}
              </small>
            ) : null}
          </label>
        </div>

        <div className="field-grid">
          <label className="field-block">
            <span>Given name</span>
            <input
              value={form.givenName}
              onChange={(event) => updateField('givenName', event.target.value)}
              onBlur={() => markFieldTouched('givenName')}
              placeholder="Ananya"
              required
              aria-invalid={fieldState('givenName').showError}
              aria-describedby={fieldState('givenName').showError ? fieldState('givenName').errorId : undefined}
              className={fieldState('givenName').showError ? 'input-error' : undefined}
            />
            {fieldState('givenName').showError ? (
              <small id={fieldState('givenName').errorId} className="field-error-text">
                {fieldState('givenName').errorText}
              </small>
            ) : null}
          </label>
          <label className="field-block">
            <span>Family name</span>
            <input
              value={form.familyName}
              onChange={(event) => updateField('familyName', event.target.value)}
              onBlur={() => markFieldTouched('familyName')}
              placeholder="Sharma"
              required
              aria-invalid={fieldState('familyName').showError}
              aria-describedby={fieldState('familyName').showError ? fieldState('familyName').errorId : undefined}
              className={fieldState('familyName').showError ? 'input-error' : undefined}
            />
            {fieldState('familyName').showError ? (
              <small id={fieldState('familyName').errorId} className="field-error-text">
                {fieldState('familyName').errorText}
              </small>
            ) : null}
          </label>
        </div>

        <div className="field-grid">
          <label className="field-block">
            <span>Email</span>
            <input
              type="email"
              value={form.email}
              onChange={(event) => updateField('email', event.target.value)}
              onBlur={() => markFieldTouched('email')}
              placeholder="patient@example.com"
              required
              aria-invalid={fieldState('email').showError}
              aria-describedby={fieldState('email').showError ? fieldState('email').errorId : undefined}
              className={fieldState('email').showError ? 'input-error' : undefined}
            />
            {fieldState('email').showError ? (
              <small id={fieldState('email').errorId} className="field-error-text">
                {fieldState('email').errorText}
              </small>
            ) : null}
          </label>
          <label className="field-block">
            <span>Phone</span>
            <input
              value={form.phone}
              onChange={(event) => updateField('phone', event.target.value)}
              onBlur={() => markFieldTouched('phone')}
              placeholder="+1 555 0100"
              required
              aria-invalid={fieldState('phone').showError}
              aria-describedby={fieldState('phone').showError ? fieldState('phone').errorId : undefined}
              className={fieldState('phone').showError ? 'input-error' : undefined}
            />
            {fieldState('phone').showError ? (
              <small id={fieldState('phone').errorId} className="field-error-text">
                {fieldState('phone').errorText}
              </small>
            ) : null}
          </label>
        </div>

        <label className="field-block">
          <span>Demographics</span>
          <textarea
            rows={4}
            value={form.demographics}
            onChange={(event) => updateField('demographics', event.target.value)}
            onBlur={() => markFieldTouched('demographics')}
            placeholder="Language, age band, accessibility needs, outreach preferences"
            required
            aria-invalid={fieldState('demographics').showError}
            aria-describedby={fieldState('demographics').showError ? fieldState('demographics').errorId : undefined}
            className={fieldState('demographics').showError ? 'input-error' : undefined}
          />
          {fieldState('demographics').showError ? (
            <small id={fieldState('demographics').errorId} className="field-error-text">
              {fieldState('demographics').errorText}
            </small>
          ) : null}
        </label>

        <label className="field-block">
          <span>ID proof with photo (optional)</span>
          <input
            type="file"
            accept="image/*,.pdf"
            onChange={(event) => setIdProofFile(event.target.files?.[0] ?? null)}
          />
          <small>Allowed: image/PDF up to 10MB. This is stored in secure Azure Blob Storage.</small>
        </label>

        <div className="form-actions">
          <button type="submit" className="primary-button" disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : 'Save patient'}
          </button>
          <button type="button" className="secondary-button" onClick={onBack} disabled={isSubmitting}>
            Cancel
          </button>
        </div>

        {statusMessage ? (
          <p className={isError ? 'form-status form-status--error' : 'form-status form-status--success'} role={isError ? 'alert' : 'status'}>
            {statusMessage}
          </p>
        ) : null}
      </form>
    </div>
  )
}
