# RULES — TaskFlow

Hard rules for any AI agent (or human) working in this repository. These are **non-negotiable**. If a rule conflicts with a request, stop and surface the conflict instead of silently breaking the rule.

---

## 0. Context Discipline

1. Read `PRD.md`, `ARCHITECTURE.md`, and `SCHEMA.md` before implementing any feature; read `DESIGN.md` before any UI work.
2. If the task is not in the PRD's MVP scope, do not build it — raise it for discussion.
3. If these documents disagree with each other or with the code, stop and ask; do not pick one silently.
4. When a decision changes (scope, schema, patterns), update the relevant context doc **in the same change**.

## 1. Project Scaffolding

5. Repository layout is fixed: `backend/` (Spring Boot, Maven), `frontend/` (Angular), `context/` (docs). Nothing at root beyond these, `docker-compose.yml`, and `README.md`.
6. Backend: package-by-feature under `com.taskflow.*` exactly as in ARCHITECTURE.md §3.2. New features get a new package with the same internal shape (controller, service, repository, entities, `dto/`).
7. Frontend: `core/` / `features/` / `shared/` exactly as in ARCHITECTURE.md §4.1. Standalone components only — **no NgModules**.
8. One class per file (Java) and one component per file (Angular). File name matches the class/component name.
9. Database changes only via new Flyway migrations (SCHEMA.md §5). Never edit a merged migration; never use `ddl-auto` beyond `validate`.

## 2. Naming Conventions

### Java
10. Classes/records: `PascalCase`. Interfaces are plain names (`TaskService`) — no `I` prefix. Implementations only get a suffix if there are ≥ 2 implementations.
11. Methods/fields/params: `camelCase`. Constants: `UPPER_SNAKE_CASE`. No abbreviations except `id`, `url`, `dto`.
12. DTOs: suffix by direction — `TaskDto` (response), `CreateTaskRequest`, `UpdateTaskRequest`. Records, not classes.
13. Tests: `{ClassUnderTest}Test`, methods `methodName_condition_expectedResult`.

### Angular / TypeScript
14. Files: `kebab-case` with type suffix — `task-board.component.ts`, `auth.interceptor.ts`, `task.store.ts`.
15. Components/classes: `PascalCase`; selectors prefixed `tf-` (`tf-task-board`). Signals and methods: `camelCase`.
16. **No `any`.** Model every API payload as an interface in the feature's `models.ts`. `unknown` + narrowing where truly dynamic.

### SQL
17. Tables and columns: `snake_case`, tables plural (`tasks`, `project_members`). Indexes `idx_{table}_{cols}`.

### API
18. Endpoints: `/api/v1/{plural-noun}` per ARCHITECTURE.md §3.5. JSON fields `camelCase`. Enum values `UPPER_SNAKE_CASE` strings.

## 3. Coding Principles

19. **KISS:** simplest working solution first. No speculative abstraction, no pattern until there is a second concrete use.
20. **DRY:** logic written twice gets extracted on the second occurrence — into `common/` (backend) or `shared/` (frontend).
21. **Layering is law:** controllers hold no business logic; services return DTOs, never entities; components never call `HttpClient` directly; repositories are only called by their own feature's service.
22. **Fail fast:** validate at the boundary (Bean Validation / typed forms), throw domain exceptions, never return null or sentinel values for error states. No empty catch blocks, ever.
23. **Authorization first line:** every service method touching project-scoped data starts with an `assert*` check (SCHEMA.md §4). No endpoint ships without an authorization decision.
24. **Semantic naming:** names state intent (`archiveProject`, not `updateFlag`). If a function needs a comment to explain *what* it does, rename or split it.
25. Comments explain **why**, not what. Public Java APIs get Javadoc; complex TS logic gets a brief rationale comment.
26. Delete dead code completely — no commented-out blocks, no `V2` suffixes, no "old" files.
27. Dependencies: standard library / Spring / Angular first. Any new third-party dependency requires a stated justification in the PR description and a comment at the import site.

## 4. Security Rules

28. Secrets (JWT secret, DB credentials) come from environment variables only. Never committed, never logged, never in `application.yml` defaults.
29. Passwords: BCrypt only. Tokens: never logged, never stored raw (refresh tokens hashed — SCHEMA.md §2.6).
30. Every query is parameterized (JPA/Pageable). String-concatenated SQL/JPQL is forbidden.
31. Error responses never leak stack traces, SQL, or internal identifiers beyond resource IDs.

## 5. Testing & Quality Gates

32. Business logic (services, stores) gets unit tests **with** the change, not "later". AAA structure, table-driven where inputs vary.
33. Every API endpoint gets at least: one happy-path test, one validation-failure test, one authorization-failure test (MockMvc).
34. Repository/query tests run against Testcontainers PostgreSQL — not H2.
35. A change is complete only when **all** pass:
    ```bash
    # backend
    ./mvnw verify                 # compiles, tests, fails on warnings
    # frontend
    npm run lint && npm test && npm run build
    ```
36. Frontend formatting: Prettier + ESLint (angular-eslint), zero warnings. Backend: Spotless (google-java-format), applied via `./mvnw spotless:apply` before commit.
37. Never commit with failing tests, skipped tests (`@Disabled`/`xit`) without a linked reason, or lint suppressions (`@SuppressWarnings`, `eslint-disable`) without a justifying comment.

## 6. Workflow Rules

38. Plan before code: for any non-trivial task, state the approach and files to be touched first; get alignment before implementing.
39. Small, focused changes — one feature or fix per change set. Unrelated refactors go in separate changes.
40. Commit messages: imperative mood, `feat|fix|refactor|test|docs|chore: summary` (Conventional Commits).
41. If an unforeseen problem appears mid-implementation (schema doesn't fit, pattern doesn't apply), **stop and discuss** — do not improvise architecture.
