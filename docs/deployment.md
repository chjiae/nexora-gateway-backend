# Nexora Gateway Deployment Guide

This document covers how to set up and run the Nexora Gateway platform in a local development environment.

---

## 1. Prerequisites

| Component       | Required Version | Notes                                      |
|-----------------|------------------|--------------------------------------------|
| JDK             | 25               | Java 25 required by Spring Boot 4.0        |
| Maven           | 3.9+             | Multi-module Maven reactor build           |
| Node.js         | 24+              | Required for the Vue 3 frontend            |
| Docker Desktop  | Latest           | For running PostgreSQL and Redis containers |
| PostgreSQL      | 17 (via Docker)  | Managed by Docker Compose                  |
| Redis           | 7 (via Docker)   | Managed by Docker Compose                  |

> **Note:** You do not need to install PostgreSQL or Redis directly on your machine. Both are provided as Docker containers via the included Docker Compose file.

---

## 2. Project Structure Overview

The Nexora Gateway backend is a Maven multi-module project with two runnable services:

| Module               | Type              | Port  | Description                            |
|----------------------|-------------------|-------|----------------------------------------|
| `ai-common`          | Library (JAR)     | N/A   | Shared code: DTOs, enums, utilities    |
| `ai-relay-gateway`   | Executable (JAR)  | 8080  | API relay hot-path (OpenAI/Anthropic)  |
| `ai-platform-service`| Executable (JAR)  | 8081  | Business logic, admin, billing, logs   |

The frontend is a **Git submodule** located at `frontend/web-console`. It is a standalone Vue 3 application that runs on port 5173.

---

## 3. Infrastructure Setup (Docker Compose)

### 3.1 Docker Compose File

The project includes a pre-configured Docker Compose file at `deploy/docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:17-alpine
    container_name: nexora-postgres
    environment:
      POSTGRES_DB: nexora_gateway
      POSTGRES_USER: nexora
      POSTGRES_PASSWORD: nexora_dev_2026
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U nexora -d nexora_gateway"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - nexora-net

  redis:
    image: redis:7-alpine
    container_name: nexora-redis
    command: redis-server --appendonly yes --notify-keyspace-events Ex
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - nexora-net

volumes:
  postgres_data:
  redis_data:

networks:
  nexora-net:
    driver: bridge
```

### 3.2 Start Infrastructure

```bash
# From the project root directory:
docker compose -f deploy/docker-compose.yml up -d
```

This starts PostgreSQL 17 on port 5432 and Redis 7 on port 6377 (inside a Docker network named `nexora-net`). Both containers include health checks.

### 3.3 Verify Infrastructure

```bash
# Check container status
docker compose -f deploy/docker-compose.yml ps

# Check PostgreSQL connectivity
docker exec nexora-postgres pg_isready -U nexora -d nexora_gateway

# Check Redis connectivity
docker exec nexora-redis redis-cli ping
```

Expected output:
- `docker compose ps`: both services show `Up` and `healthy`
- `pg_isready`: `/var/run/postgresql:5432 - accepting connections`
- `redis-cli ping`: `PONG`

### 3.4 Stop Infrastructure

```bash
# Stop containers (preserves data volumes)
docker compose -f deploy/docker-compose.yml down

# Stop containers and remove data volumes (fresh start)
docker compose -f deploy/docker-compose.yml down -v
```

---

## 4. Backend Build

### 4.1 Build All Modules

From the project root directory:

```bash
mvn clean install -DskipTests
```

This builds all three modules in order:

| Module               | Artifact                                                |
|----------------------|---------------------------------------------------------|
| `ai-common`          | `ai-common/target/ai-common-0.1.0-SNAPSHOT.jar`         |
| `ai-relay-gateway`   | `ai-relay-gateway/target/ai-relay-gateway-0.1.0-SNAPSHOT.jar` |
| `ai-platform-service`| `ai-platform-service/target/ai-platform-service-0.1.0-SNAPSHOT.jar` |

### 4.2 Build Individual Modules

```bash
# Build only the relay gateway (will also build ai-common dependency):
mvn clean install -pl ai-relay-gateway -am -DskipTests

# Build only the platform service:
mvn clean install -pl ai-platform-service -am -DskipTests
```

The `-am` flag (also-make) ensures the `ai-common` dependency is built first.

---

