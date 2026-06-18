import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import {
  approvePatientRegistration,
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
}

type SortColumn = 'fullName' | 'email' | 'phone' | 'status' | 'decisionAudit'
type SortDirection = 'asc' | 'desc'

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
  const [pageSize, setPageSize] = useState(10)

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

    return [...rows].sort((left, right) => {
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
  }, [rows, sortColumn, sortDirection])

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

  const pageStart = sortedRows.length === 0 ? 0 : (currentPage - 1) * pageSize + 1
  const pageEnd = Math.min(currentPage * pageSize, sortedRows.length)

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

  async function runAction(id: string, action: 'approve' | 'reject' | 'resend') {
    if (!token) {
      return
    }

    setActiveId(id)
    setMessage(null)
    setErrorMessage(null)

    try {
      if (action === 'approve') {
        await approvePatientRegistration(id, token)
        setMessage('Registration approved. FHIR and identity provisioning were triggered.')
      } else if (action === 'reject') {
        await rejectPatientRegistration(id, token)
        setMessage('Registration rejected and patient was notified.')
      } else {
        await resendPatientRegistrationNotification(id, token)
        setMessage('Notification resend was triggered for this registration.')
      }
      await loadRows()
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Action failed. Please retry.'
      setErrorMessage(message)
    } finally {
      setActiveId(null)
    }
  }

  return (
    <section className="registration-review-panel" aria-label="Patient registrations review">
      <header className="registration-review-header">
        <div>
          <h3>Patient registrations</h3>
          <p>Review pending registrations and decide approval outcomes.</p>
        </div>
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
                return (
                  <tr key={row.id}>
                    <td>{row.fullName}</td>
                    <td>{row.email}</td>
                    <td>{row.phone || 'n/a'}</td>
                    <td>
                      <span className={statusClassName(row.status)}>{row.status}</span>
                    </td>
                    <td>{row.decisionAudit || 'n/a'}</td>
                    <td>
                      <div className="registration-action-group">
                        {pending ? (
                          <>
                            <button
                              type="button"
                              className="primary-button"
                              disabled={busy}
                              onClick={() => void runAction(row.id, 'approve')}
                            >
                              Approve
                            </button>
                            <button
                              type="button"
                              className="secondary-button"
                              disabled={busy}
                              onClick={() => void runAction(row.id, 'reject')}
                            >
                              Reject
                            </button>
                          </>
                        ) : null}
                        <button
                          type="button"
                          className="secondary-button"
                          disabled={busy}
                          onClick={() => void runAction(row.id, 'resend')}
                        >
                          Resend notifications
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>

        <div className="registration-table-pagination">
          <span className="registration-table-page-status">
            Showing {pageStart}-{pageEnd} of {sortedRows.length}
          </span>
          <label className="registration-page-size-control">
            Rows per page
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
          <div className="registration-page-controls">
            <button
              type="button"
              className="secondary-button"
              disabled={currentPage === 1}
              onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
            >
              Previous
            </button>
            <span>Page {currentPage} of {totalPages}</span>
            <button
              type="button"
              className="secondary-button"
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
