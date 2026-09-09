# NexusGate Configuration Guide

NexusGate is configured through a YAML configuration file (`nexus.yml`) that defines:

- Backend services and their routing paths
- JWT authentication and token verification
- Service-specific public endpoints with optional JWT authentication
- Global and per-service management endpoint routing
- Environment variable substitution for sensitive configuration

---

## ⚠️ Critical: Service Responsibility Boundaries

The services and ports shown in this guide (Auth Service on port 8081, Product Service on port 8083, Order Service on port 8082) are **example configurations for development and local debugging only**.

**What NexusGate provides:**
- Token verification
- Issuer and audience validation
- Identity claim extraction and propagation
- Request routing and filtering

**What NexusGate does NOT provide:**
- Token generation
- Access token issuance
- Refresh token creation or management
- Authentication logic

**Your responsibility:**
- Implement an Auth Service that generates access tokens and handles refresh tokens
- Configure `security.jwt` in `nexus.yml` to point to your Auth Service
- Maintain and secure your Auth Service separately
- Ensure your Auth Service issues tokens with the required claims (especially the `identityClaim` configured in NexusGate)
- Manage token expiration and refresh token lifetime in your Auth Service

**Example scenario:**
1. Client authenticates with your Auth Service (not through NexusGate)
2. Auth Service returns an access token
3. Client uses the access token with requests through NexusGate
4. NexusGate verifies the token and propagates the identity
5. If token expires, client contacts Auth Service for refresh token flow
6. Auth Service issues a new token

