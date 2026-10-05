# Visual Studio Marketplace upload materials

These materials describe the Visual Studio Codex/Copilot extension, not the standalone Windows app or its Store
package. See [publishing overview](../../docs/MARKETPLACE.md). A source-only GitHub update does not perform the
publishing steps below or upload EXE/MSIX files, and does not change the shared version.

The Visual Studio build copies these files, the logo, and the VSIX to a versioned `dist/marketplace-<version>/`
directory and creates `agents-usage-visualstudio-marketplace-<version>.zip` for delivery.

## Web upload

1. Sign in to the Marketplace publisher **lanoerber** (display name **nightevil**). For the first upload of the new
   identity introduced in 1.0.15, select **New extension > Visual Studio**. Set the internal name to
   **agents-usage-visualstudio**. For later updates, open **Edit** on this listing.
2. Upload the **visualstudio** VSIX. Name, version, author, logo, short description, tags and installation targets are
   supplied by the VSIX manifest. After the first upload, retain this listing's internal name and VSIX ID for updates.
3. Paste the contents of `overview.html` into the overview editor's HTML/source view. Its images use public GitHub
   release-tag URLs. Alternatively use `overview.md` and upload `images/usage.png` and `images/settings.png` in the
   editor.
4. Use `upload-details.txt` for categories, repository, pricing and screenshot captions.
5. Review the listing preview, save, and publish the update.

Uploading a VSIX alone does **not** replace the full overview or gallery. The included Markdown/images are also
available in the package, but the web form still needs the overview entered separately.

## Optional command-line publishing

`publishManifest.json` supplies the overview, its image assets, publisher ID, pricing, categories and repository to
Microsoft's `VsixPublisher.exe`. Paths are relative to this manifest. The internal name is `agents-usage-visualstudio`.
The VSIX ID is `lanoerber.AgentsUsage.VisualStudio.82441438-356a-4f3a-a82f-47bc9e090b7e`.
Keep these identifiers stable after publishing the new listing.

## Migration from the deleted listing

The publisher reported that the previous listing was deleted and uploading its legacy VSIX ID was rejected as
already in use. Version 1.0.15 adopts a new identity with explicit migration approval. This creates a new extension,
not an automatic update to packages identified as `lanoerber.AgentsUsage.VisualStudio`.
Uninstall the old AgentMeter extension (1.0.14 or earlier) from Visual Studio, complete the uninstaller's
instructions,
then install the new VSIX. The Visual Studio package registrations are shared, so keep only one identity installed.

After authentication using your normal publishing workflow, run from the upload folder:

```powershell
& 'C:\path\to\VsixPublisher.exe' publish -payload '.\agents-usage-visualstudio-<version>.vsix' -publishManifest '.\publishManifest.json'
```

The build prepares files only. It does not authenticate, install, or publish to the Marketplace.

References:
[web publishing](https://learn.microsoft.com/en-us/visualstudio/extensibility/walkthrough-publishing-a-visual-studio-extension?view=visualstudio),
[publishing manifest and image assets](https://learn.microsoft.com/en-us/visualstudio/extensibility/walkthrough-publishing-a-visual-studio-extension-via-command-line?view=visualstudio).
