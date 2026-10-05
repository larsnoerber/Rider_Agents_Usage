param([Parameter(Mandatory = $true)][string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName PresentationCore, PresentationFramework, WindowsBase
. (Join-Path $PSScriptRoot '../../windows/Packaging/read-icon.ps1')
$chromeArtwork = Read-AgentMeterIcon (Join-Path $PSScriptRoot '../../windows/Packaging/AgentMeter.svg')
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
function Save-ChromeArtwork([string]$Name, [int]$Width, [int]$Height, [bool]$Promotion) {
    $visual = New-Object System.Windows.Media.DrawingVisual
    $context = $visual.RenderOpen()
    try {
        if ($Promotion) {
            $background = New-Object System.Windows.Media.SolidColorBrush ([System.Windows.Media.ColorConverter]::ConvertFromString('#101925'))
            $context.DrawRectangle($background, $null, (New-Object System.Windows.Rect(0, 0, $Width, $Height)))
            $context.DrawImage($chromeArtwork, (New-Object System.Windows.Rect(126, 20, 188, 188)))
            $font = New-Object System.Windows.Media.Typeface 'Segoe UI'
            $text = New-Object System.Windows.Media.FormattedText('AgentMeter', [Globalization.CultureInfo]::GetCultureInfo('en-US'),
                [System.Windows.FlowDirection]::LeftToRight, $font, 30, [System.Windows.Media.Brushes]::White, 1.0)
            $context.DrawText($text, (New-Object System.Windows.Point((($Width - $text.Width) / 2), 220)))
        } else {
            # Chrome recommends a 96px square artwork with 16px transparent padding.
            $context.DrawImage($chromeArtwork, (New-Object System.Windows.Rect(16, 16, 96, 96)))
        }
    } finally { $context.Close() }
    $bitmap = New-Object System.Windows.Media.Imaging.RenderTargetBitmap($Width, $Height, 96, 96, [System.Windows.Media.PixelFormats]::Pbgra32)
    $bitmap.Render($visual)
    $encoder = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
    $encoder.Frames.Add([System.Windows.Media.Imaging.BitmapFrame]::Create($bitmap))
    $stream = [System.IO.File]::Create((Join-Path $OutputDirectory $Name))
    try { $encoder.Save($stream) } finally { $stream.Dispose() }
}
Save-ChromeArtwork 'logo-128.png' 128 128 $false
Save-ChromeArtwork 'promo-small-440x280.png' 440 280 $true
