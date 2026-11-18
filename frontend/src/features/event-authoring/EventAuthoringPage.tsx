import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import * as api from './api'
import { CATEGORIES, type Activity, type AuthoringEvent, type Category } from './api'
import { addMinutes, btn, field, fromLocalInput, nextQuarterHour, toLocalInput, useRun } from './helpers'
import { DateTimeField } from './DateTimeField'
import { useConfirm } from '../../components/ui/use-confirm'
import { ErrorBox } from './ui'

const fmt = (iso: string) => new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })

const section = 'space-y-3 rounded-lg border border-gray-200 bg-white p-4'
const sectionTitle = 'text-base font-semibold'
const hint = 'text-sm text-gray-600'

function ActivityForm({ initial, onSubmit, onCancel }: {
  initial?: Activity; onSubmit: (b: api.ActivityInput) => void; onCancel: () => void
}) {
  const [start, setStart] = useState(toLocalInput(initial?.start ?? null))
  const [end, setEnd] = useState(toLocalInput(initial?.end ?? null))
  const [registrationOpen, setRegistrationOpen] = useState(toLocalInput(initial?.registrationOpen ?? null))
  const [registrationClose, setRegistrationClose] = useState(toLocalInput(initial?.registrationClose ?? null))

  /** Choosing the start suggests an end one hour later and a registration window that closes at the start. */
  function changeStart(value: string) {
    setStart(value)
    if (!value) return
    if (!end || end <= value) setEnd(addMinutes(value, 60))
    if (!registrationClose || registrationClose > value) setRegistrationClose(value)
    if (!registrationOpen) {
      const now = nextQuarterHour()
      if (now < value) setRegistrationOpen(now)
    }
  }

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    const text = (k: string) => String(f.get(k) ?? '').trim() || null
    onSubmit({
      title: String(f.get('title')), description: String(f.get('description')),
      start: fromLocalInput(String(f.get('start')))!, end: fromLocalInput(String(f.get('end')))!,
      location: text('location'), onlineLink: text('onlineLink'), capacity: Number(f.get('capacity')),
      registrationOpen: fromLocalInput(String(f.get('registrationOpen'))),
      registrationClose: fromLocalInput(String(f.get('registrationClose'))),
      visibility: 'Public', // private visibility is a Final-milestone feature (#33)
    })
  }

  return (
    <form onSubmit={submit} className="my-3 space-y-4 rounded-lg border border-blue-200 bg-blue-50/40 p-4" aria-label="Activity form">
      <h3 className="text-lg font-semibold">{initial ? `Edit ${initial.title}` : 'New activity'}</h3>
      <section className={section}>
        <h4 className={sectionTitle}>About</h4>
        <label className="block">Activity title<input name="title" required defaultValue={initial?.title} className={field} /></label>
        <label className="block">Activity description<textarea name="description" required rows={3} defaultValue={initial?.description} className={field} /></label>
      </section>
      <section className={section}>
        <h4 className={sectionTitle}>When</h4>
        <div className="grid gap-3 sm:grid-cols-2">
          <DateTimeField label="Start" name="start" required value={start} onChange={changeStart} />
          <DateTimeField label="End" name="end" required value={end} onChange={setEnd} />
        </div>
      </section>
      <section className={section}>
        <h4 className={sectionTitle}>Where</h4>
        <p className={hint}>Give a location, an online link, or both.</p>
        <div className="grid gap-3 sm:grid-cols-2">
          <label className="block">Location<input name="location" placeholder="Room, building" defaultValue={initial?.location ?? ''} className={field} /></label>
          <label className="block">Online link<input name="onlineLink" type="url" placeholder="https://" defaultValue={initial?.onlineLink ?? ''} className={field} /></label>
        </div>
      </section>
      <section className={section}>
        <h4 className={sectionTitle}>Registration</h4>
        <p className={hint}>Registration must close by the time the activity starts.</p>
        <label className="block sm:w-40">Capacity<input name="capacity" type="number" min={1} required defaultValue={initial?.capacity} className={field} /></label>
        <div className="grid gap-3 sm:grid-cols-2">
          <DateTimeField label="Registration opens" name="registrationOpen" value={registrationOpen} onChange={setRegistrationOpen} />
          <DateTimeField label="Registration closes" name="registrationClose" value={registrationClose} onChange={setRegistrationClose} />
        </div>
      </section>
      <div className="flex justify-end gap-2">
        <button type="button" className={btn} onClick={onCancel}>Discard</button>
        <button type="submit" className="rounded bg-blue-700 px-4 py-2 text-white">Save activity</button>
      </div>
    </form>
  )
}

