# Projects Monitor

Uptime monitoring app: register URLs, check them periodically, keep the history and see the status on a dashboard (current status, 24 h uptime and latency chart). Built on top of the check engine of [HealthCheckUrl](https://github.com/CarlosLagos29/HealthCheckUrl), ported in-process into the back.

It is a personal tool to watch my own projects, open to visitors as a demo.

- **Live demo:** https://proyectsmonitor.netlify.app
- **API docs (Swagger):** https://proyectsmonitor.onrender.com/docs

> The back runs on Render's free plan and sleeps when idle, so the first visit may show a "waking up the server" screen for up to a minute.

## Features

- **Two roles, both from `/login`:**
  - **Guest:** no sign-up. The "Start shift" button issues a 12 h token. A guest can browse the public monitors and create one monitor of their own, which is deleted with its history when the token expires.
  - **Admin:** a single user defined by environment variables (no users table). Unlimited monitors, marks each one as public or private and manages the guests' monitors.
- **Scheduled checks** every 10 minutes with a 10 s timeout, plus a rate-limited "check now".
- **Check results:** `UP`, `DOWN` (timeout, DNS, connection, TLS, 4xx, 5xx, too many redirects), `BLOCKED` (401 / 403) and `REJECTED` (private addresses or ports other than 80 and 443, blocked by the SSRF protection).
- **Dashboard:** status per monitor, uptime and a latency sparkline over the last 24 h, and a detail view with a latency chart and the list of checks.
- **Polite to third parties:** identical URLs are checked once per tick, a per-host cap and a 9 min result cache.
- **Stable errors:** `ProblemDetail` responses with a `reason` field (`SESSION_EXPIRED`, `MONITOR_LIMIT_REACHED`, `RATE_LIMITED`...), and `Retry-After` on `429` and `503`.
- English and Spanish UI, with a single dark theme ("Night shift").

## Stack

- **Back:** Kotlin 2.3, Java 21, Spring Boot 4.1, Gradle 9, PostgreSQL 18 (JPA + Flyway), OkHttp, Bucket4j, Caffeine, JWT (Spring Security OAuth2 Resource Server)
- **Front:** Vue 3 (Options API), TypeScript, Vite, Pinia, vue-router, vue-i18n, Tailwind CSS 4, hand-made SVG charts
- **Infra:** Docker Compose, GitHub Actions (one workflow per folder, with ktlint, ESLint and Prettier)

## Deployment

All on free plans:

| Piece | Where |
|---|---|
| Front | Netlify (static build) |
| Back | Render (Docker, 512 MB) |
| Database | Neon (Postgres) |
| Wake-up | cron-job.org hits `/actuator/health` every hour |

The back sleeps after ~15 min without traffic. The front wakes it up when someone opens the app and keeps it awake while the tab is open. Without visitors, the hourly cron wakes it so the scheduler runs the pending checks. The history therefore has gaps at night, and the uptime is computed over the checks that exist ("based on N checks"). The database keeps only the last 24 h of results.

## Run locally

Requirements: Docker and [Task](https://taskfile.dev).

```bash
cp back/.env.example back/.env
cp front/.env.example front/.env
task start
```

| Service | URL |
|---|---|
| Front (Vite dev server) | http://localhost:3000 |
| Back | http://localhost:8080 (Swagger at `/docs`, health at `/actuator/health`) |
| Postgres | `localhost:5432` |

Other tasks: `task logs`, `task stop`, `task build:front` (production build served by nginx), `task lint` and `task format`. Run `task --list` for the full list.

To work outside Docker: JDK 21 for the back (`./gradlew bootRun` inside `back/`) and Node 24 for the front (`npm install && npm run dev` inside `front/`).

## Project structure

```
.
├── back/                 Kotlin + Spring Boot API
├── front/                Vue 3 + TypeScript SPA
├── .github/workflows/    back.yml and front.yml, filtered by path
├── docker-compose.yml    postgres + back + front
└── Taskfile.yml          global commands
```

## Credits

- Background sound (`front/public/vhs-static.mp3`, trimmed and amplified): "VCR Tape Hiss Sound Effect VHS Camera Buzz 80's & 90's Home Video" by [audioman5000](https://www.youtube.com/@audioman5000) ([video](https://www.youtube.com/watch?v=aYaafk74CZU)).
- The screamer GIF and scream (`front/public/jumpscare/`) are a nod to *Five Nights at Freddy's*. This project is not affiliated with it.
- Fonts under the SIL Open Font License (licenses in `front/public/fonts/licenses/`).

## License

[MIT](LICENSE)
