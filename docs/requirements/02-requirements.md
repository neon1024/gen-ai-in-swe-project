# Requirements

Functional requirements (FR) and business rules (BR). **Source** is the requirement of the project brief (R1–R16) that the item refines. **Milestone** is the delivery that implements it.

## Functional requirements

### Accounts

| ID | Requirement | Source | Milestone |
|---|---|---|---|
| FR-ACC-01 | Users register with email and password; the email must be confirmed before first login | R2 | Mid-term |
| FR-ACC-02 | Users choose a unique username and a display name at registration | R2 | Mid-term |
| FR-ACC-03 | Users log in, log out and reset their password | R2 | Mid-term |
| FR-ACC-04 | Platform Admin deactivates and reactivates user accounts | R2 | Final |
| FR-ACC-05 | Platform Admin grants and revokes the Platform Admin role | R2 | Final |

### Organizations

| ID | Requirement | Source | Milestone |
|---|---|---|---|
| FR-ORG-01 | A Participant requests registration of an organization (name, description, contact email) | R2 | Mid-term |
| FR-ORG-02 | Platform Admin approves or rejects the request with a reason; on approval the requester becomes Org Admin | R2 | Mid-term |
| FR-ORG-03 | Org Admin edits the organization profile | R2 | Mid-term |
| FR-ORG-04 | A Participant asks to join an organization; Org Admin approves or rejects | R2 | Mid-term |
| FR-ORG-05 | Org Admin invites a user to join; the invitee accepts or declines in the inbox | R2 | Final |
| FR-ORG-06 | Org Admin changes member roles and removes members | R2 | Mid-term |
| FR-ORG-07 | A Member leaves an organization | R2 | Mid-term |
| FR-ORG-08 | Platform Admin deactivates and reactivates organizations | R2 | Final |

### Events and activities

| ID | Requirement | Source | Milestone |
|---|---|---|---|
| FR-EVT-01 | An Organizer creates a draft event: title, description, category, organization | R1 | Mid-term |
| FR-EVT-02 | The event team adds, edits and removes activities: title, description, start, end, location, optional online link, capacity, registration open and close times, visibility | R1 | Mid-term |
| FR-EVT-03 | The event team adds, edits and removes agenda items of an activity: time, title, optional speaker | R1 | Mid-term |
| FR-EVT-04 | Owner or Editor publishes an event | R1, R4 | Mid-term |
| FR-EVT-05 | The event team edits a published event within the business rules | R1 | Mid-term |
| FR-EVT-06 | Owner cancels an event or a single activity | R4 | Mid-term |
| FR-EVT-07 | Owner deletes a draft event | R1 | Mid-term |
| FR-EVT-08 | The system marks an event Completed when its last activity ends | R4 | Mid-term |

### Discovery

| ID | Requirement | Source | Milestone |
|---|---|---|---|
| FR-DSC-01 | Users browse published events visible to them | R6 | Mid-term |
| FR-DSC-02 | Users search events by keyword in title, description and organization name | R6 | Mid-term |
| FR-DSC-03 | Users filter events by date range, organization and category | R6 | Mid-term |
| FR-DSC-04 | Users view event details: organization, activities with schedule, agenda, location and registration status | R6 | Mid-term |
| FR-DSC-05 | Participants view their own signups and waitlist positions | R3 | Mid-term |

### Registration

| ID | Requirement | Source | Milestone |
|---|---|---|---|
| FR-REG-01 | A Participant signs up for an activity whose registration is open | R3 | Mid-term |
| FR-REG-02 | Signing up for a full activity places the Participant on its waitlist | R3 | Mid-term |
| FR-REG-03 | A Participant cancels a signup or leaves a waitlist | R3 | Mid-term |
| FR-REG-04 | The system promotes the first eligible person on the waitlist when a place frees up | R3 | Mid-term |
| FR-REG-05 | The system refuses a signup that overlaps one of the Participant's confirmed signups | R3 | Mid-term |

### Private activities

| ID | Requirement | Source | Milestone |
|---|---|---|---|
| FR-PRV-01 | A private activity is visible to and joinable by members of the organizing organization and invited users only | R3 | Final |
| FR-PRV-02 | Owner or Editor invites users to a private activity, finding them by username or display name | R5 | Final |
| FR-PRV-03 | The invitee accepts (which signs them up) or declines the invitation in the inbox | R5 | Final |

### Inbox and notifications

