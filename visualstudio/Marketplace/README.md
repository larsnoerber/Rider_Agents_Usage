# Visual Studio Marketplace upload materials

The Visual Studio build copies these files, the logo, and the VSIX to a versioned `dist/marketplace-<version>/`
directory and creates `agents-usage-visualstudio-marketplace-<version>.zip` for delivery.

## Web upload

1. Sign in to the Marketplace publisher **lanoerber** (display name **nightevil**) and edit the existing Visual Studio
   listing.
2. Upload the **visualstudio** VSIX. Name, version, author, logo, short description, tags and installation targets are
   supplied by the VSIX manifest. Keep the existing listing's internal name and stable VSIX ID.
3. Paste the contents of `overview.html` into the overview editor's HTML/source view. Its images use public GitHub
   release-tag URLs. Alternatively use `overview.md` and upload `images/usage.png` and `images/settings.png` in the
   editor.
4. Use `upload-details.txt` for categories, repository, pricing and screenshot captions.
5. Review the listing preview, save, and publish the update.

Uploading a VSIX alone does **not** replace the full overview or gallery. The included Markdown/images are also
available in the package, but the web form still needs the overview entered separately.

## Optional command-line publishing

`publishManifest.json` supplies the overview, its image assets, publisher ID, pricing, categories and repository to
Microsoft's `VsixPublisher.exe`. Paths are relative to this manifest. The example internal name `AgentsUsage` is the
default derived from the display name; **match the existing listing's internal name** before publishing an update.
The VSIX ID remains `lanoerber.AgentsUsage.VisualStudio` regardless of the listing's internal name.

After authentication using your normal publishing workflow, run from the upload folder:

```powershell
& 'C:\path\to\VsixPublisher.exe' publish -payload '.\agents-usage-visualstudio-<version>.vsix' -publishManifest '.\publishManifest.json'
```

The build prepares files only. It does not authenticate, install, or publish to the Marketplace.

References:
[web publishing](https://learn.microsoft.com/en-us/visualstudio/extensibility/walkthrough-publishing-a-visual-studio-extension?view=visualstudio),
[publishing manifest and image assets](https://learn.microsoft.com/en-us/visualstudio/extensibility/walkthrough-publishing-a-visual-studio-extension-via-command-line?view=visualstudio).