## 5. Service Startup

Both services must be running simultaneously. Start them in separate terminal windows.

### 5.1 Start Platform Service (Port 8081)

```bash
# Terminal 1
cd ai-platform-service
mvn spring-boot:run
```

This service handles all business logic: user/tenant management, API keys, provider/endpoint/route configuration, billing, call logs, and the admin API. It connects to PostgreSQL and Redis locally.

Key configurations in `ai-platform-service/src/main/resources/application.yml`:
- Server port: 8081
- PostgreSQL: `jdbc:postgresql://localhost:5432/nexora_gateway`
- Redis: `localhost:6379`
- Flyway migrations: enabled, runs on startup
- MyBatis: enabled with underscore-to-camelCase mapping

### 5.2 Start Relay Gateway (Port 8080)

```bash
# Terminal 2
cd ai-relay-gateway
mvn spring-boot:run
```

This service handles the API relay hot-path: OpenAI/Anthropic compatible endpoints, API key authentication, model routing, SSE streaming, and Redis Pub/Sub event publishing.

Key configurations in `ai-relay-gateway/src/main/resources/application.yml`:
- Server port: 8080
- Web application type: reactive (Spring WebFlux)
- Redis: `localhost:6379` (Lettuce client with connection pooling)

### 5.3 Startup Order

The recommended startup order is:

1. Start Docker Compose (PostgreSQL + Redis) — wait for `healthy` status
2. Start `ai-platform-service` (port 8081) — initializes database schema via Flyway
3. Start `ai-relay-gateway` (port 8080)

The platform service should start first because its Flyway migrations create the database tables and seed data that the relay gateway depends on at runtime.

---

## 6. Frontend Setup

The frontend is maintained in a separate Git repository and integrated as a Git submodule at `frontend/web-console`.

### 6.1 Initialize the Submodule

If you cloned the repository without `--recurse-submodules`:

```bash
# From the project root:
git submodule update --init --recursive
```

This pulls the frontend source code from the remote repository into `frontend/web-console`.

If the submodule URL needs to be updated, edit `.gitmodules` and run `git submodule sync`.

### 6.2 Install Dependencies

```bash
cd frontend/web-console
npm install
```

### 6.3 Start the Dev Server

```bash
npm run dev
```

The Vite dev server starts on **port 5173** with proxy rules configured for backend APIs:

| Path    | Proxied To              | Service          |
|---------|-------------------------|------------------|
| `/api`  | `http://localhost:8081` | Platform Service |
| `/v1`   | `http://localhost:8080` | Relay Gateway    |

### 6.4 Build for Production

```bash
cd frontend/web-console
npm run build
```

The production build output is written to `frontend/web-console/dist/`.

---

## 7. Environment Variables

All configuration properties can be overridden via environment variables or command-line arguments.

### 7.1 Platform Service (`ai-platform-service`)

| Property                          | Env Variable                        | Default Value                                      |
|-----------------------------------|-------------------------------------|----------------------------------------------------|
| `server.port`                     | `SERVER_PORT`                       | `8081`                                             |
| `spring.datasource.url`           | `SPRING_DATASOURCE_URL`             | `jdbc:postgresql://localhost:5432/nexora_gateway`  |
| `spring.datasource.username`      | `SPRING_DATASOURCE_USERNAME`        | `nexora`                                           |
| `spring.datasource.password`      | `SPRING_DATASOURCE_PASSWORD`        | `nexora_dev_2026`                                  |
| `spring.data.redis.host`          | `SPRING_DATA_REDIS_HOST`            | `localhost`                                        |
| `spring.data.redis.port`          | `SPRING_DATA_REDIS_PORT`            | `6379`                                             |
| `spring.data.redis.timeout`       | `SPRING_DATA_REDIS_TIMEOUT`         | `5000ms`                                           |
| `spring.flyway.enabled`           | `SPRING_FLYWAY_ENABLED`             | `true`                                             |
| `spring.flyway.locations`         | `SPRING_FLYWAY_LOCATIONS`           | `classpath:db/migration`                           |
| `spring.flyway.clean-disabled`    | `SPRING_FLYWAY_CLEAN_DISABLED`      | `true`                                             |
| `mybatis.configuration.log-impl`  | `MYBATIS_CONFIGURATION_LOG_IMPL`    | `org.apache.ibatis.logging.stdout.StdOutImpl`      |

