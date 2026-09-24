# Third-party dependency review

Dependency changes are reviewed in CI together with the reproducibility policy. The JVM/JS dependency set is declared
in `gradle/libs.versions.toml`; the browser test dependency set is locked in `e2e/package-lock.json`.

CI currently enforces:

- no dynamic, snapshot, or range versions in the Gradle version catalog (`dependencyPolicy`);
- exact npm lockfile resolution (`npm ci`); and
- an npm vulnerability audit with high-severity findings treated as failures (`npm audit --audit-level=high`).

Before adding a dependency, record its license and confirm compatibility with the project distribution terms in the
pull request. This file is the review checkpoint for that decision; it is intentionally not a substitute for legal
advice.
