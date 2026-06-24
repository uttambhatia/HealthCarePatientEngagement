import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import {
  approvePatientRegistration,
  downloadPatientIdProof,
  listPatients,
  rejectPatientRegistration,
  resendPatientRegistrationNotification,
} from '../../services/platformApi'

type RegistrationRow = {
  id: string
  fullName: string
  email: string
  phone: string
  status: string
  decisionAudit: string
  idProofUploaded: boolean
}

type RegistrationFilters = {
  fullName: string
  email: string
  phone: string
  status: string
}

type SortColumn = 'fullName' | 'email' | 'phone' | 'status' | 'decisionAudit'
type SortDirection = 'asc' | 'desc'
type RegistrationAction = 'approve' | 'reject' | 'notify'

type PendingAction = {
  id: string
  action: RegistrationAction
  comment: string
}

function readField(record: Record<string, unknown>, key: string) {
  const value = record[key]
  return typeof value === 'string' ? value.trim() : ''
}

function normalizeRow(record: Record<string, unknown>): RegistrationRow | null {
  const id = readField(record, 'id')
  const givenName = readField(record, 'givenName')
  const familyName = readField(record, 'familyName')
  const email = readField(record, 'email')
  const phone = readField(record, 'phone')
  const status = readField(record, 'status')
  const decisionAudit = readField(record, 'decisionAudit')
  const idProofUploaded = Boolean(record.idProofUploaded)

  if (!id || !email || !status) {
    return null
  }

  return {
    id,
    fullName: `${givenName} ${familyName}`.trim() || 'Unknown',
    email,
    phone,
    status,
    decisionAudit,
    idProofUploaded,
  }
}

function statusClassName(status: string) {
  const normalized = status.toUpperCase()
  if (normalized === 'PENDING_VERIFICATION') {
    return 'registration-status registration-status--pending'
  }
  if (normalized === 'COMPLETED') {
    return 'registration-status registration-status--approved'
  }
  if (normalized === 'REJECTED') {
    return 'registration-status registration-status--rejected'
  }
  return 'registration-status'
}

