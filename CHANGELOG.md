# Changelog

## Unreleased

- Refresh repository/edition guides, provider comparison, architecture, build instructions, privacy and publishing
  drafts for the current Windows code. Distinguish historical smoke reports from current build evidence. Keep shared
  version 1.1.0; source-only GitHub updates exclude generated EXE/MSIX artifacts and personal IDE settings.

- Reorganize Windows window/coordinator code by responsibility, centralize provider IDs and presentation metadata,
  separate settings persistence and OpenCode parsing, and move the Copilot device protocol into its provider folder.
  Add comments explaining cancellation generations, cached authentication, batch presentation and cleanup; preserve
  saved settings and existing XAML bindings. Detach bar-selection handlers during window shutdown.
- Use the supplied SVG as the Windows EXE/window/tray icon and as the source for Store logo assets. Render icon
  sizes directly from the vector source instead of resizing the previous raster image.

- Show themed refresh text and a progress bar in the Windows desktop widget. Hide provider segments during each
  full refresh and publish the completed batch together. Keep unavailable/saved-value results visible after the
  batch completes, prevent auto-collapse while loading, and avoid showing partial startup data.

- Detect OpenCode Desktop's bundled/versioned CLI and support its version 2 JSON connection/statistics formats
  alongside version 1 text reports. During Junie/OpenCode/Cline/Kilo sign-in, poll the selected provider briefly in
  the background so Connected updates without waiting for the interactive login terminal to close.

- Add OpenCode and Junie to the standalone Windows dashboard, provider settings tree, independent bar selection
  and every widget layout. Detect installed official CLIs and Rider ACP packages. OpenCode uses official auth
  inventory and 30-day local statistics; Junie uses an advertised built-in ACP statistics command for reported
  balance and saved project usage. Add official sign-in actions and optional executable/project paths.
  Authentication stays in each official CLI; Junie polling never submits a model task.
- Retain the last measured Windows usage values locally across restarts and temporary read failures. Restore
  them immediately at startup, show their original time and label them as saved values. Never restore connection
  status from cached usage; credentials, account details and raw provider responses are excluded.

- Detect Rider's bundled ACP Node runtime for the Windows Cline connection probe, even when Node is absent from
  the standalone app's PATH. Also recognize standard Node installation folders.

- Keep Windows configuration usable during official CLI sign-in. Detach the UI from the external login window,
  refresh immediately and again when the command ends, and bound the background wait without terminating the
  user's CLI. Use cmd /c for command wrappers so a completed auth command does not leave an idle shell keeping
  AgentMeter in its login state.

- Persist explicitly entered Windows OpenRouter keys in the current user's Windows Credential Manager, as requested.
  Load the saved key after app restarts, keep it out of settings files, and replace Clear session key with Remove
  saved key. Connect and Save settings both save it; report vault write/delete failures explicitly. Existing OpenCode
  authentication remains a fallback after removing the AgentMeter credential.

- Add a separate Providers shown in bar checkbox list under Windows Settings > Desktop widget > Appearance,
  persisted independently of provider activation. Apply selection immediately and preserve existing bar selection
  until the user customizes it; inactive providers remain disabled in the bar selection.
- Fix Cline/Kilo connection labels independently of usage availability: query Cline's installed official SDK for
  enabled, configured providers and Kilo's official credential inventory, including BYOK connections. Show Connected
  without requiring local usage or a Gateway balance; keep credentials within the official agent process.

- Keep a connection action on every Windows provider settings page, including Cline. Launch Cline's official
  `auth` flow through PATH or an installed Rider ACP package; allow an optional executable path. Existing Cline
  history never substitutes for verified authentication or hides its sign-in action.

- Add Windows Kilo and Cline providers with separate configuration pages, selection and all desktop widget layouts.
  Kilo uses the installed official CLI for active account/team balance and 30-day local costs/tokens, without reading
  credentials into AgentMeter. Cline reads only numeric usage from its local session database in read-only mode.
  Show money/tokens without invented percentage limits; local history is not labeled as a verified connection.
  Preserve saved provider selections and auto-detect Kilo from existing Rider ACP installations.

- Show Connected on Windows provider configuration pages after verified sign-in, preserving known connection
  state across transient failures. Accept pending OpenRouter keys via Save settings as well as Connect.
- Display OpenRouter daily/weekly/monthly spending directly on the overview. Label uncapped key budgets No limit
  in all widget layouts instead of an empty percentage, without treating account credits as unlimited.

- Replace the long Windows settings form with a dark tree navigation: General, Desktop widget Appearance/Behavior,
  and an individual page for each provider. Keep inputs intact across page changes and route OpenRouter Connect
  directly to its masked key input. Add provider-specific official sign-in actions to connection pages.

- Add a masked OpenRouter API-key field with Connect and Remove saved key controls. Use disposable session copies
  and explicitly saved current-user Windows credentials, activate OpenRouter on Connect, and refresh automatically.
  Legacy OpenCode credentials remain a fallback; no API key is serialized into settings.

- Add an optional Windows OpenRouter integration using its documented current-key API and the exact OpenRouter
  entry in OpenCode's official credential store. Show remaining key-budget percentages, reported dollar limits,
  daily/weekly/monthly spend and reset policy without inventing account balances or reset timestamps.
- Include OpenRouter in provider selection, widget layouts, ordering and click details, with a connection guide
  when no local key is available. Preserve existing selected providers and keep credentials out of settings/cache,
  with explicitly entered keys retained only in the current-user Windows credential vault.

- Render Windows context-menu color swatches as visual content instead of displaying the control type name.