See [Responsibilities](#responsibilities) section for detailed boundaries.

---

## Table of Contents

1. [Configuration Structure](#configuration-structure)
2. [Complete Configuration Example](#complete-configuration-example)
3. [Services Configuration](#services-configuration)
4. [JWT Authentication](#jwt-authentication)
5. [Public Endpoints](#public-endpoints)
6. [Management Endpoints](#management-endpoints)
7. [Environment Variables](#environment-variables)
8. [Responsibilities](#responsibilities)
9. [Configuration Rules](#configuration-rules)

---

## Configuration Structure

A minimal NexusGate configuration requires:

```yaml
services:
  service-name:
    url: http://target-service-url
    path: /path/pattern/**

management:
  enabled: true
  basePath: /management
  targetPath: /actuator
  endpoints:
    - health

security:
  jwt:
    enabled: true
    algorithm: HS256
    identityClaim: userid
    verification:
      type: shared-secret
      secret: ${JWT_SECRET}
    issuer: http://auth-service
    audience: nexusgate
```

---

## Complete Configuration Example

This example shows all available configuration options:

```yaml
services:
  auth:
    url: http://localhost:8081
    path: /api/v1/auth/**
    publicEndpoints:
      - /login
      - /register
      - /refresh

  product:
    url: http://localhost:8083
    path: /api/v1/product/**
    management:
      endpoints:
        - health

  order:
    url: http://localhost:8082
    path: /api/v1/order/**
    management:
      endpoints:
        - metrics

management:
  enabled: true
  basePath: /management
  targetPath: /actuator
  endpoints:
    - health
    - info

security:
  jwt:
    enabled: true
    algorithm: HS256
    identityClaim: userid
    
    verification:
      type: shared-secret
      secret: ${JWT_SECRET}
    
    issuer: http://localhost:8081
    audience: nexusgate
```

---

## Services Configuration

The `services` section defines the backend microservices that NexusGate routes requests to.

### Service Properties

Each service must define:

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `url` | String | Yes | Base URL of the backend service |
| `path` | String | Yes | Request path pattern to match (supports `**` wildcards) |
| `publicEndpoints` | List | No | Endpoints allowing optional JWT authentication |
| `management` | Object | No | Per-service management endpoint configuration |

### Service Example

```yaml
services:
  product:
    url: http://localhost:8083
    path: /api/v1/product/**
    publicEndpoints:
      - /list
      - /details
    management:
      endpoints:
        - health
```

### Request Routing

A request to `/api/v1/product/list` is routed as follows:

```
Client Request
  ↓
GET /api/v1/product/list (to NexusGate on port 8080)
  ↓
[Route matching] - Matches /api/v1/product/**
  ↓
[Service lookup] - product service
  ↓
[Forwarding] - GET http://localhost:8083/api/v1/product/list
  ↓
Product Service Response
```

---

## JWT Authentication

NexusGate verifies JWT Bearer tokens before forwarding protected requests to downstream services.

**⚠️ IMPORTANT: Refresh tokens are NOT verified by NexusGate.** Do not send refresh tokens as Bearer tokens.

### JWT Configuration Section

```yaml
security:
  jwt:
    enabled: true
    algorithm: HS256
    identityClaim: userid
    
    verification:
      type: shared-secret
      secret: ${JWT_SECRET}
    
    issuer: http://localhost:8081
    audience: nexusgate
```

### Configuration Properties

| Property | Description |
|----------|-------------|
| `enabled` | Enable/disable JWT verification (true/false) |
| `algorithm` | Signing algorithm used by Auth Service (HS256, RS256, etc.) |
| `identityClaim` | JWT claim name to extract as the authenticated principal (e.g., `userid`, `sub`, `user_id`) |
| `verification.type` | Verification method; currently only `shared-secret` is supported |
| `verification.secret` | Shared secret for HMAC verification (use environment variable for security) |
| `issuer` | Expected JWT issuer claim (must match token's `iss` claim) |
| `audience` | Expected JWT audience claim (must match token's `aud` claim) |

### JWT Verification Process

When a request arrives with a Bearer token, NexusGate verifies it as an **access token**:

1. Extract token from `Authorization: Bearer <token>` header
2. Verify token signature using the configured secret and algorithm
3. Validate `issuer` claim matches configured value
4. Validate `audience` claim matches configured value
5. Validate token is not expired
6. Extract identity from the claim specified by `identityClaim`
7. If all validations pass → allow request and propagate identity
8. If any validation fails → return 401 Unauthorized

**Refresh Token Note:**
- Only **access tokens** are verified by NexusGate
- **Refresh tokens are NOT validated by NexusGate**
- Refresh tokens are handled entirely by your Auth Service
- Do NOT send refresh tokens as Bearer tokens (they will fail verification with 401 Unauthorized)
- Refresh tokens can be sent via custom headers, request body, or any method your Auth Service defines

### Identity Claim

The `identityClaim` configuration specifies which JWT claim NexusGate uses as the authenticated principal:

```yaml
security:
  jwt:
    identityClaim: userid
```

If a token contains:

```json
{
  "userid": 42,
  "username": "john_doe",
  "iss": "http://localhost:8081",
  "aud": "nexusgate"
}
```

NexusGate extracts `42` as the principal and propagates it downstream as `X-Principal: 42`.

**Note**: The `identityClaim` value is flexible and depends on what the Auth Service includes in the JWT. Common values include:
- `userid` - User ID (numeric)
- `sub` - Subject (standard JWT claim)
- `user_id` - Alternative user ID format
- `id` - Generic identifier
- `email` - Email address (if used as principal)

### Auth Service vs NexusGate Responsibilities

| Responsibility | Owner | Details |
|---|---|---|
| Issuing access tokens | Auth Service | Creates JWT with claims and determines expiration |
| Token expiration/lifetime | Auth Service | Sets `exp` claim in JWT |
| Refresh token handling | Auth Service | Issues new access token when refresh token is valid |
| Token verification | NexusGate | Validates signature, issuer, audience, expiration |
| Identity extraction | NexusGate | Reads `identityClaim` and propagates as `X-Principal` |
| Scope/permission validation | Downstream Service | Each service validates what the principal can do |

---

## Public Endpoints

Public endpoints are those that allow access without JWT authentication. They are configured at the service level.

### Public Endpoint Configuration

```yaml
services:
  auth:
    url: http://localhost:8081
    path: /api/v1/auth/**
    publicEndpoints:
      - /login
      - /register
      - /refresh
```

### Path Resolution

Public endpoint paths are **relative to the service's configured route path**:

```
Service path:      /api/v1/auth/**
Public endpoint:   /login
Actual URL:        /api/v1/auth/login
```

### Request Behavior

**Scenario 1: Public endpoint, no Bearer token**
```
Request:  GET /api/v1/auth/login
Result:   ✅ Allowed through
Principal: Not set (no X-Principal header)
```

**Scenario 2: Public endpoint, valid Bearer token**
```
Request:  GET /api/v1/auth/login
          Authorization: Bearer eyJhbGc...
Result:   ✅ Allowed through
Principal: ✅ Extracted and forwarded as X-Principal
```

**Scenario 3: Public endpoint, invalid Bearer token**
```
Request:  GET /api/v1/auth/login
          Authorization: Bearer invalid-token
Result:   ❌ Rejected with 401 Unauthorized
```

**Scenario 4: Protected endpoint, no Bearer token**
```
Request:  GET /api/v1/product/items
Result:   ❌ Rejected with 401 Unauthorized
```

**Scenario 5: Protected endpoint, valid Bearer token**
```
Request:  GET /api/v1/product/items
          Authorization: Bearer eyJhbGc...
Result:   ✅ Allowed through
Principal: ✅ Extracted and forwarded as X-Principal
```

### Summary

- **Public endpoints = optional authentication**: Bearer token is optional; if supplied, it must be valid
- **Protected endpoints = required authentication**: Bearer token is mandatory and must be valid
- **Invalid tokens always rejected**: Whether the endpoint is public or protected, an invalid/malformed token results in 401 Unauthorized

### Example: Auth Service Refresh Token Flow

The `/refresh` endpoint is configured as public:

```yaml
services:
  auth:
    publicEndpoints:
      - /refresh
```

**⚠️ CRITICAL: Refresh tokens are NOT validated by NexusGate**

The refresh flow:

```
Client needs new access token
  ↓
POST /api/v1/auth/refresh
X-Refresh-Token: <refresh-token>    [OR any format you define]
  ↓
NexusGate allows through (public endpoint, does NOT validate refresh token)
  ↓
Auth Service (validates refresh token - NexusGate doesn't touch it)
  ↓
Auth Service issues new access JWT
  ↓
Client receives new JWT
  ↓
Client uses new JWT with Bearer header for protected endpoints
  ↓
NexusGate verifies new JWT on protected endpoints
```

**❌ INCORRECT - DO NOT DO THIS:**
```
POST /api/v1/auth/refresh
Authorization: Bearer <refresh-token>
  ↓
NexusGate tries to verify as access token
  ↓
Verification fails (refresh token format doesn't match JWT)
  ↓
401 Unauthorized
```

**✅ CORRECT - Use any of these approaches:**

**Option 1: Custom Header**
```
POST /api/v1/auth/refresh
X-Refresh-Token: <refresh-token>
```

**Option 2: Request Body**
```json
POST /api/v1/auth/refresh
{
  "refreshToken": "<refresh-token>"
}
```

**Option 3: Query Parameter**
```
POST /api/v1/auth/refresh?token=<refresh-token>
```

**Important:** 
- NexusGate does NOT verify refresh tokens
- Do NOT send refresh tokens as Bearer tokens
- Your Auth Service defines the refresh token format and transmission method
- NexusGate only checks if the endpoint is public and allows it through
- Auth Service is responsible for validating the refresh token
- Auth Service sets the expiration of the new access token

---

## X-Principal Header & Spoofing Protection

### Principal Propagation

When a JWT is successfully verified, NexusGate extracts the configured identity claim and forwards it to the downstream service:

```
Client Request
  ↓
Authorization: Bearer eyJhbGc...{userid: 42}...
  ↓
NexusGate extracts userid claim
  ↓
Removes any client-supplied X-Principal header
  ↓
Adds X-Principal: 42 header
  ↓
Forwards to downstream service
```

### Example

```http
Client Request to NexusGate:
POST /api/v1/product/items HTTP/1.1
Authorization: Bearer eyJhbGc...eyJ7IIwidXNlcmlkIjogNDJ9...
X-Principal: 999  (client attempts to spoof)

After NexusGate processing:
POST /api/v1/product/items HTTP/1.1
X-Principal: 42   (replaced with verified identity)
(Authorization header removed)
```

### Spoofing Protection

NexusGate **removes any client-supplied `X-Principal` header** before adding the verified principal value. This ensures:

1. Downstream services can trust the `X-Principal` header
2. Clients cannot impersonate other users by supplying a false `X-Principal`
3. Only NexusGate can set this header (after verifying the JWT)

---

## Management Endpoints

Management endpoints expose service health, configuration, and metrics through a centralized gateway path.

### Global Management Configuration

```yaml
management:
  enabled: true
  basePath: /management
  targetPath: /actuator
  endpoints:
    - health
    - info
```

### Configuration Properties

| Property | Description |
|----------|-------------|
| `enabled` | Enable/disable management endpoint routing |
| `basePath` | Client-facing management path (e.g., `/management`) |
| `targetPath` | Path in backend services (e.g., `/actuator`) |
| `endpoints` | List of allowed management endpoints (global default) |

### Path Rewriting

NexusGate rewrites management requests from the client path to the backend path:

```
Client Request
  ↓
/management/product/health
  ↓
[Management filter]
  ↓
/actuator/health
  ↓
Product Service
```

### Example Management Routes

With the configuration above, these routes are created:

```
/management/auth/health       → http://localhost:8081/actuator/health
/management/auth/info         → http://localhost:8081/actuator/info
/management/product/health    → http://localhost:8083/actuator/health
/management/order/metrics     → http://localhost:8082/actuator/metrics
```

### Per-Service Management Overrides

Services can override the global management configuration:

```yaml
management:
  enabled: true
  basePath: /management
  targetPath: /actuator
  endpoints:
    - health
    - info

services:
  product:
    url: http://localhost:8083
    path: /api/v1/product/**
    management:
      endpoints:
        - health  # Product only exposes health, not info
```

Result:

| Service | Allowed Endpoints |
|---------|-------------------|
| `auth` | health, info (inherits global) |
| `product` | health (overrides global) |
| `order` | health, info (inherits global) |

### Endpoint Filtering

Only configured endpoints are accessible. Requests for non-configured endpoints are rejected:

```
GET /management/product/info
  ↓
Product has override: [health]
  ↓
info is not configured for product
  ↓
❌ Request rejected
```

---

## Environment Variables

Sensitive configuration values (secrets, credentials) should use environment variables for security.

### Setting Environment Variables

#### On Windows (Command Prompt)

```bash
set JWT_SECRET=your-secret-key
./gradlew.bat bootRun
```

Or use the environment variable directly with the Java command:

```bash
java -jar nexus-gateway.jar
```

Windows will automatically use environment variables set via `set`.

#### On macOS/Linux

```bash
export JWT_SECRET=your-secret-key
./gradlew bootRun
```

Or:

```bash
JWT_SECRET=your-secret-key ./gradlew bootRun
```

### YAML Configuration with Environment Variables

```yaml
security:
  jwt:
    verification:
      secret: ${JWT_SECRET}
```

NexusGate replaces `${JWT_SECRET}` with the environment variable value at startup.

### Best Practices

1. **Never commit secrets** - Keep `JWT_SECRET` out of version control
2. **Use strong secrets** - For HMAC (HS256), use a cryptographically strong secret
3. **Rotate secrets** - Change secrets periodically for security
4. **Environment-specific** - Use different secrets in dev, staging, and production
5. **CI/CD integration** - Set environment variables in your deployment pipeline
6. **Documentation** - Document required environment variables (but not their values)

---

## Responsibilities

This section clarifies which component (NexusGate, Auth Service, or Downstream Service) is responsible for each concern.

### NexusGate Responsibilities

**NexusGate is a gateway and token verifier, NOT an authentication provider.**

NexusGate handles:
- Route requests based on configured service paths
- Verify JWT signature, issuer, and audience
- Validate token expiration (reject expired tokens)
- Extract identity from the configured identity claim
- Propagate identity via `X-Principal` header (with spoofing protection)
- Allow/deny requests based on public/protected endpoint configuration
- Return 401 Unauthorized for invalid/missing tokens on protected endpoints
- Rewrite management requests from client path to backend path
- Filter management endpoints based on configuration

**NexusGate does NOT handle:**
- ❌ Generating or issuing tokens
- ❌ Managing refresh tokens
- ❌ User authentication (login/password verification)
- ❌ User registration
- ❌ Token expiration time (determined by Auth Service)
- ❌ Deciding what claims go in tokens
- ❌ Authorization logic (what users can do)

### Auth Service Responsibilities

**The Auth Service is a separate microservice that YOU must implement, deploy, and maintain.**

Auth Service handles:
- User authentication (verify login credentials)
- User registration (create new accounts)
- Issue access tokens (JWTs) with required claims including the configured `identityClaim`
- Set appropriate expiration time for access tokens (`exp` claim)
- Generate refresh tokens for token renewal
- Validate refresh tokens
- Issue new access tokens when refresh tokens are valid
- Determine what claims to include in the JWT
- Implement security policies (password requirements, rate limiting, etc.)

**Example Auth Service flow:**
```
Client POST /api/v1/auth/login
  ↓
Auth Service validates credentials
  ↓
Auth Service creates JWT with claims (including userid, username, etc.)
  ↓
Auth Service returns JWT and refresh token to client
  ↓
Client uses JWT with requests through NexusGate
  ↓
NexusGate verifies JWT and propagates userid
```

### Downstream Service Responsibilities

Each downstream service (Product, Order, etc.) handles:
- Trust the `X-Principal` header as the authenticated principal (NexusGate verified it)
- Implement authorization logic (what the principal can do - e.g., can user access this resource?)
- Implement business logic for that service
- Use the principal for audit logging and service-specific access control
- Not re-verify the JWT (NexusGate already verified it)

### Responsibility Matrix

| Concern | NexusGate | Auth Service | Downstream Service |
|---------|-----------|--------------|-------------------|
| Token verification | ✅ | | |
| Token generation | | ✅ | |
| Refresh token handling | | ✅ | |
| Identity propagation | ✅ | | |
| Authorization/access control | | | ✅ |
| Business logic | | | ✅ |
| User authentication (login) | | ✅ | |
| User registration | | ✅ | |
| Audit logging | | | ✅ |
| Token expiration time | | ✅ | |
| Claim selection | | ✅ | |

---

## Configuration Rules

1. **Every service must define `url` and `path`**
   ```yaml
   services:
     myservice:
       url: http://example.com      # Required
       path: /api/v1/myservice/**   # Required
   ```

2. **Path patterns support wildcards**
   ```yaml
   path: /api/v1/product/**  # Matches all sub-paths
   ```

3. **Service names are identifiers**
   ```yaml
   services:
     product:  # This name is used in management routing
   ```

4. **Public endpoints are relative to service path**
   ```yaml
   path: /api/v1/auth/**
   publicEndpoints:
     - /login  # Actual path: /api/v1/auth/login
   ```

5. **Service-level config overrides global config**
   ```yaml
   management:
     endpoints:
       - health
       - info
   services:
     product:
       management:
         endpoints:
           - health  # Overrides global, only health is allowed
   ```

6. **JWT verification requires all claims to match**
   - Token signature must be valid
   - Token expiration (`exp`) must not be exceeded
   - `iss` claim must match `security.jwt.issuer`
   - `aud` claim must match `security.jwt.audience`
   - Identity claim (e.g., `userid`) must exist in token

7. **Invalid tokens are always rejected**
   - Malformed tokens → 401 Unauthorized
   - Expired tokens → 401 Unauthorized
   - Invalid signature → 401 Unauthorized
   - Missing identity claim → 401 Unauthorized
   - Even on public endpoints, invalid tokens are rejected

---

## Current Implementation Status

NexusGate currently supports:

✅ **Implemented Features**
- Service routing with path patterns
- JWT verification with HS256 algorithm
- Public and protected endpoint configuration
- Identity claim extraction and principal propagation
- X-Principal header spoofing protection
- Global and per-service management endpoint configuration
- Management path rewriting
- Environment variable substitution
- Bearer token validation
- Issuer and audience verification
- Configurable identity claim names

🗺️ **Planned Features** (documented when implemented)
- Rate limiting
- Additional JWT algorithms (RS256, etc.)
- Request/response transformation
- Redis caching
- Analytics and monitoring
- Prometheus metrics
- Circuit breakers
- Additional verification types (public key, etc.)

---

## Additional Resources

- [README.md](../../../README.md) - Overview and getting started
- [nexus.yml](../../../nexus.yml) - Example configuration file
- JWT Specification: [RFC 7519](https://tools.ietf.org/html/rfc7519)
- Spring Cloud Gateway: [Documentation](https://docs.spring.io/spring-cloud-gateway/reference/)