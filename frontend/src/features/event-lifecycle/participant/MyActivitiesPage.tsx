import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import { cancelSignup, getMySignups } from '../api/participant'
import type { SignupStatus, SignupSummary } from '../api/types'
import { Tabs, TabsList, TabsTrigger } from '../../../components/ui/tabs'
import { Skeleton } from '../../../components/ui/skeleton'
import { Badge } from '../../../components/ui/badge'
import { useConfirm } from '../../../components/ui/use-confirm'

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
  const { confirm, dialog } = useConfirm()
  const [now] = useState(Date.now)

  useEffect(() => {
    getMySignups().then(setSignups).catch((e: unknown) => setError(e instanceof ApiError ? e.message : 'Activities could not be loaded.'))
  }, [])

  const visible = useMemo(() => {
    if (!signups) return []
    return signups.filter((signup) => tab === 'upcoming' ? new Date(signup.endsAt).getTime() >= now : new Date(signup.endsAt).getTime() < now)
  }, [now, signups, tab])

  async function cancel(signup: SignupSummary) {
    if (!(await confirm({ title: 'Leave activity?', message: `Leave ${signup.activityTitle}? Your place may go to someone on the waitlist.`, confirmLabel: 'Leave activity', cancelLabel: 'Stay', destructive: true }))) return
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
      <Tabs value={tab} onValueChange={(value) => setTab(value as 'upcoming' | 'past')} className="tabs">
        <TabsList>
          <TabsTrigger value="upcoming">Coming up</TabsTrigger>
          <TabsTrigger value="past">Past</TabsTrigger>
        </TabsList>
      </Tabs>
      {error && <p role="alert" className="notice error">{error}</p>}
      {signups === null && !error && <div className="activity-list" aria-label="Loading activities"><Skeleton className="h-28 w-full" /><Skeleton className="h-28 w-full" /></div>}
      {signups && visible.length === 0 && <div className="empty"><strong>No {tab} activities yet.</strong><p>When you find something worth showing up for, it will appear here.</p><Link className="button primary" to="/events">Explore events</Link></div>}
      <div className="activity-list">
        {visible.map((signup) => (
          <article className="activity-card" key={signup.id}>
            <div className="activity-date">{date(signup.startsAt)}</div>
            <div className="activity-info"><h2>{signup.activityTitle}</h2><p>{signup.eventTitle}</p><p>{signup.location ?? 'Online activity'}</p></div>
            <div className="activity-actions"><Badge variant={signup.status === 'CANCELLED' ? 'destructive' : signup.status === 'WAITLISTED' ? 'secondary' : 'default'} className={`status ${signup.status.toLowerCase()}`}>{statusLabels[signup.status]}{signup.status === 'WAITLISTED' && signup.waitlistPosition ? ` · #${signup.waitlistPosition}` : ''}</Badge>{signup.status === 'CONFIRMED' && tab === 'upcoming' && <button className="text-button" disabled={cancelling === signup.id} onClick={() => cancel(signup)}>Leave activity</button>}{signup.status === 'ATTENDED' && <Link className="text-button" to={`/activities/${signup.activityId}/feedback`}>Give feedback</Link>}</div>
          </article>
        ))}
      </div>
      {dialog}
    </main>
  )
}
