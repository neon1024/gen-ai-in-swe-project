# User Stories

The product backlog. Each workflow is an epic; each story traces to its use case, functional requirements and business rules. Stories follow the INVEST criteria: large use cases are split into several stories, and behaviour delivered in a later milestone is a separate story.

**Format:** *As a* role, *I want* goal, *so that* benefit, followed by testable acceptance criteria.

**Definition of done:**
- every acceptance criterion is verified by an automated test whose name includes the story or rule ID;
- the pull request references the story issue and is reviewed by another team member;
- the change is merged into the main branch.

## WF-01 Account and organization onboarding

Users create accounts; organizations are registered, approved, and staffed with members and roles.

### US-01 Register an account
**As a** guest, **I want** to create an account with my email, a username and a display name, **so that** I can sign up for activities and join organizations.
**Milestone:** Mid-term · **Traces:** UC-01, FR-ACC-01, FR-ACC-02, BR-20
- [ ] Registration requires email, password, username and display name.
- [ ] A username already taken, ignoring case, is refused.
- [ ] I cannot log in until I confirm my email through the link sent to me.
- [ ] Registering with an email that already has an account does not reveal that the account exists.

### US-02 Log in, log out and reset password
**As a** user, **I want** to log in, log out and reset a forgotten password, **so that** I can access my account securely.
**Milestone:** Mid-term · **Traces:** UC-02, FR-ACC-03
- [ ] I can log in with my confirmed email and password, and log out.
- [ ] Wrong credentials are refused without saying which one is wrong.
- [ ] Unconfirmed and deactivated accounts cannot log in, and the reason is shown.
- [ ] I can request a password reset link by email and set a new password with it.

### US-03 Request organization registration
**As a** participant, **I want** to request registration of my student organization, **so that** it can publish events on the platform.
**Milestone:** Mid-term · **Traces:** UC-03, FR-ORG-01
- [ ] The request requires the organization's name, description and contact email.
- [ ] A name used by an active organization or a pending request is refused.
- [ ] I can see the status of my request: Pending, Approved, or Rejected with the reason.

### US-04 Review organization requests
**As a** platform admin, **I want** to approve or reject organization requests, **so that** only genuine UBB student organizations use the platform.
**Milestone:** Mid-term · **Traces:** UC-04, FR-ORG-02
- [ ] I see all pending requests.
- [ ] Approving creates the organization and makes the requester its Org Admin.
- [ ] Rejecting requires a reason, which the requester can see.

### US-05 Edit the organization profile
**As an** org admin, **I want** to edit my organization's name, description and contact email, **so that** its information stays accurate.
**Milestone:** Mid-term · **Traces:** UC-05, FR-ORG-03
- [ ] Only Org Admins of the organization can edit it.
- [ ] A name used by another organization is refused.

### US-06 Ask to join an organization
**As a** participant, **I want** to ask to join an organization, **so that** I can take part in its private activities and event teams.
**Milestone:** Mid-term · **Traces:** UC-06, FR-ORG-04
- [ ] I can send a join request to an organization I am not a member of.
- [ ] I cannot send a second request while one is pending, and I can withdraw a pending request.
- [ ] I can see whether my request is pending, approved or rejected.

### US-07 Handle join requests
**As an** org admin, **I want** to approve or reject join requests, **so that** I control who becomes a member.
**Milestone:** Mid-term · **Traces:** UC-07, FR-ORG-04
- [ ] I see all pending join requests for my organization.
- [ ] Approving adds the user as Member.
- [ ] Rejecting closes the request; the user may ask again later.

### US-08 Change member roles
**As an** org admin, **I want** to change members' roles, **so that** the right people can create events and manage the organization.
**Milestone:** Mid-term · **Traces:** UC-08, FR-ORG-06, BR-21
- [ ] I can set any member's role to Member, Organizer or Org Admin.
- [ ] A change that would leave the organization without an Org Admin is refused.

### US-09 Remove a member
**As an** org admin, **I want** to remove a member, **so that** people who left the organization lose access.
**Milestone:** Mid-term · **Traces:** UC-08, FR-ORG-06, BR-21, BR-22, BR-23
- [ ] Removing a member also removes them from the organization's event teams.
- [ ] Removing the last Org Admin is refused.
- [ ] Removing the Owner of a draft or published event is refused until ownership is transferred.

