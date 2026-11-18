import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { cancelActivity, cancelEvent, deleteDraft, getLifecycleEvent } from '../api/lifecycle'
import type { ActivityState, EventState, LifecycleEventView } from '../api/types'

const EVENT_STATE_LABELS: Record<EventState, string> = {
  DRAFT: 'Draft',
  PUBLISHED: 'Published',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
}

const ACTIVITY_STATE_LABELS: Record<ActivityState, string> = {
  SCHEDULED: 'Scheduled',
  CANCELLED: 'Cancelled',
}

const CASCADE = 'All signups and waitlist entries will be cancelled.'

function formatRange(startsAt: string | null, endsAt: string | null): string {
  if (startsAt === null) {
    return 'Not scheduled'
  }
  const options: Intl.DateTimeFormatOptions = { dateStyle: 'medium', timeStyle: 'short' }
  const start = new Date(startsAt).toLocaleString(undefined, options)
  return endsAt === null ? start : `${start} - ${new Date(endsAt).toLocaleString(undefined, options)}`
}

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : 'Something went wrong. Please try again.'
}

export function EventLifecyclePage() {
  const { eventId = '' } = useParams()
  const navigate = useNavigate()
  const [event, setEvent] = useState<LifecycleEventView | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let current = true
    getLifecycleEvent(eventId)
      .then((data) => {
        if (current) {
          setEvent(data)
          setError(null)
        }
      })
      .catch((e: unknown) => {
        if (current) {
          setError(messageOf(e))
        }
      })
    return () => {
      current = false
    }
  }, [eventId])

  async function run(question: string, action: () => Promise<LifecycleEventView | void>) {
    if (!window.confirm(question)) {
      return
    }
    try {
      const result = await action()
      if (result) {
        setEvent(result)
        setError(null)
      } else {
        navigate('/events')
      }
    } catch (e) {
      setError(messageOf(e))
    }
  }

  const readOnly = event?.state === 'COMPLETED' || event?.state === 'CANCELLED'
  const button = ''

  return (
    <main className="page lifecycle-page">
      {error && (
        <p role="alert" className="notice error">
          {error}
        </p>
      )}
      {event && (
        <>
          <h1>{event.title}</h1>
          <p className="lede">Event state: {EVENT_STATE_LABELS[event.state]}</p>
          {(event.permissions.canCancel || event.permissions.canDelete) && (
            <nav className="lifecycle-links" aria-label="Event management">
              <Link to={`/authoring/events/${event.id}`}>Edit event</Link> · <Link to={`/events/${event.id}/report`}>Report</Link>
            </nav>
          )}
          {readOnly && <p className="notice muted-notice">This event is read-only.</p>}
          {!readOnly && event.permissions.canCancel && (
            <button
              type="button"
              className={`${button} button danger`}
              onClick={() => run(`Cancel this event? ${CASCADE}`, () => cancelEvent(event.id))}
            >
              Cancel event
            </button>
          )}
          {!readOnly && event.permissions.canDelete && (
            <button
              type="button"
              className={`${button} button danger`}
              onClick={() => run('Delete this draft? This cannot be undone.', () => deleteDraft(event.id))}
            >
              Delete draft
            </button>
          )}
          <h2>Activities</h2>
          <ul className="lifecycle-activities">
            {event.activities.map((activity) => (
              <li key={activity.id} className="lifecycle-activity">
                <div>
                  <p className="font-medium">{activity.title}</p>
                  <p className="activity-date">{formatRange(activity.startsAt, activity.endsAt)}</p>
                  <p className="activity-state">{ACTIVITY_STATE_LABELS[activity.state]}</p>
                </div>
                {!readOnly && event.permissions.canCancel && activity.state === 'SCHEDULED' && (
                  <button
                    type="button"
                    className={button}
                    onClick={() => run(`Cancel this activity? ${CASCADE}`, () => cancelActivity(activity.id))}
                  >
                    Cancel activity
                  </button>
                )}
              </li>
            ))}
          </ul>
        </>
      )}
    </main>
  )
}
