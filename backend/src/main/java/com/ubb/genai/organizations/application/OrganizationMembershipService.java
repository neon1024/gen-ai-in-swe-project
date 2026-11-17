package com.ubb.genai.organizations.application;

import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import com.ubb.genai.organizations.api.JoinRequestView;
import com.ubb.genai.organizations.api.MemberView;
import com.ubb.genai.organizations.api.OrganizationCard;
import com.ubb.genai.organizations.api.OrganizationProblem;
import com.ubb.genai.organizations.domain.JoinRequestStatus;
import com.ubb.genai.organizations.domain.OrganizationRole;
import com.ubb.genai.organizations.persistence.ActiveEventOwnerRepository;
import com.ubb.genai.organizations.persistence.EventTeamMembershipRepository;
import com.ubb.genai.organizations.persistence.JoinRequestEntity;
import com.ubb.genai.organizations.persistence.JoinRequestRepository;
import com.ubb.genai.organizations.persistence.OrganizationEntity;
import com.ubb.genai.organizations.persistence.OrganizationMembershipEntity;
import com.ubb.genai.organizations.persistence.OrganizationMembershipRepository;
import com.ubb.genai.organizations.persistence.OrganizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationMembershipService {

    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;
    private final JoinRequestRepository requests;
    private final EventTeamMembershipRepository eventTeamMemberships;
    private final ActiveEventOwnerRepository activeEventOwners;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public OrganizationMembershipService(
            OrganizationRepository organizations,
            OrganizationMembershipRepository memberships,
            JoinRequestRepository requests,
            EventTeamMembershipRepository eventTeamMemberships,
            ActiveEventOwnerRepository activeEventOwners,
            CurrentUserProvider currentUser,
            Clock clock) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.requests = requests;
        this.eventTeamMemberships = eventTeamMemberships;
        this.activeEventOwners = activeEventOwners;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<OrganizationCard> browseOrganizations() {
        UUID actor = actor();
        return organizations.findAllByActiveTrueOrderByNameAsc().stream()
                .map(organization -> card(organization, actor))
                .toList();
    }

    @Transactional
    public JoinRequestView requestToJoin(UUID organizationId) {
        OrganizationEntity organization = lockOrganization(organizationId);
        UUID actor = actor();
        if (memberships.findByOrganizationIdAndUserId(organizationId, actor).isPresent()) {
            throw conflict("ALREADY_MEMBER", "You are already a member of this organization.");
        }
        if (requests.findByOrganizationIdAndApplicantIdAndStatus(organizationId, actor, JoinRequestStatus.PENDING)
                .isPresent()) {
            throw conflict("REQUEST_PENDING", "You already have a pending request for this organization.");
        }
        Instant now = clock.instant();
        JoinRequestEntity request = requests.save(new JoinRequestEntity(organization.id(), actor, now));
        return new JoinRequestView(request.id(), request.applicantId(), request.createdAt());
    }

    @Transactional
    public void withdrawRequest(UUID organizationId) {
        lockOrganization(organizationId);
        UUID actor = actor();
        JoinRequestEntity request = requests.findByOrganizationIdAndApplicantIdAndStatus(
                        organizationId, actor, JoinRequestStatus.PENDING)
                .orElseThrow(() -> conflict("NO_PENDING_REQUEST", "There is no pending request to withdraw."));
        request.decide(JoinRequestStatus.WITHDRAWN, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<JoinRequestView> listPendingRequests(UUID organizationId) {
        requireAdminForRead(organizationId);
        return requests.findAllByOrganizationIdAndStatusOrderByCreatedAtAscIdAsc(
                        organizationId, JoinRequestStatus.PENDING)
                .stream()
                .map(request -> new JoinRequestView(request.id(), request.applicantId(), request.createdAt()))
                .toList();
    }

    @Transactional
    public void approveRequest(UUID organizationId, UUID requestId) {
        lockOrganization(organizationId);
        requireAdminMembership(organizationId);
        JoinRequestEntity request = pendingRequestForUpdate(organizationId, requestId);
        if (memberships.findByOrganizationIdAndUserId(organizationId, request.applicantId()).isPresent()) {
            throw conflict("ALREADY_MEMBER", "The applicant is already a member of this organization.");
        }
        memberships.save(new OrganizationMembershipEntity(
                organizationId, request.applicantId(), OrganizationRole.MEMBER, clock.instant()));
        request.decide(JoinRequestStatus.APPROVED, clock.instant());
    }

    @Transactional
    public void rejectRequest(UUID organizationId, UUID requestId) {
        lockOrganization(organizationId);
        requireAdminMembership(organizationId);
        pendingRequestForUpdate(organizationId, requestId).decide(JoinRequestStatus.REJECTED, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<MemberView> listMembers(UUID organizationId) {
        requireAdminForRead(organizationId);
        return memberships.findAllByOrganizationIdOrderByCreatedAtAsc(organizationId).stream()
                .map(member -> memberView(organizationId, member))
                .toList();
    }

    @Transactional
    public MemberView changeRole(UUID organizationId, UUID userId, OrganizationRole newRole) {
        lockOrganization(organizationId);
        requireAdminMembership(organizationId);
        OrganizationMembershipEntity member = membership(organizationId, userId);
        if (member.role() == OrganizationRole.ORG_ADMIN
                && newRole != OrganizationRole.ORG_ADMIN
                && memberships.countByOrganizationIdAndRole(organizationId, OrganizationRole.ORG_ADMIN) <= 1) {
            throw conflict("LAST_ORG_ADMIN", "The last organization administrator cannot be demoted.");
        }
        member.changeRole(newRole);
        return memberView(organizationId, member);
    }

    @Transactional
    public void removeMember(UUID organizationId, UUID userId) {
        lockOrganization(organizationId);
        requireAdminMembership(organizationId);
        removeMembership(organizationId, userId);
    }

    @Transactional
    public void leaveOrganization(UUID organizationId) {
        lockOrganization(organizationId);
        UUID actor = actor();
        removeMembership(organizationId, actor);
    }

    private void removeMembership(UUID organizationId, UUID userId) {
        OrganizationMembershipEntity member = membership(organizationId, userId);
        if (member.role() == OrganizationRole.ORG_ADMIN
                && memberships.countByOrganizationIdAndRole(organizationId, OrganizationRole.ORG_ADMIN) <= 1) {
            throw conflict("LAST_ORG_ADMIN", "The last organization administrator cannot leave or be removed.");
        }
        if (activeEventOwners.hasActiveOwnedEvents(organizationId, userId)) {
            throw conflict("ACTIVE_EVENT_OWNER", "A member who owns a draft or published event cannot leave or be removed.");
        }
        eventTeamMemberships.deleteByOrganizationIdAndUserId(organizationId, userId);
        memberships.delete(member);
    }

    private OrganizationCard card(OrganizationEntity organization, UUID actor) {
        OrganizationMembershipEntity membership = memberships
                .findByOrganizationIdAndUserId(organization.id(), actor)
                .orElse(null);
        JoinRequestStatus status = membership != null
                ? JoinRequestStatus.APPROVED
                : requests.findFirstByOrganizationIdAndApplicantIdOrderByCreatedAtDescIdDesc(organization.id(), actor)
                        .map(JoinRequestEntity::status)
                        .filter(value -> value == JoinRequestStatus.PENDING || value == JoinRequestStatus.REJECTED)
                        .orElse(null);
        return new OrganizationCard(
                organization.id(),
                organization.name(),
                organization.description(),
                membership != null,
                membership == null ? null : membership.role(),
                status);
    }

    private MemberView memberView(UUID organizationId, OrganizationMembershipEntity member) {
        return new MemberView(
                member.userId(),
                member.role(),
                eventTeamMemberships.countByOrganizationIdAndUserId(organizationId, member.userId()),
                activeEventOwners.hasActiveOwnedEvents(organizationId, member.userId()));
    }

    private OrganizationEntity lockOrganization(UUID organizationId) {
        return organizations.findActiveForUpdate(organizationId)
                .orElseThrow(() -> notFound("ORGANIZATION_NOT_FOUND", "Organization not found."));
    }

    private void requireAdminForRead(UUID organizationId) {
        organizations.findByIdAndActiveTrue(organizationId)
                .orElseThrow(() -> notFound("ORGANIZATION_NOT_FOUND", "Organization not found."));
        requireAdminMembership(organizationId);
    }

    private void requireAdminMembership(UUID organizationId) {
        OrganizationMembershipEntity member = memberships
                .findByOrganizationIdAndUserId(organizationId, actor())
                .orElseThrow(() -> forbidden());
        if (member.role() != OrganizationRole.ORG_ADMIN) {
            throw forbidden();
        }
    }

    private OrganizationMembershipEntity membership(UUID organizationId, UUID userId) {
        return memberships.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> notFound("MEMBER_NOT_FOUND", "Organization member not found."));
    }

    private JoinRequestEntity pendingRequestForUpdate(UUID organizationId, UUID requestId) {
        JoinRequestEntity request = requests.findForUpdate(organizationId, requestId)
                .orElseThrow(() -> notFound("REQUEST_NOT_FOUND", "Join request not found."));
        if (request.status() != JoinRequestStatus.PENDING) {
            throw conflict("REQUEST_NOT_PENDING", "This join request is no longer pending.");
        }
        return request;
    }

    private UUID actor() {
        return currentUser.currentUserId()
                .orElseThrow(() -> new OrganizationProblem(401, "AUTHENTICATION_REQUIRED",
                        "A demo user identity is required."));
    }

    private static OrganizationProblem forbidden() {
        return new OrganizationProblem(403, "ORG_ADMIN_REQUIRED", "Organization administrator access is required.");
    }

    private static OrganizationProblem notFound(String code, String message) {
        return new OrganizationProblem(404, code, message);
    }

    private static OrganizationProblem conflict(String code, String message) {
        return new OrganizationProblem(409, code, message);
    }
}
