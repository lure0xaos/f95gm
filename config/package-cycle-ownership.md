# Package-cycle ownership and migration rules

The remaining cycle is currently baseline-tracked at the shared
`f95gm.server.config.logging` root because configuration, request, upstream,
cache, and launch packages still share active contracts. The former application
cycle was reduced by moving route capability ports out of `routes.context`.

Migration rules:

The route application capability contract now lives in
`f95gm.server.application.port`, so application construction no longer owns a
contract declared by `routes.context`.

- New cross-package contracts belong in `kernel`, `domain`, or an explicit
  feature port package.
- Persistence must depend on ports, not application implementations.
- Logging event contracts must not import application services.
- Remove the baseline marker only after `verifyPackageCycles` passes with an
  empty baseline and the full `verifyArchitecture` gate succeeds.
- Existing cycle edges should be migrated one boundary at a time with a
  focused test and an updated architecture check.
