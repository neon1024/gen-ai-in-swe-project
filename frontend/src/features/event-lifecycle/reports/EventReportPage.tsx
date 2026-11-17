import { Fragment, useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { ApiError } from '../api/client'
import { downloadReportCsv, getReport } from '../api/reports'
import type { ActivityState, EventReport } from '../api/types'
import { FeedbackPanel } from './FeedbackPanel'

const STATE_LABELS: Record<ActivityState, string> = {
  SCHEDULED: 'Scheduled',
  CANCELLED: 'Cancelled',
}

const DASH = '—'

function formatStart(value: string | null): string {
  return value === null ? DASH : new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

function formatPercent(rate: number | null): string {
  return rate === null ? DASH : `${(rate * 100).toFixed(1)}%`
}

function formatRating(value: number | null): string {
  return value === null ? DASH : String(Number(value.toFixed(2)))
}

export function EventReportPage() {
  const { eventId = '' } = useParams()
  const [report, setReport] = useState<EventReport | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<Map<string, boolean>>(new Map())

  useEffect(() => {
    let current = true
    getReport(eventId)
      .then((data) => {
        if (current) {
          setReport(data)
        }
      })
      .catch((e: unknown) => {
        if (current) {
          setError(e instanceof ApiError ? e.message : 'Something went wrong. Please try again.')
        }
      })
    return () => {
      current = false
    }
  }, [eventId])

  async function download() {
    try {
      await downloadReportCsv(eventId)
      setError(null)
    } catch (e: unknown) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong. Please try again.')
    }
  }

  function toggle(activityId: string) {
    setExpanded((previous) => {
      // A key stays after the first open so the loaded summary is kept while collapsed.
      const next = new Map(previous)
      next.set(activityId, !previous.get(activityId))
      return next
    })
  }

  const cell = ''

  return (
    <main className="page report-page">
      {error && (
        <p role="alert" className="notice error">
          {error}
        </p>
      )}
      {report && (
        <>
          <h1>Report: {report.eventTitle}</h1>
          <button type="button" onClick={download} className="button secondary report-download">
            Download CSV
          </button>
          <div className="report-table-wrap">
            <table className="report-table">
              <thead>
                <tr>
                  {['Activity', 'Start', 'Status', 'Confirmed', 'Waitlist', 'Cancellations', 'Attendance', 'Average rating', 'Feedback'].map(
                    (heading) => (
                      <th key={heading} scope="col" className={cell}>
                        {heading}
                      </th>
                    ),
                  )}
                </tr>
              </thead>
              <tbody>
                {report.activities.map((row) => {
                  const open = expanded.get(row.activityId) === true
                  const everOpened = expanded.has(row.activityId)
                  return (
                    <Fragment key={row.activityId}>
                      <tr>
                        <td className={cell}>{row.title}</td>
                        <td className={cell}>{formatStart(row.startsAt)}</td>
                        <td className={cell}>{STATE_LABELS[row.state]}</td>
                        <td className={cell}>{row.confirmedSignups}</td>
                        <td className={cell}>{row.waitlistSize}</td>
                        <td className={cell}>{row.cancellations}</td>
                        <td className={cell}>{formatPercent(row.attendanceRate)}</td>
                        <td className={cell}>{formatRating(row.averageRating)}</td>
                        <td className={cell}>
                          <button
                            type="button"
                            aria-expanded={open}
                            aria-label={`${open ? 'Hide' : 'Show'} feedback for ${row.title}`}
                            onClick={() => toggle(row.activityId)}
                            className="button secondary feedback-toggle"
                          >
                            {open ? 'Hide' : 'Show'}
                          </button>
                        </td>
                      </tr>
                      {everOpened && (
                        <tr hidden={!open}>
                          <td colSpan={9} className={cell}>
                            <FeedbackPanel activityId={row.activityId} title={row.title} />
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  )
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
    </main>
  )
}