export function PatientRegistrationReview() {
  const { session } = useAuth()
  const [rows, setRows] = useState<RegistrationRow[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [activeId, setActiveId] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [sortColumn, setSortColumn] = useState<SortColumn>('status')
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc')
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(5)
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null)
  const [filters, setFilters] = useState<RegistrationFilters>({
    fullName: '',
    email: '',
    phone: '',
    status: 'ALL',
  })

  const token = session?.accessToken

  async function loadRows() {
    if (!token) {
      setRows([])
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    try {
      const payload = await listPatients(token)
      const normalized = payload
        .map(normalizeRow)
        .filter((item): item is RegistrationRow => Boolean(item))
      setRows(normalized)
      setErrorMessage(null)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Unable to load registrations.'
      setErrorMessage(message)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    void loadRows()
  }, [token])

  const pendingCount = useMemo(
    () => rows.filter((row) => row.status.toUpperCase() === 'PENDING_VERIFICATION').length,
    [rows],
  )

  const filteredRows = useMemo(() => {
    const nameFilter = filters.fullName.trim().toLowerCase()
    const emailFilter = filters.email.trim().toLowerCase()
    const phoneFilter = filters.phone.trim().toLowerCase()
    const statusFilter = filters.status.toUpperCase()

    return rows.filter((row) => {
      const matchesName = !nameFilter || row.fullName.toLowerCase().includes(nameFilter)
      const matchesEmail = !emailFilter || row.email.toLowerCase().includes(emailFilter)
      const matchesPhone = !phoneFilter || row.phone.toLowerCase().includes(phoneFilter)
      const matchesStatus = statusFilter === 'ALL' || row.status.toUpperCase() === statusFilter

      return matchesName && matchesEmail && matchesPhone && matchesStatus
    })
  }, [filters, rows])

  const sortedRows = useMemo(() => {
    const rankByStatus = (status: string) => {
      const normalized = status.toUpperCase()
      if (normalized === 'PENDING_VERIFICATION') {
        return 0
      }
      if (normalized === 'COMPLETED') {
        return 1
      }
      if (normalized === 'REJECTED') {
        return 2
      }
      return 3
    }

    return [...filteredRows].sort((left, right) => {
      let result = 0

      if (sortColumn === 'status') {
        result = rankByStatus(left.status) - rankByStatus(right.status)
      } else {
        const leftValue = (left[sortColumn] || '').toLowerCase()
        const rightValue = (right[sortColumn] || '').toLowerCase()
        result = leftValue.localeCompare(rightValue)
      }

      return sortDirection === 'asc' ? result : -result
    })
  }, [filteredRows, sortColumn, sortDirection])

  const totalPages = Math.max(1, Math.ceil(sortedRows.length / pageSize))

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages)
    }
  }, [currentPage, totalPages])

  const pagedRows = useMemo(() => {
    const start = (currentPage - 1) * pageSize
    return sortedRows.slice(start, start + pageSize)
  }, [currentPage, pageSize, sortedRows])

  function applySort(column: SortColumn) {
    if (sortColumn === column) {
      setSortDirection((current) => (current === 'asc' ? 'desc' : 'asc'))
      return
    }

    setSortColumn(column)
    setSortDirection('asc')
    setCurrentPage(1)
  }

  function renderSortLabel(label: string, column: SortColumn) {
    if (sortColumn !== column) {
      return `${label} ↕`
    }
    return sortDirection === 'asc' ? `${label} ↑` : `${label} ↓`
  }

  function setFilter<K extends keyof RegistrationFilters>(key: K, value: RegistrationFilters[K]) {
    setFilters((current) => ({ ...current, [key]: value }))
    setCurrentPage(1)
  }

  function openActionPanel(id: string, action: RegistrationAction) {
    setPendingAction({ id, action, comment: '' })
    setMessage(null)
    setErrorMessage(null)
  }

  function actionSummary(action: RegistrationAction, fullName: string) {
    if (action === 'approve') {
      return {
        heading: `Approve ${fullName}?`,
        details: 'This confirms the registration and completes downstream provisioning tasks.',
        nextSteps: 'Next step: the status changes to Completed and the patient can continue onboarding.',
        confirmLabel: 'Confirm approval',
      }
    }

    if (action === 'reject') {
      return {
        heading: `Reject ${fullName}?`,
        details: 'This marks the registration as rejected and sends a decline notification.',
        nextSteps: 'Next step: review the rejection note for audit and re-open only if needed.',
        confirmLabel: 'Confirm rejection',
      }
    }

    return {
      heading: `Notify ${fullName}?`,
      details: 'This resends the pending registration notification to the patient contact channels.',
      nextSteps: 'Next step: monitor for acknowledgment or patient follow-up before re-sending again.',
      confirmLabel: 'Send notification',
    }
  }

  async function runAction(id: string, action: RegistrationAction, comment?: string) {
    if (!token) {
      return
    }

    setActiveId(id)
    setMessage(null)
    setErrorMessage(null)

    try {
      if (action === 'approve') {
        await approvePatientRegistration(id, token)
        setMessage(
          `Registration approved. FHIR and identity provisioning were triggered.${comment ? ' Reviewer note captured.' : ''}`,
        )
      } else if (action === 'reject') {
        await rejectPatientRegistration(id, token)
        setMessage(`Registration rejected and patient was notified.${comment ? ' Reviewer note captured.' : ''}`)
      } else {
        await resendPatientRegistrationNotification(id, token)
        setMessage('Notification resend was triggered for this registration.')
      }
      setPendingAction(null)
      await loadRows()
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Action failed. Please retry.'
      setErrorMessage(message)
    } finally {
      setActiveId(null)
    }
  }

  async function handleViewIdProof(id: string) {
    if (!token) {
      return
    }

    setErrorMessage(null)
    try {
      const blob = await downloadPatientIdProof(id, token)
      const url = URL.createObjectURL(blob)
      window.open(url, '_blank', 'noopener,noreferrer')
      setTimeout(() => URL.revokeObjectURL(url), 30000)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Unable to load ID proof.'
      setErrorMessage(message)
    }
  }

  return (
    <section className="registration-review-panel" aria-label="Patient registrations review">
      <header className="registration-review-header">
        <span className="registration-pending-chip">Pending: {pendingCount}</span>
      </header>

      {message ? <p className="form-status form-status--success">{message}</p> : null}
      {errorMessage ? <p className="form-status form-status--error">{errorMessage}</p> : null}

      <div className="registration-table-wrap">
        <table className="registration-table">
          <thead>
            <tr>
              <th>
                <button type="button" className="registration-sort-button" onClick={() => applySort('fullName')}>
                  {renderSortLabel('Patient full name', 'fullName')}
                </button>
              </th>
              <th>
                <button type="button" className="registration-sort-button" onClick={() => applySort('email')}>
                  {renderSortLabel('Email', 'email')}
                </button>
              </th>
              <th>
                <button type="button" className="registration-sort-button" onClick={() => applySort('phone')}>
                  {renderSortLabel('Phone number', 'phone')}
                </button>
              </th>
              <th>
                <button type="button" className="registration-sort-button" onClick={() => applySort('status')}>
                  {renderSortLabel('Status', 'status')}
                </button>
              </th>
              <th>
                <button type="button" className="registration-sort-button" onClick={() => applySort('decisionAudit')}>
                  {renderSortLabel('Decision audit', 'decisionAudit')}
                </button>
              </th>
              <th>Actions</th>
            </tr>
            <tr className="registration-filter-row">
              <th>
                <input
                  type="search"
                  className="registration-filter-input"
                  placeholder="Search name"
                  value={filters.fullName}
                  onChange={(event) => setFilter('fullName', event.target.value)}
                />
              </th>
              <th>
                <input
                  type="search"
                  className="registration-filter-input"
                  placeholder="Search email"
                  value={filters.email}
                  onChange={(event) => setFilter('email', event.target.value)}
                />
              </th>
              <th>
                <input
                  type="search"
                  className="registration-filter-input"
                  placeholder="Search phone"
                  value={filters.phone}
                  onChange={(event) => setFilter('phone', event.target.value)}
                />
              </th>
              <th>
                <select
                  className="registration-filter-input"
                  value={filters.status}
                  onChange={(event) => setFilter('status', event.target.value)}
                >
                  <option value="ALL">All statuses</option>
                  <option value="PENDING_VERIFICATION">Pending verification</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="REJECTED">Rejected</option>
                </select>
              </th>
              <th />
              <th />
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={6}>Loading registrations...</td>
              </tr>
            ) : sortedRows.length === 0 ? (
              <tr>
                <td colSpan={6}>No patient registrations found.</td>
              </tr>
            ) : (
              pagedRows.map((row) => {
                const pending = row.status.toUpperCase() === 'PENDING_VERIFICATION'
                const busy = activeId === row.id
                const panel = pendingAction?.id === row.id ? actionSummary(pendingAction.action, row.fullName) : null
                return (
                  <tr key={row.id}>
                    <td>{row.fullName}</td>
                    <td>{row.email}</td>
                    <td>{row.phone || 'n/a'}</td>
                    <td>
                      <span className={statusClassName(row.status)}>{row.status}</span>
                    </td>
                    <td>{row.decisionAudit || 'n/a'}</td>
                    <td className="registration-actions-cell">
                      <div className="registration-action-group">
                        {row.idProofUploaded ? (
                          <button
                            type="button"
                            className="secondary-button registration-action-button"
                            disabled={busy}
                            onClick={() => void handleViewIdProof(row.id)}
                          >
                            View ID
                          </button>
                        ) : null}
                        {pending ? (
                          <>
                            <button
                              type="button"
                              className="primary-button registration-action-button"
                              disabled={busy}
                              onClick={() => openActionPanel(row.id, 'approve')}
                            >
                              Approve
                            </button>
                            <button
                              type="button"
                              className="secondary-button registration-action-button registration-action-button--reject"
                              disabled={busy}
                              onClick={() => openActionPanel(row.id, 'reject')}
                            >
                              Reject
                            </button>
                          </>
                        ) : null}
                        <button
                          type="button"
                          className="secondary-button registration-action-button registration-action-button--notify"
                          disabled={busy}
                          onClick={() => openActionPanel(row.id, 'notify')}
                        >
                          Notify
                        </button>
                      </div>
                      {panel && pendingAction ? (
                        <div
                          className="registration-action-inline-panel"
                          data-registration-panel-id={row.id}
                          role="group"
                          aria-label={panel.heading}
                        >
                          <p className="registration-action-inline-title">{panel.heading}</p>
                          <p className="registration-action-inline-detail">{panel.details}</p>
                          <p className="registration-action-inline-next">{panel.nextSteps}</p>
                          {pendingAction.action !== 'notify' ? (
                            <label className="registration-action-comment-field">
                              Comment
                              <textarea
                                className="registration-action-comment-input"
                                value={pendingAction.comment}
                                rows={2}
                                maxLength={280}
                                placeholder="Add a brief reviewer comment"
                                onChange={(event) =>
                                  setPendingAction((current) =>
                                    current && current.id === row.id
                                      ? { ...current, comment: event.target.value }
                                      : current,
                                  )
                                }
                              />
                            </label>
                          ) : null}
                          <div className="registration-action-inline-controls">
                            <button
                              type="button"
                              className="secondary-button registration-action-inline-button"
                              disabled={busy}
                              onClick={() => setPendingAction(null)}
                            >
                              Cancel
                            </button>
                            <button
                              type="button"
                              className="primary-button registration-action-inline-button"
                              disabled={busy}
                              onClick={() => void runAction(row.id, pendingAction.action, pendingAction.comment.trim())}
                            >
                              {panel.confirmLabel}
                            </button>
                          </div>
                        </div>
                      ) : null}
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>

        <div className="registration-table-pagination">
          <div className="registration-page-controls">
            <button
              type="button"
              className="secondary-button registration-pagination-button"
              disabled={currentPage === 1}
              onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
            >
              Previous
            </button>
            <label className="registration-page-size-control" aria-label="Page size selector">
              <select
                value={pageSize}
                onChange={(event) => {
                  setPageSize(Number(event.target.value))
                  setCurrentPage(1)
                }}
              >
                <option value={5}>5</option>
                <option value={10}>10</option>
                <option value={20}>20</option>
              </select>
            </label>
            <span className="registration-page-indicator" aria-live="polite">
              Page {currentPage} of {totalPages}
            </span>
            <button
              type="button"
              className="secondary-button registration-pagination-button"
              disabled={currentPage === totalPages}
              onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}
