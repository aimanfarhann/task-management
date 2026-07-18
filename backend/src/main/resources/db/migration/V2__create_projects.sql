-- Projects and project membership (SCHEMA.md §2.2, §2.3).
CREATE TABLE projects (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       varchar(120) NOT NULL,
    description text,
    color_tag  varchar(20)  NOT NULL,
    archived   boolean      NOT NULL DEFAULT false,
    created_at timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE project_members (
    project_id   bigint      NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id      bigint      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    project_role varchar(20) NOT NULL CHECK (project_role IN ('OWNER', 'MEMBER')),
    joined_at    timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, user_id)
);

CREATE INDEX idx_members_user ON project_members (user_id);
