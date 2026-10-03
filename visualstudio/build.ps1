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
