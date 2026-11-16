import { useEffect, useState, type FormEvent } from 'react'
import { useParams } from 'react-router'
import * as api from './api'
import { CATEGORIES, type Activity, type AuthoringEvent, type Category } from './api'
import { btn, field, fromLocalInput, toLocalInput, useRun } from './helpers'
import { ErrorBox } from './ui'

const fmt = (iso: string) => new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })

function ActivityForm({ initial, onSubmit, onCancel }: {
  initial?: Activity; onSubmit: (b: api.ActivityInput) => void; onCancel: () => void
}) {
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
    <form onSubmit={submit} className="my-3 space-y-2 rounded border p-3" aria-label="Activity form">
      <label className="block">Activity title<input name="title" required defaultValue={initial?.title} className={field} /></label>
      <label className="block">Activity description<textarea name="description" required defaultValue={initial?.description} className={field} /></label>
      <label className="block">Start<input name="start" type="datetime-local" required defaultValue={toLocalInput(initial?.start ?? null)} className={field} /></label>
      <label className="block">End<input name="end" type="datetime-local" required defaultValue={toLocalInput(initial?.end ?? null)} className={field} /></label>
      <label className="block">Location<input name="location" defaultValue={initial?.location ?? ''} className={field} /></label>
      <label className="block">Online link<input name="onlineLink" defaultValue={initial?.onlineLink ?? ''} className={field} /></label>
      <label className="block">Capacity<input name="capacity" type="number" min={1} required defaultValue={initial?.capacity} className={field} /></label>
      <label className="block">Registration opens<input name="registrationOpen" type="datetime-local" defaultValue={toLocalInput(initial?.registrationOpen ?? null)} className={field} /></label>
      <label className="block">Registration closes<input name="registrationClose" type="datetime-local" defaultValue={toLocalInput(initial?.registrationClose ?? null)} className={field} /></label>
      <button type="submit" className="rounded bg-blue-700 px-3 py-1 text-white">Save activity</button>{' '}
      <button type="button" className={btn} onClick={onCancel}>Discard</button>
    </form>
  )
}

function AgendaForm({ onSubmit }: { onSubmit: (b: api.AgendaInput) => void }) {
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    onSubmit({
      time: fromLocalInput(String(f.get('time')))!, title: String(f.get('title')),
      speaker: String(f.get('speaker') ?? '').trim() || null,
    })
    e.currentTarget.reset()
  }
  return (
    <form onSubmit={submit} className="mt-2 flex flex-wrap items-end gap-2" aria-label="Agenda form">
      <label>Agenda time<input name="time" type="datetime-local" required className={field} /></label>
      <label>Agenda title<input name="title" required className={field} /></label>
      <label>Speaker<input name="speaker" className={field} /></label>
      <button type="submit" className={btn}>Add agenda item</button>
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

  const locked = event.state === 'COMPLETED' || event.state === 'CANCELLED'
  const canEdit = event.capabilities.canEdit && !locked

  function saveDetails(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    void write('Details saved.', (ev) => api.editDetails(ev.id, ev.version, {
      title: String(f.get('title')), description: String(f.get('description')), category: f.get('category') as Category,
    }))
  }

  return (
    <main className="mx-auto max-w-3xl p-4">
      <h1 className="text-2xl font-semibold">{event.title}</h1>
      <p className="mb-4">State: {event.state === 'DRAFT' ? 'Draft' : event.state[0] + event.state.slice(1).toLowerCase()}</p>
      <ErrorBox error={error} />
      {done && <p role="status" className="mb-4 rounded bg-green-100 p-3 text-green-800">{done}</p>}

      {canEdit && (
        <form onSubmit={saveDetails} className="mb-6 space-y-2" aria-label="Event details">
          <label className="block">Title<input name="title" required defaultValue={event.title} className={field} /></label>
          <label className="block">Description<textarea name="description" required defaultValue={event.description} className={field} /></label>
          <label className="block">Category
            <select name="category" defaultValue={event.category} className={field}>
              {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
            </select>
          </label>
          <button type="submit" className={btn}>Save details</button>
        </form>
      )}

      <h2 className="mb-2 text-lg font-medium">Activities</h2>
      {event.activities.length === 0 && <p className="text-gray-600">No activities yet. Add at least one to publish.</p>}
      <ul className="space-y-3">
        {event.activities.map((a) => (
          <li key={a.id} className="rounded border p-3">
            <p className="font-medium">{a.title}{a.cancelled && ' (cancelled)'}</p>
            <p className="text-sm text-gray-600">{fmt(a.start)} - {fmt(a.end)} · {a.location ?? a.onlineLink} · capacity {a.capacity}</p>
            <ul className="mt-2 text-sm">
              {a.agenda.map((i) => (
                <li key={i.id}>
                  {fmt(i.time)} {i.title}{i.speaker && ` (${i.speaker})`}{' '}
                  {canEdit && !a.cancelled && (
                    <button type="button" className={btn} aria-label={`Remove agenda item ${i.title}`}
                      onClick={() => write('Agenda item removed.', (ev) => api.removeAgenda(ev.id, a.id, i.id, ev.version))}>Remove</button>
                  )}
                </li>
              ))}
            </ul>
            {canEdit && !a.cancelled && (
              <>
                <AgendaForm onSubmit={(b) => write('Agenda item added.', (ev) => api.saveAgenda(ev.id, a.id, ev.version, b))} />
                <div className="mt-2 space-x-2">
                  <button type="button" className={btn} onClick={() => setEditing(a.id)}>Edit {a.title}</button>
                  <button type="button" className={btn}
                    onClick={() => window.confirm('Remove this activity?') && write('Activity removed.', (ev) => api.removeActivity(ev.id, a.id, ev.version))}>
                    Remove {a.title}
                  </button>
                </div>
                {editing === a.id && (
                  <ActivityForm initial={a} onCancel={() => setEditing(null)}
                    onSubmit={(b) => write('Activity saved.', (ev) => api.saveActivity(ev.id, ev.version, b, a.id))} />
                )}
              </>
            )}
          </li>
        ))}
      </ul>

      {canEdit && (
        <div className="mt-4 space-x-2">
          <button type="button" className={btn} onClick={() => setEditing('new')}>Add activity</button>
          {event.state === 'DRAFT' && event.capabilities.canPublish && (
            <button type="button" className="rounded bg-green-700 px-3 py-1 text-white"
              onClick={() => write('Event published.', (ev) => api.publish(ev.id, ev.version))}>Publish event</button>
          )}
        </div>
      )}
      {editing === 'new' && (
        <ActivityForm onCancel={() => setEditing(null)}
          onSubmit={(b) => write('Activity added.', (ev) => api.saveActivity(ev.id, ev.version, b))} />
      )}
    </main>
  )
}
