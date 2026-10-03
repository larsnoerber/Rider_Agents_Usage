# Agents Usage

<p><img src="docs/images/agents-usage-logo.png" alt="Agents Usage Quota Portal logo" width="180"/></p>

Agents Usage, maintained by Lars Nörber, is available for JetBrains IDEs, including IntelliJ IDEA and Rider, and as an
extension for Visual Studio Code. It displays Codex 5-hour and weekly usage, JetBrains AI credits, and GitHub Copilot
quotas in JetBrains IDEs. The VS Code extension displays Codex and GitHub Copilot quota. A native Visual Studio
2022/2026 extension also provides Codex quota in Community, Professional and Enterprise on Windows x64.
The status bar displays `OpenAi | D=78% - W=42%`, `JetBrainAi | 70%`, and `Copilot | 38%` (example balances).
OpenAI and JetBrains AI show remaining quota. Copilot shows consumed quota: 0% unused, 100% exhausted.
Each percentage uses its quota color directly; OpenAI D and W have independent colors, without dots.
Click any provider widget to open **Agents Usage**.
Visible widgets stay together in OpenAI, JetBrains AI, Copilot order when agents are toggled.

For the source layout, read [Code structure](docs/ARCHITECTURE.md). Contributors and agents should start with
[Project rules](AGENTS.md) and [Local development](docs/DEVELOPMENT.md).

> Codex is a trademark of OpenAI. This independent plugin is not affiliated with or endorsed by OpenAI.

## Features

- Separate status indicators for remaining 5-hour and weekly usage.
- Additional JetBrains AI status indicator with the remaining credit percentage when AI Assistant is installed and
  enabled.
- JetBrains AI subscription and top-up credit details in the Tool Window.
- Independent GitHub Copilot status indicator and quota details when the Copilot plugin is installed.
- Subscription plan displayed in every provider view, with Unknown when the provider has not reported its plan.
- Compact colored bars showing remaining OpenAI/JetBrains AI balances and consumed Copilot quota.
- Compact agent cards with identity accents, subscription badges, and four visible quota/reset/report facts.
- Expand **More details** per agent for category balances, full reset dates, refresh interval and availability status.
- Agent checkboxes control visibility in both the overview and status bar; deselected quota reads pause.
- Tool Window with quota, reset time, countdown, and progress details.
- Collapsible weekly quest recap with a provider party, quota forecast, and boss battle; it adapts to the Tool Window
  width and remembers its open or closed state.
- Manual refresh and configurable automatic refresh intervals.
- Automatic discovery of `codex` from the system `PATH`.
- English interface, tooltips, settings, and error messages.

## Usage preview

![Illustrated usage overview for OpenAI, JetBrains AI, and GitHub Copilot](docs/images/agents-usage-overview.png)

Illustrated preview with example balances. Available quotas depend on your subscription and installed provider plugins.
See [Marketplace media](docs/MARKETPLACE.md) for the image files and listing instructions.

## Install

### JetBrains IDEs

