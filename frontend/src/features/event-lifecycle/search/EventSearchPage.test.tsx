import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { EventSearchResult } from '../api/types'
import { eventLifecycleRoutes } from '../routes'

const ORG_A = '5e2b9c47-1d3a-4c8e-b0a6-9f4d2e7a1c33'
const ORG_B = '7a1d2c33-0b4e-4d55-8a10-3c9f6e2b4d01'

const RESULT: EventSearchResult = {
  items: [
    {
      id: '0d5c4a52-6a0e-4a38-9f3e-2f0f1c8a7b11',
      title: 'Chess night',
      description: 'Casual games',
      category: 'SOCIAL',
      organizationId: ORG_A,
      organizationName: 'Chess Club',
      nextActivityStartsAt: '2027-02-01T18:00:00Z',
    },
    {
      id: '1e6d5b63-7b1f-4b49-8a4f-3a1a2d9b8c22',
      title: 'Java workshop',
      description: 'Intro',
      category: 'WORKSHOP',
      organizationId: ORG_B,
      organizationName: 'Dev Society',
      nextActivityStartsAt: '2027-03-05T10:00:00Z',
    },
  ],
  page: 0,
  size: 20,
  totalItems: 2,
  organizations: [
    { id: ORG_A, name: 'Chess Club' },
    { id: ORG_B, name: 'Dev Society' },
  ],
}

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }),
  )
}

const fetchMock = vi.fn()

function renderPage(url = '/events') {
  const router = createMemoryRouter(eventLifecycleRoutes, { initialEntries: [url] })
  return render(<RouterProvider router={router} />)
}

function lastUrl(): URL {
  const calls = fetchMock.mock.calls
  return new URL(String(calls[calls.length - 1][0]), 'http://localhost')
}

beforeEach(() => {
  fetchMock.mockReset()
  fetchMock.mockImplementation(() => jsonResponse(RESULT))
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('EventSearchPage', () => {
  it('US-24 lists events in the order returned and shows organization and category', async () => {
    renderPage()
    const items = await screen.findAllByRole('listitem')
    expect(items).toHaveLength(2)
    expect(within(items[0]).getByText('Chess night')).toBeInTheDocument()
    expect(within(items[0]).getByText('Chess Club')).toBeInTheDocument()
    expect(within(items[0]).getByText('Social')).toBeInTheDocument()
    expect(within(items[1]).getByText('Java workshop')).toBeInTheDocument()
    expect(within(items[1]).getByText('Dev Society')).toBeInTheDocument()
    expect(within(items[1]).getByText('Workshop')).toBeInTheDocument()
  })

  it('US-24 shows the empty state when nothing matches', async () => {
    fetchMock.mockImplementation(() => jsonResponse({ ...RESULT, items: [], totalItems: 0 }))
    renderPage()
    expect(await screen.findByText('No events match your search.')).toBeInTheDocument()
  })

  it('US-24 sends the trimmed keyword as q', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findAllByRole('listitem')
    await user.type(screen.getByLabelText('Keyword'), '  java ')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    await screen.findAllByRole('listitem')
    expect(lastUrl().searchParams.get('q')).toBe('java')
    expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain('q=java')
  })

  it('US-25 offers the six categories with their labels', async () => {
    renderPage()
    await screen.findAllByRole('listitem')
    const select = screen.getByLabelText('Category')
    const labels = within(select).getAllByRole('option').map((o) => o.textContent)
    expect(labels).toEqual(['All categories', 'Workshop', 'Conference', 'Training', 'Social', 'Competition', 'Other'])
  })

  it('US-25 combines date range, organization and category with the keyword', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findAllByRole('listitem')
    await user.type(screen.getByLabelText('Keyword'), 'java')
    await user.type(screen.getByLabelText('From'), '2027-02-01')
    await user.type(screen.getByLabelText('To'), '2027-02-28')
    await user.selectOptions(screen.getByLabelText('Organization'), 'Dev Society')
    await user.selectOptions(screen.getByLabelText('Category'), 'Workshop')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    await screen.findAllByRole('listitem')
    const params = lastUrl().searchParams
    expect(params.get('q')).toBe('java')
    expect(params.get('organizationId')).toBe(ORG_B)
    expect(params.get('category')).toBe('WORKSHOP')
    expect(params.get('from')).toBe(new Date(2027, 1, 1, 0, 0, 0, 0).toISOString())
    expect(params.get('to')).toBe(new Date(2027, 1, 28, 23, 59, 59, 999).toISOString())
  })

  it('US-24 shows the API error message on failure', async () => {
    fetchMock.mockImplementation(() =>
      jsonResponse(
        { title: 'Bad Request', status: 400, detail: 'from must not be after to', code: 'VALIDATION_FAILED' },
        400,
      ),
    )
    renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent('from must not be after to')
  })
})
