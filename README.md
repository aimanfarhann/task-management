# TaskFlow

Lightweight task and project management. Spring Boot 3 (Java 21) REST API + Angular / Angular Material SPA + PostgreSQL 16.

Project documentation lives in [context/](context/) — read [RULES.md](context/RULES.md) before contributing:

| Doc | Purpose |
|---|---|
| [PRD.md](context/PRD.md) | Scope, MVP, success metrics |
| [DESIGN.md](context/DESIGN.md) | Visual identity, Material 3 usage, accessibility |
| [ARCHITECTURE.md](context/ARCHITECTURE.md) | System design, layering, folder structure |
| [SCHEMA.md](context/SCHEMA.md) | Database schema, migrations, access control |
| [RULES.md](context/RULES.md) | Hard rules for contributors and AI agents |

## Prerequisites

- Docker (for PostgreSQL and integration tests)
- JDK 21 and Node.js ≥ 20 for native development

Copy the env file first (both flows read it):

```bash
cp .env.example .env        # then set JWT_SECRET (openssl rand -hex 64)
```

### Run the whole stack in Docker

Builds production images (jar on a slim JRE; Angular bundle served by nginx that proxies `/api` to the backend) and runs everything — no local JDK/Node needed. App at http://localhost:4200.

```bash
docker compose up -d --build
```

### Native development (faster inner loop)

Run only PostgreSQL in Docker and the app on the host:

```bash
docker compose up -d postgres
```

**Backend** (http://localhost:8080):

```bash
cd backend
./mvnw spring-boot:run      # reads DB/JWT config from environment — see .env.example
```

**Frontend** (http://localhost:4200):

```bash
cd frontend
npm install
npm start
```

## Verification

```bash
cd backend && ./mvnw verify                        # compile + unit + integration tests
cd frontend && npm run lint && npm test && npm run build
```

Integration tests use Testcontainers — the Docker daemon must be running.
