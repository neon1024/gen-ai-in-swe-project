create table lifecycle_feedback (
    id uuid primary key,
    activity_id uuid not null references lifecycle_activity (id) on delete cascade,
    author_id uuid not null,
    rating smallint not null check (rating between 1 and 5),
    comment varchar(2000) null,
    created_at timestamptz not null,
    unique (activity_id, author_id)
);
