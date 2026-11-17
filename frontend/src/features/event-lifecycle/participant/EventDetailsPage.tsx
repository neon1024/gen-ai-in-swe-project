import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { getEventDetails, signup } from '../api/participant'
import type { EventActivity, EventDetail } from '../api/types'

const status: Record<EventActivity['registrationStatus'], string> = { NOT_YET_OPEN: 'Not yet open', OPEN: 'Open', FULL: 'Full', CLOSED: 'Closed' }
function date(value: string) { return new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) }

export function EventDetailsPage() {
  const { eventId = '' } = useParams()
  const [event, setEvent] = useState<EventDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  useEffect(() => { getEventDetails(eventId).then(setEvent).catch((e: unknown) => setError(e instanceof ApiError ? e.message : 'Event details could not be loaded.')) }, [eventId])
  async function register(activity: EventActivity) {
    setBusy(activity.id)
    try { const result = await signup(activity.id); setEvent((current) => current && ({ ...current, activities: current.activities.map((item) => item.id === activity.id ? { ...item, signup: result } : item) })) } catch (e) { setError(e instanceof ApiError ? e.message : 'Registration could not be completed.') } finally { setBusy(null) }
  }
  return <main className="page">{error && <p role="alert" className="notice error">{error}</p>}{!event && !error && <p className="loading">Loading event…</p>}{event && <><p className="eyebrow">{event.organizationName} · {event.category}</p><h1>{event.title}</h1><p className="lede detail-lede">{event.description}</p><section className="detail-section"><h2>Choose your activities</h2>{event.activities.map((activity) => <article className="detail-card" key={activity.id}><div><span className={`status ${activity.registrationStatus.toLowerCase()}`}>{status[activity.registrationStatus]}</span><h3>{activity.title}</h3><p>{date(activity.startsAt)} – {date(activity.endsAt)}</p><p>{activity.location ?? 'Online activity'}</p>{activity.description && <p className="muted">{activity.description}</p>}{activity.agenda.length > 0 && <details><summary>View agenda</summary><ul>{activity.agenda.map((item) => <li key={item.id}>{new Date(item.time).toLocaleTimeString(undefined, { timeStyle: 'short' })} · {item.title}{item.speaker ? ` — ${item.speaker}` : ''}</li>)}</ul></details>}</div><div className="detail-action">{activity.signup ? <Link className="button secondary" to="/my-activities">View signup</Link> : activity.registrationStatus === 'OPEN' && <button className="button primary" disabled={busy === activity.id} onClick={() => register(activity)}>{busy === activity.id ? 'Joining…' : 'Join activity'}</button>}</div></article>)}</section></>}</main>
}
