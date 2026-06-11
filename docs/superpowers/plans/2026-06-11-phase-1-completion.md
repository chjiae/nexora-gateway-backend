# Phase 1 Completion Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete all remaining Phase 1 deliverables: tests, documentation, fix tech debt, end-to-end verification.

**Architecture:** The remaining work is grouped into 5 phases: (1) Technical debt fixes (Flyway auto-migration, bcrypt), (2) Backend tests, (3) Documentation, (4) Frontend E2E tests with Playwright, (5) Final verification and delivery summary. Each phase produces independently verifiable results.

**Tech Stack:** JDK 25, Spring Boot 4.0.3, MyBatis 3.0.4, PostgreSQL 17, Redis 7, Vue 3 + Naive UI, Playwright

---

## Phase A: Technical Debt Fixes

### Task A1: Fix Flyway Auto-Migration

**Problem:** Flyway migrations did not run automatically on SB4 startup. The `V1__init_schema.sql` had to be manually executed via `psql`.

**Files:**
- Read: `ai-platform-service/src/main/resources/application.yml`
- Read: `ai-platform-service/src/main/resources/db/migration/V1__init_schema.sql`
- Maybe create: `ai-platform-service/src/main/java/com/nexora/platform/config/FlywayConfig.java`

- [ ] **Step 1: Check Flyway version and SB4 compatibility**

Run: `cd ai-platform-service && mvn dependency:tree | grep flyway`
Expected: Identify exact Flyway version bundled by SB4 starter.

- [ ] **Step 2: Try explicit Flyway configuration**

If Flyway auto-config is broken in SB4, create explicit Flyway bean:

```java
package com.nexora.platform.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class FlywayConfig {
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .cleanDisabled(true)
            .load();
        flyway.migrate();
        return flyway;
    }
}
```

- [ ] **Step 3: Clean database and restart to verify auto-migration**

Run:
```bash
docker exec nexora-postgres psql -U nexora -d nexora_gateway -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
cd ai-platform-service && mvn spring-boot:run
```

Expected: Flyway logs appear, tables created, app starts.

- [ ] **Step 4: Commit**

### Task A2: Fix bcrypt Password Hashing

**Problem:** AuthService uses plain-text password comparison as a workaround. Need proper bcrypt.

**Files:**
- Modify: `ai-platform-service/src/main/java/com/nexora/platform/service/AuthService.java:27`
- Modify: `ai-platform-service/src/main/resources/db/migration/V1__init_schema.sql` (update seed user password hash)

- [ ] **Step 1: Generate a known bcrypt hash for "admin123"**

Use a trusted online bcrypt generator or write a tiny Java program with spring-security-crypto to hash "admin123".

Expected output: `$2a$10$<valid-hash>`

- [ ] **Step 2: Update AuthService to use bcrypt**

```java
// Replace line 27
if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
```

