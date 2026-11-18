import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import {
  type JoinRequest,
  type OrganizationMember,
  type OrganizationMembership,
  type OrganizationMembershipService,
  type OrganizationRole,
} from './organizationMembershipService'
import { OrganizationMembershipPage } from './OrganizationMembershipPage'
import { AuthProvider } from '../authentication/context'

const ids = {
  ai: '10000000-0000-4000-8000-000000000001',
  robotics: '10000000-0000-4000-8000-000000000002',
  language: '10000000-0000-4000-8000-000000000003',
  photo: '10000000-0000-4000-8000-000000000004',
  student: '00000000-0000-4000-8000-000000000001',
  admin: '00000000-0000-4000-8000-000000000002',
  coAdmin: '00000000-0000-4000-8000-000000000006',
  organizer: '00000000-0000-4000-8000-000000000007',
  member: '00000000-0000-4000-8000-000000000008',
}

const SESSION_KEY = 'ubb.events.session'
const currentUserId = () => (JSON.parse(localStorage.getItem(SESSION_KEY) ?? '{}') as { userId?: string }).userId

function createService() {
  const statuses: Record<string, OrganizationMembership['requestStatus']> = {
    [ids.ai]: null,
    [ids.robotics]: 'PENDING',
    [ids.language]: 'APPROVED',
    [ids.photo]: 'REJECTED',
  }
  const members: OrganizationMember[] = [
    { userId: ids.admin, displayName: 'AI Society Admin', role: 'ORG_ADMIN', eventTeamAssignmentCount: 0, ownsActiveEvents: false },
    { userId: ids.coAdmin, displayName: 'AI Society Co-admin', role: 'ORG_ADMIN', eventTeamAssignmentCount: 1, ownsActiveEvents: false },
    { userId: ids.organizer, displayName: 'Demo Organizer', role: 'ORGANIZER', eventTeamAssignmentCount: 1, ownsActiveEvents: false },
    { userId: ids.member, displayName: 'Demo Member', role: 'MEMBER', eventTeamAssignmentCount: 0, ownsActiveEvents: false },
  ]
  let pending: JoinRequest[] = [{
    id: '12000000-0000-4000-8000-000000000001',
    applicantId: ids.student,
    applicantName: 'Demo Applicant',
    createdAt: '2026-11-16T00:00:00Z',
  }]

  const service: OrganizationMembershipService = {
    async listOrganizations() {
      const currentUser = currentUserId()
      return [
        { id: ids.ai, name: 'AI Society', description: 'AI projects.', member: currentUser === ids.admin || (currentUser === ids.student && statuses[ids.ai] === 'APPROVED'), role: currentUser === ids.admin ? 'ORG_ADMIN' : currentUser === ids.student && statuses[ids.ai] === 'APPROVED' ? 'MEMBER' : null, requestStatus: currentUser === ids.admin || (currentUser === ids.student && statuses[ids.ai] === 'APPROVED') ? 'APPROVED' : statuses[ids.ai] },
        { id: ids.robotics, name: 'Robotics Club', description: 'Robotics projects.', member: currentUser === ids.organizer, role: null, requestStatus: currentUser === ids.organizer ? 'APPROVED' : statuses[ids.robotics] },
        { id: ids.language, name: 'Language Exchange', description: 'Language practice.', member: currentUser === ids.member || currentUser === ids.student, role: currentUser === ids.member || currentUser === ids.student ? 'MEMBER' : null, requestStatus: currentUser === ids.member || currentUser === ids.student ? 'APPROVED' : statuses[ids.language] },
        { id: ids.photo, name: 'Photography Group', description: 'Photo workshops.', member: false, role: null, requestStatus: statuses[ids.photo] },
      ]
    },
    async requestToJoin(organizationId) {
      statuses[organizationId] = 'PENDING'
    },
    async withdrawRequest(organizationId) {
      statuses[organizationId] = null
    },
    async listPendingRequests() {
      return pending
    },
    async approveRequest(_organizationId, requestId) {
      const request = pending.find((item) => item.id === requestId)
      pending = pending.filter((item) => item.id !== requestId)
      if (request) {
        statuses[_organizationId] = 'APPROVED'
        members.push({ userId: request.applicantId, displayName: request.applicantName, role: 'MEMBER', eventTeamAssignmentCount: 0, ownsActiveEvents: false })
      }
    },
    async rejectRequest(_organizationId, requestId) {
      pending = pending.filter((item) => item.id !== requestId)
      statuses[_organizationId] = 'REJECTED'
    },
    async listMembers() {
      return members
    },
    async changeRole(_organizationId, userId, role: OrganizationRole) {
      const member = members.find((item) => item.userId === userId)
      if (member) {
        member.role = role
      }
    },
    async removeMember(_organizationId, userId) {
      const index = members.findIndex((item) => item.userId === userId)
      if (index >= 0) members.splice(index, 1)
    },
    async leaveOrganization() {
      const currentUser = currentUserId()
      const index = members.findIndex((item) => item.userId === currentUser)
      if (index >= 0) members.splice(index, 1)
    },
  }
  return { service, members, pending }
}

