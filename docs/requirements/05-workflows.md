# Main Workflows

A workflow is an end-to-end process made of several use cases.

| ID | Workflow | Use cases | Milestone |
|---|---|---|---|
| WF-01 | Account and organization onboarding | UC-01 – UC-09, UC-31 | Mid-term |
| WF-02 | Event lifecycle | UC-10 – UC-14, UC-19 | Mid-term |
| WF-03 | Discovery and registration | UC-15 – UC-18 | Mid-term |
| WF-04 | Invitations and inbox | UC-20 – UC-23 | Final |
| WF-05 | Event preparation | UC-24 – UC-27 | Final |
| WF-06 | Event day and follow-up | UC-28 – UC-30 | Final |

Parts of WF-01 to WF-03 are delivered in the Final milestone; [User Stories](08-user-stories.md) gives the milestone of each part.

## State machines

### Event

```mermaid
stateDiagram-v2
  [*] --> Draft : create
  Draft --> Published : publish
  Draft --> Cancelled : cancel
  Draft --> [*] : delete
  Published --> Cancelled : cancel / all activities cancelled
  Published --> Completed : last activity ends
```

Each activity's registration status (Not yet open, Open, Full, Closed) and timing (upcoming, ongoing, ended) are derived from its dates and capacity, not stored.

### Signup

```mermaid
stateDiagram-v2
  [*] --> Confirmed : sign up, place free
  [*] --> Waitlisted : sign up, activity full
  Waitlisted --> Confirmed : promoted
  Waitlisted --> Cancelled : leaves / removed for overlap / activity cancelled
  Confirmed --> Cancelled : cancels before start / activity cancelled
  Confirmed --> Attended : marked present
  Attended --> Confirmed : attendance corrected
```

## WF-01 Account and organization onboarding

A user creates an account, requests an organization, and a Platform Admin approves it. Other users join, and the Org Admin assigns roles.

```mermaid
flowchart TD
  A["Guest registers and confirms email"] --> B["Participant requests an organization"]
  B --> C{"Platform Admin review"}
  C -- Reject --> D["Request rejected with reason"]
  C -- Approve --> E["Organization active, requester is Org Admin"]
  E --> F["Participants ask to join"]
  F --> G{"Org Admin decision"}
  G -- Reject --> H["Request rejected"]
  G -- Approve --> I["Member"]
  I --> J["Org Admin assigns roles: Member, Organizer, Org Admin"]
```

## WF-02 Event lifecycle

An Organizer drafts an event, the team plans its activities and agendas, and an Editor publishes it. The team keeps it up to date until it completes or is cancelled.

```mermaid
flowchart TD
  A["Organizer creates draft event"] --> B["Team adds activities and agendas"]
  B --> C{"Publishable? (BR-02)"}
  C -- No --> B
  C -- Yes --> D["Published"]
  D --> E["Editors update details, affected participants notified"]
  E --> D
  D --> F{"Owner cancels?"}
  F -- Yes --> G["Cancelled, signups cancelled, participants notified"]
  F -- No --> H["Last activity ends"]
  H --> I["Completed, 7 days for attendance and feedback"]
```

## WF-03 Discovery and registration

People find events and sign up for the activities they want. Waitlists fill freed places automatically.

```mermaid
flowchart TD
  A["Search and browse events"] --> B["Open activity"]
  B --> C{"Logged in?"}
  C -- No --> D["Log in or register"] --> B
  C -- Yes --> E{"Registration open?"}
  E -- No --> F["Show registration window"]
  E -- Yes --> G{"Overlaps a confirmed signup?"}
  G -- Yes --> H["Refuse and name the overlapping activity"]
  G -- No --> I{"Place free?"}
  I -- Yes --> J["Confirmed"]
  I -- No --> K["Waitlisted"]
  J -- cancels before start --> L["Place freed"]
  L --> M{"Anyone on waitlist?"}
  M -- Yes --> N{"First person has an overlapping confirmed signup?"}
  N -- Yes --> O["Remove from waitlist"] --> M
  N -- No --> P["Promote to Confirmed and notify"]
```

## WF-04 Invitations and inbox

Org Admins invite users to their organization, and Editors invite users to private activities. Invitations and all other notifications arrive in the inbox and by email.

```mermaid
flowchart TD
  A["Org Admin invites user to organization"] --> C["Invitation in inbox and by email"]
  B["Editor invites user to private activity"] --> C
  C --> D{"Invitee responds"}
  D -- Decline --> E["Invitation closed"]
  D -- "Accept (organization)" --> F["User becomes Member"]
  D -- "Accept (activity)" --> G["Signup as in WF-03: confirmed, waitlisted or refused on overlap"]
```

## WF-05 Event preparation

The event team organizes itself, shares tasks and keeps participants informed.

```mermaid
flowchart TD
  A["Owner and Editors build the team"] --> B["Editors create and assign tasks"]
  B --> C["Assignees complete tasks"]
  A --> D["Editors send announcements"]
  E["24 hours before each activity"] --> F["System reminds confirmed participants"]
```

## WF-06 Event day and follow-up

Staff record attendance, attendees give feedback, and the team reviews the results.

```mermaid
flowchart TD
  A["Attendance window opens, 1 hour before start"] --> B["Participant shows QR code"]
  B --> C{"Confirmed signup for this activity?"}
  C -- Yes --> D["Marked Attended"]
  C -- No --> E["Refused with reason"]
  B -.->|no QR code| F["Staff finds participant in list and marks present"]
  F --> D
  D --> G["Activity ends, attendees give feedback within 7 days"]
  G --> H["Team views feedback and report, exports CSV"]
```
