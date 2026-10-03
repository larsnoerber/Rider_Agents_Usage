# Agents Usage

Agents Usage is a plugin maintained by Lars Nörber for IntelliJ Platform IDEs, including IntelliJ IDEA and Rider.
It displays Codex 5-hour and weekly usage, JetBrains AI credits, and GitHub Copilot quotas.
The status bar displays `OpenAi D 78% - W 42%`, `JetbrainAi 70%`, and `Copilot 62%` (example balances).
Click any provider widget to open **Agents Usage**.

For the source layout, read [Code structure](docs/ARCHITECTURE.md). Contributors and agents should start with
[Project rules](AGENTS.md) and [Local development](docs/DEVELOPMENT.md).

> Codex is a trademark of OpenAI. This independent plugin is not affiliated with or endorsed by OpenAI.

## Features

- Separate status indicators for remaining 5-hour and weekly usage.
- Additional JetBrains AI status indicator with remaining credits when AI Assistant is installed and enabled.
- JetBrains AI subscription and top-up credit details in the Tool Window.
- Independent GitHub Copilot status indicator and quota details when the Copilot plugin is installed.
- Subscription plan displayed in every provider view, with Unknown when the provider has not reported its plan.
- Compact colored usage bars with remaining balances for all providers.
- Single-line quota rows, compact provider headers, and reset/sync details available on hover.
- Tool Window with quota, reset time, countdown, and progress details.
- Manual refresh and configurable automatic refresh intervals.
- Automatic discovery of `codex` from the system `PATH`.
- English interface, tooltips, settings, and error messages.

## Usage preview

![Illustrated usage overview for OpenAI, JetBrains AI, and GitHub Copilot](docs/images/agents-usage-overview.png)

Illustrated preview with example balances. Available quotas depend on your subscription and installed provider plugins.
See [Marketplace media](docs/MARKETPLACE.md) for the image files and listing instructions.

## Install

1. Download the plugin ZIP from [GitHub Releases](https://github.com/larsnoerber/Rider_Agents_Usage/releases/latest).
2. In your IDE, open **Settings > Plugins**, click the gear icon, and choose **Install Plugin from Disk**.
3. Select the ZIP and restart the IDE if prompted.
4. Open **View > Tool Windows > Agents Usage**, or click the `OpenAi` status bar widget.

Agents Usage uses the plugin ID `io.github.larsnoerber.agentsusage`. Disable or uninstall an earlier installation of
Codex Usage Monitor before installing Agents Usage to avoid duplicate widgets.

The current CLI integration requires Windows and an IntelliJ Platform IDE version 2026.1 or later. Codex CLI must
already be installed and signed in locally.

## Settings

Usage refreshes every 60 seconds by default. Open **Settings > Tools > Agents Usage** to set the CLI path or a refresh
interval between 10 and 3600 seconds. Leave the path empty to discover Codex CLI automatically from `PATH`.

The Tool Window also provides refresh presets of 30 seconds, 1 minute, and 5 minutes, plus a custom interval.
The first provider section is labeled **OpenAI**. The **Agent settings** menu configures the shared refresh interval
for OpenAI, JetBrains AI, and GitHub Copilot. Applying settings refreshes all installed providers.

The same refresh interval applies to all providers. The plugin reads the running AI Assistant quota service and
requests updates through AI Assistant. Sign in to JetBrains AI Assistant to see your balance. Click either status
indicator to open the Tool Window; **Refresh** updates all available providers. The JetBrains AI widget can be toggled
independently in the status bar context menu.

JetBrains AI integration uses an internal AI Assistant API, including its own quota-to-credit conversion. Future AI
Assistant versions may require an integration update. If the API or balance is unavailable, the widget displays `—` with
an explanation instead of a credit amount.

## Build

Use a JDK 25 installation, such as the JetBrains Runtime bundled with a compatible IDE, and set `JAVA_HOME` to its
directory. The plugin targets IntelliJ IDEA 2026.1.2 and compiles to Java 21 bytecode.

```powershell
.\gradlew.bat buildPlugin
```

The installable ZIP is written to `build/distributions/agents-usage-<version>.zip`.

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