### US-10 Leave an organization
**As a** member, **I want** to leave an organization, **so that** I am no longer part of it.
**Milestone:** Mid-term · **Traces:** UC-09, FR-ORG-07, BR-21, BR-22, BR-23
- [ ] Leaving removes my membership and my roles in the organization's event teams.
- [ ] The last Org Admin cannot leave.
- [ ] The Owner of a draft or published event cannot leave until ownership is transferred.

### US-11 Deactivate user accounts
**As a** platform admin, **I want** to deactivate and reactivate user accounts, **so that** I can stop abuse of the platform.
**Milestone:** Final · **Traces:** UC-31, FR-ACC-04, BR-12
- [ ] A deactivated user cannot log in.
- [ ] Deactivation cancels the user's signups for activities that have not started, and the freed places go to the waitlists.
- [ ] Reactivation restores login but not the cancelled signups.

### US-12 Manage Platform Admins
**As a** platform admin, **I want** to grant and revoke the Platform Admin role, **so that** administration does not depend on one person.
**Milestone:** Final · **Traces:** UC-31, FR-ACC-05, BR-25
- [ ] I can grant the Platform Admin role to any active user and revoke it.
- [ ] Revoking the last Platform Admin is refused.

### US-13 Deactivate organizations
**As a** platform admin, **I want** to deactivate and reactivate organizations, **so that** inactive or abusive organizations stop appearing.
**Milestone:** Final · **Traces:** UC-31, FR-ORG-08, BR-24
- [ ] Events of a deactivated organization are hidden and closed to signups.
- [ ] Reactivating an organization makes its events visible again.

## WF-02 Event lifecycle

Organizers plan events as activities with agendas, publish them and keep them up to date; the system completes them.

### US-14 Create a draft event
**As an** organizer, **I want** to create a draft event for my organization, **so that** my team can plan it before publishing.
**Milestone:** Mid-term · **Traces:** UC-10, FR-EVT-01, BR-03
- [ ] A draft requires title, description, category, and an organization where I am Organizer or Org Admin.
- [ ] I become the event's Owner.
- [ ] The draft is visible only to its event team and the organization's Org Admins.

### US-15 Add and edit activities
**As an** editor, **I want** to add, edit and remove activities, **so that** each part of the event has its own schedule, place and capacity.
**Milestone:** Mid-term · **Traces:** UC-10, FR-EVT-02, BR-07, BR-08
- [ ] An activity has title, description, start, end, location and/or online link, capacity, optional registration open and close times, and visibility.
- [ ] An activity that ends before it starts, or has capacity below 1, is refused.
- [ ] Without explicit times, registration opens at publication and closes at activity start.
- [ ] A registration close time after the activity start is refused.

### US-16 Manage an activity's agenda
**As an** editor, **I want** to add agenda items to an activity, **so that** participants know what happens and when.
**Milestone:** Mid-term · **Traces:** UC-10, FR-EVT-03, BR-07
- [ ] An agenda item has a time, a title and an optional speaker.
- [ ] An agenda item outside the activity's start and end is refused.

### US-17 Publish an event
**As an** editor, **I want** to publish an event, **so that** people can find it and sign up.
**Milestone:** Mid-term · **Traces:** UC-11, FR-EVT-04, BR-01, BR-02
- [ ] Publishing requires at least one activity, each with start, end, location or online link, and capacity.
- [ ] If the requirements are not met, publishing is refused with the list of problems.
- [ ] A published event appears in search, and its activities follow their registration windows.
- [ ] Staff cannot publish.

### US-18 Edit a published event
**As an** editor, **I want** to change a published event's details, **so that** its information stays correct.
**Milestone:** Mid-term · **Traces:** UC-12, FR-EVT-05, BR-12, BR-14
- [ ] Event, activity and agenda details can be edited, and activities can be added.
- [ ] Capacity cannot be set below the number of confirmed signups.
- [ ] Raising capacity promotes people from the waitlist.
- [ ] An activity with signups cannot be deleted, only cancelled.

### US-19 Notify participants of changes and cancellations
**As a** participant, **I want** to be told when an activity I signed up for changes or is cancelled, **so that** I do not go at the wrong time or to the wrong place.
**Milestone:** Final · **Traces:** UC-12, UC-13, FR-NTF-03
- [ ] Changing an activity's time or location notifies everyone with a confirmed or waitlisted signup.
- [ ] Cancelling an activity or event notifies everyone with a confirmed or waitlisted signup.
- [ ] Other edits do not send notifications.

