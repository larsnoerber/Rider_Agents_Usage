# Agents Usage project rules

These rules apply to this repository. Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
before changing package boundaries and [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for local commands.

## Structure and responsibilities

- Keep provider models, readers, services, and provider-specific UI together in `providers/<provider>/`.
- Put reusable contracts, formatting, polling, and reflection in `core/`. Core must not import providers, UI, or
  settings.
- Keep cross-provider user actions in `application/UsageRefreshCoordinator.kt`.
- `settings/` stores user choices. Apply running-service changes through the coordinator, not the settings model.
- `ui/components/` contains reusable Swing components; `ui/toolwindow/` composes views and navigation.
- Keep refresh controls in `ui/settings/`. Provider panels own their subscriptions and timers.
- Match Kotlin packages to directories. After moving extension classes, update `META-INF/plugin.xml`.
- Prefer small files with one clear responsibility. Share actual repeated behavior; avoid abstractions for a single
  call.

## Runtime and compatibility

- Run CLI/process and provider work outside the Swing event dispatch thread. Notify UI listeners on the EDT.
- Remove listeners and stop timers/processes in `dispose()`. Prevent refreshes after disposal.
- Read optional providers only when their plugins are loaded. Do not add a required AI Assistant or Copilot dependency.
- Isolate internal provider API reflection in readers and `core/reflection/`. Handle unavailable APIs explicitly.
- Preserve the plugin ID, widget IDs, Tool Window ID, and persisted settings identifiers unless migration is requested.
- Keep the requested status format: `OpenAi | D=<percent>% - W=<percent>%`, `JetBrainAi | <percent>%`, and
  `Copilot | <percent>%`. Keep compact usage bars and visible subscription plans.
- Color percentage text directly with its quota color; do not show usage dots in status widgets.
- Respect persisted agent checkboxes in both overview and status widgets, and pause deselected provider reads.
- OpenAI and JetBrains AI show remaining quota; Copilot shows consumed quota (0% unused, 100% exhausted).
  Copilot bars grow with consumption; warning colors reflect how close the quota is to exhaustion.
- Use English product strings. Escape provider text in HTML tooltips. Do not read or log credentials.

## Working on changes

- Preserve existing user edits. Do not reset files, commit, publish, or install plugins unless requested.
- Use IDE semantic refactoring when it supports the sources. If it cannot index this Kotlin project, update source,
  imports, package paths, and plugin registrations together and explain the fallback.
- Use `rg` to find files and references. Keep generated IDE/Gradle directories out of source edits.
- Build the plugin for requested build work and report the resulting artifact. Add/run tests only when requested.
- Document structural changes in `docs/ARCHITECTURE.md` and user-facing changes in `CHANGELOG.md`.
- Report changes, the checks actually performed, and any unresolved limitations clearly.
