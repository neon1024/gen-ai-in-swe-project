package com.ubb.genai.eventlifecycle.reports;

import com.ubb.genai.eventlifecycle.feedback.FeedbackRepository;
import com.ubb.genai.eventlifecycle.feedback.RatingAggregate;
import com.ubb.genai.eventlifecycle.shared.access.EventAccessPolicy;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.port.SignupPort;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the per-activity report of an event for its team (US-49). Allowed in every event state. */
@Service
public class EventReportService {

    private static final Comparator<LifecycleActivity> ORDER = Comparator
            .comparing(LifecycleActivity::getStartsAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(LifecycleActivity::getTitle);

    private final EventAccessPolicy access;
    private final SignupPort signups;
    private final FeedbackRepository feedback;

    public EventReportService(EventAccessPolicy access, SignupPort signups, FeedbackRepository feedback) {
        this.access = access;
        this.signups = signups;
        this.feedback = feedback;
    }

    @Transactional(readOnly = true)
    public EventReport report(UUID eventId, Optional<UUID> userId) {
        UUID user = access.requireUser(userId);
        LifecycleEvent event = access.requireVisibleEvent(eventId, userId);
        access.requireTeamMember(event, user);

        List<LifecycleActivity> activities = event.getActivities().stream().sorted(ORDER).toList();
        Map<UUID, RatingAggregate> ratings = activities.isEmpty()
                ? Map.of()
                : feedback.aggregateByActivityIds(activities.stream().map(LifecycleActivity::getId).toList()).stream()
                        .collect(Collectors.toMap(RatingAggregate::activityId, Function.identity()));
        List<ActivityReportRow> rows = activities.stream()
                .map(a -> {
                    RatingAggregate rating = ratings.get(a.getId());
                    return ReportCalculator.row(a, signups.figures(a.getId()),
                            rating == null ? 0 : rating.count(), rating == null ? null : rating.average());
                })
                .toList();
        return new EventReport(event.getId(), event.getTitle(), event.getState(), rows);
    }
}
