import { useState } from 'react'
import { createPatient, type CreatePatientRequest, type PatientResponse } from '../../services/platformApi'

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

export function PatientRegistrationForm({ onBack, onSaved }: PatientRegistrationFormProps) {
  const [form, setForm] = useState<CreatePatientRequest>(initialForm)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [statusMessage, setStatusMessage] = useState<string | null>(null)
  const [isError, setIsError] = useState(false)

  async function handleSave() {
    const requiredFields = [
      form.externalReference,
      form.givenName,
      form.familyName,
      form.birthDate,
      form.email,
      form.phone,
      form.demographics,
    ]

    if (requiredFields.some((value) => !value.trim())) {
      setIsError(true)
      setStatusMessage('All patient registration fields are required.')
      return
    }

    setIsSubmitting(true)
    setIsError(false)
    setStatusMessage(null)

    try {
      const patient = await createPatient(form)
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
            <input value={form.externalReference} onChange={(event) => updateField('externalReference', event.target.value)} placeholder="EXT-1001" />
          </label>
          <label className="field-block">
            <span>Birth date</span>
            <input type="date" value={form.birthDate} onChange={(event) => updateField('birthDate', event.target.value)} />
          </label>
        </div>

        <div className="field-grid">
          <label className="field-block">
            <span>Given name</span>
            <input value={form.givenName} onChange={(event) => updateField('givenName', event.target.value)} placeholder="Ananya" />
          </label>
          <label className="field-block">
            <span>Family name</span>
            <input value={form.familyName} onChange={(event) => updateField('familyName', event.target.value)} placeholder="Sharma" />
          </label>
        </div>

        <div className="field-grid">
          <label className="field-block">
            <span>Email</span>
            <input type="email" value={form.email} onChange={(event) => updateField('email', event.target.value)} placeholder="patient@example.com" />
          </label>
          <label className="field-block">
            <span>Phone</span>
            <input value={form.phone} onChange={(event) => updateField('phone', event.target.value)} placeholder="+1 555 0100" />
          </label>
        </div>

        <label className="field-block">
          <span>Demographics</span>
          <textarea
            rows={4}
            value={form.demographics}
            onChange={(event) => updateField('demographics', event.target.value)}
            placeholder="Language, age band, accessibility needs, outreach preferences"
          />
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
