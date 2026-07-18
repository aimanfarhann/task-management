# PRD — TaskFlow

A lightweight task and project management web application. Teams create projects, add members, and track tasks through a simple workflow.

**Stack:** Spring Boot 3 (Java 21) REST API · Angular 22 + Angular Material SPA · PostgreSQL 16

---

## 1. Problem Statement

Small teams need a simple way to organize work into projects and tasks without the complexity (and cost) of enterprise tools. Existing tools are either too heavy (Jira) or too unstructured (shared docs). TaskFlow targets the middle: structured enough to track work, light enough to adopt in minutes.

## 2. Goals

- **G1** — A team can sign up, create a project, and assign tasks in under 5 minutes.
- **G2** — Every task has a clear owner, status, and priority at all times.
- **G3** — Members see an at-a-glance dashboard of what is assigned to them.
- **G4** — The codebase serves as a clean reference implementation of Spring Boot + Angular best practices.

## 3. Non-Goals (explicitly out of scope)

- Real-time collaboration (WebSockets, live cursors)
- File attachments / uploads
- Email or push notifications
- Third-party integrations (Slack, GitHub, calendar)
- Mobile native apps (responsive web only)
- Multi-tenancy / organizations layer (single workspace)
- Sprints, epics, story points, or any agile ceremony tooling

If a feature is not listed in the MVP below, it is out of scope until this document is updated.

## 4. Users & Roles

| Role | Description |
|---|---|
| **USER** | Default role. Can create projects, join projects, manage tasks in their projects. |
| **ADMIN** | System administrator. All USER abilities plus user management and visibility into all projects. |

Within a project, membership carries a project role:

| Project role | Abilities |
|---|---|
| **OWNER** | Edit/archive project, manage members, all MEMBER abilities. |
| **MEMBER** | Create/edit tasks, comment, change task status/assignee. |

## 5. MVP Scope

### 5.1 Authentication
- Register with email + password (server-side validation, BCrypt hashing)
- Login returns a JWT access token (60 min) + refresh token (7 days)
- Logout invalidates the refresh token
- Route guarding: unauthenticated users only see login/register

### 5.2 Projects
- Create project (name, description, color tag)
- List my projects (owned + member of)
- Edit / archive project (OWNER only)
- Add / remove members by email (OWNER only)

### 5.3 Tasks
- Create task within a project: title, description, priority (LOW/MEDIUM/HIGH), due date, assignee
- Status workflow: `TODO → IN_PROGRESS → DONE` (any direction allowed)
- Board view: three status columns with drag-and-drop
- List view: sortable/filterable table (status, priority, assignee, due date)
- Edit and delete tasks (any project member)
- Comments on tasks (plain text, chronological)

### 5.4 Dashboard
- "My tasks" across all projects, grouped by due date (overdue / today / upcoming)
- Per-project task counts by status

### 5.5 Admin
- List all users, deactivate a user
- No separate admin UI beyond a users page (reuse existing components)

## 6. Technical Requirements

### Functional
- REST API, JSON only, versioned under `/api/v1`
- Stateless backend — all session state in JWT, horizontally scalable
- Database migrations via Flyway; schema is never edited manually
- All input validated server-side (Bean Validation) regardless of client validation

### Performance
- API p95 latency < 300 ms for CRUD endpoints under nominal load
- First contentful paint < 2 s on a mid-range laptop, cold cache
- Task board interactions (drag-drop, status change) reflect optimistically in < 100 ms

### Security
- Passwords: BCrypt, cost factor ≥ 10
- JWT signed HS256 with secret from environment (never committed)
- Authorization enforced in the service layer on every request (see SCHEMA.md §Access Control)
- OWASP basics: parameterized queries only (JPA), CORS locked to the SPA origin, no stack traces in API error responses

### Compatibility
- Evergreen browsers (last 2 versions of Chrome, Firefox, Edge, Safari)
- Responsive from 360 px width up

## 7. Success Metrics

| Metric | Target |
|---|---|
| Time from registration → first task created | < 5 min |
| API p95 latency (CRUD endpoints) | < 300 ms |
| Lighthouse accessibility score | ≥ 90 |
| Backend unit test coverage (service layer) | ≥ 80 % |
| Zero critical/high findings in dependency scan | 0 |
| Uncaught frontend errors per session | < 0.1 |

## 8. Release Plan

1. **M1 — Foundation:** auth end-to-end, project CRUD, DB schema v1
2. **M2 — Core:** task CRUD, board + list views, comments
3. **M3 — Polish:** dashboard, admin page, accessibility pass, performance validation

Each milestone ships behind a working build; no milestone is "done" without tests passing and the checks in RULES.md.
