CREATE SCHEMA IF NOT EXISTS event_authoring;

CREATE TABLE event_authoring.event (
    id UUID PRIMARY KEY,
    organization_ref TEXT NOT NULL CHECK (organization_ref ~ '[^[:space:]]'),
    owner_subject TEXT NOT NULL CHECK (owner_subject ~ '[^[:space:]]'),
    title TEXT NOT NULL CHECK (title ~ '[^[:space:]]'),
    description TEXT NOT NULL CHECK (description ~ '[^[:space:]]'),
    category TEXT NOT NULL CHECK (category IN ('WORKSHOP', 'CONFERENCE', 'TRAINING', 'SOCIAL', 'COMPETITION', 'OTHER')),
    state TEXT NOT NULL CHECK (state IN ('DRAFT', 'PUBLISHED', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    version BIGINT NOT NULL
);

CREATE TABLE event_authoring.activity (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES event_authoring.event(id),
    title TEXT NOT NULL CHECK (title ~ '[^[:space:]]'),
    description TEXT NOT NULL CHECK (description ~ '[^[:space:]]'),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    start_offset_seconds INT NOT NULL,
    end_offset_seconds INT NOT NULL,
    location TEXT,
    online_link TEXT,
    capacity INT NOT NULL CHECK (capacity >= 1),
    registration_open TIMESTAMPTZ,
    registration_close TIMESTAMPTZ,
    registration_open_offset_seconds INT,
    registration_close_offset_seconds INT,
    visibility TEXT NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    cancelled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT activity_interval CHECK (ends_at > starts_at),
    CONSTRAINT activity_place CHECK (coalesce(location, '') ~ '[^[:space:]]' OR coalesce(online_link, '') ~ '[^[:space:]]'),
    CONSTRAINT activity_registration_close CHECK (registration_close <= starts_at),
    CONSTRAINT activity_registration_open_offset CHECK ((registration_open IS NULL) = (registration_open_offset_seconds IS NULL)),
    CONSTRAINT activity_registration_close_offset CHECK ((registration_close IS NULL) = (registration_close_offset_seconds IS NULL))
);
CREATE INDEX activity_event_idx ON event_authoring.activity(event_id);

CREATE TABLE event_authoring.agenda_item (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES event_authoring.activity(id),
    time TIMESTAMPTZ NOT NULL,
    offset_seconds INT NOT NULL,
    title TEXT NOT NULL CHECK (title ~ '[^[:space:]]'),
    speaker TEXT
);
CREATE INDEX agenda_item_activity_idx ON event_authoring.agenda_item(activity_id);
