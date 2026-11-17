CREATE TABLE users (
    id              uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           varchar(256)    NOT NULL,
    password_hash   varchar(256)    NOT NULL,
    username        varchar(64)     NOT NULL,
    display_name    varchar(128)    NOT NULL,
    status          varchar(16)     NOT NULL DEFAULT 'PENDING',
    platform_admin  boolean         NOT NULL DEFAULT false,
    created_at      timestamptz     NOT NULL DEFAULT now(),
    updated_at      timestamptz     NOT NULL DEFAULT now(),
    CONSTRAINT users_status_check CHECK (status IN ('PENDING', 'ACTIVE', 'DEACTIVATED'))
);

CREATE UNIQUE INDEX users_email_lower_uq ON users (lower(email));
CREATE UNIQUE INDEX users_username_lower_uq ON users (lower(username));
