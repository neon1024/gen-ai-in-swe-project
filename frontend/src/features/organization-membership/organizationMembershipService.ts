import { apiFetch } from '../event-lifecycle/api/client'

export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | null
export type OrganizationRole = 'MEMBER' | 'ORGANIZER' | 'ORG_ADMIN'

export interface OrganizationMembership {
  id: string
  name: string
  description: string
  member: boolean
  role: OrganizationRole | null
  requestStatus: RequestStatus
}

export interface JoinRequest {
  id: string
  applicantId: string
  createdAt: string
}

export interface OrganizationMember {
  userId: string
  role: OrganizationRole
  eventTeamAssignmentCount: number
  ownsActiveEvents: boolean
}

export interface OrganizationMembershipService {
  listOrganizations(): Promise<OrganizationMembership[]>
  requestToJoin(organizationId: string): Promise<void>
  withdrawRequest(organizationId: string): Promise<void>
  listPendingRequests(organizationId: string): Promise<JoinRequest[]>
  approveRequest(organizationId: string, requestId: string): Promise<void>
  rejectRequest(organizationId: string, requestId: string): Promise<void>
  listMembers(organizationId: string): Promise<OrganizationMember[]>
  changeRole(organizationId: string, userId: string, role: OrganizationRole): Promise<void>
  removeMember(organizationId: string, userId: string): Promise<void>
  leaveOrganization(organizationId: string): Promise<void>
}

export const organizationMembershipApi: OrganizationMembershipService = {
  listOrganizations: () => apiFetch('/api/organizations'),

  async requestToJoin(organizationId) {
    await apiFetch(`/api/organizations/${organizationId}/join-requests`, { method: 'POST' })
  },

  async withdrawRequest(organizationId) {
    await apiFetch(`/api/organizations/${organizationId}/join-requests/me`, { method: 'DELETE' })
  },

  listPendingRequests: (organizationId) =>
    apiFetch(`/api/organizations/${organizationId}/join-requests`),

  async approveRequest(organizationId, requestId) {
    await apiFetch(`/api/organizations/${organizationId}/join-requests/${requestId}/approve`, { method: 'POST' })
  },

  async rejectRequest(organizationId, requestId) {
    await apiFetch(`/api/organizations/${organizationId}/join-requests/${requestId}/reject`, { method: 'POST' })
  },

  listMembers: (organizationId) => apiFetch(`/api/organizations/${organizationId}/members`),

  async changeRole(organizationId, userId, role) {
    await apiFetch(`/api/organizations/${organizationId}/members/${userId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ role }),
    })
  },

  async removeMember(organizationId, userId) {
    await apiFetch(`/api/organizations/${organizationId}/members/${userId}`, { method: 'DELETE' })
  },

  async leaveOrganization(organizationId) {
    await apiFetch(`/api/organizations/${organizationId}/members/me`, { method: 'DELETE' })
  },
}
