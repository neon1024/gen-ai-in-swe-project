import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { eventLifecycleRoutes } from '../routes'

const ACTIVITY_ID = '2a6e1d33-8c2f-4b71-9d10-4e5f7a8b9c01'

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }),
  )
}

const fetchMock = vi.fn()

function renderPage() {
  const router = createMemoryRouter(eventLifecycleRoutes, {
    initialEntries: [`/activities/${ACTIVITY_ID}/feedback`],
  })
  render(<RouterProvider router={router} />)
}

function eligibleBackend() {
  fetchMock.mockImplementation((_url: string, init?: RequestInit) =>
    init?.method === 'POST'
      ? jsonResponse({ id: 'f1' }, 201)
      : jsonResponse({ eligible: true, reason: null }),
  )
}

function posts() {
  return fetchMock.mock.calls.filter(([, init]) => init?.method === 'POST')
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('GiveFeedbackPage', () => {
  it('US-47 eligible attendee submits a rating and comment', async () => {
    const user = userEvent.setup()
    eligibleBackend()
    renderPage()

    await user.click(await screen.findByRole('radio', { name: '4' }))
    await user.type(screen.getByLabelText('Comment (optional)'), 'Nice')
    await user.click(screen.getByRole('button', { name: 'Submit feedback' }))

    expect(await screen.findByText('Thank you for your feedback.')).toBeInTheDocument()
    expect(posts()).toHaveLength(1)
    const [url, init] = posts()[0]
    expect(url).toBe(`/api/feedback/activities/${ACTIVITY_ID}`)
    expect(init.body).toBe('{"rating":4,"comment":"Nice"}')
  })

  it('US-47 rating is required before submitting', async () => {
    const user = userEvent.setup()
    eligibleBackend()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Submit feedback' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Choose a rating from 1 to 5.')
    expect(posts()).toHaveLength(0)
  })

  it('US-47 a failed submission shows the server message', async () => {
    const user = userEvent.setup()
    fetchMock.mockImplementation((_url: string, init?: RequestInit) =>
      init?.method === 'POST'
        ? jsonResponse({ code: 'VALIDATION_FAILED', detail: 'Already rated.' }, 400)
        : jsonResponse({ eligible: true, reason: null }),
    )
    renderPage()

    await user.click(await screen.findByRole('radio', { name: '2' }))
    await user.click(screen.getByRole('button', { name: 'Submit feedback' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Already rated.')
    expect(screen.queryByText('Thank you for your feedback.')).not.toBeInTheDocument()
  })

  it('US-47 a refusal code is shown with its reason message, not the raw server text', async () => {
    const user = userEvent.setup()
    fetchMock.mockImplementation((_url: string, init?: RequestInit) =>
      init?.method === 'POST'
        ? jsonResponse({ code: 'FEEDBACK_WINDOW_CLOSED', detail: 'Feedback is not possible: FEEDBACK_WINDOW_CLOSED' }, 409)
        : jsonResponse({ eligible: true, reason: null }),
    )
    renderPage()

    await user.click(await screen.findByRole('radio', { name: '4' }))
    await user.click(screen.getByRole('button', { name: 'Submit feedback' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Feedback closed 7 days after the activity ended.')
  })

  it.each([
    ['NOT_ATTENDED', 'Only participants marked present can give feedback.'],
    ['FEEDBACK_NOT_OPEN', 'Feedback opens when the activity ends.'],
    ['FEEDBACK_WINDOW_CLOSED', 'Feedback closed 7 days after the activity ended.'],
    ['FEEDBACK_ALREADY_GIVEN', 'You have already rated this activity.'],
    ['ACTIVITY_CANCELLED', 'This activity was cancelled.'],
  ])('BR-16 ineligible reason %s shows its message', async (reason, message) => {
    fetchMock.mockImplementation(() => jsonResponse({ eligible: false, reason }))
    renderPage()

    expect(await screen.findByText(message)).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Submit feedback' })).not.toBeInTheDocument())
  })
})
