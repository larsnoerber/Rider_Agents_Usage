# Agents Usage for Visual Studio

Native Visual Studio extension for **Visual Studio 2022 and 2026**, in Community, Professional and Enterprise on
Windows x64. It uses stable Visual Studio 2022 SDK APIs and an open installation version range (`[17.0,)`).
Older 32-bit Visual Studio releases are not targeted. A declared installation target is not a substitute for
checking the extension in each IDE version.

## Features

- OpenAI Codex remaining 5-hour and weekly quotas with colored bars.
- Provider-reported subscription plan, credits and local reset times.
- Manual refresh and automatic refresh while the usage window is open.
- Provider visibility, CLI path and refresh interval under **Tools > Options > Agents Usage > General**.
- AI logo, visible package version and GitHub repository link.

The Codex CLI must already be installed and signed in (`codex login`). This extension reads only provider-reported
quota from the local app-server; it does not read credentials or estimate usage from local activity.
Copilot quota integration is not included in this first Visual Studio package. Its VS Code authentication cannot
be reused in Visual Studio. JetBrains AI is specific to the JetBrains package.

## Build

Use Windows with Visual Studio 2022 or 2026 MSBuild and a compatible .NET SDK. NuGet restores the Visual Studio SDK,
VSIX build tools and .NET Framework 4.7.2 reference assemblies; runtime code targets .NET Framework 4.7.2.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\visualstudio\build.ps1
```

For a custom Visual Studio installation, add `-MSBuildPath 'C:\path\to\MSBuild.exe'`.
The script reads the shared version from `gradle.properties` and writes
`visualstudio/dist/agents-usage-visualstudio-<version>.vsix`. It does not install the extension.

## Install and open

1. Close Visual Studio and open the Visual Studio VSIX package with Visual Studio's VSIX Installer.
2. Select your Visual Studio installation and install the extension.
3. Open Visual Studio and choose **View > Other Windows > Agents Usage**.
4. Use **Settings** to configure the CLI path or refresh interval.

The Visual Studio and VS Code packages both use the `.vsix` suffix but have different manifests and runtimes.
Choose the package whose filename contains **visualstudio** for Visual Studio Community.

Codex is a trademark of OpenAI. This independent extension is not affiliated with or endorsed by OpenAI.
