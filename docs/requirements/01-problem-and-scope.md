# Problem Statement and Scope

## Problem statement

Student organizations at Babeș-Bolyai University (UBB) run workshops, conferences, training sessions, social activities and competitions throughout the academic year. Their coordination is spread across separate tools: online forms for signups, spreadsheets for attendance, social-media posts for announcements and group chats for team coordination. As a result:

- students have no single place to find events;
- capacity and waitlists are managed by hand;
- multi-part events force all-or-nothing signups, so places go unused;
- organizer teams lack a shared view of who is responsible for what;
- attendance and feedback are rarely collected consistently.

## Goal

Provide one platform covering the full event lifecycle: onboarding organizations, planning events as one or more activities, publishing and discovery, per-activity registration with waitlists, team coordination, check-in, and feedback and reporting.

This document set covers functional requirements. User interface, architecture and technology are documented separately.

## In scope

- User accounts with a confirmed email address and a unique username
- Organizations: registration with approval, membership, per-organization roles
- Events made of activities with agendas; event lifecycle (Draft, Published, Completed, Cancelled)
- Public and private activities; invitations through an in-app inbox
- Per-activity signup, cancellation, overlap prevention, waitlist with automatic promotion
- Event team (Owner, Editor, Staff) and task assignment
- Notifications in the inbox and by email: changes, cancellations, waitlist promotions, announcements, reminders
- Event search by keyword, date, organization and category
- Attendance by QR code or manual check-in
- Activity feedback and event reports with CSV export

## Out of scope

| Excluded | Reason |
|---|---|
| Venue booking and clash detection | Rooms are booked through university processes |
| Restricting activities to UBB students | University affiliation cannot be verified reliably; private activities cover restricted access |
| Event approval before publishing | Organizations are responsible for their own events |
| Platform-wide statistics | Event reports cover the organizations' needs |
| Following organizations, free-form tags, free-places filter | Keyword search and category filters cover discovery |
| Attendance certificates | Not part of the core event lifecycle |
| Walk-in registration | Participants sign up before the activity |
| Notification preferences | All notifications go to the inbox and by email |
| Team registration for competitions | Participants sign up individually |

## Milestones

| Milestone | Scope |
|---|---|
| Mid-term | Accounts; organization registration and approval; membership and roles; creating, publishing, editing, cancelling and completing events with activities and agendas; search and browsing; signup, cancellation and waitlist |
| Final | Private activities, invitations and inbox; email notifications and reminders; event team and tasks; check-in; feedback and event reports; administration of accounts and organizations |
