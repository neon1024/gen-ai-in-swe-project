-- Local demonstration event: a published AI Society code competition owned by organizer@example.invalid.
-- Dates are relative to when the database is created, so the event is always upcoming after a reset.
-- Written to both the authoring model and the lifecycle copy that discovery reads (normally synced on publish).
-- Loaded only when app.flyway.demo-location points here (run-app.sh, tests).

INSERT INTO event_authoring.event
    (id, organization_ref, owner_subject, title, description, category, state, created_at, published_at, version)
VALUES
    ('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001',
     '00000000-0000-4000-8000-000000000007', 'AI Society Code Sprint',
     'A four-hour team coding competition. Teams of up to four solve a challenge revealed at the start and present their projects to the jury.',
     'COMPETITION', 'PUBLISHED', now() - interval '2 days', now() - interval '1 day', 1);

INSERT INTO event_authoring.activity
    (id, event_id, title, description, starts_at, ends_at, start_offset_seconds, end_offset_seconds,
     location, online_link, capacity, registration_open, registration_close,
     registration_open_offset_seconds, registration_close_offset_seconds, visibility, cancelled, created_at)
VALUES
    ('21000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'Code competition',
     'Teams implement a solution to the challenge, then present it. The best projects are announced at the end.',
     date_trunc('day', now()) + interval '7 days 14 hours', date_trunc('day', now()) + interval '7 days 18 hours', 0, 0,
     'Main auditorium', NULL, 40,
     date_trunc('hour', now()), date_trunc('day', now()) + interval '7 days 13 hours', 0, 0,
     'PUBLIC', FALSE, now() - interval '2 days');

INSERT INTO event_authoring.agenda_item (id, activity_id, time, offset_seconds, title, speaker) VALUES
    ('22000000-0000-4000-8000-000000000001', '21000000-0000-4000-8000-000000000001',
     date_trunc('day', now()) + interval '7 days 14 hours', 0, 'Challenge revealed', 'AI Society Admin'),
    ('22000000-0000-4000-8000-000000000002', '21000000-0000-4000-8000-000000000001',
     date_trunc('day', now()) + interval '7 days 14 hours 15 minutes', 0, 'Teams implement their solutions', NULL),
    ('22000000-0000-4000-8000-000000000003', '21000000-0000-4000-8000-000000000001',
     date_trunc('day', now()) + interval '7 days 17 hours', 0, 'Projects presented', NULL),
    ('22000000-0000-4000-8000-000000000004', '21000000-0000-4000-8000-000000000001',
     date_trunc('day', now()) + interval '7 days 17 hours 45 minutes', 0, 'Results announced', 'AI Society Admin');

INSERT INTO lifecycle_event (id, organization_id, title, description, category, state, published_at)
SELECT id, organization_ref::uuid, title, description, category, state, published_at
FROM event_authoring.event WHERE id = '20000000-0000-4000-8000-000000000001';

INSERT INTO lifecycle_activity
    (id, event_id, title, description, starts_at, ends_at, location, online_link, capacity,
     registration_opens_at, registration_closes_at, visibility)
SELECT id, event_id, title, description, starts_at, ends_at, location, online_link, capacity,
       registration_open, registration_close, visibility
FROM event_authoring.activity WHERE event_id = '20000000-0000-4000-8000-000000000001';

INSERT INTO lifecycle_agenda_item (id, activity_id, starts_at, title, speaker)
SELECT id, activity_id, time, title, speaker
FROM event_authoring.agenda_item WHERE activity_id = '21000000-0000-4000-8000-000000000001';
