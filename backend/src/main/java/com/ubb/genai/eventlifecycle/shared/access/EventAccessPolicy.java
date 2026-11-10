package com.ubb.genai.eventlifecycle.shared.access;

import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.port.EventRole;
import com.ubb.genai.eventlifecycle.shared.port.EventTeamDirectory;
import com.ubb.genai.eventlifecycle.shared.port.OrganizationDirectory;
import com.ubb.genai.eventlifecycle.shared.port.PrivateActivityInvitations;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Visibility and authorization rules for events and activities (BR-03, BR-18). */
@Component
public class EventAccessPolicy {

    private final OrganizationDirectory organizations;
    private final EventTeamDirectory team;
    private final PrivateActivityInvitations invitations;
    private final LifecycleEventRepository events;

    public EventAccessPolicy(
            OrganizationDirectory organizations,
            EventTeamDirectory team,
            PrivateActivityInvitations invitations,
            LifecycleEventRepository events) {
        this.organizations = organizations;
        this.team = team;
        this.invitations = invitations;
        this.events = events;
    }

    public boolean hasOwnerRights(LifecycleEvent event, UUID userId) {
        return team.roleOf(event.getId(), userId).filter(r -> r == EventRole.OWNER).isPresent()
                || organizations.isOrgAdmin(event.getOrganizationId(), userId);
    }

    public boolean isTeamMember(LifecycleEvent event, UUID userId) {
        return team.roleOf(event.getId(), userId).isPresent()
                || organizations.isOrgAdmin(event.getOrganizationId(), userId);
    }

    public boolean canSeeEvent(LifecycleEvent event, Optional<UUID> userId) {
        return event.getState() != EventState.DRAFT
                || userId.map(id -> isTeamMember(event, id)).orElse(false);
    }

    /**
     * BR-18: an event whose activities are all private is hidden from everyone except the team and those who can
     * see at least one activity. Reads the activities, so the event must be loaded with them.
     */
    public boolean canSeeEventDetails(LifecycleEvent event, Optional<UUID> userId) {
        if (!canSeeEvent(event, userId)) {
            return false;
        }
        if (userId.map(id -> isTeamMember(event, id)).orElse(false)) {
            return true;
        }
        return event.getActivities().isEmpty()
                || event.getActivities().stream()
                        .anyMatch(a -> canSeeActivity(a, event.getOrganizationId(), userId));
    }

    public boolean canSeeActivity(LifecycleActivity activity, UUID organizationId, Optional<UUID> userId) {
        if (activity.getVisibility() == ActivityVisibility.PUBLIC) {
            return true;
        }
        return userId.map(id -> organizations.isMember(organizationId, id)
                        || invitations.isInvited(activity.getId(), id))
                .orElse(false);
    }

    public UUID requireUser(Optional<UUID> userId) {
        return userId.orElseThrow(LifecycleProblem::unauthenticated);
    }

    public LifecycleEvent requireVisibleEvent(UUID eventId, Optional<UUID> userId) {
        return events.findWithActivitiesById(eventId)
                .filter(e -> canSeeEventDetails(e, userId))
                .orElseThrow(() -> LifecycleProblem.notFound("EVENT_NOT_FOUND"));
    }

    public void requireOwnerRights(LifecycleEvent event, UUID userId) {
        if (!hasOwnerRights(event, userId)) {
            throw LifecycleProblem.forbidden("OWNER_RIGHTS_REQUIRED");
        }
    }

    public void requireTeamMember(LifecycleEvent event, UUID userId) {
        if (!isTeamMember(event, userId)) {
            throw LifecycleProblem.forbidden("TEAM_MEMBER_REQUIRED");
        }
    }
}
