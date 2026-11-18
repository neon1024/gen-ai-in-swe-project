import { render, screen, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AuthoringEvent } from './api'
import { eventAuthoringRoutes } from './routes'

const json = (b: unknown) =>
  Promise.resolve(new Response(JSON.stringify(b), { status: 200, headers: { 'Content-Type': 'application/json' } }))

function ev(o: Partial<AuthoringEvent>): AuthoringEvent {
  return {
    id: 'e', organizationRef: 'org', title: 'Event', description: 'd', category: 'Social',
    state: 'Draft', version: 1, activities: [], capabilities: { canEdit: true, canPublish: true }, ...o,
  }
}

const orgs = [{ id: 'org', name: 'AI Society', description: '', member: true, role: 'ORGANIZER', requestStatus: 'APPROVED' }]

const fetchMock = vi.fn()
beforeEach(() => vi.stubGlobal('fetch', fetchMock))
afterEach(() => { fetchMock.mockReset(); vi.unstubAllGlobals() })

function renderPage() {
  const router = createMemoryRouter(eventAuthoringRoutes, { initialEntries: ['/authoring/events'] })
  render(<RouterProvider router={router} />)
}

function respond(events: AuthoringEvent[]) {
  fetchMock.mockImplementation((url: string) => json(url.startsWith('/api/organizations') ? orgs : events))
}

describe('US-14 my events', () => {
  it('lists drafts separately from published events, each linking to its editor', async () => {
    respond([
      ev({ id: 'd1', title: 'Chess night' }),
      ev({ id: 'p1', title: 'AI talk', state: 'Published' }),
    ])
    renderPage()

    const drafts = await screen.findByRole('region', { name: 'Drafts' })
    const link = within(drafts).getByRole('link', { name: /Chess night/ })
    expect(link).toHaveAttribute('href', '/authoring/events/d1')
    expect(within(drafts).getByText(/AI Society/)).toBeInTheDocument()
    const published = screen.getByRole('region', { name: 'Published' })
    expect(within(published).getByRole('link', { name: /AI talk/ })).toHaveAttribute('href', '/authoring/events/p1')
  })

  it('points to event creation when there are no events yet', async () => {
    respond([])
    renderPage()

    expect(await screen.findByText('You have no events yet.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Create an event' })).toHaveAttribute('href', '/authoring/events/new')
  })
})