### US-20 Cancel an activity
**As an** owner, **I want** to cancel a single activity, **so that** the rest of the event can go ahead.
**Milestone:** Mid-term · **Traces:** UC-13, FR-EVT-06, BR-04, BR-14
- [ ] Cancelling an activity cancels all its signups and waitlist entries.
- [ ] When every activity of an event is cancelled, the event is cancelled.
- [ ] Editors and Staff cannot cancel.

### US-21 Cancel an event
**As an** owner, **I want** to cancel an event, **so that** participants know it will not happen.
**Milestone:** Mid-term · **Traces:** UC-13, FR-EVT-06, BR-01, BR-05
- [ ] Only draft and published events can be cancelled.
- [ ] Cancelling an event cancels all its activities, signups and waitlist entries.
- [ ] A cancelled event is read-only.

### US-22 Delete a draft event
**As an** owner, **I want** to delete a draft, **so that** abandoned plans do not clutter the organization.
**Milestone:** Mid-term · **Traces:** UC-14, FR-EVT-07, BR-05
- [ ] A draft event is deleted with its activities and agenda items.
- [ ] Published events cannot be deleted, only cancelled.

### US-23 Complete events automatically
**As an** organizer, **I want** events to complete on their own, **so that** nobody has to close them by hand.
**Milestone:** Mid-term · **Traces:** UC-19, FR-EVT-08, BR-01, BR-04, BR-06
- [ ] An event becomes Completed when its last non-cancelled activity ends.
- [ ] A completed event is read-only, apart from attendance and feedback until 7 days after each activity ends.

## WF-03 Discovery and registration

People find events and sign up for the activities they want; waitlists fill freed places.

### US-24 Browse and search events
**As a** guest or participant, **I want** to browse and search published events, **so that** I find the ones that interest me.
**Milestone:** Mid-term · **Traces:** UC-15, FR-DSC-01, FR-DSC-02, BR-03, BR-18
- [ ] The list shows upcoming published events visible to me, soonest first.
- [ ] Keyword search matches event title, description and organization name.
- [ ] Draft events and private activities I cannot access never appear.

### US-25 Filter events
**As a** participant, **I want** to filter events by date range, organization and category, **so that** I see only relevant events.
**Milestone:** Mid-term · **Traces:** UC-15, FR-DSC-03
- [ ] I can filter by date range, organization and category, alone or combined with keyword search.
- [ ] Categories are Workshop, Conference, Training, Social, Competition and Other.

### US-26 View event details
**As a** participant, **I want** to see an event's full details, **so that** I can decide which activities to attend.
**Milestone:** Mid-term · **Traces:** UC-15, FR-DSC-04, BR-09
- [ ] Details show organization, description, category, and each activity's schedule, location or online link, agenda and registration status.
- [ ] Registration status is Not yet open, Open, Full or Closed.
- [ ] Guests can view the details of public events.

### US-27 Sign up for an activity
**As a** participant, **I want** to sign up for individual activities of an event, **so that** I take a place only in the parts I will attend.
**Milestone:** Mid-term · **Traces:** UC-16, FR-REG-01, FR-REG-02, FR-REG-05, BR-09, BR-10, BR-11, BR-18
- [ ] Signing up for an open activity with free places creates a confirmed signup.
- [ ] Signing up for a full activity puts me on the waitlist and shows my position.
- [ ] Signing up is refused while registration is not open, and the registration window is shown.
- [ ] Signing up is refused if the activity overlaps one of my confirmed signups, and that activity is named.
- [ ] I cannot hold two signups for the same activity.

### US-28 Cancel a signup or leave a waitlist
**As a** participant, **I want** to cancel a signup or leave a waitlist, **so that** my place goes to someone else.
**Milestone:** Mid-term · **Traces:** UC-17, FR-REG-03, BR-13
- [ ] I can cancel a confirmed signup or a waitlist entry until the activity starts.
- [ ] After the activity starts, cancelling is refused.

