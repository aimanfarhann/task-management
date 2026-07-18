-- Tasks and task comments (SCHEMA.md §2.4, §2.5) plus their indexes (§3).
-- Ships in schema v1; the JPA entities and endpoints arrive with milestone M2.
CREATE TABLE tasks (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id  bigint       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    title       varchar(200) NOT NULL,
    description text,
    status      varchar(20)  NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    priority    varchar(20)  NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    due_date    date,
    assignee_id bigint       REFERENCES users (id) ON DELETE SET NULL,
    created_by  bigint       NOT NULL REFERENCES users (id),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE task_comments (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id    bigint      NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    author_id  bigint      NOT NULL REFERENCES users (id),
    body       text        NOT NULL CHECK (length(body) <= 2000),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_project_status ON tasks (project_id, status);
CREATE INDEX idx_tasks_assignee ON tasks (assignee_id) WHERE assignee_id IS NOT NULL;
CREATE INDEX idx_tasks_due_date ON tasks (due_date) WHERE due_date IS NOT NULL;
CREATE INDEX idx_comments_task ON task_comments (task_id);
