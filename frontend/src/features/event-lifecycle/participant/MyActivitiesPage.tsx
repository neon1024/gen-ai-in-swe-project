import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import { cancelSignup, getMySignups } from '../api/participant'
import type { SignupStatus, SignupSummary } from '../api/types'

const statusLabels: Record<SignupStatus, string> = {
  CONFIRMED: 'Confirmed',
  WAITLISTED: 'Waitlisted',
  CANCELLED: 'Cancelled',
  ATTENDED: 'Attended',
}

function date(value: string) {
  return new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

export function MyActivitiesPage() {
  const [signups, setSignups] = useState<SignupSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cancelling, setCancelling] = useState<string | null>(null)
  const [tab, setTab] = useState<'upcoming' | 'past'>('upcoming')
  const [now] = useState(Date.now)

  useEffect(() => {
    getMySignups().then(setSignups).catch((e: unknown) => setError(e instanceof ApiError ? e.message : 'Activities could not be loaded.'))
  }, [])

  const visible = useMemo(() => {
    if (!signups) return []
    return signups.filter((signup) => tab === 'upcoming' ? new Date(signup.endsAt).getTime() >= now : new Date(signup.endsAt).getTime() < now)
  }, [now, signups, tab])

  async function cancel(signup: SignupSummary) {
    if (!window.confirm(`Leave ${signup.activityTitle}?`)) return
    setCancelling(signup.id)
    try {
      await cancelSignup(signup.id)
      setSignups((current) => current?.map((item) => item.id === signup.id ? { ...item, status: 'CANCELLED' } : item) ?? null)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'This signup could not be cancelled.')
    } finally {
      setCancelling(null)
    }
  }

  return (
    <main className="page">
      <div className="page-heading">
        <div><p className="eyebrow">Your calendar</p><h1>My activities</h1><p className="lede">Everything you signed up for, in one place.</p></div>
      </div>
      <div className="tabs" role="tablist">
        {(['upcoming', 'past'] as const).map((value) => <button key={value} role="tab" aria-selected={tab === value} className={tab === value ? 'tab active' : 'tab'} onClick={() => setTab(value)}>{value === 'upcoming' ? 'Coming up' : 'Past'}</button>)}
      </div>
      {error && <p role="alert" className="notice error">{error}</p>}
      {signups === null && !error && <p className="loading">Loading your activities…</p>}
      {signups && visible.length === 0 && <div className="empty"><strong>No {tab} activities yet.</strong><p>When you find something worth showing up for, it will appear here.</p><Link className="button primary" to="/events">Explore events</Link></div>}
      <div className="activity-list">
        {visible.map((signup) => (
          <article className="activity-card" key={signup.id}>
            <div className="activity-date">{date(signup.startsAt)}</div>
            <div className="activity-info"><h2>{signup.activityTitle}</h2><p>{signup.eventTitle}</p><p>{signup.location ?? 'Online activity'}</p></div>
            <div className="activity-actions"><span className={`status ${signup.status.toLowerCase()}`}>{statusLabels[signup.status]}{signup.status === 'WAITLISTED' && signup.waitlistPosition ? ` · #${signup.waitlistPosition}` : ''}</span>{signup.status === 'CONFIRMED' && tab === 'upcoming' && <button className="text-button" disabled={cancelling === signup.id} onClick={() => cancel(signup)}>Leave activity</button>}{signup.status === 'ATTENDED' && <Link className="text-button" to={`/activities/${signup.activityId}/feedback`}>Give feedback</Link>}</div>
          </article>
        ))}
      </div>
    </main>
  )
}
