# AgentMeter project rules

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
- Use English product strings. Escape provider text in HTML tooltips. Never print, log, or persist credentials. When a
  provider usage integration requires authentication, read credentials only from that provider's official local store,
  keep them in memory for the minimum authenticated quota request to that provider, and never forward them elsewhere.

## Working on changes

- Preserve existing user edits. Do not reset files, commit, publish, or install plugins unless requested.
- Use IDE semantic refactoring when it supports the sources. If it cannot index this Kotlin project, update source,
  imports, package paths, and plugin registrations together and explain the fallback.
- Use `rg` to find files and references. Keep generated IDE/Gradle directories out of source edits.
- Build the plugin for requested build work and report the resulting artifact. Add/run tests only when requested.
- Default build requests to the Rider plugin only (`buildPlugin`). Build Visual Studio Code or Visual Studio
  packages only when explicitly requested; run `buildAllExtensions` only when all editor packages are explicitly
  requested.
- Document structural changes in `docs/ARCHITECTURE.md` and user-facing changes in `CHANGELOG.md`.
- Report changes, the checks actually performed, and any unresolved limitations clearly.

## Standalone Windows application

- `windows/` is a separate .NET 10 WPF project. Open its `.csproj` in Rider for C# indexing; the Gradle project does
  not include these symbols. Try IDE semantic tools first and explain a source-edit fallback when indexing is absent.
- Keep shared contracts and stable IDs in `Core/`, provider code in `Providers/<Provider>/`, orchestration in
  `Application/`, choice persistence in `Settings/`, and WPF presentation in `UI/`. Window partial files retain their
  existing namespace and XAML event names.
- Full and targeted refreshes share the coordinator's background read/cache/publication path. Preserve cancellation
  generations, selected-provider polling, complete-batch bar reveal, and UI dispatcher delivery.
- Provider activation and bar visibility are independent. Cached numeric values retain original times and never
  establish authentication. Official CLIs retain their own sessions.
- The user explicitly authorized saving entered OpenRouter keys in current-user Windows Credential Manager under
  `AgentMeter/OpenRouter`. This is the specific exception to the no-persistence rule; never put keys in settings,
  usage cache, logs or diagnostics, and preserve the Remove saved key action.
- App/window/tray icons share `Packaging/AgentMeter.svg` and generated ICO frames. Build changes to SVG inputs must
  regenerate artwork. Document limitations of the supported SVG subset.
- Windows builds are explicit: `windows/build.ps1` for the portable EXE and `windows/build-msix.ps1` for the unsigned
  MSIX. Do not run tests without a request. Build success is not live authentication or Store certification.
- Source-only GitHub updates keep the shared version unless a version change is requested. Exclude generated output,
  EXE/MSIX files and personal IDE settings from commits; do not create a release or upload assets without authorization.
