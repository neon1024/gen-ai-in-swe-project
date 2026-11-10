import { useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { searchEvents } from '../api/discovery'
import type { EventCategory, EventSearchResult } from '../api/types'
import { CATEGORY_LABELS } from '../categories'

const CATEGORIES = Object.keys(CATEGORY_LABELS) as EventCategory[]

function parseDate(value: string, endOfDay: boolean): string | undefined {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) {
    return undefined
  }
  const [, year, month, day] = match.map(Number)
  const date = endOfDay
    ? new Date(year, month - 1, day, 23, 59, 59, 999)
    : new Date(year, month - 1, day, 0, 0, 0, 0)
  return date.toISOString()
}

function formatStart(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

export function EventSearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''
  const from = searchParams.get('from') ?? ''
  const to = searchParams.get('to') ?? ''
  const organizationId = searchParams.get('organizationId') ?? ''
  const category = searchParams.get('category') ?? ''

  const [form, setForm] = useState({ q, from, to, organizationId, category })
  const [result, setResult] = useState<EventSearchResult | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let current = true
    searchEvents({
      q: q.trim(),
      from: parseDate(from, false),
      to: parseDate(to, true),
      organizationId,
      category: (CATEGORIES as string[]).includes(category) ? (category as EventCategory) : undefined,
    })
      .then((data) => {
        if (current) {
          setResult(data)
          setError(null)
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
  }, [q, from, to, organizationId, category])

  function submit(e: FormEvent) {
    e.preventDefault()
    const next = new URLSearchParams()
    const values = { ...form, q: form.q.trim() }
    for (const [key, value] of Object.entries(values)) {
      if (value) {
        next.set(key, value)
      }
    }
    setSearchParams(next)
  }

  const field = 'mt-1 block w-full rounded border border-gray-300 px-2 py-1'

  return (
    <main className="mx-auto max-w-3xl p-4">
      <h1 className="mb-4 text-2xl font-semibold">Events</h1>
      <form onSubmit={submit} className="mb-6 grid gap-3 sm:grid-cols-2">
        <label className="sm:col-span-2">
          Keyword
          <input className={field} value={form.q} onChange={(e) => setForm({ ...form, q: e.target.value })} />
        </label>
        <label>
          From
          <input type="date" className={field} value={form.from} onChange={(e) => setForm({ ...form, from: e.target.value })} />
        </label>
        <label>
          To
          <input type="date" className={field} value={form.to} onChange={(e) => setForm({ ...form, to: e.target.value })} />
        </label>
        <label>
          Organization
          <select
            className={field}
            value={form.organizationId}
            onChange={(e) => setForm({ ...form, organizationId: e.target.value })}
          >
            <option value="">All organizations</option>
            {result?.organizations.map((o) => (
              <option key={o.id} value={o.id}>
                {o.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Category
          <select className={field} value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
            <option value="">All categories</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {CATEGORY_LABELS[c]}
              </option>
            ))}
          </select>
        </label>
        <button type="submit" className="rounded bg-blue-600 px-4 py-2 text-white sm:col-span-2">
          Search
        </button>
      </form>

      {error && (
        <p role="alert" className="rounded bg-red-100 p-3 text-red-800">
          {error}
        </p>
      )}

      {!error && result && result.items.length === 0 && <p>No events match your search.</p>}

      {!error && result && result.items.length > 0 && (
        <ul className="space-y-3">
          {result.items.map((event) => (
            <li key={event.id} className="rounded border border-gray-200 p-3">
              <h2 className="text-lg font-medium">{event.title}</h2>
              <p className="text-sm text-gray-600">{event.organizationName}</p>
              <p className="text-sm">
                <span>{CATEGORY_LABELS[event.category]}</span> · <time dateTime={event.nextActivityStartsAt}>{formatStart(event.nextActivityStartsAt)}</time>
              </p>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
