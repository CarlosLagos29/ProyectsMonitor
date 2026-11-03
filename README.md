# Projects Monitor

Uptime monitoring app: register URLs, check them periodically, keep the history and see the status on a dashboard. Built on top of the check engine of [HealthCheckUrl](https://github.com/CarlosLagos29/HealthCheckUrl).

> Work in progress. This README will be completed at the end of the project.

## Stack

- **Back:** Kotlin 2.3, Java 21, Spring Boot 4.1, Gradle 9, PostgreSQL 18 (JPA + Flyway)
- **Front:** Vue 3, TypeScript, Vite
- **Infra:** Docker Compose, GitHub Actions

## Run locally

Requirements: Docker and [Task](https://taskfile.dev).

```bash
cp .env.example .env
task start
```

Front at http://localhost:3000, API at http://localhost:8080 (health check at `/actuator/health`).

## License

[MIT](LICENSE)