function AgendaForm({ activity, onSubmit }: { activity: Activity; onSubmit: (b: api.AgendaInput) => void }) {
  const [round, setRound] = useState(0)
  const [withSpeaker, setWithSpeaker] = useState(false)
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    onSubmit({
      time: fromLocalInput(String(f.get('time')))!, title: String(f.get('title')),
      speaker: String(f.get('speaker') ?? '').trim() || null,
    })
    e.currentTarget.reset()
    setRound(round + 1)
    setWithSpeaker(false)
  }
  return (
    <form onSubmit={submit} className="mt-3 space-y-2 rounded bg-gray-50 p-3" aria-label="Agenda form">
      <div className="grid items-end gap-2 sm:grid-cols-[auto_1fr_auto]">
        {/* The date starts on the activity's day; agenda items must fall within the activity. */}
        <DateTimeField key={round} label="Agenda time" name="time" required defaultDate={toLocalInput(activity.start).slice(0, 10)} />
        <label className="block">Agenda title<input name="title" required className={field} /></label>
        <button type="submit" className={`${btn} h-10`}>Add agenda item</button>
      </div>
      {withSpeaker ? (
        <div className="flex items-end gap-2">
          <label className="block flex-1 sm:max-w-sm">Speaker <span className="text-gray-500">(optional)</span>
            <input name="speaker" autoFocus className={field} />
          </label>
          <button type="button" className="h-10 text-sm text-gray-700 underline" onClick={() => setWithSpeaker(false)}>Remove speaker</button>
        </div>
      ) : (
        <button type="button" className="text-sm text-blue-700 underline" onClick={() => setWithSpeaker(true)}>+ Add speaker (optional)</button>
      )}
    </form>
  )
}

