# Nexus Gateway

A reactive Spring Cloud Gateway server providing API gateway and reverse proxy capabilities for microservices routing. NexusGate is the single entry point for client applications, routing requests to multiple downstream microservices with JWT authentication, service-specific public endpoints, and centralized management endpoint access.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [JWT Authentication](#jwt-authentication)
- [Public Endpoints](#public-endpoints)
- [Management Endpoints](#management-endpoints)
- [Project Structure](#project-structure)
- [Building the Project](#building-the-project)
- [Running the Application](#running-the-application)
- [API Routes](#api-routes)
- [Testing](#testing)
- [Technologies Used](#technologies-used)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## 🎯 Overview

**NexusGate** is the central API Gateway component of the Nexus microservices architecture. It provides:

- **Reverse proxy and request routing** to downstream microservices
- **YAML-based service configuration** for flexible deployment
- **JWT authentication** with configurable identity claims
- **Service-specific public endpoints** with optional JWT verification
- **Centralized management endpoint routing** with per-service overrides
- **Principal propagation** via `X-Principal` headers with spoofing protection

The gateway is built using **Spring Cloud Gateway** with reactive (WebFlux) programming model for handling high-throughput asynchronous requests efficiently.

### ⚠️ Important Note: Responsibility Boundaries

**NexusGate is an API Gateway and reverse proxy.** The example services and ports (Auth Service on port 8081, Product Service on port 8083, Order Service on port 8082) shown throughout this documentation are for development and debugging purposes only. **for development and debugging purposes only**.

**Critical Responsibilities:**

- **NexusGate handles**: NexusGate handles: Request routing, reverse proxying, JWT verification, issuer/audience validation, identity extraction, principal propagation, and centralized gateway-level policies
- **Auth Service handles**: Access token generation, refresh token validation, token expiration management, and new token issuance
- **Your responsibility**: Implement, maintain, and configure the Auth Service according to your security requirements

**NexusGate does NOT generate, issue, or manage tokens.** The Auth Service is a separate microservice that you must develop, deploy, and configure.

## ✨ Features

- **API Gateway & Reverse Proxy**: Non-blocking request routing to multiple microservices
- **Service-Based Routing**: Path-based routing with YAML configuration for each service
- **JWT Authentication**: 
  - Token verification before forwarding protected requests
  - Issuer and audience validation
  - Configurable signing algorithm (HS256, RS256, etc.)
  - Configurable identity claim extraction
- **Public Endpoint Support**: 
  - Service-specific public endpoints with optional JWT authentication
  - Authentication is optional when no Bearer token is supplied
  - Invalid/malformed tokens are rejected with 401 Unauthorized
- **Principal Propagation**: 
  - Authenticated identity forwarded to downstream services via `X-Principal` header
  - Client-supplied `X-Principal` headers removed to prevent spoofing
- **Management Endpoints**: 
  - Centralized routing for service actuator endpoints
  - Global default configuration with per-service overrides
  - Endpoint filtering (health, info, metrics, etc.)
- **Reactive Architecture**: High-performance, non-blocking request processing
- **YAML Configuration**: Externalized service configuration via `nexus.yml`
- **Environment Variable Support**: Secure configuration of secrets like `JWT_SECRET`
- **For Configuration guidance, see [Configuration Guide](./docs/configuration.md)**

## 🏗️ Architecture

### Request Flow

```
Client Request
    ↓
NexusGate Gateway (Port 8080)
    ↓
[Route Matching] - Service path matching (/api/v1/auth/**, /api/v1/product/**, etc.)
    ↓
[Authentication Filter]
  ├─ Is endpoint public? ─→ [No Bearer token?] ─→ Allow through
  │                            ↓ Yes (Bearer provided)
  │                            → Verify JWT
  │
  └─ Is endpoint protected? ─→ Require valid Bearer JWT
                                    ↓
                                [Verify Token]
                                ├─ Valid? ─→ Extract identity claim
                                │              ↓
                                │         Propagate X-Principal header
                                │              ↓
                                │         Forward to service
                                │
                                └─ Invalid? ─→ Return 401 Unauthorized
    ↓
[Management Route Filter]
  └─ Rewrite path: /management/{service}/{endpoint} → /actuator/{endpoint}
    ↓
Downstream Service
    ↓
Response → NexusGate → Client
```

### Key Components

- **Route Builder**: Dynamically creates routes based on service configuration
- **JWT Authentication Filter**: Verifies tokens, validates claims, propagates identity
- **Management Endpoint Filter**: Routes and rewrites management/actuator requests
- **Service Configuration Loader**: Reads YAML configuration with environment variable resolution

## 📦 Prerequisites

- **Java 21** (Required)
- **Gradle 8.x** (Build tool)
- **Git** (Version control)
- **Auth Service**: A separate microservice for token generation and refresh token validation
  - You must implement and deploy this separately
  - Configure the `security.jwt` section in `nexus.yml` to point to your Auth Service
  - See [Auth Service Responsibilities](#auth-service-responsibilities) for details
- Downstream microservices running on configured ports:
  - **Example ports (development only)**:
    - Auth Service on `http://localhost:8081`
    - Order Service on `http://localhost:8082`
    - Product Service on `http://localhost:8083`
  - These are example configurations for local development and debugging
  - Your production setup will have different service URLs and ports

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/ByteGitAccnt/nexus-gateway.git
cd nexus-gateway
```

### 2. Verify Java Installation

Ensure you have Java 21 installed:

```bash
java -version
javac -version
```

### 3. Build the Project

Using the included Gradle wrapper:

```bash
# On Windows
./gradlew.bat build

# On macOS/Linux
./gradlew build
```

This will download dependencies and compile the project.

## ⚙️ Configuration

NexusGate is configured using a YAML file (`nexus.yml`) that defines services, security, and management settings. See [Configuration Guide](./docs/configuration.md) for detailed documentation.

### Minimal Configuration Example

```yaml
services:
  auth:
    url: http://localhost:8081
    path: /api/v1/auth/**

management:
  enabled: true
  basePath: /management
  targetPath: /actuator
  endpoints:
    - health
```

### Complete Configuration Example with JWT and Public Endpoints

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

### Configuration Sections

#### Services

Each service defines:
- **url**: The target URL of the downstream service
- **path**: The request path pattern to match (supports `**` wildcards)
- **publicEndpoints** (optional): List of endpoints that allow optional JWT authentication
- **management** (optional): Overrides global management endpoint configuration

#### JWT Authentication

When enabled, NexusGate verifies JWTs before forwarding protected requests:
- **enabled**: Enable/disable JWT verification
- **algorithm**: Signing algorithm (HS256, RS256, etc.)
- **identityClaim**: JWT claim to extract as the authenticated principal (e.g., `userid`, `sub`)
- **verification.type**: Verification method (`shared-secret`)
- **verification.secret**: Shared secret for HMAC verification (use environment variable like `${JWT_SECRET}`)
- **issuer**: Expected JWT issuer claim
- **audience**: Expected JWT audience claim

#### Management Endpoints

- **enabled**: Enable/disable management endpoint routing
- **basePath**: Base path for management endpoints (e.g., `/management`)
- **targetPath**: Target path in backend services (e.g., `/actuator`)
- **endpoints**: List of allowed management endpoints (global default)

#### Public Endpoints

Public endpoints allow optional JWT authentication:
- If no `Authorization` header is supplied, the request is allowed through
- If a valid `Authorization: Bearer <token>` header is supplied, the JWT is verified and identity is propagated
- If an invalid or malformed Bearer token is supplied, the request is rejected with `401 Unauthorized`

### Environment Variables

Sensitive configuration values should use environment variables:

```yaml
security:
  jwt:
    verification:
      secret: ${JWT_SECRET}
```

Set the environment variable before starting the gateway:

```bash
# On Windows
set JWT_SECRET=your-secret-key

# On macOS/Linux
export JWT_SECRET=your-secret-key

# Then start the application
./gradlew.bat bootRun
```

### Application Properties

Modify `application.properties` or `application.yml` in `src/main/resources/` for application-level configurations like:

```properties
server.port=8080
server.servlet.context-path=/gateway
spring.application.name=nexus-gateway
```

## 📁 Project Structure

```
nexus-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/nexusgate/nexus_gateway/
│   │   │   ├── NexusGatewayApplication.java       # Entry point
│   │   │   ├── config/                            # Configuration classes
│   │   │   ├── filter/                            # Custom gateway filters
│   │   │   ├── controller/                        # REST controllers
│   │   │   └── ...
│   │   └── resources/
│   │       ├── application.yml                    # Spring Boot config
│   │       ├── application-dev.yml                # Development profile
│   │       └── ...
│   └── test/
│       ├── java/com/nexusgate/nexus_gateway/      # Unit & integration tests
│       └── resources/
├── build.gradle                                   # Gradle build configuration
├── settings.gradle                                # Gradle settings
├── nexus.yml                                      # Gateway routing configuration
├── gradle/                                        # Gradle wrapper files
├── gradlew & gradlew.bat                          # Gradle wrapper executables
├── README.md                                      # This file
└── HELP.md                                        # Spring Boot generated help

```

## 🔨 Building the Project

### Development Build

```bash
./gradlew.bat build
```

### Production Build (Skip Tests)

```bash
./gradlew.bat build -x test
```

### Clean Build

```bash
./gradlew.bat clean build
```

### Build Docker Image

```bash
./gradlew.bat bootBuildImage
```

## ▶️ Running the Application

### Development Mode

```bash
# Using Gradle
./gradlew.bat bootRun

# Or run the built JAR
java -jar build/libs/nexus-gateway-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080` (default port).

### With Custom Port

```bash
./gradlew.bat bootRun --args='--server.port=9000'
```

### With Specific Profile

```bash
./gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

## 🛣️ API Routes & Request Handling

### Service Routing

Routes are configured in `nexus.yml` and support path-based matching with wildcard patterns.

#### Auth Service Routes
```
All Methods /api/v1/auth/**      → http://localhost:8081/api/v1/auth/**
```

**Public Endpoints** (optional JWT authentication):
- `/api/v1/auth/login` - Public endpoint, no JWT required
- `/api/v1/auth/register` - Public endpoint, no JWT required
- `/api/v1/auth/refresh` - Public endpoint for refresh token flow (see refresh token section below)

#### Product Service Routes
```
All Methods /api/v1/product/**   → http://localhost:8083/api/v1/product/**
```

**Protected Endpoints** (require valid JWT):
- All product endpoints require Bearer JWT authentication

#### Order Service Routes
```
All Methods /api/v1/order/**     → http://localhost:8082/api/v1/order/**
```

**Protected Endpoints** (require valid JWT):
- All order endpoints require Bearer JWT authentication

### 🔐 JWT Token Lifecycle (Auth Service Responsibility)

**Important:** NexusGate does NOT generate, issue, or manage tokens. Refresh tokens are NOT validated by NexusGate.

Token lifecycle and responsibilities:

```
1. Client → Auth Service (your responsibility to implement)
   POST /api/v1/auth/login with credentials
   ↓
2. Auth Service (YOUR SERVICE, NOT NexusGate)
   - Verifies credentials
   - Creates JWT with claims (userid, username, etc.)
   - Returns access token + refresh token
   ↓
3. Client → NexusGate with Bearer Token
   Authorization: Bearer <access-token-from-auth-service>
   ↓
4. NexusGate (gateway verification only)
   - Verifies token signature and expiration
   - Extracts identity claim
   - Propagates as X-Principal header
   - Forwards to downstream service
   ↓
5. When access token expires → Client contacts Auth Service
   POST /api/v1/auth/refresh
   X-Refresh-Token: <refresh-token>    [Do NOT use Bearer header]
   [Or any custom format your Auth Service defines]
   ↓
6. Auth Service (YOUR SERVICE, NOT NexusGate)
   - Validates refresh token (NexusGate does NOT touch it)
   - Issues new access token
   - Returns new access token to client
   ↓
7. Client → NexusGate with new Bearer Token
   Authorization: Bearer <new-access-token>
   [Back to step 4]
```

**Your responsibility:** 
- Deploy and maintain an Auth Service that issues tokens and handles refresh flows
- Decide how refresh tokens are transmitted (custom header, body, parameter, etc.)
- Do NOT send refresh tokens as Bearer tokens (NexusGate will reject them with 401)

### JWT Authentication Behavior

**Public Endpoints:**
```
No Authorization header          → ✅ Request allowed (no identity propagated)
Authorization: Bearer <valid-jwt>  → ✅ Request allowed + X-Principal header added
Authorization: Bearer <invalid>    → ❌ Request rejected (401 Unauthorized)
```

**Protected Endpoints:**
```
No Authorization header          → ❌ Request rejected (401 Unauthorized)
Authorization: Bearer <valid-jwt>  → ✅ Request allowed + X-Principal header added
Authorization: Bearer <invalid>    → ❌ Request rejected (401 Unauthorized)
```

### Principal Propagation

When a JWT is successfully verified, NexusGate extracts the identity claim (configured as `identityClaim`, default: `userid`) and forwards it downstream:

```http
GET /api/v1/product/items
Authorization: Bearer eyJhbGc...

[NexusGate extracts "userid": 42 from JWT]

Forwarded to downstream service as:
GET /api/v1/product/items
X-Principal: 42
```

The gateway automatically removes any client-supplied `X-Principal` headers to prevent identity spoofing.

### ⚠️ Refresh Token Handling - Important

**Refresh tokens are NOT validated by NexusGate.** Sending a refresh token as a Bearer token will result in `401 Unauthorized`.

**Why?**
- NexusGate only validates **access tokens** (JWTs)
- Refresh tokens have a different format and purpose
- Only your **Auth Service** understands and validates refresh tokens

**Correct refresh token flow:**

```
Client wants a new access token:
  ↓
POST /api/v1/auth/refresh
X-Refresh-Token: <refresh-token>    ← Use custom header
[Or request body, or any format you define]
  ↓
NexusGate allows through (public endpoint, no Bearer validation)
  ↓
Auth Service receives request
  ↓
Auth Service validates refresh token
  ↓
Auth Service issues new access token
  ↓
Client receives new access token
  ↓
Client uses new access token with Bearer header for protected endpoints
```

**❌ INCORRECT (will fail with 401):**
```
POST /api/v1/auth/refresh
Authorization: Bearer <refresh-token>    ← NexusGate will try to verify this as an access token
                                          ↓
                                         Verification fails (invalid access token)
                                          ↓
                                         401 Unauthorized
```

**✅ CORRECT (recommended approaches):**

**Option 1: Custom Header**
```http
POST /api/v1/auth/refresh
X-Refresh-Token: eyJhbGc...refresh-token...
```

**Option 2: Request Body**
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc...refresh-token..."
}
```

**Option 3: Query Parameter**
```http
POST /api/v1/auth/refresh?token=eyJhbGc...refresh-token...
```

Your Auth Service defines how it accepts refresh tokens. **Do NOT send refresh tokens as Bearer tokens through NexusGate.**

## 📊 Management Endpoints

Management endpoints expose service health, configuration, and metrics through a centralized path.

### Accessing Management Endpoints

With the configured `basePath: /management` and `targetPath: /actuator`:

```
Client Request
  ↓
/management/{service}/{endpoint}
  ↓
NexusGate rewrites to
  ↓
/actuator/{endpoint}
  ↓
Downstream service
```

### Example Management Routes

#### Global Endpoints (from global configuration)

```bash
# Health check for Auth service (uses global config)
curl http://localhost:8080/management/auth/health

# Info for Auth service (uses global config)
curl http://localhost:8080/management/auth/info
```

#### Service-Specific Endpoints (overrides global)

```bash
# Health check for Product service (from service-level override)
curl http://localhost:8080/management/product/health

# Metrics for Order service (from service-level override)
curl http://localhost:8080/management/order/metrics

# This request is rejected (info not configured for Product):
curl http://localhost:8080/management/product/info  # ❌ Not allowed
```

### Configuration Precedence

When accessing a management endpoint for a service:

1. If service has `management.endpoints` configured → Use those endpoints only
2. Otherwise → Use global `management.endpoints`

Example:
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

### Health Check Response Example

```bash
curl http://localhost:8080/management/auth/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": { "total": "1000GB", "free": "500GB", "threshold": "10MB" }
    },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

## ✅ Testing

### Run All Tests

```bash
./gradlew.bat test
```

### Run Specific Test Class

```bash
./gradlew.bat test --tests ClassName
```

### Run Tests with Coverage

```bash
./gradlew.bat test --info
```

### Test Structure

Tests are located in `src/test/java/` with the same package structure as main code:

```
src/test/java/com/nexusgate/nexus_gateway/
├── NexusGatewayApplicationTests.java
├── config/ConfigurationTests.java
├── filter/FilterTests.java
└── ...
```

**Test Dependencies:**
- JUnit 5 Platform (via Spring Boot)
- Reactor Test (for reactive testing)
- Spring Boot Test (actuator test utilities)
- Lombok (for test helpers)

## 🛠️ Technologies Used

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Core language |
| Spring Boot | 4.1.0 | Framework foundation |
| Spring Cloud Gateway | 2025.1.2 | API Gateway implementation |
| Spring WebFlux | Latest | Reactive web framework |
| Spring Cloud Dependency Management | 2025.1.2 | Dependency management |
| Project Lombok | Latest | Boilerplate reduction |
| JUnit 5 | Latest | Testing framework |
| Reactor Test | Latest | Reactive testing utilities |
| Gradle | 8.x | Build automation |
| SnakeYAML | Latest | YAML parsing |

## 💻 Development

### IDE Setup

#### IntelliJ IDEA
1. Open the project folder
2. IDEA will auto-detect Gradle build system
3. Install Lombok plugin if prompted
4. Build → Build Project

#### VS Code
1. Install "Extension Pack for Java" (by Microsoft)
2. Open project folder
3. Terminal → Run Build Task (uses Gradle)

### Code Style

- Follow Google Java Style Guide
- Use 4-space indentation
- Lombok annotations for reducing boilerplate
- Reactive programming patterns for WebFlux components

### Debugging

```bash
# Debug mode with breakpoint support
./gradlew.bat bootRun --args='--debug'
```

Connect your IDE debugger to `localhost:5005` for remote debugging.

### Hot Reload (Development)

Enable Spring DevTools for automatic restarts on file changes:

Add to `build.gradle`:
```gradle
developmentOnly 'org.springframework.boot:spring-boot-devtools'
```

## 🤝 Contributing

### Development Workflow

1. Create a feature branch: `git checkout -b feature/gateway-feature`
2. Make changes and test locally
3. Commit with clear messages: `git commit -m "Add gateway feature"`
4. Push to repository: `git push origin feature/gateway-feature`
5. Open a Pull Request with detailed description

### Code Review Checklist

- [ ] Code follows project style guidelines
- [ ] All tests pass: `./gradlew.bat test`
- [ ] New functionality has tests
- [ ] Documentation is updated
- [ ] No hardcoded values or secrets
- [ ] Build succeeds: `./gradlew.bat build`

## 📄 License

This project is part of the Nexus microservices platform. Check the LICENSE file for details.

## 📞 Support

### Issues & Bug Reports

Submit issues on the project repository with:
- Clear title and description
- Steps to reproduce
- Expected vs actual behavior
- Environment details (Java version, OS, etc.)

### Questions & Discussions

- Create a discussion in the repository
- Contact the development team
- Check existing documentation and FAQs

### Useful Links

- [Spring Cloud Gateway Documentation](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux.html)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/4.1.0/reference/)
- [Spring WebFlux Guide](https://docs.spring.io/spring-framework/reference/web-reactive.html)
- [Project Gradle Build](https://docs.gradle.org)

---

**Last Updated:** August 2026

**Project Maintainer:** ByteGitAccnt

**Status:** Active Development
