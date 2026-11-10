import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { LifecycleEventView } from '../api/types'
import { eventLifecycleRoutes } from '../routes'

const EVENT_ID = '0d5c4a52-6a0e-4a38-9f3e-2f0f1c8a7b11'
const ACT_1 = '2a6e1d33-8c2f-4b71-9d10-4e5f7a8b9c01'
const ACT_2 = '3b7f2e44-9d30-4c82-8e21-5f6a8b9c0d02'

function view(overrides: Partial<LifecycleEventView> = {}): LifecycleEventView {
  return {
    id: EVENT_ID,
    title: 'Chess night',
    state: 'PUBLISHED',
    activities: [
      { id: ACT_1, title: 'Opening round', startsAt: '2027-02-01T18:00:00Z', endsAt: '2027-02-01T20:00:00Z', state: 'SCHEDULED' },
      { id: ACT_2, title: 'Finals', startsAt: '2027-02-02T18:00:00Z', endsAt: '2027-02-02T20:00:00Z', state: 'CANCELLED' },
    ],
    permissions: { canCancel: true, canDelete: false },
    ...overrides,
  }
}

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }),
  )
}

const fetchMock = vi.fn()

function renderPage() {
  const router = createMemoryRouter(eventLifecycleRoutes, {
    initialEntries: [`/events/${EVENT_ID}/lifecycle`],
  })
  render(<RouterProvider router={router} />)
  return router
}

function requests(): string[] {
  return fetchMock.mock.calls.map(([url, init]) => `${init?.method ?? 'GET'} ${url}`)
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('EventLifecyclePage', () => {
  it('US-20 cancelling an activity confirms the cascade and shows it cancelled', async () => {
    const user = userEvent.setup()
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const cancelled = view()
    cancelled.activities[0] = { ...cancelled.activities[0], state: 'CANCELLED' }
    fetchMock.mockImplementation((_url: string, init?: RequestInit) =>
      jsonResponse(init?.method === 'POST' ? cancelled : view()),
    )
    renderPage()

    const items = await screen.findAllByRole('listitem')
    expect(within(items[0]).getByText('Scheduled')).toBeInTheDocument()
    expect(within(items[1]).queryByRole('button', { name: 'Cancel activity' })).not.toBeInTheDocument()
    await user.click(within(items[0]).getByRole('button', { name: 'Cancel activity' }))

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('All signups and waitlist entries will be cancelled.'))
    expect(requests()).toContain(`POST /api/lifecycle/activities/${ACT_1}/cancel`)
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Cancel activity' })).not.toBeInTheDocument())
    expect(screen.getAllByText('Cancelled')).toHaveLength(2)
  })

  it('US-20 sends no request when the confirmation is declined', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    fetchMock.mockImplementation(() => jsonResponse(view()))
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Cancel activity' }))
    expect(requests()).toEqual([`GET /api/lifecycle/events/${EVENT_ID}`])
  })

  it('US-20 hides cancel actions without owner rights', async () => {
    fetchMock.mockImplementation(() => jsonResponse(view({ permissions: { canCancel: false, canDelete: false } })))
    renderPage()

    await screen.findByText('Opening round')
    expect(screen.queryByRole('button', { name: 'Cancel activity' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancel event' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete draft' })).not.toBeInTheDocument()
  })

  it('US-21 cancelling the event shows the cancelled read-only state', async () => {
    const user = userEvent.setup()
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const cancelled = view({
      state: 'CANCELLED',
      activities: view().activities.map((a) => ({ ...a, state: 'CANCELLED' as const })),
    })
    fetchMock.mockImplementation((_url: string, init?: RequestInit) =>
      jsonResponse(init?.method === 'POST' ? cancelled : view()),
    )
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Cancel event' }))

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('All signups and waitlist entries will be cancelled.'))
    expect(requests()).toContain(`POST /api/lifecycle/events/${EVENT_ID}/cancel`)
    expect(await screen.findByText('This event is read-only.')).toBeInTheDocument()
    expect(screen.getByText('Event state: Cancelled')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('US-22 delete is offered only for drafts and navigates to the search page', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const draft = view({ state: 'DRAFT', permissions: { canCancel: false, canDelete: true } })
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (init?.method === 'DELETE') {
        return Promise.resolve(new Response(null, { status: 204 }))
      }
      return url.startsWith('/api/discovery/')
        ? jsonResponse({ items: [], page: 0, size: 20, totalItems: 0, organizations: [] })
        : jsonResponse(draft)
    })
    const router = renderPage()

    expect(await screen.findByRole('button', { name: 'Delete draft' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancel event' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Delete draft' }))

    expect(requests()).toContain(`DELETE /api/lifecycle/events/${EVENT_ID}`)
    expect(await screen.findByRole('heading', { name: 'Events' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/events')
  })

  it('BR-06 a completed event shows no actions', async () => {
    fetchMock.mockImplementation(() => jsonResponse(view({ state: 'COMPLETED' })))
    renderPage()

    expect(await screen.findByText('This event is read-only.')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('US-20 shows the server refusal message', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    fetchMock.mockImplementation((_url: string, init?: RequestInit) =>
      init?.method === 'POST'
        ? jsonResponse({ title: 'Conflict', status: 409, detail: 'The activity has already ended', code: 'ACTIVITY_ENDED' }, 409)
        : jsonResponse(view()),
    )
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Cancel activity' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('The activity has already ended')
  })

  it('US-20 an activity without times shows Not scheduled instead of an invalid date', async () => {
    fetchMock.mockImplementation(() =>
      jsonResponse(
        view({
          activities: [{ id: ACT_1, title: 'TBD session', startsAt: null, endsAt: null, state: 'SCHEDULED' }],
        }),
      ),
    )
    renderPage()

    const item = await screen.findByRole('listitem')
    expect(within(item).getByText('Not scheduled')).toBeInTheDocument()
    expect(item).not.toHaveTextContent('1970')
    expect(item).not.toHaveTextContent('Invalid')
  })
})
