# Changelog

## 1.0.12

- Add a Visual Studio Code extension with Codex and Copilot quota views and status indicators, packaged alongside
  the JetBrains plugin; include illustrated usage and configuration previews in its Marketplace description.
- Organize JetBrains Tool Window settings into agent selection, refresh interval, and action sections.
- Open the usage overview when a provider status bar widget is clicked.
- Show the plugin version in the Agents Usage overview and add a direct GitHub repository link to configuration.
- Fix VS Code Copilot quota selection: use chat for Free plans, ignore empty 0/0 snapshots, and display unlimited quotas
  explicitly.
- Show Copilot's quota category and reported reset date; avoid request counts for AI credit billing.
- Request Copilot account quota using the API version used by the official VS Code Copilot extension.
- Restore visible, colored quota bars using native progress elements that work with the webview security policy.
- Use a transparent AI monogram adapted from the Rider Tool Window icon for the VS Code Activity Bar.

## 1.0.11

- Reuse the existing AI usage logo as the VS Code Marketplace package icon.
- Use the AI icon in the VS Code Activity Bar, add color-coded Copilot quota and status indicators, and organize
  provider selection and refresh settings.
- Add GitHub Copilot quota reporting to the VS Code extension through VS Code GitHub authentication and GitHub's
  internal quota endpoint.

## 1.0.10

- Avoid restarting Codex when visibility changes affect only other agents.
- Skip Codex UI notifications when the published usage has not changed.

## 1.0.9

- Verify Rider 2026.2.3.1 compatibility while retaining the build 261 minimum platform requirement.
- Keep provider services, Tool Window panels, and status widgets disposable for dynamic plugin unload and reload.
- Replace internal status bar and plugin registry calls with public APIs, and use JVM default methods to reduce
  compatibility warnings.
- Add illustrated Marketplace images for expanded agent details and the status bar, with editable SVG versions.
- Extend the media renderer to generate all three PNG/SVG pairs and document their gallery captions.

## 1.0.8

- Expand agent summaries to four visible facts in a compact two-column grid, with further details on demand.
- Show OpenAI credits, update time and both resets; JetBrains AI remaining, used and total credits; and Copilot used,
  available, reset and report values. Expanded views include plan, category balances, refresh interval and status.
- Prefer registered JetBrains AI application services and their published activation snapshot for subscription
  lookup. Add active license-journey and product-code fallback paths; identify workspace access without inventing
  a personal subscription tier.
- Expose a safe subscription lookup explanation in details/tooltips when the plan remains unavailable. Diagnostics
  contain only metadata getter/class names and exception types, without account data or credentials.

## 1.0.7

- Read Copilot Free's included quota from chat, matching Copilot's own dialog, instead of showing its unused premium
  quota as exhausted. With 100% available, consumption is 0% and green.
- Keep visible status widgets adjacent in OpenAI, JetBrains AI, Copilot order after enabling/disabling them.
- Resolve JetBrains AI activation and subscription metadata through loaded content-module class loaders when they
  are unavailable from the parent plugin loader. Share this compatibility lookup with the quota reader.

## 1.0.6

- Separate agents into compact cards with identity accents and subscription badges.
- Add a small visible details line for credits, consumed/remaining quota, and reset times.
- Keep the overview fitted to the Tool Window width and remove empty JetBrains AI quota rows.
- Show status labels as `OpenAi | D=<percent>% - W=<percent>%`, `JetBrainAi | <percent>%`, and
  `Copilot | <percent>%`; color each percentage directly and remove usage dots.
- Add persisted agent checkboxes to IDE Settings and the Tool Window configuration. Deselected agents disappear
  from both the overview and status bar, and their background quota reads pause.
- Keep settings and refresh available when all agents are hidden, and omit unavailable optional providers.
- Update the illustrated Marketplace preview and usage documentation.

## 1.0.5

- Centralize project identity/version in `gradle.properties`, regenerate the Gradle wrapper, and pin its distribution
  checksum.
- Keep platform-specific wrapper line endings and document all required Gradle build files.
- Place usage color dots directly after percentages in all status widgets.
- Give OpenAI D and W separate trailing dots colored by their respective balances.
- Dim stale OpenAI values on refresh errors and preserve error details in the tooltip.
- Update the illustrated Marketplace preview to match the new indicator positions.

## 1.0.4

- Show consumed GitHub Copilot quota in the status bar and overview: 0% unused, 100% exhausted.
- Fill Copilot bars as consumption increases and warn as the remaining quota decreases.
- Keep used and remaining counts in tooltips; show unlimited quotas without an exhaustion percentage.
- Update usage documentation and the illustrated Marketplace preview.

## 1.0.3

- Show OpenAI usage as `OpenAi D <percent>% - W <percent>%` with one indicator using the lower remaining balance.
- Show remaining JetBrains AI usage as `JetbrainAi <percent>%` instead of a credit amount; retain credit details in
  tooltips.
- Keep `Copilot <percent>%` and update the illustrated Marketplace preview to match the status bar.

## 1.0.2

- Read JetBrains AI's selected license name for the new AI Access activation, with legacy tier fallback.
- Add an illustrated usage preview to the plugin description and README, with Marketplace media instructions.
- Group each provider's models, readers, services, and UI in its own feature directory.
- Split Tool Window composition, refresh settings, Codex panels, and countdown cards into focused files.
- Share status widget layout, listener cleanup, provider headers, tooltips, and date formatting.
- Coordinate provider refreshes centrally and keep settings persistence independent of running services.
- Supply refresh intervals to shared polling instead of coupling it to settings.
- Discover only loaded optional plugins and update plugin class registrations for the new packages.
- Document project rules, architecture, development commands, and compatibility identifiers.

## 1.0.1

- Fit each usage bar, category, and remaining balance into a single compact row.
- Show subscription plans beside provider names and move reset/sync details into tooltips.
- Reduce overview padding, row spacing, and toolbar button size.
- Use compact status bar labels: OpenAI, JB AI, and Copilot.
- Replace the plugin logo with an AI chip and usage bars, with a matching Tool Window icon.

## 1.0.0

- Monitor OpenAI Codex, JetBrains AI, and GitHub Copilot usage in IntelliJ IDEA and Rider.
- Display independent status bar widgets with remaining usage and color indicators.
- Show remaining Codex 5-hour and weekly quotas, credits, reset times, and countdowns.
- Show JetBrains AI subscription credits, top-up credits, and subscription reset times.
- Show GitHub Copilot premium request or AI credit usage, chat and completion quotas, and reset times.
- Display the reported subscription plan in each provider view.
- Present remaining usage as compact colored bars in a scrollable overview.
- Refresh all available agents manually or automatically using shared Agent settings.
- Configure refresh presets or a custom interval between 10 and 3600 seconds.
- Discover the local Codex CLI automatically or configure its path manually.
- Read usage through the local Codex CLI and installed provider plugins.
- Provide an English interface, settings, tooltips, and error messages.
