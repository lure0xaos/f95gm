# Lifecycle and infrastructure boundaries

The composition root owns process-wide infrastructure. Route and application code receives ports, not concrete infrastructure implementations.

## Ownership

- `ServerRuntimeServices` owns the lifecycle scope, persistence services, upstream services, caches, and route composition.
- Lifecycle resources close in reverse dependency order. A resource must not depend on a later registration.
- `ServerResources.closeAndJoin()` is the suspend-aware shutdown path: it cancels owned work, waits for cooperative completion, and then closes owned clients.
- Shutdown is idempotent. Repeated close requests must not recreate or close a resource twice.

## Boundary rules

- `TransactionBoundary` is the persistence port; repositories do not depend on `DatabaseExecutor`.
- `UpstreamHttpClient` is the HTTP port; upstream request policy is kept outside route handlers.
- `BrowserLauncher` and `StaticFileSystem` isolate desktop and filesystem effects for tests.
- `WallClock` supplies epoch timestamps. `MonotonicClock` supplies elapsed-time measurements. TTL, retry, rate-limit, and lifecycle decisions must use the appropriate clock domain.

When adding a resource, define its owner, dependencies, close behavior, and test seam at the composition boundary before wiring it into routes.
