import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../authentication/context'
import {
  organizationMembershipApi,
  type JoinRequest,
  type OrganizationMember,
  type OrganizationMembership,
  type OrganizationMembershipService,
  type OrganizationRole,
} from './organizationMembershipService'

interface AdminData {
  requests: JoinRequest[]
  members: OrganizationMember[]
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'The request could not be completed.'
}

function isOrganizationRole(value: string): value is OrganizationRole {
  return value === 'MEMBER' || value === 'ORGANIZER' || value === 'ORG_ADMIN'
}

function statusLabel(organization: OrganizationMembership): string {
  if (organization.member) {
    return `Member · ${organization.role?.toLowerCase().replace('_', ' ') ?? 'member'}`
  }
  switch (organization.requestStatus) {
    case 'PENDING':
      return 'Pending'
    case 'APPROVED':
      return 'Approved'
    case 'REJECTED':
      return 'Rejected'
    default:
      return 'No request'
  }
}

export function OrganizationMembershipPage({
  service = organizationMembershipApi,
}: {
  service?: OrganizationMembershipService
}) {
  const userId = useAuth().user?.userId
  const [organizations, setOrganizations] = useState<OrganizationMembership[]>([])
  const [adminData, setAdminData] = useState<Record<string, AdminData>>({})
  const [error, setError] = useState<string | null>(null)
  const [announcement, setAnnouncement] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [busyAction, setBusyAction] = useState<string | null>(null)

  const loadData = useCallback(async () => {
    const listedOrganizations = await service.listOrganizations()
    const managed = await Promise.all(
      listedOrganizations
        .filter((organization) => organization.role === 'ORG_ADMIN')
        .map(async (organization) => {
          const [requests, members] = await Promise.all([
            service.listPendingRequests(organization.id),
            service.listMembers(organization.id),
          ])
          return [organization.id, { requests, members }] as const
        }),
    )
    return { organizations: listedOrganizations, adminData: Object.fromEntries(managed) }
  }, [service])

  useEffect(() => {
    let active = true
    void loadData()
      .then((data) => {
        if (!active) return
        setOrganizations(data.organizations)
        setAdminData(data.adminData)
        setError(null)
      })
      .catch((cause: unknown) => {
        if (!active) return
        setError(errorMessage(cause))
        setOrganizations([])
        setAdminData({})
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [loadData, userId])

  async function runAction(key: string, success: string, action: () => Promise<void>) {
    setBusyAction(key)
    setError(null)
    setAnnouncement(null)
    try {
      await action()
      const data = await loadData()
      setOrganizations(data.organizations)
      setAdminData(data.adminData)
      setAnnouncement(success)
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setBusyAction(null)
    }
  }

  function changeJoinRequest(organization: OrganizationMembership) {
    if (organization.requestStatus === 'PENDING') {
      void runAction(
        `withdraw-${organization.id}`,
        `Your request to ${organization.name} was withdrawn.`,
        () => service.withdrawRequest(organization.id),
      )
    } else {
      void runAction(
        `request-${organization.id}`,
        `Your request to join ${organization.name} is pending.`,
        () => service.requestToJoin(organization.id),
      )
    }
  }

  function changeMemberRole(organization: OrganizationMembership, member: OrganizationMember, role: OrganizationRole) {
    void runAction(
      `role-${organization.id}-${member.userId}`,
      'Member role updated.',
      () => service.changeRole(organization.id, member.userId, role),
    )
  }

  return (
    <main className="mx-auto max-w-5xl p-4">
      <nav className="mb-5 text-sm">
        <Link className="text-blue-700 underline" to="/events">
          Events
        </Link>
      </nav>
      <header className="mb-6">
        <p className="text-sm font-medium uppercase tracking-wide text-gray-600">Organization membership</p>
        <h1 className="mt-1 text-2xl font-semibold">Organizations</h1>
        <p className="mt-2 text-gray-700">
          Request membership, manage requests, and maintain organization roles.
        </p>
      </header>

      {error && (
        <p role="alert" className="mb-4 rounded border border-red-300 bg-red-50 p-3 text-red-800">
          {error}
        </p>
      )}
      {announcement && (
        <p role="status" className="mb-4 rounded border border-green-300 bg-green-50 p-3 text-green-900">
          {announcement}
        </p>
      )}
      {loading && <p role="status" className="mb-4 text-gray-700">Loading organizations…</p>}

      <ul className="space-y-4">
        {organizations.map((organization) => {
          const admin = adminData[organization.id]
          return (
            <li key={organization.id} className="rounded border border-gray-200 p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <h2 className="text-lg font-medium">{organization.name}</h2>
                  <p className="mt-1 text-sm text-gray-700">{organization.description}</p>
                  <p className="mt-2 text-sm" aria-label={`${organization.name} membership status`}>
                    Status: <span className="font-medium">{statusLabel(organization)}</span>
                  </p>
                </div>
                {organization.member ? (
                  <button
                    type="button"
                    disabled={loading || busyAction !== null}
                    onClick={() => void runAction(
                      `leave-${organization.id}`,
                      `You left ${organization.name}.`,
                      () => service.leaveOrganization(organization.id),
                    )}
                    className="rounded border border-gray-400 px-3 py-2 text-gray-800 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    Leave organization
                  </button>
                ) : (
                  <button
                    type="button"
                    disabled={loading || busyAction !== null}
                    onClick={() => changeJoinRequest(organization)}
                    className="rounded bg-blue-700 px-3 py-2 text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {organization.requestStatus === 'PENDING'
                      ? 'Withdraw request'
                      : organization.requestStatus === 'REJECTED'
                        ? 'Request to join again'
                        : 'Request to join'}
                  </button>
                )}
              </div>

              {admin && (
                <div className="mt-5 grid gap-6 border-t border-gray-200 pt-4 md:grid-cols-2">
                  <section aria-label={`${organization.name} pending requests`}>
                    <h3 className="font-semibold">Pending join requests</h3>
                    {admin.requests.length === 0 ? (
                      <p className="mt-2 text-sm text-gray-600">No pending requests.</p>
                    ) : (
                      <ul className="mt-2 space-y-2">
                        {admin.requests.map((request) => (
                          <li key={request.id} className="flex flex-wrap items-center justify-between gap-2 rounded bg-gray-50 p-3">
                            <span className="break-all text-sm">Applicant: {request.applicantId}</span>
                            <div className="flex gap-2">
                              <button
                                type="button"
                                disabled={busyAction !== null}
                                onClick={() => void runAction(
                                  `approve-${request.id}`,
                                  'Join request approved.',
                                  () => service.approveRequest(organization.id, request.id),
                                )}
                                className="rounded bg-green-700 px-3 py-1.5 text-sm text-white disabled:opacity-60"
                              >
                                Accept
                              </button>
                              <button
                                type="button"
                                disabled={busyAction !== null}
                                onClick={() => void runAction(
                                  `reject-${request.id}`,
                                  'Join request rejected.',
                                  () => service.rejectRequest(organization.id, request.id),
                                )}
                                className="rounded border border-gray-400 px-3 py-1.5 text-sm disabled:opacity-60"
                              >
                                Reject
                              </button>
                            </div>
                          </li>
                        ))}
                      </ul>
                    )}
                  </section>

                  <section aria-label={`${organization.name} members`}>
                    <h3 className="font-semibold">Members</h3>
                    <ul className="mt-2 space-y-2">
                      {admin.members.map((member) => (
                        <li key={member.userId} className="rounded bg-gray-50 p-3">
                          <div className="flex flex-wrap items-center justify-between gap-2">
                            <span className="break-all text-sm">{member.userId}</span>
                            <div className="flex flex-wrap items-center gap-2">
                              <label className="sr-only" htmlFor={`role-${organization.id}-${member.userId}`}>
                                Role for {member.userId}
                              </label>
                              <select
                                id={`role-${organization.id}-${member.userId}`}
                                aria-label={`Role for ${member.userId}`}
                                className="rounded border border-gray-300 bg-white px-2 py-1 text-sm"
                                value={member.role}
                                disabled={busyAction !== null}
                                onChange={(event) => {
                                  if (isOrganizationRole(event.target.value)) {
                                    changeMemberRole(organization, member, event.target.value)
                                  }
                                }}
                              >
                                <option value="MEMBER">Member</option>
                                <option value="ORGANIZER">Organizer</option>
                                <option value="ORG_ADMIN">Org Admin</option>
                              </select>
                              {member.userId !== userId && (
                                <button
                                  type="button"
                                  disabled={busyAction !== null}
                                  onClick={() => void runAction(
                                    `remove-${member.userId}`,
                                    'Member removed from the organization.',
                                    () => service.removeMember(organization.id, member.userId),
                                  )}
                                  className="rounded border border-red-400 px-2 py-1 text-sm text-red-800 disabled:opacity-60"
                                >
                                  Remove
                                </button>
                              )}
                            </div>
                          </div>
                          <p className="mt-2 text-xs text-gray-600">
                            Event-team assignments: {member.eventTeamAssignmentCount}
                            {member.ownsActiveEvents ? ' · Owns a draft or published event' : ''}
                          </p>
                        </li>
                      ))}
                    </ul>
                  </section>
                </div>
              )}
            </li>
          )
        })}
      </ul>
    </main>
  )
}