export function EventAuthoringPage() {
  const { eventId = '' } = useParams()
  const { error, run } = useRun()
  const [event, setEvent] = useState<AuthoringEvent | null>(null)
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<'new' | string | null>(null)
  const [done, setDone] = useState<string | null>(null)
  const { confirm, dialog } = useConfirm()

  useEffect(() => {
    let current = true
    void run(async () => {
      const e = await api.getEvent(eventId)
      if (current) setEvent(e)
    }).then(() => current && setLoading(false))
    return () => { current = false }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventId])

  /** Every write returns the fresh EventView, which carries the new version. */
  const write = (message: string, action: (e: AuthoringEvent) => Promise<AuthoringEvent>) =>
    run(async () => {
      setDone(null)
      setEvent(await action(event!))
      setEditing(null)
      setDone(message)
    })

  if (loading) return <main className="p-4">Loading…</main>
  if (!event) return <main className="mx-auto max-w-3xl p-4"><ErrorBox error={error} /></main>

  const locked = event.state === 'Completed' || event.state === 'Cancelled'
  const canEdit = event.capabilities.canEdit && !locked

  function saveDetails(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    void write('Details saved.', (ev) => api.editDetails(ev.id, ev.version, {
      title: String(f.get('title')), description: String(f.get('description')), category: f.get('category') as Category,
    }))
  }

  const isDraft = event.state === 'Draft'

  return (
    <main className="mx-auto max-w-3xl space-y-6 p-4">
      <header>
        <Link to="/authoring/events" className="text-sm text-blue-700 underline">← My events</Link>
        <p className="mt-2 text-sm font-medium uppercase tracking-wide text-gray-600">{isDraft ? 'Draft event' : 'Event'}</p>
        <h1 className="text-2xl font-semibold">{event.title}</h1>
        <p className="mt-2"><span className="rounded-full bg-gray-100 px-3 py-1 text-sm font-medium">State: {event.state}</span></p>
        {!isDraft && (
          <nav className="mt-3 space-x-3 text-sm" aria-label="Event management">
            <Link className="text-blue-700 underline" to={`/events/${event.id}/lifecycle`}>Manage event</Link>
            <Link className="text-blue-700 underline" to={`/events/${event.id}/report`}>Report</Link>
          </nav>
        )}
      </header>
      <ErrorBox error={error} />
      {done && <p role="status" className="rounded bg-green-100 p-3 text-green-800">{done}</p>}

      {canEdit && (
        <section className={section} aria-labelledby="details-heading">
          <h2 id="details-heading" className="text-lg font-semibold">1. Details</h2>
          <form onSubmit={saveDetails} className="space-y-3" aria-label="Event details">
            <label className="block">Title<input name="title" required defaultValue={event.title} className={field} /></label>
            <label className="block">Description<textarea name="description" required rows={3} defaultValue={event.description} className={field} /></label>
            <label className="block sm:w-64">Category
              <select name="category" defaultValue={event.category} className={field}>
                {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
              </select>
            </label>
            <div className="flex justify-end"><button type="submit" className={btn}>Save details</button></div>
          </form>
        </section>
      )}

      <section className={section} aria-labelledby="activities-heading">
        <div className="flex items-center justify-between gap-2">
          <h2 id="activities-heading" className="text-lg font-semibold">{canEdit ? '2. Activities' : 'Activities'}</h2>
          {canEdit && editing !== 'new' && (
            <button type="button" className="rounded bg-blue-700 px-3 py-1.5 text-white" onClick={() => setEditing('new')}>Add activity</button>
          )}
        </div>
        {event.activities.length === 0 && editing !== 'new' && (
          <p className={hint}>No activities yet. Add at least one to publish.</p>
        )}
        <ul className="space-y-3">
          {event.activities.map((a) => (
            <li key={a.id} className="rounded border border-gray-200 p-3">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <p className="font-medium">{a.title}{a.cancelled && ' (cancelled)'}</p>
                  <p className="text-sm text-gray-600">{fmt(a.start)} - {fmt(a.end)} · {a.location ?? a.onlineLink} · capacity {a.capacity}</p>
                </div>
                {canEdit && !a.cancelled && (
                  <div className="flex gap-2">
                    <button type="button" className={btn} onClick={() => setEditing(a.id)}>Edit {a.title}</button>
                    <button type="button" className={btn}
                      onClick={async () => {
                        if (await confirm({ title: 'Remove activity?', message: `Remove ${a.title} and its agenda from this event?`, confirmLabel: 'Remove activity', destructive: true }))
                          void write('Activity removed.', (ev) => api.removeActivity(ev.id, a.id, ev.version))
                      }}>
                      Remove {a.title}
                    </button>
                  </div>
                )}
              </div>
              {a.agenda.length > 0 && (
                <ul className="mt-2 space-y-1 text-sm">
                  {a.agenda.map((i) => (
                    <li key={i.id}>
                      {fmt(i.time)} {i.title}{i.speaker && ` (${i.speaker})`}{' '}
                      {canEdit && !a.cancelled && (
                        <button type="button" className="text-red-700 underline" aria-label={`Remove agenda item ${i.title}`}
                          onClick={() => write('Agenda item removed.', (ev) => api.removeAgenda(ev.id, a.id, i.id, ev.version))}>Remove</button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
              {canEdit && !a.cancelled && (
                <AgendaForm activity={a} onSubmit={(b) => write('Agenda item added.', (ev) => api.saveAgenda(ev.id, a.id, ev.version, b))} />
              )}
              {editing === a.id && (
                <ActivityForm initial={a} onCancel={() => setEditing(null)}
                  onSubmit={(b) => write('Activity saved.', (ev) => api.saveActivity(ev.id, ev.version, b, a.id))} />
              )}
            </li>
          ))}
        </ul>
        {editing === 'new' && (
          <ActivityForm onCancel={() => setEditing(null)}
            onSubmit={(b) => write('Activity added.', (ev) => api.saveActivity(ev.id, ev.version, b))} />
        )}
      </section>

      {canEdit && isDraft && event.capabilities.canPublish && (
        <section className={section} aria-labelledby="publish-heading">
          <h2 id="publish-heading" className="text-lg font-semibold">3. Publish</h2>
          <p className={hint}>
            Each activity needs a schedule, a location or online link, a capacity and a registration window.
            If something is missing, every problem is listed when you publish.
          </p>
          <div className="flex justify-end">
            <button type="button" className="rounded bg-green-700 px-4 py-2 text-white"
              onClick={async () => {
                if (await confirm({ title: 'Publish event?', message: `Publish ${event.title}? It becomes visible in Discover and people can sign up.`, confirmLabel: 'Publish' }))
                  void write('Event published.', (ev) => api.publish(ev.id, ev.version))
              }}>Publish event</button>
          </div>
        </section>
      )}
      {dialog}
    </main>
  )
}
