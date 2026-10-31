# CONTRIBUTING

Rules for contributing to NotificationTimer 2.0.

## Workflow
- Branch from `feature/2.0`.
- Keep commits small and focused.
- Use conventional commit messages: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`.
- Zero AI slop in code or docs. Direct, concise, bullet points.

## Architecture Rules
- Read [AGENT.md](AGENT.md) and [docs/architecture.md](docs/architecture.md).
- `:core:domain` is pure Kotlin. No Android imports.
- Only `:core:domain` state machine modifies timer state.
- Features depend only on `:core:domain` and `:core:ui`.
- Compute remaining time from timestamps. Never store ticks.

## Testing & Gate (Mandatory)
Every code change must pass validation before commit:
- **Unit tests**:
  - Add or update tests for any changed logic.
  - Run: `./gradlew test`
- **Maestro E2E tests**:
  - Add or update Maestro flow if UI or notification shade flow touched.
  - Run: `maestro test maestro/<flow>.yaml`
  - Tag in-progress flows with `tags: [wip]`.
- **Quality gate**:
  - Windows: `.\scripts\gate.ps1`
  - Linux/Mac: `./scripts/gate.sh`
  - Gate checks: `spotlessCheck`, `detekt`, `test`, `check`.
  - Fix format issues: `./gradlew spotlessApply`.
- **Code coverage**:
  - Deferred for now. Will be enforced in a future phase.

## Definition of Done
- [ ] Unit tests pass (`./gradlew test`).
- [ ] Maestro E2E test added or updated if UI/notification affected.
- [ ] Quality gate passes clean with zero errors (`.\scripts\gate.ps1` or `./scripts/gate.sh`).
- [ ] No architecture invariants broken.
- [ ] Strings localized in `values/strings.xml`, `values-fr/strings.xml`, `values-es/strings.xml`.
