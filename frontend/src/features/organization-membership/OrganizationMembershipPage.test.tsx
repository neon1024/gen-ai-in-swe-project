import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import {
  demoUsers,
  type JoinRequest,
  type OrganizationMember,
  type OrganizationMembership,
  type OrganizationMembershipService,
  type OrganizationRole,
} from './organizationMembershipService'
import { OrganizationMembershipPage } from './OrganizationMembershipPage'

const ids = {
  ai: '10000000-0000-4000-8000-000000000001',
  robotics: '10000000-0000-4000-8000-000000000002',
  language: '10000000-0000-4000-8000-000000000003',
  photo: '10000000-0000-4000-8000-000000000004',
  student: demoUsers[0].id,
  admin: demoUsers[1].id,
  organizer: demoUsers[6].id,
  member: demoUsers[7].id,
}

function createService() {
  const statuses: Record<string, OrganizationMembership['requestStatus']> = {
    [ids.ai]: null,
    [ids.robotics]: 'PENDING',
    [ids.language]: 'APPROVED',
    [ids.photo]: 'REJECTED',
  }
  const members: OrganizationMember[] = [
    { userId: ids.admin, role: 'ORG_ADMIN', eventTeamAssignmentCount: 0, ownsActiveEvents: false },
    { userId: demoUsers[5].id, role: 'ORG_ADMIN', eventTeamAssignmentCount: 1, ownsActiveEvents: false },
    { userId: ids.organizer, role: 'ORGANIZER', eventTeamAssignmentCount: 1, ownsActiveEvents: false },
    { userId: ids.member, role: 'MEMBER', eventTeamAssignmentCount: 0, ownsActiveEvents: false },
  ]
  let pending: JoinRequest[] = [{
    id: '12000000-0000-4000-8000-000000000001',
    applicantId: ids.student,
    createdAt: '2026-11-16T00:00:00Z',
  }]

  const service: OrganizationMembershipService = {
    async listOrganizations() {
      const currentUser = localStorage.getItem('devUserId')
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
        members.push({ userId: request.applicantId, role: 'MEMBER', eventTeamAssignmentCount: 0, ownsActiveEvents: false })
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
      const currentUser = localStorage.getItem('devUserId')
      const index = members.findIndex((item) => item.userId === currentUser)
      if (index >= 0) members.splice(index, 1)
    },
  }
  return { service, members, pending }
}

function renderPage(service: OrganizationMembershipService) {
  const router = createMemoryRouter([
    { path: '/organizations', element: <OrganizationMembershipPage service={service} /> },
  ], { initialEntries: ['/organizations'] })
  return render(<RouterProvider router={router} />)
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

    expect(await screen.findByText('Pending')).toBeInTheDocument()
    expect(within(organizationCard('Robotics Club')).getByRole('button', { name: 'Withdraw request' })).toBeInTheDocument()
    expect(within(organizationCard('Language Exchange')).getByText('Member · member')).toBeInTheDocument()
    expect(within(organizationCard('Photography Group')).getByText('Rejected')).toBeInTheDocument()
    expect(within(organizationCard('Photography Group')).getByRole('button', { name: 'Request to join again' })).toBeInTheDocument()
  })

  it('US-06 submits and withdraws a request using the API service', async () => {
    const user = userEvent.setup()
    const { service } = createService()
    renderPage(service)
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
    renderPage(service)
    await user.selectOptions(screen.getByLabelText('Act as'), ids.admin)

    const pendingRequests = await screen.findByRole('region', { name: 'AI Society pending requests' })
    expect(within(pendingRequests).getByText(`Applicant: ${ids.student}`)).toBeInTheDocument()
    await user.click(within(pendingRequests).getByRole('button', { name: 'Accept' }))
    expect(await screen.findByText('Join request approved.')).toBeInTheDocument()
    expect(within(pendingRequests).getByText('No pending requests.')).toBeInTheDocument()
  })

  it('US-07 allows an applicant to request again after rejection', async () => {
    const user = userEvent.setup()
    const { service } = createService()
    renderPage(service)
    await user.selectOptions(screen.getByLabelText('Act as'), ids.admin)

    const pendingRequests = await screen.findByRole('region', { name: 'AI Society pending requests' })
    await user.click(within(pendingRequests).getByRole('button', { name: 'Reject' }))
    expect(await screen.findByText('Join request rejected.')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Act as'), ids.student)
    const photography = await screen.findByRole('heading', { name: 'Photography Group' })
    expect(within(organizationCard('AI Society')).getByRole('button', { name: 'Request to join again' })).toBeInTheDocument()
    await user.click(within(organizationCard('AI Society')).getByRole('button', { name: 'Request to join again' }))
    expect(await screen.findByText('Your request to join AI Society is pending.')).toBeInTheDocument()
    expect(photography).toBeInTheDocument()
  })

  it('US-08 lets an organization admin change a member role', async () => {
    const user = userEvent.setup()
    const { service, members } = createService()
    renderPage(service)
    await user.selectOptions(screen.getByLabelText('Act as'), ids.admin)

    const roleSelector = await screen.findByRole('combobox', { name: `Role for ${ids.member}` })
    await user.selectOptions(roleSelector, 'ORGANIZER')
    expect(await screen.findByText('Member role updated.')).toBeInTheDocument()
    expect(members.find((member) => member.userId === ids.member)?.role).toBe('ORGANIZER')
  })

  it('US-09 lets an organization admin remove a member', async () => {
    const user = userEvent.setup()
    const { service, members } = createService()
    renderPage(service)
    await user.selectOptions(screen.getByLabelText('Act as'), ids.admin)

    const memberText = await screen.findByText(ids.member)
    const memberRow = memberText.closest('li')
    if (!memberRow) throw new Error('Target member row not found.')
    await user.click(within(memberRow).getByRole('button', { name: 'Remove' }))
    expect(await screen.findByText('Member removed from the organization.')).toBeInTheDocument()
    expect(members.some((member) => member.userId === ids.member)).toBe(false)
  })

  it('US-10 lets a member leave without being an organization admin', async () => {
    const user = userEvent.setup()
    const { service, members } = createService()
    renderPage(service)
    await user.selectOptions(screen.getByLabelText('Act as'), ids.member)

    const languageExchange = within(organizationCard('Language Exchange'))
    await user.click(languageExchange.getByRole('button', { name: 'Leave organization' }))
    expect(await screen.findByText('You left Language Exchange.')).toBeInTheDocument()
    expect(members.some((member) => member.userId === ids.member)).toBe(false)
  })
})
