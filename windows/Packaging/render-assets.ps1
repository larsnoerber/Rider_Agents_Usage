param([Parameter(Mandatory = $true)][string]$OutputDirectory)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName PresentationCore, PresentationFramework, WindowsBase
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
. (Join-Path $PSScriptRoot 'read-icon.ps1')
$taskIconSource = Read-AgentMeterIcon (Join-Path $PSScriptRoot 'AgentMeter.svg')
$taskEntries = @(@('StoreLogo.png', 50), @('Square44x44Logo.png', 44), @('Square150x150Logo.png', 150))
$taskIconFrames = @()
foreach ($taskSize in @(16, 20, 24, 32, 40, 48, 64, 128, 256))
{
    $taskEntries += ,@("Icon$taskSize.png", $taskSize)
}
foreach ($entry in $taskEntries)
{
    $taskSize = [int]$entry[1]
    $taskView = New-Object System.Windows.Controls.Image
    $taskView.Source = $taskIconSource
    $taskView.Stretch = [System.Windows.Media.Stretch]::Uniform
    $taskView.Width = $taskSize
    $taskView.Height = $taskSize
    [System.Windows.Media.RenderOptions]::SetBitmapScalingMode($taskView, [System.Windows.Media.BitmapScalingMode]::HighQuality)
    $taskView.Measure((New-Object System.Windows.Size($taskSize, $taskSize)))
    $taskView.Arrange((New-Object System.Windows.Rect(0, 0, $taskSize, $taskSize)))
    $taskView.UpdateLayout()
    $taskBitmap = New-Object System.Windows.Media.Imaging.RenderTargetBitmap($taskSize, $taskSize, 96, 96, [System.Windows.Media.PixelFormats]::Pbgra32)
    $taskBitmap.Render($taskView)
    $taskEncoder = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
    $taskEncoder.Frames.Add([System.Windows.Media.Imaging.BitmapFrame]::Create($taskBitmap))
    $taskStream = [System.IO.File]::Create((Join-Path $OutputDirectory $entry[0]))
    try
    {
        $taskEncoder.Save($taskStream)
    }
    finally
    {
        $taskStream.Dispose()
    }
    if ($entry[0] -like 'Icon*.png')
    {
        $taskIconFrames += [pscustomobject]@{ Size = $taskSize; Bytes = [System.IO.File]::ReadAllBytes((Join-Path $OutputDirectory $entry[0])) }
    }
}
$taskIconStream = [System.IO.File]::Create((Join-Path $OutputDirectory 'AgentMeter.ico'))
$taskWriter = New-Object System.IO.BinaryWriter($taskIconStream)
try
{
    $taskWriter.Write([uint16]0)
    $taskWriter.Write([uint16]1)
    $taskWriter.Write([uint16]$taskIconFrames.Count)
    $taskOffset = 6 + 16 * $taskIconFrames.Count
    foreach ($taskFrame in $taskIconFrames)
    {
        $taskDimension = if ($taskFrame.Size -eq 256)
        {
            0
        }
        else
        {
            $taskFrame.Size
        }
        $taskWriter.Write([byte]$taskDimension)
        $taskWriter.Write([byte]$taskDimension)
        $taskWriter.Write([byte]0)
        $taskWriter.Write([byte]0)
        $taskWriter.Write([uint16]1)
        $taskWriter.Write([uint16]32)
        $taskWriter.Write([uint32]$taskFrame.Bytes.Length)
        $taskWriter.Write([uint32]$taskOffset)
        $taskOffset += $taskFrame.Bytes.Length
    }
    foreach ($taskFrame in $taskIconFrames)
    {
        $taskWriter.Write([byte[]]$taskFrame.Bytes)
    }
}
finally
{
    $taskWriter.Dispose();$taskIconStream.Dispose()
}