/** Renders the page for the logged-in user, as AuthProvider restores it from the stored session. */
function renderPage(service: OrganizationMembershipService, userId: string = ids.student) {
  localStorage.setItem(SESSION_KEY, JSON.stringify({ userId, email: 'u@example.invalid', username: 'u', displayName: 'U', status: 'ACTIVE' }))
  const router = createMemoryRouter([
    { path: '/organizations', element: <AuthProvider><OrganizationMembershipPage service={service} /></AuthProvider> },
  ], { initialEntries: ['/organizations'] })
  return render(<RouterProvider router={router} />)
}

async function openDiscover() {
  await userEvent.setup().click(await screen.findByRole('tab', { name: /Discover/ }))
}

function organizationCard(name: string): HTMLLIElement {
  const card = screen.getByRole('heading', { name }).closest('li')
  if (!card) throw new Error(`Organization card not found for ${name}.`)
  return card
}

beforeEach(() => {
  window.localStorage.clear()
})

describe('OrganizationMembershipPage', () => {
  it('US-06 lists organizations and displays request and member states', async () => {
    const { service } = createService()
    renderPage(service)

    expect(await screen.findByText('Member · member')).toBeInTheDocument()
    expect(within(organizationCard('Language Exchange')).getByText('Member · member')).toBeInTheDocument()
    await openDiscover()
    expect(await screen.findByText('Pending')).toBeInTheDocument()
    expect(within(organizationCard('Robotics Club')).getByRole('button', { name: 'Withdraw request' })).toBeInTheDocument()
    expect(within(organizationCard('Photography Group')).getByText('Rejected')).toBeInTheDocument()
    expect(within(organizationCard('Photography Group')).getByRole('button', { name: 'Request to join again' })).toBeInTheDocument()
  })

  it('US-06 submits and withdraws a request using the API service', async () => {
    const user = userEvent.setup()
    const { service } = createService()
    renderPage(service)
    await openDiscover()
    await screen.findByRole('heading', { name: 'AI Society' })
    const aiSociety = within(organizationCard('AI Society'))

    await user.click(aiSociety.getByRole('button', { name: 'Request to join' }))
    expect(await aiSociety.findByText('Pending')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Your request to join AI Society is pending.')

    await user.click(aiSociety.getByRole('button', { name: 'Withdraw request' }))
    expect(await aiSociety.findByText('No request')).toBeInTheDocument()
  })

  it('US-07 lets organization admins accept and reject pending requests', async () => {
    const user = userEvent.setup()
    const { service } = createService()
    renderPage(service, ids.admin)

    const pendingRequests = await screen.findByRole('region', { name: 'AI Society pending requests' })
    expect(within(pendingRequests).getByText('Demo Applicant')).toBeInTheDocument()
    await user.click(within(pendingRequests).getByRole('button', { name: 'Accept' }))
    expect(await screen.findByText('Join request approved.')).toBeInTheDocument()
    expect(within(pendingRequests).getByText('No pending requests.')).toBeInTheDocument()
  })

  it('US-07 allows an applicant to request again after rejection', async () => {
    const user = userEvent.setup()
    const { service } = createService()
    renderPage(service, ids.admin)

    const pendingRequests = await screen.findByRole('region', { name: 'AI Society pending requests' })
    await user.click(within(pendingRequests).getByRole('button', { name: 'Reject' }))
    expect(await screen.findByRole('dialog', { name: 'Reject request?' })).toHaveTextContent("Reject Demo Applicant's request to join AI Society?")
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Reject' }))
    expect(await screen.findByText('Join request rejected.')).toBeInTheDocument()

    cleanup()
    renderPage(service, ids.student)
    await openDiscover()
    const photography = await screen.findByRole('heading', { name: 'Photography Group' })
    expect(within(organizationCard('AI Society')).getByRole('button', { name: 'Request to join again' })).toBeInTheDocument()
    await user.click(within(organizationCard('AI Society')).getByRole('button', { name: 'Request to join again' }))
    expect(await screen.findByText('Your request to join AI Society is pending.')).toBeInTheDocument()
    expect(photography).toBeInTheDocument()
  })

  it('US-08 lets an organization admin change a member role', async () => {
    const user = userEvent.setup()
    const { service, members } = createService()
    renderPage(service, ids.admin)

    const roleSelector = await screen.findByRole('combobox', { name: 'Role for Demo Member' })
    await user.selectOptions(roleSelector, 'ORGANIZER')
    const dialog = screen.getByRole('dialog', { name: 'Change role?' })
    expect(dialog).toHaveTextContent('Change Demo Member from Member to Organizer?')
    await user.click(within(dialog).getByRole('button', { name: 'Change role' }))
    expect(await screen.findByText('Member role updated.')).toBeInTheDocument()
    expect(members.find((member) => member.userId === ids.member)?.role).toBe('ORGANIZER')
  })

  it('US-09 lets an organization admin remove a member', async () => {
    const user = userEvent.setup()
    const { service, members } = createService()
    renderPage(service, ids.admin)

    const memberText = await screen.findByText('Demo Member')
    const memberRow = memberText.closest('li')
    if (!memberRow) throw new Error('Target member row not found.')
    await user.click(within(memberRow).getByRole('button', { name: 'Remove' }))
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Remove member' }))
    expect(await screen.findByText('Member removed from the organization.')).toBeInTheDocument()
    expect(members.some((member) => member.userId === ids.member)).toBe(false)
  })

  it('US-10 lets a member leave without being an organization admin', async () => {
    const user = userEvent.setup()
    const { service, members } = createService()
    renderPage(service, ids.member)

    await screen.findByRole('heading', { name: 'Language Exchange' })
    const languageExchange = within(organizationCard('Language Exchange'))
    await user.click(languageExchange.getByRole('button', { name: 'Leave organization' }))
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Leave organization' }))
    expect(await screen.findByText('You left Language Exchange.')).toBeInTheDocument()
    expect(members.some((member) => member.userId === ids.member)).toBe(false)
  })

  it('US-07 and US-08 show display names instead of user ids', async () => {
    const { service } = createService()
    renderPage(service, ids.admin)

    const pendingRequests = await screen.findByRole('region', { name: 'AI Society pending requests' })
    expect(within(pendingRequests).getByText('Demo Applicant')).toBeInTheDocument()
    const memberList = screen.getByRole('region', { name: 'AI Society members' })
    expect(within(memberList).getByText('Demo Organizer')).toBeInTheDocument()
    expect(within(memberList).queryByText(ids.organizer)).not.toBeInTheDocument()
  })

  it('US-08 cancelling the role confirmation keeps the current role', async () => {
    const user = userEvent.setup()
    const { service, members } = createService()
    renderPage(service, ids.admin)

    const roleSelector = await screen.findByRole('combobox', { name: 'Role for Demo Member' })
    await user.selectOptions(roleSelector, 'ORG_ADMIN')
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(roleSelector).toHaveValue('MEMBER')
    expect(members.find((member) => member.userId === ids.member)?.role).toBe('MEMBER')
  })

  it('US-06 separates my organizations from the ones to discover', async () => {
    const { service } = createService()
    renderPage(service)

    const mine = await screen.findByRole('tabpanel', { name: /My organizations/ })
    expect(within(mine).getByRole('heading', { name: 'Language Exchange' })).toBeInTheDocument()
    expect(within(mine).queryByRole('heading', { name: 'Robotics Club' })).not.toBeInTheDocument()
    await openDiscover()
    const discover = screen.getByRole('tabpanel', { name: /Discover/ })
    expect(within(discover).getByRole('heading', { name: 'Robotics Club' })).toBeInTheDocument()
    expect(within(discover).queryByRole('heading', { name: 'Language Exchange' })).not.toBeInTheDocument()
  })

  it('US-10 staying in the leave confirmation keeps the membership', async () => {
    const user = userEvent.setup()
    const { service, members } = createService()
    renderPage(service, ids.member)

    await screen.findByRole('heading', { name: 'Language Exchange' })
    await user.click(within(organizationCard('Language Exchange')).getByRole('button', { name: 'Leave organization' }))
    await user.click(within(screen.getByRole('dialog', { name: 'Leave organization?' })).getByRole('button', { name: 'Stay' }))
    expect(members.some((member) => member.userId === ids.member)).toBe(true)
  })
})
