package com.ubb.genai.eventlifecycle.shared.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ubb.genai.eventlifecycle.EventLifecycleFixtures;
import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.port.EventTeamDirectory;
import com.ubb.genai.eventlifecycle.shared.port.EventRole;
import com.ubb.genai.eventlifecycle.shared.port.OrganizationDirectory;
import com.ubb.genai.eventlifecycle.shared.port.PrivateActivityInvitations;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventAccessPolicyTest {

    private final OrganizationDirectory organizations = mock(OrganizationDirectory.class);
    private final EventTeamDirectory team = mock(EventTeamDirectory.class);
    private final PrivateActivityInvitations invitations = mock(PrivateActivityInvitations.class);
    private final LifecycleEventRepository events = mock(LifecycleEventRepository.class);
    private final EventAccessPolicy policy = new EventAccessPolicy(organizations, team, invitations, events);

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final LifecycleEvent draft = EventLifecycleFixtures.draftEvent(orgId);

    @Test
    @DisplayName("BR-03 a draft event is hidden from users without a role and from guests")
    void br03_draftHiddenFromNonTeamUsersAndGuests() {
        when(team.roleOf(draft.getId(), userId)).thenReturn(Optional.empty());
        when(organizations.isOrgAdmin(orgId, userId)).thenReturn(false);

        assertThat(policy.canSeeEvent(draft, Optional.of(userId))).isFalse();
        assertThat(policy.canSeeEvent(draft, Optional.empty())).isFalse();
    }

    @Test
    @DisplayName("BR-03 a draft event is visible to team members and Org Admins")
    void br03_draftVisibleToTeamAndOrgAdmins() {
        UUID admin = UUID.randomUUID();
        when(team.roleOf(draft.getId(), userId)).thenReturn(Optional.of(EventRole.STAFF));
        when(team.roleOf(draft.getId(), admin)).thenReturn(Optional.empty());
        when(organizations.isOrgAdmin(orgId, admin)).thenReturn(true);

        assertThat(policy.canSeeEvent(draft, Optional.of(userId))).isTrue();
        assertThat(policy.canSeeEvent(draft, Optional.of(admin))).isTrue();
    }

    @Test
    @DisplayName("BR-18 a private activity is visible only to organization members and invitees")
    void br18_privateActivityVisibleOnlyToMembersAndInvitees() {
        Instant start = Instant.parse("2027-01-01T10:00:00Z");
        LifecycleActivity priv = LifecycleActivity.scheduled("Private", start, start.plusSeconds(3600), ActivityVisibility.PRIVATE);
        LifecycleActivity pub = LifecycleActivity.scheduled("Public", start, start.plusSeconds(3600), ActivityVisibility.PUBLIC);
        UUID stranger = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        when(organizations.isMember(orgId, member)).thenReturn(true);
        when(invitations.isInvited(priv.getId(), invitee)).thenReturn(true);

        assertThat(policy.canSeeActivity(priv, orgId, Optional.empty())).isFalse();
        assertThat(policy.canSeeActivity(priv, orgId, Optional.of(stranger))).isFalse();
        assertThat(policy.canSeeActivity(priv, orgId, Optional.of(member))).isTrue();
        assertThat(policy.canSeeActivity(priv, orgId, Optional.of(invitee))).isTrue();
        assertThat(policy.canSeeActivity(pub, orgId, Optional.empty())).isTrue();
    }

    @Test
    @DisplayName("US-20 owner rights belong to the event Owner and Org Admins only")
    void us20_ownerRightsForOwnerAndOrgAdminOnly() {
        UUID editor = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID admin = UUID.randomUUID();
        when(team.roleOf(draft.getId(), editor)).thenReturn(Optional.of(EventRole.EDITOR));
        when(team.roleOf(draft.getId(), staff)).thenReturn(Optional.of(EventRole.STAFF));
        when(team.roleOf(draft.getId(), owner)).thenReturn(Optional.of(EventRole.OWNER));
        when(team.roleOf(draft.getId(), admin)).thenReturn(Optional.empty());
        when(organizations.isOrgAdmin(orgId, admin)).thenReturn(true);

        assertThat(policy.hasOwnerRights(draft, editor)).isFalse();
        assertThat(policy.hasOwnerRights(draft, staff)).isFalse();
        assertThat(policy.hasOwnerRights(draft, owner)).isTrue();
        assertThat(policy.hasOwnerRights(draft, admin)).isTrue();
    }

    @Test
    @DisplayName("US-48 a team member includes Staff")
    void us48_teamMemberIncludesStaff() {
        UUID staff = UUID.randomUUID();
        when(team.roleOf(draft.getId(), staff)).thenReturn(Optional.of(EventRole.STAFF));
        when(team.roleOf(draft.getId(), userId)).thenReturn(Optional.empty());

        assertThat(policy.isTeamMember(draft, staff)).isTrue();
        assertThat(policy.isTeamMember(draft, userId)).isFalse();
    }
}
