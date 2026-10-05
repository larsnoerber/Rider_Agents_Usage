param()
$ErrorActionPreference = 'Stop'
$browserRoot = $PSScriptRoot
$repositoryRoot = Split-Path -Parent $browserRoot
$versionLine = Get-Content -LiteralPath (Join-Path $repositoryRoot 'gradle.properties') | Where-Object { $_ -match '^pluginVersion=' }
$browserVersion = ($versionLine -split '=', 2)[1].Trim()
if ($browserVersion -notmatch '^\d+\.\d+\.\d+(\.\d+)?$')
{
    throw 'Shared version is not a valid browser extension version.'
}
# Validate JavaScript syntax and referenced assets before producing the installable directory.
& node (Join-Path $browserRoot 'scripts/package.mjs') $browserVersion
if ($LASTEXITCODE -ne 0)
{
    throw 'Browser extension packaging failed.'
}
$outputRoot = Join-Path $browserRoot 'dist'
$archive = Join-Path $outputRoot "AgentMeter-$browserVersion-chromium.zip"
Compress-Archive -LiteralPath (Get-ChildItem -LiteralPath (Join-Path $outputRoot 'unpacked') | Select-Object -ExpandProperty FullName) -DestinationPath $archive -Force
Write-Output "Load unpacked: $( Join-Path $outputRoot 'unpacked' )"
Write-Output "Package: $archive"
