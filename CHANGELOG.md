# Changelog

## 1.1.0 — First stable Marketplace release

- Monitor OpenAI Codex, JetBrains AI, GitHub Copilot, Claude and Cursor in the usage overview and status bar.
- Show subscription plans, provider-reported quotas, reset countdowns and local usage charts; click a badge to toggle details.
- Configure provider visibility and refresh intervals. New Rider installations enable only JetBrains AI.
- Optional weekly insights, quota forecasts, boss battles with visible hits, and Tic-Tac-Toe; Weekly and Games are off by default.
- Use existing local provider sign-ins without telemetry or credential storage in plugin settings.

## Development previews

Versions below document development builds before the first stable Marketplace release.

## 1.0.19

- Reuse ACP sign-ins for Copilot quotas, Cursor usage and Cline account credits. Show Claude's API connection or
  local tokens when subscription quotas do not apply.
- Default new Rider installations to JetBrains AI only, with Weekly and Games disabled. Preserve saved choices.
- Hide missing agents from Usage and the status bar; show missing ACP package information in configuration.
  Remove installation buttons.
- Toggle provider details and usage charts by clicking the subscription badge; remove the separate details link.
- Visualize boss hits with the provider and points, an impact flash and shake. Persist damage and count each observed
  quota point as 4 damage points without replaying repeated refreshes.
- Add Tic-Tac-Toe with saved scores and configurable Games visibility.
- Fix the usage chart initialization crash and reading Cline's local session history.

## 1.0.18

- Improve wrapping of weekly forecasts, battle events and help text in narrow Tool Windows.

## 1.0.17

- Preserve weekly boss progress across plugin updates and restarts.
- Refresh selected providers when Usage opens and improve Marketplace previews.

## 1.0.16

- Add collapsible weekly quests with rotating bosses, quota forecasts, provider parties and reset celebrations.
- Add Quota Portal branding and improve the weekly view at narrow widths.

## 1.0.15

- Change the Visual Studio extension identity for the new Marketplace listing. Uninstall version 1.0.14 or earlier
  before installing this version.

## 1.0.14

- Add Visual Studio Copilot quotas, subscription plans and reset times.
- Add the Visual Studio toolbar icon, clickable status indicator and Marketplace package.

## 1.0.13

- Add a native Visual Studio 2022/2026 extension with Codex quotas and provider settings.
- Fix Visual Studio packaging and Marketplace publisher identifiers.

## 1.0.12

- Add the packaged VS Code extension with Codex/Copilot usage views and status indicators.
- Improve settings navigation, quota selection and colored usage bars.

## 1.0.11

- Add Copilot usage reporting and provider settings to VS Code.

## 1.0.10

- Reduce unnecessary Codex restarts and UI updates.

## 1.0.9

- Verify Rider 2026.2.3.1 compatibility and improve plugin unload/reload handling.

## 1.0.8

- Add compact provider summaries and expandable account details.
- Improve JetBrains AI subscription detection and unavailable-state explanations.

## 1.0.7

- Correct Copilot Free quota selection and keep enabled status widgets together.
- Improve compatibility with JetBrains AI subscription APIs.

## 1.0.6

- Add provider cards, subscription badges and persisted visibility checkboxes.
- Show quota-colored percentages directly in compact status widgets.

## 1.0.5

- Improve status indicators and feedback for stale OpenAI usage.

## 1.0.4

- Show Copilot consumption from 0% unused to 100% exhausted, including unlimited quota categories.

## 1.0.3

- Show remaining OpenAI and JetBrains AI quota percentages in the status bar.

## 1.0.2

- Improve JetBrains AI plan detection and compatibility with optional provider plugins.
- Organize provider integrations and shared UI components.

## 1.0.1

- Add subscription labels and compact usage rows with smaller status widgets.

## 1.0.0

- Monitor OpenAI Codex, JetBrains AI and GitHub Copilot quotas in Rider and IntelliJ IDEA.
- Show subscription plans, quota bars, reset times and independent status widgets.
- Support manual refresh, configurable intervals and automatic Codex CLI discovery.
