import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AuthoringEvent } from './api'
import { eventAuthoringRoutes } from './routes'

const ID = '0d5c4a52-6a0e-4a38-9f3e-2f0f1c8a7b11'
const ACT = '2a6e1d33-8c2f-4b71-9d10-4e5f7a8b9c01'

function ev(o: Partial<AuthoringEvent> = {}): AuthoringEvent {
  return {
    id: ID, organizationRef: 'org', title: 'Chess night', description: 'Fun', category: 'Social',
    state: 'DRAFT', version: 3, activities: [], capabilities: { canEdit: true, canPublish: true }, ...o,
  }
}
const activity = {
  id: ACT, title: 'Opening', description: 'd', start: '2027-02-01T18:00:00Z', end: '2027-02-01T20:00:00Z',
  location: 'Room A', onlineLink: null, capacity: 10, registrationOpen: null, registrationClose: null,
  visibility: 'Public', cancelled: false, agenda: [],
}
const json = (b: unknown, status = 200) =>
  Promise.resolve(new Response(JSON.stringify(b), { status, headers: { 'Content-Type': 'application/json' } }))

const fetchMock = vi.fn()
beforeEach(() => vi.stubGlobal('fetch', fetchMock))
afterEach(() => { fetchMock.mockReset(); vi.unstubAllGlobals() })