### 7.2 Relay Gateway (`ai-relay-gateway`)

| Property                          | Env Variable                        | Default Value                                      |
|-----------------------------------|-------------------------------------|----------------------------------------------------|
| `server.port`                     | `SERVER_PORT`                       | `8080`                                             |
| `spring.data.redis.host`          | `SPRING_DATA_REDIS_HOST`            | `localhost`                                        |
| `spring.data.redis.port`          | `SPRING_DATA_REDIS_PORT`            | `6379`                                             |
| `spring.data.redis.timeout`       | `SPRING_DATA_REDIS_TIMEOUT`         | `3000ms`                                           |

### 7.3 Override Examples

```bash
# Run platform service on a different port with a different DB password:
cd ai-platform-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9091 --spring.datasource.password=my_password"

# Run relay gateway with a remote Redis:
cd ai-relay-gateway
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.data.redis.host=redis.example.com --spring.data.redis.port=6380"
```

Using environment variables:

```bash
# On Windows (PowerShell):
$env:SPRING_DATASOURCE_PASSWORD = "custom_password"
cd ai-platform-service
mvn spring-boot:run
```

---

## 8. Verification

After starting all services, run the following health checks to confirm everything is operational.

### 8.1 Platform Service Health

```bash
curl http://localhost:8081/actuator/health
```

Expected response (HTTP 200):

```json
{"status": "UP"}
```

### 8.2 Relay Gateway Health

```bash
curl http://localhost:8080/actuator/health
```

Expected response (HTTP 200):

```json
{"status": "UP"}
```

### 8.3 Frontend

```bash
curl http://localhost:5173
```

Expected: Returns the Vue 3 application HTML. Open this URL in a browser to access the Nexora Gateway web console.

### 8.4 End-to-End Quick Test

```bash
# Test the platform login endpoint
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Test the relay gateway OpenAI-compatible endpoint
curl http://localhost:8080/v1/models \
  -H "Authorization: Bearer <your-api-key>"
```

---

## 9. Default Login

A platform super admin user is created automatically by the Flyway migration seed data:

| Field    | Value                       |
|----------|-----------------------------|
| Username | `admin`                     |
| Password | `admin123`                  |
| Email    | `admin@nexora.io`           |
| Role     | Platform Super Admin        |

Log in at `http://localhost:5173` using these credentials.

> **Security note:** These are development credentials. Change the password immediately for any non-local deployment.

---

## 10. Troubleshooting

### 10.1 Port 8080 Already in Use

**Symptom:** `ai-relay-gateway` fails to start with `Web server failed to start. Port 8080 was already in use.`

**Resolution (Windows):**

```powershell
# Find the process using port 8080
netstat -ano | findstr :8080

# Kill the process (replace <PID> with the actual process ID)
taskkill /PID <PID> /F
```

Alternatively, start the relay gateway on a different port:

```bash
cd ai-relay-gateway
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
```

### 10.2 Port 8081 Already in Use

Same steps as above but for port 8081:

```powershell
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### 10.3 PostgreSQL Connection Refused

**Symptom:** Platform service fails with `Connection refused` to PostgreSQL.

**Resolution:**

```bash
# Check Docker containers are running
docker compose -f deploy/docker-compose.yml ps

# If stopped, restart them
docker compose -f deploy/docker-compose.yml up -d

# Verify PostgreSQL is accepting connections
docker exec nexora-postgres pg_isready -U nexora -d nexora_gateway
```

If the container exists but won't start, check logs:

```bash
docker compose -f deploy/docker-compose.yml logs postgres
```

### 10.4 Redis Connection Refused

**Symptom:** Either service fails to connect to Redis.

**Resolution:**

```bash
# Check Redis container status
docker compose -f deploy/docker-compose.yml ps redis

# Verify Redis is responsive
docker exec nexora-redis redis-cli ping

