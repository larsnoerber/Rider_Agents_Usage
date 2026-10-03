# Local development

Use JDK 25. The JetBrains Runtime bundled with a compatible IDE can provide it.
The project targets IntelliJ IDEA 2026.1.2 and produces Java 21 bytecode.
Commands below are run from the repository root; replace the sample JDK path with your own installation.

```powershell
$env:JAVA_HOME = 'C:/path/to/jdk-25'
.\gradlew.bat buildPlugin
```

The installable archive is `build/distributions/agents-usage-<version>.zip`.
Install it through **Settings > Plugins > Install Plugin from Disk**.

To launch the isolated development IDE with the current plugin:

```powershell
.\gradlew.bat runIde
```

The development IDE uses its own sandbox settings. Open a project to see the status widgets.
JetBrains AI and Copilot must be installed and signed in within that instance to expose their optional widgets.

To compile Kotlin without packaging:

```powershell
.\gradlew.bat compileKotlin
```

## Before editing

Read [../AGENTS.md](../AGENTS.md) and [ARCHITECTURE.md](ARCHITECTURE.md).
Keep provider details within their feature folders, and update `plugin.xml` whenever a registered class moves.
Preserve existing user edits and the persisted settings identifiers.

Rider's semantic refactoring requires indexed source files in a loaded solution. If the IDE cannot index this Kotlin
Gradle project, source refactoring must update package declarations, imports, and plugin registrations together.
The Gradle compiler is the source of build diagnostics in that case.

## Provider integrations

### OpenAI Codex

`providers/codex/CodexUsageReader` runs the local `codex app-server --stdio` process.
It initializes the connection, reads account information, and requests `account/rateLimits/read`.
`CodexUsageParser` converts the response to the displayed quota snapshot.
The CLI must be installed and signed in. The current transport uses Windows `where.exe` and `cmd.exe` when needed.
The reader owns the active process and its timeout; disposal also stops child processes.

### JetBrains AI

`providers/jetbrainsai/JetBrainsAiUsageReader` reads the loaded AI Assistant quota service.
Credit conversion comes from AI Assistant itself. The reader isolates subscription metadata lookup, mangled Kotlin
getters, and module class-loader compatibility. API failures become an unavailable balance rather than an invented
quota.

`JetBrainsAiSubscriptionReader` prefers the activation manager's selected license: AI Access exposes the product
display name through `JcpLicense.reported`, while legacy activation exposes `AipLicense.productType` and trial/pack
flags. The auth facade is a compatibility fallback; its generic `JbaiOther` marker is not a concrete subscription.
Only product metadata is read. Neither credentials nor account identifiers are accessed or logged.

### GitHub Copilot

`providers/copilot/GitHubCopilotUsageReader` reads Copilot's quota service and its last report time.
Premium request billing can expose exact counts; credit billing uses the reported remaining percentage.
Only quota categories actually reported by Copilot are shown.

AI Assistant and Copilot APIs are internal and may change with provider updates.
`core/reflection/` caches getter lookup and discovers only loaded provider plugins.
These integrations use the installed plugins' existing connections; no additional usage-reporting server is introduced.

## Checks and delivery

Use the Gradle task appropriate to the requested work and report its actual result.
Do not add or run tests unless requested. There is currently no repository test suite.
Build success checks compilation and packaging; it does not verify live quota APIs, account state, or UI behavior in
Rider.
Do not publish, install, commit, or restart the user's IDE unless requested.