### US-29 Promote from the waitlist automatically
**As a** participant on a waitlist, **I want** to get a place automatically when one frees up, **so that** I do not have to keep checking.
**Milestone:** Mid-term · **Traces:** UC-17, FR-REG-04, BR-12
- [ ] When a confirmed signup is cancelled before the activity starts, the first person on the waitlist becomes confirmed.
- [ ] A waitlisted person who now has an overlapping confirmed signup is removed, and the next person is promoted.
- [ ] Raising capacity promotes as many people as there are new places.
- [ ] No promotions happen after the activity starts.

### US-30 Notify promoted participants
**As a** participant, **I want** to be told when I get a place from the waitlist, **so that** I can plan to attend or cancel.
**Milestone:** Final · **Traces:** UC-17, FR-NTF-04
- [ ] A promoted participant receives a notification naming the activity, its time and location.

### US-31 View my activities
**As a** participant, **I want** to see all my signups, **so that** I know where I am going.
**Milestone:** Mid-term · **Traces:** UC-18, FR-DSC-05
- [ ] I see upcoming and past signups with their status: Confirmed, Waitlisted (with position), Cancelled or Attended.

## WF-04 Invitations and inbox

Organizations and event teams invite people through an in-app inbox; every notification also arrives by email.

### US-32 Invite a user to an organization
**As an** org admin, **I want** to invite a user to my organization, **so that** I can recruit people directly.
**Milestone:** Final · **Traces:** UC-20, FR-ORG-05, BR-19
- [ ] I find users by username or display name; their email is never shown.
- [ ] The invitation appears in the user's inbox and is sent by email.
- [ ] Members and users with a pending invitation cannot be invited again.

### US-33 Make an activity private
**As an** editor, **I want** to make an activity private, **so that** only members and invited people can see and join it.
**Milestone:** Final · **Traces:** UC-21, FR-PRV-01, BR-18
- [ ] Activities are public by default.
- [ ] A private activity is visible to and joinable by organization members and invited users only.
- [ ] An event whose activities are all private is hidden from everyone else.

### US-34 Invite users to a private activity
**As an** editor, **I want** to invite specific people to a private activity, **so that** non-members can attend without making it public.
**Milestone:** Final · **Traces:** UC-21, FR-PRV-02, BR-19
- [ ] I find users by username or display name; their email is never shown.
- [ ] Each invited user receives the invitation in the inbox and by email, and can then see the activity.
- [ ] Organization members and already-invited users cannot be invited.

### US-35 Respond to an invitation
**As a** participant, **I want** to accept or decline invitations in my inbox, **so that** I decide whether to join.
**Milestone:** Final · **Traces:** UC-22, FR-PRV-03, FR-ORG-05, BR-10, BR-11
- [ ] Accepting an activity invitation signs me up: confirmed, waitlisted if full, or refused if it overlaps a confirmed signup.
- [ ] Accepting an organization invitation makes me a Member.
- [ ] Declining closes the invitation.
- [ ] Invitations to started or cancelled activities cannot be accepted.

### US-36 Use the inbox
**As a** user, **I want** an inbox with my notifications and invitations, **so that** I have one place for everything the platform tells me.
**Milestone:** Final · **Traces:** UC-23, FR-NTF-01
- [ ] The inbox lists notifications and pending invitations, newest first.
- [ ] Unread items are marked; opening an item marks it read.

### US-37 Receive notifications by email
**As a** user, **I want** every notification by email too, **so that** I do not miss it when I am not using the platform.
**Milestone:** Final · **Traces:** UC-23, FR-NTF-02, BR-19
- [ ] Every inbox notification and invitation is also sent to my email address.
- [ ] Emails never contain other users' email addresses.

## WF-05 Event preparation

The event team organizes itself, shares tasks and keeps participants informed.

### US-38 Manage Staff and Editors
**As an** editor, **I want** to add and remove Staff, **so that** the event has enough people to run it.
**Milestone:** Final · **Traces:** UC-26, FR-TEAM-01, BR-22
- [ ] Owner and Editors can add organization members as Staff and remove Staff.
- [ ] Only the Owner can promote Staff to Editor or remove Editors.
- [ ] Users outside the organization cannot join the team.

### US-39 Transfer event ownership
**As an** owner, **I want** to hand my event to an Editor, **so that** it stays managed if I step away.
**Milestone:** Final · **Traces:** UC-26, FR-TEAM-02, BR-23
- [ ] The Owner can transfer ownership to an Editor of the event.
- [ ] The previous Owner becomes an Editor.

