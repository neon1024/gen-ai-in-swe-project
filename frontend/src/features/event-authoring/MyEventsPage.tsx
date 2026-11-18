import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { listEvents, type AuthoringEvent } from './api'
import { useRun } from './helpers'
import { ErrorBox } from './ui'
import { organizationMembershipApi } from '../organization-membership/organizationMembershipService'

const fmt = (iso: string) => new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })

function firstStart(event: AuthoringEvent): string | null {
  const starts = event.activities.filter((a) => !a.cancelled).map((a) => a.start).sort()
  return starts[0] ?? null
}

function EventList({ title, events, organizationNames }: {
  title: string; events: AuthoringEvent[]; organizationNames: Record<string, string>
}) {
  if (events.length === 0) return null
  return (
    <section aria-label={title} className="space-y-2">
      <h2 className="text-lg font-semibold">{title} ({events.length})</h2>
      <ul className="space-y-2">
        {events.map((event) => {
          const start = firstStart(event)
          return (
            <li key={event.id}>
              <Link to={`/authoring/events/${event.id}`}
                className="block rounded-lg border border-gray-200 bg-white p-4 hover:border-blue-400">
                <span className="font-medium text-blue-800">{event.title}</span>
                <span className="mt-1 block text-sm text-gray-600">
                  {organizationNames[event.organizationRef] ?? 'Organization'} · {event.category}
                  {' · '}{event.activities.length} {event.activities.length === 1 ? 'activity' : 'activities'}
                  {start && ` · starts ${fmt(start)}`}
                </span>
              </Link>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

/** Drafts and published events the user may edit, so drafts can be finished before publishing. */
export function MyEventsPage() {
  const { error, run } = useRun()
  const [events, setEvents] = useState<AuthoringEvent[] | null>(null)
  const [organizationNames, setOrganizationNames] = useState<Record<string, string>>({})

  useEffect(() => {
    let current = true
    void run(async () => {
      const [list, organizations] = await Promise.all([listEvents(), organizationMembershipApi.listOrganizations()])
      if (!current) return
      setEvents(list)
      setOrganizationNames(Object.fromEntries(organizations.map((o) => [o.id, o.name])))
    })
    return () => { current = false }
    // run is recreated each render; load once on mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const drafts = events?.filter((e) => e.state === 'Draft') ?? []
  const published = events?.filter((e) => e.state === 'Published') ?? []

  return (
    <main className="mx-auto max-w-3xl space-y-6 p-4">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-sm font-medium uppercase tracking-wide text-gray-600">Organizer</p>
          <h1 className="text-2xl font-semibold">My events</h1>
          <p className="mt-2 text-gray-700">Finish your drafts and publish them, or update published events.</p>
        </div>
        <Link to="/authoring/events/new" className="rounded bg-blue-700 px-4 py-2 text-white">New event</Link>
      </header>
      <ErrorBox error={error} />
      {events === null && !error && <p role="status">Loading…</p>}
      {events?.length === 0 && (
        <div className="rounded-lg border border-dashed border-gray-300 p-6 text-center">
          <p className="text-gray-700">You have no events yet.</p>
          <Link to="/authoring/events/new" className="mt-2 inline-block text-blue-700 underline">Create an event</Link>
        </div>
      )}
      <EventList title="Drafts" events={drafts} organizationNames={organizationNames} />
      <EventList title="Published" events={published} organizationNames={organizationNames} />
    </main>
  )
}
