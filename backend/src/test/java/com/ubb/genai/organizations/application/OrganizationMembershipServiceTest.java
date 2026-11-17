package com.ubb.genai.organizations.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
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
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationMembershipServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000007");
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final OrganizationMembershipRepository memberships = mock(OrganizationMembershipRepository.class);
    private final JoinRequestRepository requests = mock(JoinRequestRepository.class);
    private final EventTeamMembershipRepository eventTeamMemberships = mock(EventTeamMembershipRepository.class);
    private final ActiveEventOwnerRepository activeEventOwners = mock(ActiveEventOwnerRepository.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final OrganizationEntity organization = new OrganizationEntity(
            ORGANIZATION_ID, "AI Society", "AI projects.", "ai-society@example.invalid");
    private OrganizationMembershipService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationMembershipService(
                organizations,
                memberships,
                requests,
                eventTeamMemberships,
                activeEventOwners,
                currentUser,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(organizations.findActiveForUpdate(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
    }

    @Test
    void US06_requestCreatesPendingRequestAndRejectsDuplicatePendingRequest() {
        UUID applicantId = UUID.randomUUID();
        when(currentUser.currentUserId()).thenReturn(Optional.of(applicantId));
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, applicantId)).thenReturn(Optional.empty());
        when(requests.findByOrganizationIdAndApplicantIdAndStatus(
                ORGANIZATION_ID, applicantId, JoinRequestStatus.PENDING)).thenReturn(Optional.empty());
        when(requests.save(any(JoinRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.requestToJoin(ORGANIZATION_ID);
        assertEquals(applicantId, created.applicantId());
        verify(requests).save(any(JoinRequestEntity.class));

        when(requests.findByOrganizationIdAndApplicantIdAndStatus(
                ORGANIZATION_ID, applicantId, JoinRequestStatus.PENDING))
                .thenReturn(Optional.of(new JoinRequestEntity(ORGANIZATION_ID, applicantId, NOW)));
        OrganizationProblem duplicate = assertThrows(
                OrganizationProblem.class, () -> service.requestToJoin(ORGANIZATION_ID));
        assertEquals("REQUEST_PENDING", duplicate.code());
    }

    @Test
    void US06_doesNotShowAnOldApprovedRequestAsCurrentMembershipAfterRemoval() {
        when(currentUser.currentUserId()).thenReturn(Optional.of(ADMIN_ID));
        when(organizations.findAllByActiveTrueOrderByNameAsc()).thenReturn(java.util.List.of(organization));
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, ADMIN_ID)).thenReturn(Optional.empty());
        JoinRequestEntity oldApproval = new JoinRequestEntity(ORGANIZATION_ID, ADMIN_ID, NOW);
        oldApproval.decide(JoinRequestStatus.APPROVED, NOW);
        when(requests.findFirstByOrganizationIdAndApplicantIdOrderByCreatedAtDescIdDesc(ORGANIZATION_ID, ADMIN_ID))
                .thenReturn(Optional.of(oldApproval));

        var card = service.browseOrganizations().getFirst();

        assertFalse(card.member());
        assertNull(card.requestStatus());
    }

    @Test
    void US07_approvalCreatesMember() {
        UUID applicantId = UUID.randomUUID();
        JoinRequestEntity pending = new JoinRequestEntity(ORGANIZATION_ID, applicantId, NOW);
        when(currentUser.currentUserId()).thenReturn(Optional.of(ADMIN_ID));
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, ADMIN_ID))
                .thenReturn(Optional.of(new OrganizationMembershipEntity(
                        ORGANIZATION_ID, ADMIN_ID, OrganizationRole.ORG_ADMIN, NOW)));
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, applicantId)).thenReturn(Optional.empty());
        when(requests.findForUpdate(ORGANIZATION_ID, pending.id())).thenReturn(Optional.of(pending));
        when(memberships.save(any(OrganizationMembershipEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.approveRequest(ORGANIZATION_ID, pending.id());

        assertEquals(JoinRequestStatus.APPROVED, pending.status());
        verify(memberships).save(any(OrganizationMembershipEntity.class));
    }

    @Test
    void US07_rejectedRequestCanBeSubmittedAgain() {
        UUID applicantId = UUID.randomUUID();
        JoinRequestEntity pending = new JoinRequestEntity(ORGANIZATION_ID, applicantId, NOW);
        when(currentUser.currentUserId()).thenReturn(Optional.of(ADMIN_ID));
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, ADMIN_ID))
                .thenReturn(Optional.of(new OrganizationMembershipEntity(
                        ORGANIZATION_ID, ADMIN_ID, OrganizationRole.ORG_ADMIN, NOW)));
        when(requests.findForUpdate(ORGANIZATION_ID, pending.id())).thenReturn(Optional.of(pending));

        service.rejectRequest(ORGANIZATION_ID, pending.id());
        assertEquals(JoinRequestStatus.REJECTED, pending.status());

        when(currentUser.currentUserId()).thenReturn(Optional.of(applicantId));
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, applicantId)).thenReturn(Optional.empty());
        when(requests.findByOrganizationIdAndApplicantIdAndStatus(
                ORGANIZATION_ID, applicantId, JoinRequestStatus.PENDING)).thenReturn(Optional.empty());
        when(requests.save(any(JoinRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var newRequest = service.requestToJoin(ORGANIZATION_ID);
        assertEquals(applicantId, newRequest.applicantId());
        verify(requests).save(any(JoinRequestEntity.class));
    }

    @Test
    void US08_cannotDemoteTheLastOrganizationAdmin() {
        when(currentUser.currentUserId()).thenReturn(Optional.of(ADMIN_ID));
        OrganizationMembershipEntity admin = new OrganizationMembershipEntity(
                ORGANIZATION_ID, ADMIN_ID, OrganizationRole.ORG_ADMIN, NOW);
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, ADMIN_ID)).thenReturn(Optional.of(admin));
        when(memberships.countByOrganizationIdAndRole(ORGANIZATION_ID, OrganizationRole.ORG_ADMIN)).thenReturn(1L);

        OrganizationProblem problem = assertThrows(
                OrganizationProblem.class,
                () -> service.changeRole(ORGANIZATION_ID, ADMIN_ID, OrganizationRole.MEMBER));

        assertEquals("LAST_ORG_ADMIN", problem.code());
        assertEquals(OrganizationRole.ORG_ADMIN, admin.role());
    }

    @Test
    void US09_removalCleansEventTeamRolesAndBlocksActiveEventOwners() {
        when(currentUser.currentUserId()).thenReturn(Optional.of(ADMIN_ID));
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, ADMIN_ID))
                .thenReturn(Optional.of(new OrganizationMembershipEntity(
                        ORGANIZATION_ID, ADMIN_ID, OrganizationRole.ORG_ADMIN, NOW)));
        OrganizationMembershipEntity member = new OrganizationMembershipEntity(
                ORGANIZATION_ID, MEMBER_ID, OrganizationRole.ORGANIZER, NOW);
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, MEMBER_ID)).thenReturn(Optional.of(member));
        when(activeEventOwners.hasActiveOwnedEvents(ORGANIZATION_ID, MEMBER_ID)).thenReturn(false);

        service.removeMember(ORGANIZATION_ID, MEMBER_ID);

        verify(eventTeamMemberships).deleteByOrganizationIdAndUserId(ORGANIZATION_ID, MEMBER_ID);
        verify(memberships).delete(member);

        when(activeEventOwners.hasActiveOwnedEvents(ORGANIZATION_ID, MEMBER_ID)).thenReturn(true);
        OrganizationProblem activeOwner = assertThrows(
                OrganizationProblem.class, () -> service.removeMember(ORGANIZATION_ID, MEMBER_ID));
        assertEquals("ACTIVE_EVENT_OWNER", activeOwner.code());
        verify(eventTeamMemberships, times(1)).deleteByOrganizationIdAndUserId(ORGANIZATION_ID, MEMBER_ID);
    }

    @Test
    void US10_memberCanLeaveButLastAdminCannot() {
        when(currentUser.currentUserId()).thenReturn(Optional.of(MEMBER_ID));
        OrganizationMembershipEntity member = new OrganizationMembershipEntity(
                ORGANIZATION_ID, MEMBER_ID, OrganizationRole.MEMBER, NOW);
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, MEMBER_ID)).thenReturn(Optional.of(member));
        when(activeEventOwners.hasActiveOwnedEvents(ORGANIZATION_ID, MEMBER_ID)).thenReturn(false);

        service.leaveOrganization(ORGANIZATION_ID);

        verify(eventTeamMemberships).deleteByOrganizationIdAndUserId(ORGANIZATION_ID, MEMBER_ID);
        verify(memberships).delete(member);

        when(currentUser.currentUserId()).thenReturn(Optional.of(ADMIN_ID));
        OrganizationMembershipEntity admin = new OrganizationMembershipEntity(
                ORGANIZATION_ID, ADMIN_ID, OrganizationRole.ORG_ADMIN, NOW);
        when(memberships.findByOrganizationIdAndUserId(ORGANIZATION_ID, ADMIN_ID)).thenReturn(Optional.of(admin));
        when(memberships.countByOrganizationIdAndRole(ORGANIZATION_ID, OrganizationRole.ORG_ADMIN)).thenReturn(1L);
        OrganizationProblem lastAdmin = assertThrows(
                OrganizationProblem.class, () -> service.leaveOrganization(ORGANIZATION_ID));
        assertEquals("LAST_ORG_ADMIN", lastAdmin.code());
    }
}