| ID | Requirement | Source | Milestone |
|---|---|---|---|
| FR-NTF-01 | Users have an inbox with notifications and pending invitations | R5 | Final |
| FR-NTF-02 | Every notification is also sent by email | R5 | Final |
| FR-NTF-03 | The system notifies users with a confirmed or waitlisted signup when an activity's time or location changes, or when the activity or event is cancelled | R5 | Final |
| FR-NTF-04 | The system notifies a user when they are promoted from a waitlist | R3 | Final |
| FR-NTF-05 | Owner or Editor sends an announcement to everyone with a confirmed or waitlisted signup for the event or for one activity | R5 | Final |
| FR-NTF-06 | The system sends confirmed participants a reminder 24 hours before an activity starts | R5 | Final |

### Event team and tasks

| ID | Requirement | Source | Milestone |
|---|---|---|---|
| FR-TEAM-01 | Owner or Editor adds and removes Staff; Owner promotes and removes Editors | R5 | Final |
| FR-TEAM-02 | Owner transfers ownership to an Editor | R5 | Final |
| FR-TEAM-03 | Owner or Editor creates and assigns tasks: title, description, assignees, optional due date, optional activity | R5 | Final |
| FR-TEAM-04 | An assignee marks their task done; the team sees all tasks and their status | R5 | Final |

### Attendance

| ID | Requirement | Source | Milestone |
|---|---|---|---|
| FR-ATT-01 | Each confirmed signup has a personal QR code | R5 | Final |
| FR-ATT-02 | Staff scan a QR code to mark the participant present | R5 | Final |
| FR-ATT-03 | Staff find a participant in the signup list and mark them present by hand | R5 | Final |

### Feedback and reporting

| ID | Requirement | Source | Milestone |
|---|---|---|---|
| FR-FBK-01 | A participant marked present rates the activity 1–5 with an optional comment | R4 | Final |
| FR-FBK-02 | The event team views the combined feedback for each activity | R4 | Final |
| FR-RPT-01 | The event team views an event report: signups, waitlist size, cancellations, attendance rate and average rating per activity | R5 | Final |
| FR-RPT-02 | The event team exports the event report as CSV | R5 | Final |

## Business rules

### Event lifecycle

| ID | Rule |
|---|---|
| BR-01 | Event states: Draft, Published, Completed, Cancelled. Allowed transitions: Draft → Published (publish), Draft or Published → Cancelled (cancel), Published → Completed (automatic). |
| BR-02 | An event can be published only if it has at least one activity, and every activity has a start, end, location or online link, and capacity. |
| BR-03 | Draft events are visible only to their event team and the organization's Org Admins. |
| BR-04 | An event becomes Completed when its last non-cancelled activity ends. If every activity of an event is cancelled, the event is cancelled. |
| BR-05 | Published events cannot be deleted, only cancelled. Cancelling an event cancels all its activities and signups. |
| BR-06 | Completed and cancelled events are read-only, apart from attendance and feedback (BR-15, BR-16). |

### Activities and registration

| ID | Rule |
|---|---|
| BR-07 | An activity ends after it starts and has a capacity of at least 1. Its agenda items fall within its start and end. |
| BR-08 | Registration opens at publication unless a later opening time is set, and closes at activity start unless an earlier closing time is set. |
| BR-09 | An activity's registration status (Not yet open, Open, Full, Closed) is derived from its registration window and number of confirmed signups. |
| BR-10 | A user has at most one signup per activity, either confirmed or waitlisted. |
| BR-11 | A signup or waitlist entry is refused if the activity overlaps one of the user's confirmed signups. |
| BR-12 | The waitlist is first come, first served. When a place frees up before the activity starts (cancellation or capacity increase), the first person is promoted. Anyone who now has an overlapping confirmed signup is removed from the waitlist and the next person is promoted. |
| BR-13 | Participants can cancel until the activity starts. |
| BR-14 | Capacity cannot be lowered below the number of confirmed signups. An activity with signups cannot be deleted, only cancelled. |

### Attendance and feedback

| ID | Rule |
|---|---|
| BR-15 | Attendance can be recorded from one hour before an activity starts until 7 days after it ends. |
| BR-16 | Only participants marked present can give feedback: once per activity, within 7 days after it ends. |
| BR-17 | The event team sees feedback without the author's identity. |

### Visibility and privacy

| ID | Rule |
|---|---|
| BR-18 | A private activity is visible to and joinable by organization members and invited users only. An event whose activities are all private is hidden from everyone else. |
| BR-19 | Email addresses are never shown to other users. |
| BR-20 | Usernames are unique, ignoring case. |

### Organizations, teams and administration

| ID | Rule |
|---|---|
| BR-21 | An organization always has at least one Org Admin. |
| BR-22 | Event team members must be members of the organizing organization. Leaving the organization removes them from its event teams. |
| BR-23 | The Owner of a draft or published event must transfer ownership before leaving or being removed from the organization. |
| BR-24 | Events of a deactivated organization are hidden and closed to signups. |
| BR-25 | The platform always has at least one Platform Admin. |
