package com.ubb.genai.events.authoring.application;

import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Revalidates the complete aggregate and returns every actionable publication problem. Cancelled
 * activities (cancellation owned by #23) are neither counted nor revalidated.
 */
public final class PublicationReadiness {
  private PublicationReadiness() {}

  public static List<AuthoringException.Violation> violations(AuthoringEvent event) {
    var problems = new ArrayList<AuthoringException.Violation>();
    text(problems, "title", event.title());
    text(problems, "description", event.description());
    check(problems, "category", event.category() != null, "Category is required");
    check(
        problems,
        "activities",
        event.activities().stream().anyMatch(activity -> !activity.cancelled()),
        "At least one valid non-cancelled activity is required");
    for (int index = 0; index < event.activities().size(); index++) {
      var activity = event.activities().get(index);
      if (activity.cancelled()) continue;
      String path = "activities[" + index + "]";
      text(problems, path + ".title", activity.title());
      text(problems, path + ".description", activity.description());
      check(
          problems,
          path + ".location",
          present(activity.location()) || present(activity.onlineLink()),
          "Location or online link is required");
      optionalText(problems, path + ".location", activity.location());
      optionalText(problems, path + ".onlineLink", activity.onlineLink());
      check(
          problems, path + ".capacity", activity.capacity() >= 1, "Capacity must be at least one");
      check(
          problems,
          path + ".schedule",
          activity.schedule() != null,
          "Activity schedule is required");
      check(
          problems,
          path + ".registration",
          activity.registration() != null,
          "Registration window is required");
      check(
          problems,
          path + ".visibility",
          activity.visibility() == com.ubb.genai.events.authoring.domain.ActivityVisibility.PUBLIC,
          "Only Public visibility is available");
      check(
          problems,
          path + ".createdAt",
          activity.createdAt() != null,
          "Creation timestamp is required");
      if (activity.schedule() != null && activity.registration() != null) {
        var closing = activity.registration().effectiveClose(activity.schedule());
        check(
            problems,
            path + ".registrationClose",
            !closing.isAfter(activity.schedule().start().toInstant()),
            "Registration must close by activity start");
        check(
            problems,
            path + ".registrationOpen",
            activity.registration().open() == null
                || activity.registration().open().toInstant().isBefore(closing),
            "Registration opening must precede closing");
      }
      for (int itemIndex = 0; itemIndex < activity.agenda().size(); itemIndex++) {
        var item = activity.agenda().get(itemIndex);
        String itemPath = path + ".agenda[" + itemIndex + "]";
        text(problems, itemPath + ".title", item.title());
        optionalText(problems, itemPath + ".speaker", item.speaker());
        check(
            problems,
            itemPath + ".time",
            activity.schedule() != null && activity.schedule().contains(item.time()),
            "Agenda time must be within activity interval");
      }
    }
    return List.copyOf(problems);
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }

  private static void text(List<AuthoringException.Violation> problems, String path, String value) {
    check(problems, path, present(value), "Value is required");
    optionalText(problems, path, value);
  }

  private static void optionalText(
      List<AuthoringException.Violation> problems, String path, String value) {
    check(
        problems,
        path,
        value == null || value.indexOf(0) < 0,
        "Value contains an unsupported character");
  }

  private static void check(
      List<AuthoringException.Violation> problems, String path, boolean valid, String message) {
    if (!valid) problems.add(new AuthoringException.Violation(path, "INVALID", message));
  }
}
