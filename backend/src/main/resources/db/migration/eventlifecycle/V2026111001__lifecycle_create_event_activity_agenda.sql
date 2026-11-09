create table lifecycle_event (
    id uuid primary key,
    organization_id uuid not null,
    title varchar(200) not null,
    description text not null,
    category varchar(20) not null
        check (category in ('WORKSHOP', 'CONFERENCE', 'TRAINING', 'SOCIAL', 'COMPETITION', 'OTHER')),
    state varchar(20) not null
        check (state in ('DRAFT', 'PUBLISHED', 'COMPLETED', 'CANCELLED')),
    published_at timestamptz null,
    cancelled_at timestamptz null,
    completed_at timestamptz null,
    version bigint not null default 0
);

create index idx_lifecycle_event_state on lifecycle_event (state);

create table lifecycle_activity (
    id uuid primary key,
    event_id uuid not null references lifecycle_event (id) on delete cascade,
    title varchar(200) not null,
    description text not null default '',
    starts_at timestamptz null,
    ends_at timestamptz null,
    location varchar(300) null,
    online_link varchar(500) null,
    capacity int null check (capacity >= 1),
    registration_opens_at timestamptz null,
    registration_closes_at timestamptz null,
    visibility varchar(10) not null default 'PUBLIC'
        check (visibility in ('PUBLIC', 'PRIVATE')),
    state varchar(10) not null default 'SCHEDULED'
        check (state in ('SCHEDULED', 'CANCELLED')),
    cancelled_at timestamptz null,
    check (ends_at is null or starts_at is null or ends_at > starts_at)
);

create index idx_lifecycle_activity_event on lifecycle_activity (event_id);

create table lifecycle_agenda_item (
    id uuid primary key,
    activity_id uuid not null references lifecycle_activity (id) on delete cascade,
    starts_at timestamptz not null,
    title varchar(200) not null,
    speaker varchar(200) null
);
