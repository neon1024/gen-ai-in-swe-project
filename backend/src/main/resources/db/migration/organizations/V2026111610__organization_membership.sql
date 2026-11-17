CREATE SCHEMA IF NOT EXISTS organizations;

CREATE TABLE organizations.organization (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL CHECK (name ~ '[^[:space:]]'),
    description VARCHAR(2000) NOT NULL CHECK (description ~ '[^[:space:]]'),
    contact_email VARCHAR(254) NOT NULL CHECK (contact_email ~ '[^[:space:]]'),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX organization_active_name_ci_uq
    ON organizations.organization (lower(name))
    WHERE active = TRUE;

CREATE TABLE organizations.organization_membership (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations.organization(id),
    user_id UUID NOT NULL,
    role VARCHAR(16) NOT NULL CHECK (role IN ('MEMBER', 'ORGANIZER', 'ORG_ADMIN')),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT organization_membership_org_user_uq UNIQUE (organization_id, user_id)
);

CREATE INDEX organization_membership_user_idx
    ON organizations.organization_membership (user_id);

CREATE TABLE organizations.join_request (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations.organization(id),
    applicant_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX join_request_pending_org_applicant_uq
    ON organizations.join_request (organization_id, applicant_id)
    WHERE status = 'PENDING';

CREATE INDEX join_request_org_status_created_idx
    ON organizations.join_request (organization_id, status, created_at);

CREATE TABLE organizations.event_team_membership (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations.organization(id),
    event_id UUID NOT NULL REFERENCES event_authoring.event(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(8) NOT NULL CHECK (role IN ('EDITOR', 'STAFF')),
    CONSTRAINT event_team_membership_event_user_uq UNIQUE (event_id, user_id)
);

CREATE INDEX event_team_membership_org_user_idx
    ON organizations.event_team_membership (organization_id, user_id);
