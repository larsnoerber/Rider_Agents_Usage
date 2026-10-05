# AgentMeter for Windows privacy policy

Documentation updated: October 5, 2026.

AgentMeter is published by nighteviL. It displays usage quotas from supported AI providers using users' own official
agent installations and subscriptions.

AgentMeter has no analytics or telemetry and does not send usage data to the publisher. It does not collect passwords,
read prompt history or store provider credentials in its application settings.

OpenAI Codex and GitHub Copilot authentication and quota requests are handled by their official agent processes.
AgentMeter reads the official Claude Code, Cursor Agent and Gemini CLI local authentication stores only to
request usage from the respective provider. When Gemini CLI quota data is unavailable, it can also read only the two
Gemini Apps session cookies from the installed Gemini app's local profile or the user's Edge profile. Credentials remain
in memory during those
requests and are sent only to the corresponding provider; they are not forwarded to other providers, logged or
persisted by AgentMeter. Requests use fixed provider HTTPS destinations and do not follow redirects. The official
agents manage their own login, credential storage and network connections. This statement has one explicit
user-controlled exception: entered OpenRouter keys are retained in Windows Credential Manager as described below.
Gemini Apps web usage uses an internal, undocumented provider interface that may change.

Kilo and OpenCode use official CLI connection/statistics commands. Junie uses its advertised built-in ACP statistics
command, without submitting a model task. Cline's isolated official-SDK helper returns only a readiness boolean;
its separate database reader selects numeric local-usage fields. AgentMeter does not read prompt/conversation
bodies for these integrations. Connection inventories do not remotely validate every stored API key.

## Explicitly saved OpenRouter key

Connect or Save settings stores a pending key in the current Windows user's Credential Manager under
`AgentMeter/OpenRouter`. The masked input clears, and keys are loaded after restarts for requests only to OpenRouter's
current-key quota endpoint. Keys never enter settings, usage cache, diagnostic files or logs. Remove saved key deletes
the vault entry and session copy. A separate legacy OpenCode `openrouter` credential can remain a fallback; removing
AgentMeter's key does not remove that official provider connection. No management key/account balance is requested.

Settings are stored locally and contain selected providers, refresh interval, executable paths, desktop widget
preferences, Gemini Workspace Cloud project ID and window position. The application does not upload these settings.
The Junie project directory is another saved choice. `saved-usage.json` under the local AgentMeter folder stores
constrained numeric usage values, quota labels and observation/reset times across restarts. It excludes credentials,
account details, raw responses, plan text and authentication state. Saved measurements are labeled with their original
time and never establish current sign-in. Opt-in Gemini diagnostics store safe quota metadata/exception types locally.
Consumption charts retain numeric quota observations in memory for up to 24 hours during the app session. They contain
no credentials, prompts or conversation history and are cleared when the app exits.

Selecting Sign in launches the provider's official login flow. If the required agent is missing, AgentMeter opens
its public installation documentation in the default browser. Those services and websites have their own privacy
policies.

You may remove the application's saved settings through Windows app reset/uninstall for the packaged app or by
deleting its local AgentMeter folder for a portable installation. Remove the OpenRouter key through AgentMeter
Settings or Windows Credential Manager separately: deleting settings files is not a vault deletion. For packaged-app
reset/uninstall, verify remaining local files and the vault entry separately if removing all records; no additional
cleanup is claimed. Provider logins are managed separately by the official provider programs.

For questions, use the publisher's support link provided on the AgentMeter Microsoft Store listing.
See the [Windows guide](../README.md)
and [repository support](https://github.com/larsnoerber/Rider_Agents_Usage/issues).
