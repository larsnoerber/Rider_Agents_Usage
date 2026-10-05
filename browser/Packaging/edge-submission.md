# Edge Add-ons submission — AgentMeter 1.1.0

## Existing entry

Product ID from the supplied Partner Center screenshot: `9872c332-f86b-4675-80ec-e274e523745d`.
Use the already-created AgentMeter extension entry. The public listing URL is not available until publication.
Do not create another entry or add a manifest key based on a truncated public key.

## Upload files

Run `powershell -NoProfile -ExecutionPolicy Bypass -File browser/build-store.ps1` from the repository root.
The output directory is `browser/dist/edge-store/`:

- `AgentMeter-1.1.0-chromium.zip`: upload under Packages; manifest.json is at the ZIP root.
- `logo-300.png`: store logo.
- `screenshot-bar-1280x800.png`: actual page-bar UI rendered with clearly labelled demo quota values.
- `screenshot-settings-1280x800.png`: actual configuration UI rendered with demo browser permissions.
- The copied listing, privacy-field and certification-note documents: paste into their corresponding fields.

Only the extension ZIP belongs in Packages. Store images and text are uploaded or pasted separately.
Generated ZIPs and store images remain local build output; the renderer and listing sources are committed.

## Partner Center fields

| Field | Value/source |
| --- | --- |
| Name | AgentMeter |
| Language | English; the product UI is English |
| Category | Productivity, if available in the current category list |
| Visibility | Public, if public distribution is intended |
| Markets | Select your intended countries in Partner Center |
| Website | https://github.com/larsnoerber/Rider_Agents_Usage/tree/main/browser |
| Support | https://github.com/larsnoerber/Rider_Agents_Usage/issues |
| Privacy | https://github.com/larsnoerber/Rider_Agents_Usage/blob/main/browser/PRIVACY.md |
| Description/search terms | edge-listing-en.md |
| Purpose/permissions/data practices | edge-privacy-fields.md |
| Certification notes | edge-certification-notes.txt |

There is no mature content. Publisher/legal identity is entered through your verified Edge developer account.
Review the portal's actual data definitions before certifying declarations; locally processed quota pages and
provider authentication are disclosed in the supplied privacy fields.

## Checks and remaining account work

Packaging checks syntax, local references and assets; store rendering produces presentation images, not live
provider validation. The user has reported working OpenAI and GitHub readings. Signed-in checks for all providers,
the latest drag/settings behavior, and Microsoft certification are not established by packaging.
Do not claim these checks have passed in the listing. Provider sessions and subscriptions affect reported quotas.
Registration/verification, final form declarations and submission are performed in Partner Center.

No shared version change, GitHub tag, GitHub release or release-asset upload is part of this preparation.

Source: [Microsoft's submission guide](https://learn.microsoft.com/en-us/microsoft-edge/extensions/publish/publish-extension).
Current documented image sizes are 300×300 recommended for the logo (128×128 minimum), and 1280×800 or 640×480
for screenshots. Optional promotional tiles are not needed for this submission.
