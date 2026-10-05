# Local Windows verification

Documentation updated: October 5, 2026. No test suite was run for the latest provider/refactoring/icon changes.
Build success and user feedback are distinct from automated checks and Store certification.

## Current evidence

- The self-contained x64 Windows build completed after the provider, bar-loading and code-organization changes.
- The final x64 MSIX was built and MakeAppx manifest/package validation passed for the current source. It remains
  unsigned; this does not verify installation, account access or Store certification.
- The SVG-generated icon output was visually inspected; EXE, window and tray share the generated ICO.
- Local OpenCode Desktop v2 read-only JSON inventory/statistics commands returned successfully.
- Junie ACP responded, and the user subsequently confirmed usage appeared after login. This is not verification of
  every account, balance or parser variant.
- The user reported that the refactored app/icon looked good. Existing nullable warnings remain in linked legacy
  Visual Studio Codex/process sources.

## Historical automated results

The following results were recorded during earlier development. They predate the latest provider/refactoring changes
and have not been rerun for this source snapshot; the historical count is not current coverage.

- Earlier startup regression run: 89 checks passed. It opens the actual dashboard Window and transparent desktop
  widget with explicit empty provider settings, checks the Window content host and uses software rendering.
  On-screen capture remains unverified because the native Computer Use pipe is unavailable; image renders cannot
  prove the desktop compositor output. The initial black-window root cause was not confirmed by debugger inspection:
  Rider's attach tool confirmed the process, but no debugger session was available to query.
- Normal self-contained x64 executable built successfully. The release build now creates only this app folder and the
  separately requested Store MSIX; it no longer creates ZIP or runtime-dependent variants. MSIX is a separate build.
- MSIX built successfully with the reserved Partner Center identity; MakeAppx manifest/package validation passed.
- Previous local smoke checks passed: JSON quota/date validation, fixed HTTPS destinations, unavailable agents,
  disabled/disposed coordinator behavior, exact desktop labels, consumed/remaining colors, unlimited and missing
  quota rendering, provider selection, invalid saved coordinates, button contrast and scrollable settings. Added
  authentication-state transitions, Claude API-key metadata, numeric history retention/deduplication, badge expansion,
  chart ranges, dropdown/context-menu contrast, rich tooltip bars, icons, opacity, close/Exit behavior and Gemini CLI
  model quotas.
- Click-popup checks verify disabled hover tooltips, open/close on agent clicks, dismissal configuration, preserved
  anchors across quota refresh and actual native popup placement above the clicked segment using synthetic data.
- Grip checks exercise repeated drag gestures after opening details, dismissal, movement and position saving.
  Native popup checks verify opening below at the upper screen edge and returning above when space permits.
- Desktop segment templates remain borderless with no focus-frame triggers; hover styling is independent of focus.
- Pixel alpha checks verify transparency in the embedded window/EXE logo and the actual tray icon bitmap.
- Gemini CLI checks use isolated fixtures: missing/expired OAuth stores, missing/numeric project IDs, user environment
  parsing, reported plans, bucket fractions/counts/reset times, unavailable/invalid buckets and signed-in UI/status.
  No real Google credentials or quota requests are used in the smoke suite.
- WPF renderings inspected; button, tooltip and dropdown text contrast issues were corrected. Tooltips contain actual
  synthetic quota bars and supplemental details show observed chart samples without repeating the current quota bars.
- Published executable startup check passed: main window created and process responding. The owned test process
  was stopped afterward. This check does not establish successful provider quota reporting.
- `git diff --check` passed.

The compiler reports nullable warnings in the linked legacy Visual Studio Codex/process sources. The new app compiled
and packaged successfully. Rider currently does not index the new Windows project for its normal IDE diagnostics.

Run the existing local checks only when tests are explicitly requested:

```powershell
dotnet run --project .\windows\tests\AgentMeter.Windows.SmokeTests.csproj -c Release -p:BaseOutputPath=bin/checks/ -p:UseAppHost=false -- .\windows\dist\smoke
```

The smoke suite uses synthetic quotas and missing executable paths. It does not request account balances, sign in,
install the package or save new user settings. Renderings are sample WPF surfaces, not live account screenshots.

## Manual checks still required

The Computer Use native connection was unavailable, so interactive actions have not been verified.

1. Run the executable path printed by the chosen build. Check actual quotas against the official agents.
   Copilot with 97% remaining should show **3% used** and **97% available**; status text is `Copilot | 3%`.
2. Select **Desktop widget**. Drag its ridged left grip with details open and closed, including repeated drags.
   Move the bar to the top edge and check that details open below it. Reopen the dashboard by double-click, and use
   its right-click refresh/pin/hide
   menu. Restart the app to check saved desktop mode and position.
3. Change provider selections and refresh interval. Confirm disabled providers disappear from both views.
   Hover each status segment: no details should open. Click to open details above the bar, click again/outside to
   dismiss, and check dragging/double-click still work. Adjust status-bar opacity and restart to check persistence.
   Expand subscription badges, switch chart ranges and verify details remain expanded after refresh.
4. Use **Sign in** with a missing or expired account. Complete authentication in the official agent/browser.
   Confirm Connected updates automatically during the Junie/OpenCode/Cline/Kilo readiness monitor, without requiring
   the terminal to close. Check Copilot's device flow separately. Inventory readiness is not remote key validation.
   Verify Sign in hides after a connection is established and is rechecked after restart; saved quotas do not prove
   login.
   Close/minimize to tray, restore by double-click, and confirm Exit removes both tray icon and status bar.
5. Check the packaged application after signing/installing through an appropriate local testing process or Store
   private distribution. The unsigned MSIX provided here cannot be directly installed as a trusted package.
6. Check all six bar layouts and five palettes, equal Cards/Circles heights, independent Providers shown in bar,
   loading text/progress and simultaneous provider reveal. A missing provider must not hold the loading state forever.
7. Save an OpenRouter key, restart and verify reuse; remove it and check the vault entry independently of OpenCode.
   Check that settings, usage cache and diagnostics contain no credentials.
8. Configure a Junie coding project with saved sessions; compare project statistics and reported balance. Check
   OpenCode v1/v2 formats where available. No monitoring command should submit a model task.
9. Restart during a provider outage: saved usage keeps its original timestamp and does not establish sign-in.
   The dashboard restores saved values, while the bar waits for its initial completed batch. Session charts start fresh.
10. Check the supplied SVG in EXE, dashboard and tray, then Exit and confirm owned processes/timers are gone.

JetBrains AI Assistant's IDE quota service and Grok are excluded from Windows; Junie CLI is separately supported.
Gemini CLI/Workspace quota integrations require the official
CLI sign-in for live verification. For Gemini Apps fallback, sign in to the standalone Gemini app and check the
reported consumer 5-hour/weekly quotas while Gemini remains open; the locked-store fallback is automatic. The live
Google account, organization permissions and actual quota responses remain manual checks.
Consumer web, CLI/Workspace and API quotas are separate.