# Check Redis logs
docker compose -f deploy/docker-compose.yml logs redis
```

### 10.5 Flyway Migration Issues

**Symptom:** Platform service fails with `Flyway migration error` or schema mismatch.

**Resolution:**

1. Check Flyway configuration in `ai-platform-service/src/main/resources/application.yml`:
   - `spring.flyway.enabled` should be `true`
   - `spring.flyway.locations` should be `classpath:db/migration`
   - `spring.flyway.clean-disabled` should be `true` (prevents accidental schema drops)

2. Verify migration files exist at:
   ```
   ai-platform-service/src/main/resources/db/migration/
   ```

3. If you need a fresh database:
   ```bash
   # Remove and recreate the database volume
   docker compose -f deploy/docker-compose.yml down -v
   docker compose -f deploy/docker-compose.yml up -d
   ```

4. Check Flyway migration status:
   ```bash
   cd ai-platform-service
   mvn flyway:info -Dflyway.url=jdbc:postgresql://localhost:5432/nexora_gateway -Dflyway.user=nexora -Dflyway.password=nexora_dev_2026
   ```

### 10.6 Frontend Build Errors

**Symptom:** `npm install` fails or `npm run dev` shows build errors.

**Resolution:**

1. Ensure Node.js version is 24+:
   ```bash
   node --version
   ```

2. Delete `node_modules` and reinstall:
   ```bash
   cd frontend/web-console
   rm -rf node_modules package-lock.json
   npm install
   ```

3. Clear Vite cache:
   ```bash
   cd frontend/web-console
   rm -rf node_modules/.vite
   npm run dev
   ```

4. Verify TypeScript compilation:
   ```bash
   cd frontend/web-console
   npx vue-tsc --noEmit
   ```

### 10.7 Frontend Cannot Reach Backend

**Symptom:** API calls from the frontend fail with network errors.

**Resolution:**

1. Ensure both backend services are running on their expected ports
2. Verify Vite proxy configuration in `frontend/web-console/vite.config.ts`:
   - `/api` should proxy to `http://localhost:8081`
   - `/v1` should proxy to `http://localhost:8080`
3. Restart the Vite dev server after making proxy changes

### 10.8 Java Version Mismatch

**Symptom:** Maven build fails with Java version errors.

**Resolution:**

```bash
# Check your Java version
java --version

# Expected: Java 25
# If different, set JAVA_HOME:
# Windows (PowerShell):
$env:JAVA_HOME = "C:\path\to\jdk-25"
```

### 10.9 Maven Build Failures

**Symptom:** `mvn clean install` fails with dependency resolution errors.

**Resolution:**

1. Clear Maven local cache for Nexora artifacts:
   ```bash
   rm -rf ~/.m2/repository/com/nexora
   ```

2. Rebuild with verbose output:
   ```bash
   mvn clean install -DskipTests -X
   ```

3. Build modules one at a time to isolate the issue:
   ```bash
   mvn clean install -pl ai-common -DskipTests
   mvn clean install -pl ai-platform-service -DskipTests
   mvn clean install -pl ai-relay-gateway -DskipTests
   ```

---

## 11. Quick Start Summary

For a fresh checkout, here is the complete startup sequence:

```bash
# 1. Start infrastructure
docker compose -f deploy/docker-compose.yml up -d

# 2. Wait for healthy status
docker compose -f deploy/docker-compose.yml ps

# 3. Build backend
mvn clean install -DskipTests

# 4. Start Platform Service (Terminal 1)
cd ai-platform-service && mvn spring-boot:run

# 5. Start Relay Gateway (Terminal 2)
cd ai-relay-gateway && mvn spring-boot:run

# 6. Initialize and build frontend (Terminal 3)
git submodule update --init --recursive
cd frontend/web-console
npm install
npm run dev

# 7. Open browser
# http://localhost:5173
# Login: admin / admin123
```

---

## 12. Production Notes

This guide covers local development deployment. For production deployments, additional steps are required:

- **Secrets management:** Do not store database passwords in `application.yml`. Use environment variables, Spring Cloud Config, or a secrets manager.
- **Provider API keys:** Encrypt provider API keys using AES-256 or a KMS; do not hardcode plaintext keys.
- **HTTPS:** Terminate TLS at a reverse proxy (Nginx, Caddy) in front of both services.
- **Database backup:** Set up automated `pg_dump` backups for the PostgreSQL data volume.
- **Monitoring:** Enable Spring Boot Actuator metrics and integrate with Prometheus/Grafana.
- **Log aggregation:** Ship logs to a centralized system (ELK, Loki, Datadog).
- **Frontend:** Serve the built `dist/` folder via Nginx or a CDN instead of the Vite dev server.

These topics will be covered in a separate production operations guide.
