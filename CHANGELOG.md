# Changelog

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
