# Marketplace media

The plugin description in `src/main/resources/META-INF/plugin.xml` embeds the public PNG:

![Illustrated usage overview](images/agents-usage-overview.png)

- `images/agents-usage-overview.png`: 1280 × 800 image for the description and Marketplace Media gallery.
- `images/agents-usage-overview.svg`: editable vector version.
- `../tools/render_marketplace_preview.py`: reproducible renderer using Python and Pillow. Run it from any directory;
  its default font is Windows Segoe UI.

This is an **illustrated preview with example balances**, not a screenshot of a live account. The provider labels,
plans, compact bars, and status text follow the current UI. Account-specific categories can differ.
Keep that caption when using the image, and replace both assets when changing the illustrated UI.

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