1. Download the plugin ZIP from [GitHub Releases](https://github.com/larsnoerber/Rider_Agents_Usage/releases/latest).
2. In your IDE, open **Settings > Plugins**, click the gear icon, and choose **Install Plugin from Disk**.
3. Select the ZIP and restart the IDE if prompted.
4. Open **View > Tool Windows > Agents Usage**, or click the `OpenAi` status bar widget.

Agents Usage uses the plugin ID `io.github.larsnoerber.agentsusage`. Disable or uninstall an earlier installation of
Codex Usage Monitor before installing Agents Usage to avoid duplicate widgets.

The current CLI integration requires Windows and an IntelliJ Platform IDE version 2026.1 or later. Codex CLI must
already be installed and signed in locally.

### Visual Studio Code

Download `agents-usage-vscode-<version>.vsix` from GitHub Releases, then run **Extensions: Install from VSIX...** in
VS Code. The extension requires Codex CLI to be installed and signed in. Open the **Agents Usage** Activity Bar view,
or click its status bar item to see the reported 5-hour and weekly quotas.
Agents Usage displays quota only; install
the [OpenAI Codex extension](https://marketplace.visualstudio.com/items?itemName=openai.chatgpt)
separately to use Codex as a coding agent in VS Code.

### Visual Studio Community, Professional and Enterprise

Build or download `agents-usage-visualstudio-<version>.vsix`, close Visual Studio and open the package with
Visual Studio's VSIX Installer. Then choose **View > Other Windows > Agents Usage**.
Use **Settings** in the window to configure Codex/Copilot visibility, status display, CLI path and refresh interval.
Codex reports remaining quota; GitHub Copilot reports consumption through Visual Studio's existing quota service.
Click the AI icon in the Standard toolbar or status bar to open Usage. Older Copilot builds may not expose quotas.
The build includes [Visual Studio Marketplace upload materials](visualstudio/Marketplace/README.md).
See [Visual Studio setup](visualstudio/README.md).

## Settings

Usage refreshes every 60 seconds by default. Open **Settings > Tools > Agents Usage** to set the CLI path or a refresh
interval between 10 and 3600 seconds. Leave the path empty to discover Codex CLI automatically from `PATH`.

The Tool Window also provides refresh presets of 30 seconds, 1 minute, and 5 minutes, plus a custom interval.
The toolbar stays visible above the agent cards. The **Agent settings** menu and IDE Settings both offer **Visible
agents** checkboxes for OpenAI, JetBrains AI, and GitHub Copilot. Uncheck an agent to remove its overview card and
status widget and pause its background quota reads. Optional providers appear only when their plugins are loaded.
Applying settings updates visibility and refreshes selected providers. Zero quota remains visible.

The same refresh interval applies to all providers. The plugin reads the running AI Assistant quota service and
requests updates through AI Assistant. Sign in to JetBrains AI Assistant to see your balance. Click either status
indicator to open the Tool Window; **Refresh** updates all available providers. The JetBrains AI widget can be toggled
independently in the status bar context menu.

JetBrains AI integration uses an internal AI Assistant API, including its own quota-to-credit conversion. Future AI
Assistant versions may require an integration update. If the API or balance is unavailable, the widget displays `—` with
an explanation instead of a credit amount.

## Build

Use a JDK 25 installation, such as the JetBrains Runtime bundled with a compatible IDE, and set `JAVA_HOME` to its
directory. The plugin targets IntelliJ IDEA 2026.1.2 and compiles to Java 21 bytecode. Its compatibility range starts
at build 261 and has no upper bound; the build verifies against Rider 2026.2.3.1.

```powershell
.\gradlew.bat buildPlugin
```

The installable ZIP is written to `build/distributions/agents-usage-<version>.zip`.
Project identity and the release version are configured in `gradle.properties`.
Both Gradle wrapper scripts are required for builds; see [Build files](docs/DEVELOPMENT.md#gradle-build-files).

To build the VS Code extension, install Node.js 22 or later and run `npm install` from `vscode/`. Then run
`npm run package` in that directory to create `agents-usage-vscode-<version>.vsix`. After installing these npm
dependencies, `.\gradlew.bat buildAllExtensions` builds the JetBrains and VS Code packages. On Windows it also builds
the Visual Studio package when Visual Studio MSBuild is installed.
See [VS Code development](docs/DEVELOPMENT.md#visual-studio-code-extension).

To launch a development IDE:

```powershell
.\gradlew.bat runIde
```

## CLI integration

The plugin invokes the local Codex CLI app-server and requests `account/rateLimits/read`. It does not depend on an
interactive terminal or browser cookies. Changes to this local protocol in future CLI versions may require a plugin
update.

## Privacy

The plugin invokes the locally installed Codex CLI and reads quota information from the installed JetBrains AI
Assistant and GitHub Copilot plugins. Balance refreshes use those plugins' existing provider connections. The plugin
displays this information inside the IDE and does not upload it to any additional server.

## Maintainer and license

Agents Usage is maintained by **Lars Nörber**. Source code and support are available
at [larsnoerber/Rider_Agents_Usage](https://github.com/larsnoerber/Rider_Agents_Usage).

The source code is licensed under the [MIT License](LICENSE). Use of the distributed plugin is governed by
the [End-User License Agreement](EULA.md).

See [CHANGELOG.md](CHANGELOG.md) for release changes.
