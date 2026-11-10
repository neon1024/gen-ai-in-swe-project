import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { EventReport, FeedbackSummary } from '../api/types'
import { eventLifecycleRoutes } from '../routes'

const EVENT_ID = '0d5c4a52-6a0e-4a38-9f3e-2f0f1c8a7b11'
const ACT_1 = '2a6e1d33-8c2f-4b71-9d10-4e5f7a8b9c01'
const ACT_2 = '3b7f2e44-9d30-4c82-8e21-5f6a8b9c0d02'

const report: EventReport = {
  eventId: EVENT_ID,
  eventTitle: 'Chess night',
  eventState: 'COMPLETED',
  activities: [
    {
      activityId: ACT_1, title: 'Opening round', startsAt: '2027-02-01T18:00:00Z', state: 'SCHEDULED',
      confirmedSignups: 4, waitlistSize: 2, cancellations: 1, attendanceRate: 0.75, averageRating: 4.5, ratingCount: 2,
    },
    {
      activityId: ACT_2, title: 'Finals', startsAt: null, state: 'CANCELLED',
      confirmedSignups: 0, waitlistSize: 0, cancellations: 3, attendanceRate: null, averageRating: null, ratingCount: 0,
    },
  ],
}

const summary: FeedbackSummary = {
  activityId: ACT_1,
  activityTitle: 'Opening round',
  count: 2,
  averageRating: 4.5,
  distribution: { 1: 0, 2: 0, 3: 0, 4: 1, 5: 1 },
  comments: [
    { rating: 5, comment: 'Great pace' },
    { rating: 4, comment: null },
  ],
}

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }),
  )
}

const fetchMock = vi.fn()

function renderPage() {
  const router = createMemoryRouter(eventLifecycleRoutes, {
    initialEntries: [`/events/${EVENT_ID}/report`],
  })
  render(<RouterProvider router={router} />)
}

function requests(): string[] {
  return fetchMock.mock.calls.map(([url, init]) => `${init?.method ?? 'GET'} ${url}`)
}

beforeEach(() => {
  fetchMock.mockReset()
  fetchMock.mockImplementation((url: string) =>
    url.endsWith('/summary') ? jsonResponse(summary) : jsonResponse(report),
  )
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('EventReportPage', () => {
  it('US-49 report table shows figures and a dash for missing rates', async () => {
    renderPage()

    const first = (await screen.findByRole('row', { name: /Opening round/ }))
    const cells = within(first).getAllByRole('cell').map((c) => c.textContent)
    expect(cells.slice(2, 8)).toEqual(['Scheduled', '4', '2', '1', '75.0%', '4.5'])

    const second = screen.getByRole('row', { name: /Finals/ })
    const dashes = within(second).getAllByRole('cell').map((c) => c.textContent).slice(0, 8)
    expect(dashes).toEqual(['Finals', '—', 'Cancelled', '0', '0', '3', '—', '—'])
  })

  it('US-50 the Download CSV button fetches the CSV through the API client', async () => {
    const user = userEvent.setup()
    URL.createObjectURL = vi.fn(() => 'blob:report')
    URL.revokeObjectURL = vi.fn()
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    fetchMock.mockImplementation((url: string) =>
      url.endsWith('/csv')
        ? Promise.resolve(new Response('a,b', { status: 200 }))
        : url.endsWith('/summary') ? jsonResponse(summary) : jsonResponse(report),
    )
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Download CSV' }))

    await waitFor(() => expect(click).toHaveBeenCalledTimes(1))
    expect(requests()).toContain(`GET /api/reports/events/${EVENT_ID}/csv`)
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1)
  })

  it('US-50 a refused CSV download shows the error in the alert region', async () => {
    const user = userEvent.setup()
    fetchMock.mockImplementation((url: string) =>
      url.endsWith('/csv')
        ? jsonResponse({ code: 'TEAM_MEMBER_REQUIRED', detail: 'Team members only' }, 403)
        : jsonResponse(report),
    )
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Download CSV' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Team members only')
  })

  it('US-48 feedback panel shows average, distribution and comments without authors', async () => {
    const user = userEvent.setup()
    renderPage()

    const toggle = await screen.findByRole('button', { name: 'Show feedback for Opening round' })
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(requests().filter((r) => r.endsWith('/summary'))).toHaveLength(0)
    await user.click(toggle)

    const panel = await screen.findByRole('region', { name: 'Feedback for Opening round' })
    expect(toggle).toHaveAttribute('aria-expanded', 'true')
    expect(within(panel).getByText('Average rating: 4.5 (2 ratings)')).toBeInTheDocument()
    const rows = within(panel).getAllByRole('listitem', { name: /star/ }).map((li) => li.textContent)
    expect(rows).toEqual(['5 stars1', '4 stars1', '3 stars0', '2 stars0', '1 star0'])
    expect(within(panel).getByText('Great pace')).toBeInTheDocument()
    expect(within(panel).getByText('5/5')).toBeInTheDocument()
    expect(requests()).toContain(`GET /api/feedback/activities/${ACT_1}/summary`)

    await user.click(toggle)
    await user.click(toggle)
    await screen.findByRole('region', { name: 'Feedback for Opening round' })
    expect(requests().filter((r) => r.endsWith('/summary'))).toHaveLength(1)
  })
})
