param([Parameter(Mandatory = $true)][string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName PresentationCore, PresentationFramework, WindowsBase
. (Join-Path $PSScriptRoot '../../windows/Packaging/read-icon.ps1')
$storeIcon = Read-AgentMeterIcon (Join-Path $PSScriptRoot '../../windows/Packaging/AgentMeter.svg')
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$storeView = New-Object System.Windows.Controls.Image
$storeView.Source = $storeIcon
$storeView.Stretch = [System.Windows.Media.Stretch]::Uniform
$storeView.Width = 300
$storeView.Height = 300
$storeView.Measure((New-Object System.Windows.Size(300, 300)))
$storeView.Arrange((New-Object System.Windows.Rect(0, 0, 300, 300)))
$storeView.UpdateLayout()
$storeBitmap = New-Object System.Windows.Media.Imaging.RenderTargetBitmap(300, 300, 96, 96, [System.Windows.Media.PixelFormats]::Pbgra32)
$storeBitmap.Render($storeView)
$storeEncoder = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
$storeEncoder.Frames.Add([System.Windows.Media.Imaging.BitmapFrame]::Create($storeBitmap))
$storeStream = [System.IO.File]::Create((Join-Path $OutputDirectory 'logo-300.png'))
try { $storeEncoder.Save($storeStream) } finally { $storeStream.Dispose() }
