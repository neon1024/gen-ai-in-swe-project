import { useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { searchEvents } from '../api/discovery'
import type { EventCategory, EventSearchResult } from '../api/types'
import { CATEGORY_LABELS } from '../categories'
import { Button } from '../../../components/ui/button'
import { Input } from '../../../components/ui/input'
import { Label } from '../../../components/ui/label'
import { Card, CardContent } from '../../../components/ui/card'

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

function formatStart(iso: string | null): string {
  if (iso === null) {
    return 'Not scheduled'
  }
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

export function EventSearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''
  const from = searchParams.get('from') ?? ''
  const to = searchParams.get('to') ?? ''
  const organizationId = searchParams.get('organizationId') ?? ''
  const category = searchParams.get('category') ?? ''
  const parsedPage = Number.parseInt(searchParams.get('page') ?? '0', 10)
  const page = Number.isInteger(parsedPage) && parsedPage > 0 ? parsedPage : 0

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
      page: page > 0 ? page : undefined,
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
  }, [q, from, to, organizationId, category, page])

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

  function goToPage(target: number) {
    const next = new URLSearchParams(searchParams)
    if (target > 0) {
      next.set('page', String(target))
    } else {
      next.delete('page')
    }
    setSearchParams(next)
  }

  return (
    <main className="page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Find your next room full of people</p>
          <h1>Events</h1>
          <p className="lede">Browse workshops, talks, and gatherings across campus.</p>
        </div>
      </div>
      <form onSubmit={submit} className="search-form">
        <Label className="search-keyword">
          Keyword
          <Input value={form.q} onChange={(e) => setForm({ ...form, q: e.target.value })} placeholder="Try “design”, “music”, or “career”" />
        </Label>
        <Label>
          From
          <Input type="date" value={form.from} onChange={(e) => setForm({ ...form, from: e.target.value })} />
        </Label>
        <Label>
          To
          <Input type="date" value={form.to} onChange={(e) => setForm({ ...form, to: e.target.value })} />
        </Label>
        <Label>
          Organization
          <select
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
        </Label>
        <Label>
          Category
          <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
            <option value="">All categories</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {CATEGORY_LABELS[c]}
              </option>
            ))}
          </select>
        </Label>
        <Button type="submit" className="search-submit">
          Search
        </Button>
      </form>

      {error && (
        <p role="alert" className="notice error">
          {error}
        </p>
      )}

      {!error && result && result.items.length === 0 && <Card className="empty"><CardContent><strong>No events match your search.</strong><p>Try a broader keyword or remove a filter.</p></CardContent></Card>}

      {!error && result && result.items.length > 0 && (
        <ul className="event-results">
          {result.items.map((event) => (
            <li key={event.id} className="event-result">
              <h2>{event.title}</h2>
              <p className="event-organization">{event.organizationName}</p>
              <p className="event-meta">
                <span>{CATEGORY_LABELS[event.category]}</span> · {event.nextActivityStartsAt === null ? (
                  formatStart(null)
                ) : (
                  <time dateTime={event.nextActivityStartsAt}>{formatStart(event.nextActivityStartsAt)}</time>
                )}
              </p>
            </li>
          ))}
        </ul>
      )}

      {!error && result && result.items.length > 0 && (
        <nav aria-label="Pagination" className="pagination">
          <Button
            type="button"
            disabled={page <= 0}
            onClick={() => goToPage(page - 1)}
            variant="outline"
          >
            Previous
          </Button>
          <span className="pagination-label">
            Page {page + 1} of {Math.max(1, Math.ceil(result.totalItems / result.size))}
          </span>
          <Button
            type="button"
            disabled={(page + 1) * result.size >= result.totalItems}
            onClick={() => goToPage(page + 1)}
            variant="outline"
          >
            Next
          </Button>
        </nav>
      )}
    </main>
  )
}
