package com.ubb.genai.eventlifecycle.shared.port;

import java.util.Set;
import java.util.UUID;

/** Signup, waitlist and attendance data. */
public interface SignupPort {

    /** Cancels every signup of the activity and returns the users who held a Confirmed or Waitlisted signup. */
    Set<UUID> cancelAllSignups(UUID activityId);

    SignupFigures figures(UUID activityId);

    boolean wasMarkedPresent(UUID activityId, UUID userId);
}
