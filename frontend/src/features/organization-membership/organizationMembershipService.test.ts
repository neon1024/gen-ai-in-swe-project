import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { organizationMembershipApi } from './organizationMembershipService'

const organizationId = '10000000-0000-4000-8000-000000000001'
const userId = '00000000-0000-4000-8000-000000000001'
const requestId = '12000000-0000-4000-8000-000000000001'

const fetchMock = vi.fn<typeof fetch>()

beforeEach(() => {
  localStorage.clear()
  document.cookie = 'XSRF-TOKEN=csrf-123'
  fetchMock.mockReset()
  fetchMock.mockResolvedValue(new Response(null, { status: 204 }))
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('organizationMembershipApi', () => {
  it('sends the session cookie and CSRF token, never an identity header', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify([]), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }))

    await organizationMembershipApi.listOrganizations()

    const [path, init] = fetchMock.mock.calls[0]
    expect(path).toBe('/api/organizations')
    expect(init?.credentials).toBe('include')
    expect(new Headers(init?.headers).get('X-XSRF-TOKEN')).toBe('csrf-123')
    expect(new Headers(init?.headers).get('X-User-Id')).toBeNull()
  })

  it('maps membership actions to their backend routes and role payload', async () => {
    await organizationMembershipApi.requestToJoin(organizationId)
    await organizationMembershipApi.withdrawRequest(organizationId)
    await organizationMembershipApi.listPendingRequests(organizationId)
    await organizationMembershipApi.approveRequest(organizationId, requestId)
    await organizationMembershipApi.rejectRequest(organizationId, requestId)
    await organizationMembershipApi.listMembers(organizationId)
    await organizationMembershipApi.changeRole(organizationId, userId, 'ORGANIZER')
    await organizationMembershipApi.removeMember(organizationId, userId)
    await organizationMembershipApi.leaveOrganization(organizationId)

    const calls = fetchMock.mock.calls.map(([path, init]) => ({
      path,
      method: init?.method ?? 'GET',
      body: init?.body,
    }))
    expect(calls.map(({ path, method }) => [path, method])).toEqual([
      [`/api/organizations/${organizationId}/join-requests`, 'POST'],
      [`/api/organizations/${organizationId}/join-requests/me`, 'DELETE'],
      [`/api/organizations/${organizationId}/join-requests`, 'GET'],
      [`/api/organizations/${organizationId}/join-requests/${requestId}/approve`, 'POST'],
      [`/api/organizations/${organizationId}/join-requests/${requestId}/reject`, 'POST'],
      [`/api/organizations/${organizationId}/members`, 'GET'],
      [`/api/organizations/${organizationId}/members/${userId}`, 'PATCH'],
      [`/api/organizations/${organizationId}/members/${userId}`, 'DELETE'],
      [`/api/organizations/${organizationId}/members/me`, 'DELETE'],
    ])
    expect(calls[6].body).toBe(JSON.stringify({ role: 'ORGANIZER' }))
  })
})
