package com.ubb.genai.eventlifecycle.feedback;

import com.ubb.genai.eventlifecycle.shared.access.EventAccessPolicy;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleActivityRepository;
import com.ubb.genai.eventlifecycle.shared.port.SignupPort;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Lets attendees rate an activity once, within seven days after it ends (US-47, BR-16). */
@Service
public class FeedbackService {

    private final LifecycleActivityRepository activities;
    private final FeedbackRepository feedback;
    private final SignupPort signups;
    private final EventAccessPolicy access;
    private final Clock clock;

    public FeedbackService(
            LifecycleActivityRepository activities,
            FeedbackRepository feedback,
            SignupPort signups,
            EventAccessPolicy access,
            Clock clock) {
        this.activities = activities;
        this.feedback = feedback;
        this.signups = signups;
        this.access = access;
        this.clock = clock;
    }

    public FeedbackEligibilityView eligibility(UUID activityId, Optional<UUID> userId) {
        UUID user = access.requireUser(userId);
        Optional<FeedbackRefusal> refusal = refusal(findActivity(activityId), user);
        return new FeedbackEligibilityView(refusal.isEmpty(), refusal.map(Enum::name).orElse(null));
    }

    /**
     * Stores the feedback and returns its id. Deliberately not transactional: the insert is flushed in its own
     * transaction so a concurrent duplicate surfaces here as a unique-constraint violation and becomes a 409.
     */
    public UUID give(UUID activityId, Optional<UUID> userId, GiveFeedbackRequest request) {
        UUID user = access.requireUser(userId);
        refusal(findActivity(activityId), user).ifPresent(r -> {
            throw r == FeedbackRefusal.NOT_ATTENDED
                    ? LifecycleProblem.forbidden(r.name())
                    : LifecycleProblem.conflict(r.name(), "Feedback is not possible: " + r.name());
        });
        return persist(activityId, user, request);
    }

    UUID persist(UUID activityId, UUID user, GiveFeedbackRequest request) {
        String comment = request.comment() == null || request.comment().isBlank() ? null : request.comment().trim();
        try {
            return feedback
                    .saveAndFlush(new Feedback(activityId, user, request.rating(), comment, clock.instant()))
                    .getId();
        } catch (DataIntegrityViolationException e) {
            throw LifecycleProblem.conflict(
                    FeedbackRefusal.FEEDBACK_ALREADY_GIVEN.name(), "Feedback was already given for this activity");
        }
    }

    private LifecycleActivity findActivity(UUID activityId) {
        return activities.findById(activityId).orElseThrow(() -> LifecycleProblem.notFound("ACTIVITY_NOT_FOUND"));
    }

    private Optional<FeedbackRefusal> refusal(LifecycleActivity activity, UUID user) {
        return FeedbackEligibility.check(
                activity,
                clock.instant(),
                signups.wasMarkedPresent(activity.getId(), user),
                feedback.existsByActivityIdAndAuthorId(activity.getId(), user));
    }
}
