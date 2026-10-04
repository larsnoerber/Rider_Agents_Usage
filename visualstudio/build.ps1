param([string]$MSBuildPath = '')

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$versionLine = Get-Content -LiteralPath (Join-Path $repositoryRoot 'gradle.properties') | Where-Object { $_.StartsWith('pluginVersion=') } | Select-Object -First 1
if (-not $versionLine)
{
    throw 'pluginVersion is missing from gradle.properties.'
}
$releaseVersion = $versionLine.Substring(14).Trim()
if ($releaseVersion -notmatch '^\d+\.\d+\.\d+$')
{
    throw "Visual Studio packaging requires a numeric release version: $releaseVersion"
}

if (-not $MSBuildPath)
{
    $vswherePath = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe'
    if (Test-Path -LiteralPath $vswherePath)
    {
        $MSBuildPath = & $vswherePath -latest -products '*' -requires Microsoft.Component.MSBuild -find 'MSBuild\**\Bin\MSBuild.exe' | Select-Object -First 1
    }
    if (-not $MSBuildPath)
    {
        foreach ($generation in @('18', '2026', '2022'))
        {
            foreach ($edition in @('Community', 'Professional', 'Enterprise', 'BuildTools'))
            {
                $candidate = Join-Path $env:ProgramFiles "Microsoft Visual Studio\$generation\$edition\MSBuild\Current\Bin\MSBuild.exe"
                if (Test-Path -LiteralPath $candidate)
                {
                    $MSBuildPath = $candidate; break
                }
            }
            if ($MSBuildPath)
            {
                break
            }
        }
    }
}
if (-not $MSBuildPath -or -not (Test-Path -LiteralPath $MSBuildPath))
{
    throw 'Visual Studio MSBuild was not found. Install Visual Studio or specify -MSBuildPath.'
}

$manifestPath = Join-Path $PSScriptRoot 'source.extension.vsixmanifest'
[xml]$manifest = Get-Content -LiteralPath $manifestPath -Raw
$manifest.PackageManifest.Metadata.Identity.Version = $releaseVersion
$manifest.Save($manifestPath)
$publishMetadata = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'Marketplace\publishManifest.json') -Raw | ConvertFrom-Json

& $MSBuildPath (Join-Path $PSScriptRoot 'AgentsUsage.VisualStudio.csproj') /restore /t:Rebuild /p:Configuration=Release "/p:Version=$releaseVersion" /p:DeployExtension=false /v:minimal /nologo
if ($LASTEXITCODE -ne 0)
{
    throw "Visual Studio extension build failed (exit $LASTEXITCODE)."
}
$package = Join-Path $PSScriptRoot 'bin\Release\net472\AgentsUsage.VisualStudio.vsix'
if (-not (Test-Path -LiteralPath $package))
{
    throw 'The build did not produce the Visual Studio VSIX.'
}
$distribution = Join-Path $PSScriptRoot 'dist'
New-Item -ItemType Directory -Path $distribution -Force | Out-Null
$artifact = Join-Path $distribution "agents-usage-visualstudio-$releaseVersion.vsix"
Copy-Item -LiteralPath $package -Destination $artifact -Force
Write-Output "Visual Studio package: $artifact"

# Prepare a self-contained upload folder; publishing remains an explicit user action.
$uploadFolder = Join-Path $distribution "marketplace-$releaseVersion"
New-Item -ItemType Directory -Path $uploadFolder -Force | Out-Null
Copy-Item -Path (Join-Path $PSScriptRoot 'Marketplace\*') -Destination $uploadFolder -Recurse -Force
Copy-Item -LiteralPath $artifact -Destination $uploadFolder -Force
Copy-Item -LiteralPath (Join-Path $repositoryRoot 'vscode\resources\icon.png') -Destination (Join-Path $uploadFolder 'logo.png') -Force
$htmlPath = Join-Path $uploadFolder 'overview.html'
$html = [System.IO.File]::ReadAllText($htmlPath).Replace('@VERSION@', $releaseVersion)
[System.IO.File]::WriteAllText($htmlPath, $html,[System.Text.UTF8Encoding]::new($false))
$details = @"
Display name: AgentMeter
Version: $releaseVersion
Publisher ID: $( $publishMetadata.publisher )
Publisher display name / VSIX author: $( $manifest.PackageManifest.Metadata.Identity.Publisher )
VSIX ID: $( $manifest.PackageManifest.Metadata.Identity.Id )
Internal name: $( $publishMetadata.identity.internalName )
Upload: New extension > Visual Studio for this new identity; later updates use Edit on this listing
Migration: Uninstall AgentMeter 1.0.14 or earlier before installing this new identity
Short description: Track OpenAI Codex and GitHub Copilot quotas in Visual Studio with colored bars, subscription plans, reset times and clickable AI status indicators.
Versions: Visual Studio 2022 and 2026
Editions: Community, Professional, Enterprise
Architecture: Windows x64
Type: Tools
Categories: Coding, Other
Pricing: Free
Tags: AI, OpenAI, Codex, GitHub, Copilot, usage, quota, credits, status bar
Repository: https://github.com/larsnoerber/Rider_Agents_Usage
Release notes: https://github.com/larsnoerber/Rider_Agents_Usage/blob/v$releaseVersion/CHANGELOG.md
Logo: logo.png
Overview: paste overview.html in the web editor, or use overview.md with VsixPublisher
Screenshot 1: images/usage.png - Usage overview and clickable AI status indicators (illustrated example)
Screenshot 2: images/settings.png - Provider selection and refresh/display settings (illustrated example)
Enable Q&A: Yes
"@
[System.IO.File]::WriteAllText((Join-Path $uploadFolder 'upload-details.txt'), $details,[System.Text.UTF8Encoding]::new($false))
$uploadArchive = Join-Path $distribution "agents-usage-visualstudio-marketplace-$releaseVersion.zip"
Compress-Archive -Path (Join-Path $uploadFolder '*') -DestinationPath $uploadArchive -Force
Write-Output "Marketplace upload materials: $uploadArchive"
