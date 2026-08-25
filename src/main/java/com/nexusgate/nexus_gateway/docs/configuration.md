# NexusGate Configuration

NexusGate is configured through a YAML configuration file.

The configuration defines:

- Backend services and their routes
- Global management endpoint settings
- Per-service management endpoint overrides

---

## Configuration Structure

A basic configuration looks like this:

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
    - info
```

---

# Services

The `services` section defines the backend services that NexusGate routes requests to.

```yaml
services:
  auth:
    url: http://localhost:8081
    path: /api/v1/auth/**

  product:
    url: http://localhost:8083
    path: /api/v1/product/**

  order:
    url: http://localhost:8082
    path: /api/v1/order/**
```

Each service contains:

| Property | Description |
|----------|-------------|
| `url` | Base URL of the backend service |
| `path` | Request path used by NexusGate to route traffic |

### Example

```yaml
services:
  product:
    url: http://localhost:8083
    path: /api/v1/product/**
```

A request matching:

```text
/api/v1/product/**
```

is routed to:

```text
http://localhost:8083
```

The service name (`product`) is used by NexusGate to identify the service.

---

# Management Endpoints

NexusGate can expose management endpoints of backend services through a centralized management path.

```yaml
management:
  enabled: true
  basePath: /management
  targetPath: /actuator
  endpoints:
    - health
    - info
```

## `enabled`

Enables or disables management endpoint routing.

```yaml
enabled: true
```

When enabled, NexusGate creates management routes for configured services.

---

## `basePath`

Defines the management path exposed by NexusGate.

```yaml
basePath: /management
```

For example:

```text
/management/product/health
```

---

## `targetPath`

Defines the management endpoint path used by the backend service.

```yaml
targetPath: /actuator
```

NexusGate rewrites the management request before forwarding it.

For example:

```text
/management/product/health
        ↓
/actuator/health
        ↓
Product Service
```

The client therefore does not need to know the backend service's management endpoint structure.

---

## `endpoints`

Defines the management endpoints allowed by default.

```yaml
endpoints:
  - health
  - info
```

Services without a management override inherit these endpoints.

For example:

```text
/management/auth/health
/management/auth/info
```

are allowed when the `auth` service has no management override.

---

# Per-Service Management Overrides

A service can override the global management endpoint list.

```yaml
services:
  product:
    url: http://localhost:8083
    path: /api/v1/product/**
    management:
      endpoints:
        - health
```

The service now uses:

```text
health
```

instead of the global:

```text
health
info
```

Another example:

```yaml
services:
  order:
    url: http://localhost:8082
    path: /api/v1/order/**
    management:
      endpoints:
        - metrics
```

The order service allows:

```text
/management/order/metrics
```

while an endpoint such as:

```text
/management/order/health
```

is rejected because it is not configured for that service.

---

# Global Defaults and Overrides

The management configuration follows this rule:

```text
Service-specific endpoints
        ↓
   if configured
        ↓
      use them

Otherwise
        ↓
Use global management endpoints
```

For example:

```yaml
management:
  endpoints:
    - health
    - info

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
```

The result is:

| Service | Allowed management endpoints |
|---------|-------------------------------|
| `auth` | `health`, `info` |
| `product` | `health` |

---

# Complete Example

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

## Resulting Routes

### Business Routes

```text
/api/v1/auth/**      → http://localhost:8081
/api/v1/product/**   → http://localhost:8083
/api/v1/order/**     → http://localhost:8082
```

### Management Routes

```text
/management/auth/health
    → http://localhost:8081/actuator/health

/management/auth/info
    → http://localhost:8081/actuator/info

/management/product/health
    → http://localhost:8083/actuator/health

/management/order/metrics
    → http://localhost:8082/actuator/metrics
```

A management endpoint that is not configured for a service is rejected by NexusGate.

---

# Configuration Rules

1. Every service must define a `url`.
2. Every service must define a `path`.
3. Management routing is created only when `management.enabled` is `true`.
4. Global management endpoints act as defaults.
5. A service-level `management.endpoints` list overrides the global list.
6. Management requests are rewritten from the NexusGate management path to the backend's management path.
7. Only configured management endpoints are allowed through NexusGate.

---

# Current Configuration Scope

The current configuration supports:

- Service routing
- Global management configuration
- Per-service management endpoint overrides
- Management path rewriting
- Management endpoint filtering

Additional configuration options will be documented here as NexusGate features are added.