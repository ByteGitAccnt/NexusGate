# Nexus Gateway

A reactive Spring Cloud Gateway server for microservices routing and orchestration. This API gateway serves as the single entry point for client applications, routing requests to multiple downstream microservices (Auth, Product, and Order services).

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Building the Project](#building-the-project)
- [Running the Application](#running-the-application)
- [API Routes](#api-routes)
- [Management Endpoints](#management-endpoints)
- [Testing](#testing)
- [Technologies Used](#technologies-used)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## 🎯 Overview

**Nexus Gateway** is the central API Gateway component of the Nexus microservices architecture. It provides intelligent request routing, API composition, and cross-cutting concerns like request/response transformation, authentication, and monitoring.

The gateway is built using **Spring Cloud Gateway** with reactive (WebFlux) programming model for handling high-throughput asynchronous requests efficiently.

## ✨ Features

- **Reactive Request Routing**: Non-blocking, high-performance request routing using WebFlux
- **Dynamic Service Discovery**: Routes requests to multiple microservices based on paths
- **Management Endpoints**: Built-in actuator endpoints for health checks and metrics
- **Validation Framework**: Request validation using Spring validation annotations
- **Microservices Integration**: 
  - Auth Service (Port 8081)
  - Product Service (Port 8083)
  - Order Service (Port 8082)
- **Actuator Monitoring**: Health, info, and metrics endpoints for operational visibility
- **YAML Configuration**: Externalized service configuration via `nexus.yml`
- **For Configuration guidance, see [Configuration Guide](./docs/configuration.md)**

## 📦 Prerequisites

- **Java 21** (Required)
- **Gradle 8.x** (Build tool)
- **Git** (Version control)
- Downstream microservices running on configured ports:
  - Auth Service on `http://localhost:8081`
  - Order Service on `http://localhost:8082`
  - Product Service on `http://localhost:8083`

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

### Gateway Configuration (`nexus.yml`)

The gateway routes are configured in `nexus.yml`. Below is the current service configuration:

```yaml
services:
  auth:
    url: http://localhost:8081
    path: /api/v1/auth/**
  
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
```

#### Configuration Details:

- **services**: Defines downstream microservices and their routing rules
- **path**: The URL path pattern to match for routing
- **url**: The target URL where requests will be forwarded
- **management**: Defines which actuator endpoints are exposed per service
- **basePath**: Base path for management endpoints (`/management`)
- **targetPath**: Maps to Spring Boot's actuator (`/actuator`)

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

## 🛣️ API Routes

### Auth Service Routes
```
GET  /api/v1/auth/**           → http://localhost:8081/api/v1/auth/**
POST /api/v1/auth/**           → http://localhost:8081/api/v1/auth/**
```

### Product Service Routes
```
GET    /api/v1/product/**      → http://localhost:8083/api/v1/product/**
POST   /api/v1/product/**      → http://localhost:8083/api/v1/product/**
PUT    /api/v1/product/**      → http://localhost:8083/api/v1/product/**
DELETE /api/v1/product/**      → http://localhost:8083/api/v1/product/**
```

### Order Service Routes
```
GET    /api/v1/order/**        → http://localhost:8082/api/v1/order/**
POST   /api/v1/order/**        → http://localhost:8082/api/v1/order/**
PUT    /api/v1/order/**        → http://localhost:8082/api/v1/order/**
DELETE /api/v1/order/**        → http://localhost:8082/api/v1/order/**
```

## 📊 Management Endpoints

### Base Path: `/management/actuator`

#### Health Check
```bash
curl http://localhost:8080/management/actuator/health
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

#### Application Info
```bash
curl http://localhost:8080/management/actuator/info
```

#### Metrics (Per-Service)
```bash
# Order Service Metrics
curl http://localhost:8080/management/actuator/metrics
```

#### Available Endpoints
- `/management/actuator/health` - Application health status
- `/management/actuator/info` - Application information
- `/management/actuator/metrics` - Performance metrics (for configured services)

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