- Add a Windows Mini widget layout, optional reset countdowns and explicit stale/unavailable data labels with
  timestamps and provider notices in click details.
- Add persisted screen-edge snapping, position locking, provider ordering and delayed hover expansion/collapse;
  keep expanded widgets open while their menu/details are in use and stop collapse timers on shutdown.
- Show palette swatches in Windows widget color dropdowns and context menus. Keep display preferences independent
  of provider polling and use the existing dashboard timer for time labels.

- Respect dropdown display labels in the Windows dark theme, showing only layout/color style names instead of
  internal record fields and color values.

- Keep Windows Cards and Circles provider badges at equal heights and align circle meters consistently.
- Add five persisted desktop bar color styles (Midnight, Ocean, Forest, Plum and Graphite), independently selectable
  from the layout in Settings or the context menu. Apply palettes to bar surfaces, badges, tracks and quota meters
  while preserving amber/red exhaustion warnings.

- Add five persisted Windows desktop widget styles: Classic, Compact, Cards, Circles and Vertical, selectable
  immediately from Settings or the bar's context menu. Use a fully dark context-menu template, including checkmarks,
  separators and nested style selection, without the system's white icon gutter.
- Add a Gemini diagnostic mode to the actual Windows executable, reporting only safe quota metadata and exception
  types, so locked-store access can be checked in the packaged runtime.
- Refresh Gemini Apps quotas while its desktop app remains open, using a read-only duplicate of the app's cookie
  file handle and a bounded SQLite snapshot in RAM. Retry temporary failures automatically and retain last reported
  quota values with their original timestamp and a stale-data notice; keep unrelated CLI setup advice in details.
- Preserve known Gemini sign-in state when its running desktop app locks the local cookie store.
- Open the installed Gemini desktop app from its Sign in button (Edge/web fallback if absent), with a separate CLI
  sign-in action in details. Distinguish rejected quota cookie requests from the desktop app's own login state.
- Let the Gemini Apps quota response establish authentication when the page omits its session token; report rejected
  sessions accurately and try other local Edge profiles instead of stopping at rejected Gemini app cookies.
- Use software rendering for the Windows dashboard and transparent widget, and defer tray/quota startup until the
  window can paint its first frame to avoid blank black startup windows.
- Match the Windows quota cards to the compact Rider layout, with broad horizontal bars, visible two-column details,
  distinct subscription badges, charcoal surfaces and a high-contrast history range dropdown.
- Keep Gemini marked as signed in when its local app session exists, and explain when Google no longer supplies the
  undocumented usage-page session token needed to read quota data.
- Limit Windows release outputs to the normal self-contained app and Store MSIX; stop producing portable ZIPs,
  lightweight runtime-dependent EXEs and duplicate Store staging packages.
- Use the transparent Quota Portal artwork for Windows app, tray and Store icons.
- Refresh the Windows dashboard with a compact black theme, grouped settings, quieter controls and tighter quota cards.
- Keep supplemental badge details free of duplicated current quota values and remove Grok from the Windows app.
- Fall back to Gemini Apps web usage from only the installed Gemini app's session cookies when CLI quotas are absent.
- Read Gemini session cookies from Edge profiles too, and correctly convert Chromium cookie expiry timestamps.
- Read Gemini CLI Google/Workspace model quota buckets, remaining counts, reset times and reported Code Assist plan.
  Reuse only the official local CLI OAuth store; add CLI sign-in and configurable Workspace Cloud project ID.
- Add a ridged left-hand desktop drag grip; dismiss quota details before dragging and save the new position.
- Keep desktop agent buttons borderless after clicks and highlight them only on hover.
- Remove the dark logo tile and retain transparency in EXE, window, tray and packaged logo assets.
- Open desktop quota details below the bar when there is insufficient room above it on the current monitor.
- Open desktop quota details on agent click above the bar; disable hover popups and retain live refresh while open.
- Add an optional small Windows EXE that uses an installed .NET 10 Windows Desktop Runtime.
- Add expandable Windows subscription badges with quota detail tiles, reset countdowns and observed 1/6/24-hour charts.
- Improve Windows dropdown/menu contrast and desktop tooltips with dark surfaces and colored quota bars.
- Separate desktop agent segments with dividers and save adjustable status-bar opacity.
- Hide Windows sign-in actions for authenticated agents and reuse their persisted official sessions after restart.
- Use AgentMeter branding for the EXE, dashboard and system tray; close/minimize to tray and add explicit Exit actions.
- Refresh the Windows dashboard with dark window chrome and remove JetBrains AI from the standalone provider list.

- Clarify consumed quota with the available percentage in standalone provider cards and desktop tooltips.

- Package the standalone app as a Microsoft Store MSIX with the reserved AgentMeter identity.
- Add a movable desktop status bar with colored percentages, provider selection, saved position and an always-on-top
  option.

- Add a portable standalone Windows app with OpenAI, Copilot, Claude and Cursor quota cards, plans, reset countdowns,
  provider selection, automatic refresh and an always-on-top option.
- Reuse official local agent sign-ins and provide official agent login buttons, including Copilot device authorization.
  Missing agents open installation documentation.

## 1.1.0 — First stable Marketplace release

- Monitor OpenAI Codex, JetBrains AI, GitHub Copilot, Claude and Cursor in the usage overview and status bar.
- Show subscription plans, provider-reported quotas, reset countdowns and local usage charts; click a badge to toggle
  details.
- Configure provider visibility and refresh intervals. New Rider installations enable only JetBrains AI.
- Optional weekly insights, quota forecasts, boss battles with visible hits, and Tic-Tac-Toe; Weekly and Games are off
  by default.
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
