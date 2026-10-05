# Render the checked-in SVG as WPF vector geometry, keeping all icon sizes independent of a raster source.
# This reader intentionally supports only the shape/gradient subset used by AgentMeter.svg.
function Read-AgentMeterIcon([string]$Path)
{
    $svgSettings = [System.Xml.XmlReaderSettings]::new()
    $svgSettings.DtdProcessing = [System.Xml.DtdProcessing]::Prohibit
    $svgSettings.XmlResolver = $null
    $svgReader = [System.Xml.XmlReader]::Create($Path, $svgSettings)
    $svgDocument = [System.Xml.XmlDocument]::new()
    try
    {
        $svgDocument.Load($svgReader)
    }
    finally
    {
        $svgReader.Dispose()
    }
    $svgBrushes = @{ }
    foreach ($gradient in $svgDocument.DocumentElement.SelectNodes("*[local-name()='defs']/*"))
    {
        if ($gradient.LocalName -eq 'linearGradient')
        {
            $brush = [System.Windows.Media.LinearGradientBrush]::new()
            $brush.MappingMode = [System.Windows.Media.BrushMappingMode]::Absolute
            $brush.StartPoint = [System.Windows.Point]::new([double]$gradient.x1, [double]$gradient.y1)
            $brush.EndPoint = [System.Windows.Point]::new([double]$gradient.x2, [double]$gradient.y2)
        }
        elseif ($gradient.LocalName -eq 'radialGradient')
        {
            $brush = [System.Windows.Media.RadialGradientBrush]::new()
            $brush.MappingMode = [System.Windows.Media.BrushMappingMode]::Absolute
            $brush.Center = [System.Windows.Point]::new([double]$gradient.cx, [double]$gradient.cy)
            $brush.GradientOrigin = $brush.Center
            $brush.RadiusX = [double]$gradient.r; $brush.RadiusY = [double]$gradient.r
            # SVG applies these transforms from right to left; WPF groups apply them in collection order.
            $transform = [System.Windows.Media.TransformGroup]::new()
            $transform.Children.Add([System.Windows.Media.ScaleTransform]::new(16, 16))
            $transform.Children.Add([System.Windows.Media.RotateTransform]::new(52))
            $transform.Children.Add([System.Windows.Media.TranslateTransform]::new(15, 14))
            if ($gradient.gradientTransform -ne 'translate(15 14) rotate(52) scale(16)')
            {
                throw 'Unsupported SVG gradient transform.'
            }
            $brush.Transform = $transform
        }
        else
        {
            throw 'Unsupported SVG gradient.'
        }
        foreach ($stop in $gradient.ChildNodes)
        {
            $offset = if ( $stop.HasAttribute('offset'))
            {
                [double]$stop.offset
            }
            else
            {
                0
            }
            $brush.GradientStops.Add([System.Windows.Media.GradientStop]::new(
                    [System.Windows.Media.ColorConverter]::ConvertFromString($stop.GetAttribute('stop-color')), $offset))
        }
        $svgBrushes[$gradient.id] = $brush
    }

    function Get-SvgBrush([string]$Value, [string]$Opacity)
    {
        if (!$Value -or $Value -eq 'none')
        {
            return $null
        }
        if ($Value -match '^url\(#([\w-]+)\)$')
        {
            $paint = $svgBrushes[$Matches[1]].Clone()
        }
        else
        {
            $paint = [System.Windows.Media.SolidColorBrush]::new([System.Windows.Media.ColorConverter]::ConvertFromString($Value))
        }
        if ($Opacity)
        {
            $paint.Opacity = [double]$Opacity
        }
        return $paint
    }

    $drawing = [System.Windows.Media.DrawingGroup]::new()
    $drawing.ClipGeometry = [System.Windows.Media.RectangleGeometry]::new([System.Windows.Rect]::new(0, 0, 40, 40))
    foreach ($shape in $svgDocument.DocumentElement.ChildNodes)
    {
        if ($shape.LocalName -eq 'defs')
        {
            continue
        }
        switch ($shape.LocalName)
        {
            'rect' {
                $geometry = [System.Windows.Media.RectangleGeometry]::new([System.Windows.Rect]::new(0, 0, [double]$shape.width, [double]$shape.height), [double]$shape.rx, [double]$shape.rx)
            }
            'circle' {
                $geometry = [System.Windows.Media.EllipseGeometry]::new([System.Windows.Point]::new([double]$shape.cx, [double]$shape.cy), [double]$shape.r, [double]$shape.r)
            }
            'path' {
                $geometry = [System.Windows.Media.Geometry]::Parse($shape.d)
            }
            default {
                throw 'Unsupported SVG shape.'
            }
        }
        $pen = $null
        $stroke = Get-SvgBrush $shape.stroke $shape.GetAttribute('stroke-opacity')
        if ($stroke)
        {
            $pen = [System.Windows.Media.Pen]::new($stroke, [double]$shape.GetAttribute('stroke-width'))
            if ( $shape.GetAttribute('stroke-dasharray'))
            {
                $dashes = [System.Windows.Media.DoubleCollection]::new()
                foreach ($dash in ($shape.GetAttribute('stroke-dasharray') -split ' '))
                {
                    $dashes.Add([double]$dash / $pen.Thickness)
                }
                $pen.DashStyle = [System.Windows.Media.DashStyle]::new($dashes, 0)
            }
            if ($shape.GetAttribute('stroke-linecap') -eq 'round')
            {
                $pen.StartLineCap = 'Round'; $pen.EndLineCap = 'Round'
            }
            if ($shape.GetAttribute('stroke-linejoin') -eq 'round')
            {
                $pen.LineJoin = 'Round'
            }
        }
        $item = [System.Windows.Media.DrawingGroup]::new()
        $item.Children.Add([System.Windows.Media.GeometryDrawing]::new((Get-SvgBrush $shape.fill $shape.GetAttribute('fill-opacity')), $pen, $geometry))
        if ($shape.transform -match '^rotate\((-?[0-9.]+) ([0-9.]+) ([0-9.]+)\)$')
        {
            $item.Transform = [System.Windows.Media.RotateTransform]::new([double]$Matches[1], [double]$Matches[2], [double]$Matches[3])
        }
        elseif ($shape.transform -match '^translate\((-?[0-9.]+) (-?[0-9.]+)\)$')
        {
            $item.Transform = [System.Windows.Media.TranslateTransform]::new([double]$Matches[1], [double]$Matches[2])
        }
        elseif ($shape.transform)
        {
            throw 'Unsupported SVG shape transform.'
        }
        $drawing.Children.Add($item)
    }
    $source = [System.Windows.Media.DrawingImage]::new($drawing)
    $source.Freeze()
    return $source
}
