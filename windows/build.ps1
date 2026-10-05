param(
    [ValidateSet('win-x64', 'win-arm64')][string]$Runtime = 'win-x64',
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$versionLine = Get-Content -LiteralPath (Join-Path $taskRoot 'gradle.properties') | Where-Object { $_ -match '^pluginVersion=' } | Select-Object -First 1
$version = ($versionLine -split '=', 2)[1].Trim()
if ($version -notmatch '^\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.-]+)?$')
{
    throw 'Invalid project version.'
}
$destination = Join-Path $PSScriptRoot "dist/AgentMeter-$version-$Runtime"
if ($OutputDirectory)
{
    $destination = [System.IO.Path]::GetFullPath($OutputDirectory)
}

& dotnet publish (Join-Path $PSScriptRoot 'AgentMeter.Windows.csproj') -c Release -r $Runtime --self-contained true `
    -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -p:DebugType=None -p:DebugSymbols=false `
    "-p:Version=$version" -o $destination
if ($LASTEXITCODE -ne 0)
{
    throw 'Windows application build failed.'
}
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'README.md') -Destination $destination
Copy-Item -LiteralPath (Join-Path $taskRoot 'LICENSE') -Destination $destination
Write-Output "Executable: $( Join-Path $destination 'AgentMeter.exe' )"
Write-Output 'Self-contained Windows version; no ZIP or runtime-dependent variant is created.'