(Keep this line — the `passwordEncoder` field already exists, just wasn't used because of the workaround)

- [ ] **Step 3: Update seed data with the verified hash**

In `V1__init_schema.sql`, replace the hardcoded bcrypt hash with the one generated in Step 1.

- [ ] **Step 4: Test login**

```bash
curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Expected: 200 with token in response.

- [ ] **Step 5: Commit**

---

## Phase B: Backend Tests

### Task B1: ApiKeyService Test

**Files:**
- Create: `ai-platform-service/src/test/java/com/nexora/platform/service/ApiKeyServiceTest.java`

- [ ] **Step 1: Write test for API key creation**

```java
@Test
void createApiKey_shouldReturnRawKeyAndHash() {
    ApiKeyService.CreatedKey result = apiKeyService.createApiKey(1L, "PLATFORM", null, "test-key");
    assertNotNull(result.rawKey());
    assertTrue(result.rawKey().startsWith("sk-nex-"));
    assertEquals("test-key", result.name());
    // Verify key is stored in DB
    AiApiKey stored = apiKeyMapper.findByHash(ApiKeyService.hashKey(result.rawKey()));
    assertNotNull(stored);
    assertEquals("ACTIVE", stored.getStatus());
}
```

- [ ] **Step 2: Write test for key listing**

```java
@Test
void listApiKeys_shouldReturnUserKeys() {
    apiKeyService.createApiKey(1L, "PLATFORM", null, "key1");
    apiKeyService.createApiKey(1L, "PLATFORM", null, "key2");
    List<AiApiKey> keys = apiKeyService.listApiKeys(1L, 1);
    assertFalse(keys.isEmpty());
}
```

- [ ] **Step 3: Write test for key disable**

```java
@Test
void disableApiKey_shouldSetStatusToDisabled() {
    ApiKeyService.CreatedKey key = apiKeyService.createApiKey(1L, "PLATFORM", null, "test");
    apiKeyService.disableApiKey(key.id(), 1L);
    AiApiKey stored = apiKeyMapper.findById(key.id());
    assertEquals("DISABLED", stored.getStatus());
}
```

- [ ] **Step 4: Run tests**

Run: `mvn test -pl ai-platform-service -Dtest=ApiKeyServiceTest`
Expected: PASS for all tests.

- [ ] **Step 5: Commit**

### Task B2: UrlBuilder Edge Case Tests

**Files:**
- Modify: `ai-common/src/test/java/com/nexora/common/util/UrlBuilderTest.java`

- [ ] **Step 1: Add edge case tests**

```java
@Test
void nullBaseUrlShouldNotThrow() {
    assertDoesNotThrow(() -> UrlBuilder.build(null, ProtocolType.OPENAI, UpstreamOperation.CHAT_COMPLETIONS));
}

@Test
void doubleSlashInBaseUrlShouldBeNormalized() {
    String url = UrlBuilder.build("https://api.openai.com//v1/",
            ProtocolType.OPENAI, UpstreamOperation.MODELS);
    assertFalse(url.contains("//"));
}

@Test
void pathPrefixWithSlashesShouldBeNormalized() {
    String url = UrlBuilder.build("https://gateway.example.com", "///custom//prefix///",
            ProtocolType.OPENAI, UpstreamOperation.CHAT_COMPLETIONS);
    assertEquals("https://gateway.example.com/custom/prefix/v1/chat/completions", url);
}

@Test
void anthropicBaseUrlWithV1BuiltIn() {
    // Anthropic's base doesn't have /v1, so we add it
    String url = UrlBuilder.build("https://api.anthropic.com",
            ProtocolType.ANTHROPIC, UpstreamOperation.MESSAGES);
    assertEquals("https://api.anthropic.com/v1/messages", url);
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl ai-common -Dtest=UrlBuilderTest`
Expected: All 15 tests pass (11 original + 4 new).

- [ ] **Step 3: Commit**

### Task B3: RouteResolutionService Test

**Files:**
- Create: `ai-relay-gateway/src/test/java/com/nexora/gateway/service/RouteResolutionServiceTest.java`

- [ ] **Step 1: Write test**

```java
@Test
void resolve_shouldReturnHighestPriorityRoute() {
    // Pre-populate route cache
    List<ResolvedModelRoute> routes = List.of(
        ResolvedModelRoute.builder()
            .modelAlias("gpt-4o").enabled(true).priority(50)
            .upstreamProtocol(ProtocolType.OPENAI)
            .clientProtocolSupport(List.of(ProtocolType.OPENAI))
            .endpoint(ProviderEndpointSnapshot.builder()
                .enabled(true).healthStatus("HEALTHY").build())
            .build(),
        ResolvedModelRoute.builder()
            .modelAlias("gpt-4o").enabled(true).priority(100)
            .upstreamProtocol(ProtocolType.OPENAI)
            .clientProtocolSupport(List.of(ProtocolType.OPENAI))
            .endpoint(ProviderEndpointSnapshot.builder()
                .enabled(true).healthStatus("HEALTHY").build())
            .build()
    );
    routeResolutionService.updateRoutes(Map.of("gpt-4o", routes));
    
    ResolvedModelRoute result = routeResolutionService.resolve("gpt-4o", "OPENAI").block();
    assertEquals(100, result.getPriority());
}

@Test
void resolve_shouldFilterUnhealthyEndpoints() {
    List<ResolvedModelRoute> routes = List.of(
        ResolvedModelRoute.builder()
            .modelAlias("gpt-4o").enabled(true).priority(100)
            .upstreamProtocol(ProtocolType.OPENAI)
            .clientProtocolSupport(List.of(ProtocolType.OPENAI))
            .endpoint(ProviderEndpointSnapshot.builder()
                .enabled(true).healthStatus("UNHEALTHY").build())
            .build()
    );
    routeResolutionService.updateRoutes(Map.of("gpt-4o", routes));
    
    assertThrows(RouteResolutionService.RouteException.class, () ->
        routeResolutionService.resolve("gpt-4o", "OPENAI").block()
    );
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl ai-relay-gateway -Dtest=RouteResolutionServiceTest`
Expected: PASS.

- [ ] **Step 3: Commit**

---

## Phase C: Documentation

### Task C1: Write architecture.md

**Files:**
- Create: `docs/architecture.md`

**Content skeleton:**
```markdown
# Nexora Gateway Architecture

## System Overview
[Two-service design diagram, data flow]

## Module Boundaries
- ai-common: Shared models, enums, events. NOT a service.
- ai-relay-gateway: Hot-path relay. WebFlux + Netty. No DB queries.
- ai-platform-service: All business logic. Spring MVC + MyBatis.

## Request Lifecycle
[Diagram: Client -> relay-gateway -> Redis lookup -> upstream -> SSE -> Redis Pub/Sub -> platform-service -> PostgreSQL]

## Provider / Endpoint / Protocol Design
[Decoupling explanation, three endpoint patterns, cross-protocol routing]

## Event Flow
[Redis Pub/Sub channels, event types, persistence flow]

## Database Design
[ER diagram, 15 tables, key relationships]

## Security
[API Key hashing, Provider key encryption, tenant isolation]
```

- [ ] **Step 1: Write document based on existing code and requirements**

- [ ] **Step 2: Commit**

### Task C2: Write api.md

**Files:**
- Create: `docs/api.md`

**Content:** List all relay-gateway and platform-service endpoints with request/response examples.

- [ ] **Step 1: Write document**

- [ ] **Step 2: Commit**

### Task C3: Write deployment.md

**Files:**
- Create: `docs/deployment.md`

**Content:** Docker Compose setup, environment variables, required infrastructure, service startup sequence.

- [ ] **Step 1: Write document**

- [ ] **Step 2: Commit**

### Task C4: Write redis-pubsub-events.md

**Files:**
- Create: `docs/redis-pubsub-events.md`

**Content:** Channel definitions, event payload schemas, consumer flow, idempotency guarantees.

- [ ] **Step 1: Write document**

- [ ] **Step 2: Commit**

---

## Phase D: E2E Verification with Playwright

### Task D1: Test complete login → dashboard → navigation flow

**Files:**
- Create: `frontend/web-console/tests/e2e/login.spec.ts`

- [ ] **Step 1: Write Playwright E2E test for login**

```typescript
import { test, expect } from '@playwright/test';

test('login flow', async ({ page }) => {
  await page.goto('http://localhost:5173');
  await expect(page).toHaveURL(/\/login/);
  
  await page.getByPlaceholder('Enter username').fill('admin');
  await page.getByPlaceholder('Enter password').fill('admin123');
  await page.getByRole('button', { name: 'Sign In' }).click();
  
  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByText('Overview')).toBeVisible();
});
```

- [ ] **Step 2: Write test for menu navigation**

```typescript
test('sidebar navigation', async ({ page }) => {
  // Login first
  await page.goto('http://localhost:5173/login');
  await page.getByPlaceholder('Enter username').fill('admin');
  await page.getByPlaceholder('Enter password').fill('admin123');
  await page.getByRole('button', { name: 'Sign In' }).click();
  
  // Navigate to Chat
  await page.getByRole('menuitem', { name: 'Chat' }).click();
  await expect(page).toHaveURL(/\/chat/);
  
  // Navigate to Playground
  await page.getByRole('menuitem', { name: 'Playground' }).click();
  await expect(page).toHaveURL(/\/playground/);
  
  // Navigate to API Keys
  await page.getByRole('menuitem', { name: 'API Keys' }).click();
  await expect(page).toHaveURL(/\/api-keys/);
});
```

- [ ] **Step 3: Run E2E tests**

Run: `cd frontend/web-console && npx playwright test`
Expected: All tests pass.

- [ ] **Step 4: Commit**

### Task D2: Test API key create flow with Playwright

**Files:**
- Create: `frontend/web-console/tests/e2e/api-keys.spec.ts`

- [ ] **Step 1: Write test for API key creation and masking**

- [ ] **Step 2: Run and verify**

- [ ] **Step 3: Commit**

---

## Phase E: Final Verification & Delivery

### Task E1: Full build and test verification

- [ ] **Step 1: Full backend build**

Run: `mvn clean test`
Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 2: Frontend build**

Run: `cd frontend/web-console && npm run build`
Expected: Build succeeds, dist/ created.

- [ ] **Step 3: Service startup verification**

Run: `docker compose up -d` then start both services.
Check: `curl http://localhost:8081/actuator/health` and `curl http://localhost:8080/actuator/health` both return 200.

- [ ] **Step 4: Commit final state**

### Task E2: Write final delivery summary

- [ ] **Step 1: Create `docs/phase-1-delivery-summary.md`**

Include: project structure, tech stack, module responsibilities, DB schema, Redis usage, events, UrlBuilder, relay hot-path, known issues, next steps.

---
