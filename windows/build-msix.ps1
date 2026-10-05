param(
    [ValidateSet('win-x64', 'win-arm64')][string]$Runtime = 'win-x64',
    [string]$MakeAppxPath = ''
)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$versionLine = Get-Content -LiteralPath (Join-Path $taskRoot 'gradle.properties') | Where-Object { $_ -match '^pluginVersion=' } | Select-Object -First 1
$version = ($versionLine -split '=', 2)[1].Trim()
if ($version -notmatch '^\d+\.\d+\.\d+$')
{
    throw 'MSIX requires a numeric release version.'
}
$architecture = $Runtime.Substring(4)
$identity = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'Packaging/StoreIdentity.json') | ConvertFrom-Json
if (!$identity.name -or !$identity.publisher.StartsWith('CN=') -or !$identity.publisherDisplayName)
{
    throw 'Store identity is incomplete.'
}

$stage = Join-Path $PSScriptRoot "dist/msix-stage-$Runtime"
& dotnet publish (Join-Path $PSScriptRoot 'AgentMeter.Windows.csproj') -c Release -r $Runtime --self-contained true `
    -p:PublishSingleFile=false -p:DebugType=None -p:DebugSymbols=false "-p:Version=$version" -o $stage
if ($LASTEXITCODE -ne 0)
{
    throw 'MSIX application build failed.'
}

$manifest = [xml](Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'Packaging/AppxManifest.xml'))
$manifest.Package.Identity.Name = $identity.name
$manifest.Package.Identity.Publisher = $identity.publisher
$manifest.Package.Identity.Version = "$version.0"
$manifest.Package.Identity.ProcessorArchitecture = $architecture
$manifest.Package.Properties.PublisherDisplayName = $identity.publisherDisplayName
$manifest.Save((Join-Path $stage 'AppxManifest.xml'))
$assets = Join-Path $stage 'Assets'
New-Item -ItemType Directory -Force -Path $assets | Out-Null
& powershell -NoProfile -STA -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Packaging/render-assets.ps1') -OutputDirectory $assets
if ($LASTEXITCODE -ne 0)
{
    throw 'Store asset rendering failed.'
}
Copy-Item -LiteralPath (Join-Path $taskRoot 'LICENSE') -Destination $stage

if (!$MakeAppxPath)
{
    $nugetRoot = $env:NUGET_PACKAGES
    if (!$nugetRoot)
    {
        $nugetRoot = Join-Path $env:USERPROFILE '.nuget/packages'
    }
    $sdkPackage = Join-Path $nugetRoot 'microsoft.windows.sdk.buildtools/10.0.26100.9169'
    $MakeAppxPath = Get-ChildItem -LiteralPath $sdkPackage -Filter 'makeappx.exe' -Recurse |
            Where-Object { $_.Directory.Name -eq 'x64' } | Select-Object -First 1 -ExpandProperty FullName
}
if (!$MakeAppxPath -or !(Test-Path -LiteralPath $MakeAppxPath -PathType Leaf))
{
    throw 'MakeAppx.exe not found. Pass -MakeAppxPath from the Windows SDK.'
}
$msix = Join-Path $PSScriptRoot "dist/AgentMeter-$version-$Runtime.msix"
& $MakeAppxPath pack /d $stage /p $msix /o
if ($LASTEXITCODE -ne 0)
{
    throw 'MSIX packaging or manifest validation failed.'
}
# Remove only the resolved, owned staging directory after successful packaging.
$stageResolved = (Resolve-Path -LiteralPath $stage).ProviderPath
$distResolved = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'dist')).ProviderPath
$expectedStage = [IO.Path]::GetFullPath((Join-Path $distResolved "msix-stage-$Runtime"))
if (![string]::Equals($stageResolved, $expectedStage, [StringComparison]::OrdinalIgnoreCase) -or
        (Get-Item -LiteralPath $stageResolved).LinkType)
{
    throw 'Refusing to remove an unexpected MSIX staging directory.'
}
Remove-Item -LiteralPath $stageResolved -Recurse -Force
Write-Output "Unsigned Microsoft Store package: $msix"