function renderAt(path: string) {
  const router = createMemoryRouter(eventAuthoringRoutes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
  return router
}
const lastBody = () => JSON.parse(fetchMock.mock.calls.at(-1)![1].body)

describe('US-14 create a draft event', () => {
  it('posts the draft and opens its authoring page', async () => {
    fetchMock.mockReturnValueOnce(json(ev(), 201)).mockReturnValueOnce(json(ev()))
    const router = renderAt('/authoring/events/new')
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Title'), 'Chess night')
    await user.type(screen.getByLabelText('Description'), 'Fun')
    await user.type(screen.getByLabelText('Organization'), 'org')
    await user.click(screen.getByRole('button', { name: 'Create draft' }))
    await waitFor(() => expect(router.state.location.pathname).toBe(`/authoring/events/${ID}`))
    expect(fetchMock.mock.calls[0][1].method).toBe('POST')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toMatchObject({ title: 'Chess night', category: 'Workshop', organizationRef: 'org' })
  })

  it('shows the server refusal', async () => {
    fetchMock.mockReturnValueOnce(json({ code: 'FORBIDDEN', message: 'Not allowed', violations: [] }, 403))
    renderAt('/authoring/events/new')
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Title'), 'x')
    await user.type(screen.getByLabelText('Description'), 'x')
    await user.type(screen.getByLabelText('Organization'), 'x')
    await user.click(screen.getByRole('button', { name: 'Create draft' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Not allowed')
  })
})

describe('US-15 and US-16 activities and agenda', () => {
  it('adds an activity with the current version and shows the refreshed list', async () => {
    fetchMock.mockReturnValueOnce(json(ev()))
      .mockReturnValueOnce(json(ev({ version: 4, activities: [activity] }, ), 201))
    renderAt(`/authoring/events/${ID}`)
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Add activity' }))
    const form = screen.getByRole('form', { name: 'Activity form' })
    await user.type(within(form).getByLabelText('Activity title'), 'Opening')
    await user.type(within(form).getByLabelText('Activity description'), 'd')
    await user.type(within(form).getByLabelText('Start'), '2027-02-01T18:00')
    await user.type(within(form).getByLabelText('End'), '2027-02-01T20:00')
    await user.type(within(form).getByLabelText('Location'), 'Room A')
    await user.type(within(form).getByLabelText('Capacity'), '10')
    await user.click(within(form).getByRole('button', { name: 'Save activity' }))
    expect(await screen.findByText(/capacity 10/)).toBeInTheDocument()
    expect(lastBody()).toMatchObject({ title: 'Opening', capacity: 10, expectedVersion: 3, visibility: 'Public' })
  })

  it('adds an agenda item and removes an activity', async () => {
    fetchMock.mockReturnValueOnce(json(ev({ activities: [activity] })))
      .mockReturnValueOnce(json(ev({ version: 4, activities: [{ ...activity, agenda: [{ id: 'i1', time: '2027-02-01T18:30:00Z', title: 'Talk', speaker: 'Ana' }] }] }), 201))
      .mockReturnValueOnce(json(ev({ version: 5 })))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderAt(`/authoring/events/${ID}`)
    const user = userEvent.setup()
    await screen.findByText('Opening')
    const agenda = screen.getByRole('form', { name: 'Agenda form' })
    await user.type(within(agenda).getByLabelText('Agenda time'), '2027-02-01T18:30')
    await user.type(within(agenda).getByLabelText('Agenda title'), 'Talk')
    await user.click(within(agenda).getByRole('button', { name: 'Add agenda item' }))
    expect(await screen.findByText(/Talk \(Ana\)/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Remove Opening' }))
    await waitFor(() => expect(screen.queryByText('Opening')).not.toBeInTheDocument())
    expect(fetchMock.mock.calls.at(-1)![0]).toContain('expectedVersion=4')
    expect(fetchMock.mock.calls.at(-1)![1].method).toBe('DELETE')
  })
})

describe('US-17 publish', () => {
  it('lists every readiness problem when publishing is refused', async () => {
    fetchMock.mockReturnValueOnce(json(ev()))
      .mockReturnValueOnce(json({ code: 'NOT_READY', message: 'Event is not ready', violations: [
        { path: 'activities', code: 'REQUIRED', message: 'At least one activity is required' },
        { path: 'title', code: 'X', message: 'Second problem' }] }, 422))
    renderAt(`/authoring/events/${ID}`)
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Publish event' }))
    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('At least one activity is required')
    expect(alert).toHaveTextContent('Second problem')
    expect(lastBody()).toEqual({ expectedVersion: 3 })
  })

  it('shows Published after success and hides the publish button', async () => {
    fetchMock.mockReturnValueOnce(json(ev())).mockReturnValueOnce(json(ev({ state: 'PUBLISHED', version: 4 })))
    renderAt(`/authoring/events/${ID}`)
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Publish event' }))
    expect(await screen.findByText('State: Published')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Publish event' })).not.toBeInTheDocument()
  })

  it('hides editing and publishing without the capability (Staff)', async () => {
    fetchMock.mockReturnValueOnce(json(ev({ capabilities: { canEdit: false, canPublish: false } })))
    renderAt(`/authoring/events/${ID}`)
    await screen.findByText('Chess night')
    expect(screen.queryByRole('button', { name: 'Publish event' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add activity' })).not.toBeInTheDocument()
  })
})

describe('US-18 edit a published event', () => {
  it('saves details with the expected version and surfaces a version conflict', async () => {
    fetchMock.mockReturnValueOnce(json(ev({ state: 'PUBLISHED' })))
      .mockReturnValueOnce(json({ code: 'VERSION_CONFLICT', message: 'Changed by someone else', violations: [] }, 409))
    renderAt(`/authoring/events/${ID}`)
    const user = userEvent.setup()
    const title = await screen.findByLabelText('Title')
    await user.clear(title)
    await user.type(title, 'Chess gala')
    await user.click(screen.getByRole('button', { name: 'Save details' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Changed by someone else')
    expect(lastBody()).toMatchObject({ expectedVersion: 3, title: 'Chess gala' })
  })

  it('refuses capacity below confirmed signups by showing the server message', async () => {
    fetchMock.mockReturnValueOnce(json(ev({ state: 'PUBLISHED', activities: [activity] })))
      .mockReturnValueOnce(json({ code: 'CAPACITY_BELOW_CONFIRMED', message: 'Capacity is below confirmed signups', violations: [] }, 409))
    renderAt(`/authoring/events/${ID}`)
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Edit Opening' }))
    const form = screen.getByRole('form', { name: 'Activity form' })
    const cap = within(form).getByLabelText('Capacity')
    await user.clear(cap)
    await user.type(cap, '1')
    await user.click(within(form).getByRole('button', { name: 'Save activity' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Capacity is below confirmed signups')
  })
})
