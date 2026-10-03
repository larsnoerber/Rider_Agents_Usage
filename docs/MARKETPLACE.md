# Marketplace media

## Visual Studio Code

The VS Code extension's `vscode/README.md` embeds `vscode/resources/previews/usage.png` and `settings.png` in its
Marketplace description. These are VS Code illustrations with example balances, not live account screenshots.
Regenerate them with `python tools/render_vscode_preview.py` (Pillow and Windows Segoe UI required).
Packaging includes both PNGs and links the description images to the matching GitHub release tag.

## JetBrains IDEs

The plugin description in `src/main/resources/META-INF/plugin.xml` embeds the public PNG:

![Illustrated usage overview](images/agents-usage-overview.png)

- `images/agents-usage-overview.png`: 1280 × 980 image for the description and Marketplace Media gallery.
- `images/agents-usage-overview.svg`: editable vector version.
- `images/agents-usage-details.png`: 1600 × 1080 illustration of all three expanded agent detail views,
  arranged side by side for readability.
- `images/agents-usage-statusbar.png`: 1280 × 720 illustration of colored status percentages and agent visibility.
- `images/agents-usage-details.svg` / `images/agents-usage-statusbar.svg`: matching editable vector versions.
- `../tools/render_marketplace_preview.py`: reproducible renderer using Python and Pillow. Run it from any directory;
  its default font is Windows Segoe UI.

This is an **illustrated preview with example balances**, not a screenshot of a live account. The provider labels,
plans, compact bars, and status text follow the current UI. Account-specific categories can differ.
Keep that caption when using the images, and regenerate the PNG/SVG pairs when changing the illustrated UI.

Run `python tools/render_marketplace_preview.py` to regenerate all three image pairs.
For the Media gallery, use the PNG files. Suggested captions:

- **Usage overview — illustrated example**
- **Expanded agent details — illustrated example**
- **Status bar and agent visibility — illustrated example**

## Showing the image on Marketplace

1. Push the image to the repository's public `main` branch. The description uses an absolute HTTPS raw GitHub URL;
   local paths and images inside the plugin ZIP are not public Marketplace image URLs.
2. Upload the rebuilt plugin ZIP. Select the description from `plugin.xml` in the listing's General Information
   settings if an independently edited Marketplace description is currently selected.
3. To make the image available in the zoomable gallery, additionally upload the PNG under **Media** in the plugin
   admin panel, with a caption such as **Usage overview and status indicators — illustrated example**.

The image is prepared in the repository; no Marketplace upload is performed by building the plugin.
JetBrains recommends the Media section for images, and 600–800 pixels of display width for images embedded in
descriptions. The embedded preview uses a width of 760 pixels.
See [JetBrains listing documentation](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html).