### US-40 Create and assign tasks
**As an** editor, **I want** to create tasks and assign them to team members, **so that** everyone knows their responsibilities.
**Milestone:** Final · **Traces:** UC-27, FR-TEAM-03
- [ ] A task has title, description, one or more assignees from the event team, optional due date and optional activity.
- [ ] New tasks are To do.
- [ ] Editors can edit, reassign and delete tasks.

### US-41 Complete tasks and track status
**As a** staff member, **I want** to mark my tasks done and see the team's tasks, **so that** the team knows what is left.
**Milestone:** Final · **Traces:** UC-27, FR-TEAM-04
- [ ] Assignees can mark their tasks Done and reopen them.
- [ ] The whole team sees all tasks with assignees, due dates and status.

### US-42 Send an announcement
**As an** editor, **I want** to message everyone signed up, **so that** I can share practical information.
**Milestone:** Final · **Traces:** UC-24, FR-NTF-05
- [ ] I choose the audience: the whole event or one activity.
- [ ] Everyone with a confirmed or waitlisted signup in that audience receives the announcement.

### US-43 Receive activity reminders
**As a** participant, **I want** a reminder before my activity starts, **so that** I do not forget it.
**Milestone:** Final · **Traces:** UC-25, FR-NTF-06
- [ ] Confirmed participants receive a reminder 24 hours before the activity starts.
- [ ] Signups made less than 24 hours before the start receive no reminder.
- [ ] Cancelled activities send no reminders.

## WF-06 Event day and follow-up

Staff record attendance, attendees give feedback, and the team reviews the results.

### US-44 Get a personal QR code
**As a** participant, **I want** a personal QR code for each confirmed signup, **so that** I can check in quickly.
**Milestone:** Final · **Traces:** UC-28, FR-ATT-01
- [ ] Each confirmed signup has a unique QR code visible only to its participant.

### US-45 Check in by QR code
**As a** staff member, **I want** to scan participants' QR codes, **so that** check-in is fast.
**Milestone:** Final · **Traces:** UC-28, FR-ATT-02, BR-15
- [ ] Scanning a valid code for this activity marks the participant Attended.
- [ ] Codes for another activity, or for cancelled or waitlisted signups, are refused with the reason.
- [ ] Scanning a code already marked Attended says so.
- [ ] Check-in is possible from one hour before the start until 7 days after the end.

### US-46 Check in manually and correct attendance
**As a** staff member, **I want** to mark attendance by hand and fix mistakes, **so that** attendance is right even without a QR code.
**Milestone:** Final · **Traces:** UC-28, FR-ATT-03, BR-15
- [ ] I can search the activity's confirmed signups by display name or username and mark someone present.
- [ ] I can unmark a participant marked by mistake.
- [ ] Changes are refused more than 7 days after the activity ends.

### US-47 Give feedback
**As an** attendee, **I want** to rate an activity, **so that** organizers can improve future events.
**Milestone:** Final · **Traces:** UC-29, FR-FBK-01, BR-16
- [ ] I can give a 1–5 rating with an optional comment, once per activity I attended.
- [ ] Feedback is accepted only within 7 days after the activity ends.
- [ ] Participants not marked present cannot give feedback.

### US-48 View feedback
**As a** staff member, **I want** to see the feedback on each activity, **so that** the team learns what worked.
**Milestone:** Final · **Traces:** UC-30, FR-FBK-02, BR-17
- [ ] For each activity I see the average rating, the rating distribution and the comments.
- [ ] Feedback never shows who wrote it.

### US-49 View the event report
**As a** staff member, **I want** an event report, **so that** the organization knows how the event went.
**Milestone:** Final · **Traces:** UC-30, FR-RPT-01
- [ ] For each activity the report shows confirmed signups, waitlist size, cancellations, attendance rate and average rating.
- [ ] Attendance rate is the share of confirmed participants marked Attended.

### US-50 Export the event report
**As a** staff member, **I want** to export the report as CSV, **so that** the organization can keep its own records.
**Milestone:** Final · **Traces:** UC-30, FR-RPT-02
- [ ] The report downloads as a CSV file with one row per activity.
- [ ] The export contains aggregate figures only, no personal data.
