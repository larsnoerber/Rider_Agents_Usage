using System.Windows;
using System.Diagnostics;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using AgentMeter.Windows.UI.Components;

namespace AgentMeter.Windows.UI;

// Drag, collapse and popup gestures share the window lifetime.
internal sealed partial class DesktopWidgetWindow
{
    private void FinishMove()
    {
        WidgetPlacement.Constrain(this, options.WidgetSnapToEdges);
        PositionSaved?.Invoke(Left, Top);
    }

    private void ScheduleCollapse()
    {
        collapseDelay.Stop();
        if (!closing && !refreshing && firstRefreshCompleted && IsVisible && options.WidgetAutoCollapse && !IsMouseOver && !menuOpen && !detailsPopup.IsOpen && !dragging)
            collapseDelay.Start();
    }

    private void CollapseTick(object? sender, EventArgs args)
    {
        collapseDelay.Stop();
        if (!closing && !refreshing && firstRefreshCompleted && options.WidgetAutoCollapse && !IsMouseOver && !menuOpen && !detailsPopup.IsOpen && !dragging) SetCollapsed(true);
    }

    private void SetCollapsed(bool value)
    {
        value = value && !WaitingForProviders;
        collapsed = value;
        strip.Visibility = value || WaitingForProviders ? Visibility.Collapsed : Visibility.Visible;
        collapsedLabel.Visibility = value ? Visibility.Visible : Visibility.Collapsed;
        collapsedLabel.Text = $"AgentMeter | {selected.Length}";
        collapsedLabel.Foreground = colors.TextBrush;
        collapsedLabel.Measure(new Size(double.PositiveInfinity, double.PositiveInfinity));
        surface.Width = value ? collapsedLabel.DesiredSize.Width + dragGrip.Width + dragGrip.Margin.Left
            + dragGrip.Margin.Right + surface.Padding.Left + surface.Padding.Right + 2 : double.NaN;
    }

    public void RefreshTime()
    {
        if (!closing) Render();
    }

    private void PrepareGripDrag()
    {
        // Dismiss popup capture before Thumb takes capture for this drag gesture.
        detailsPopup.IsOpen = false;
        if (Mouse.Captured != null) Mouse.Capture(null);
        dragStart = null;
        dragging = false;
        dismissedProvider = null;
        lastDismissedProvider = null;
    }

    private Button CreateSegment(string id)
    {
        var segment = new Button
        {
            Content = new TextBlock
            {
                VerticalAlignment = VerticalAlignment.Center,
                Foreground = new SolidColorBrush(Color.FromRgb(191, 216, 246))
            },
            ContentTemplate = null,
            Background = Brushes.Transparent,
            BorderThickness = new Thickness(0),
            Padding = new Thickness(0),
            Margin = new Thickness(0),
            Cursor = Cursors.Hand,
            Style = (Style)FindResource("DesktopSegmentButtonStyle")
        };
        ToolTipService.SetIsEnabled(segment, false);
        System.Windows.Automation.AutomationProperties.SetName(segment, $"Show {id} quota details");
        segment.PreviewMouseLeftButtonDown += (_, args) =>
        {
            dismissedProvider = detailsPopup.IsOpen ? detailsProvider :
                Stopwatch.GetElapsedTime(dismissedAt).TotalMilliseconds < 250 ? lastDismissedProvider : null;
            dragging = false;
            dragStart = args.GetPosition(this);
            detailsPopup.IsOpen = false;
            if (args.ClickCount == 2)
            {
                dragStart = null;
                detailsPopup.IsOpen = false;
                dashboardAction();
                args.Handled = true;
            }
        };
        segment.PreviewMouseMove += (_, args) =>
        {
            if (options.WidgetPositionLocked || dragStart is not Point start || args.LeftButton != MouseButtonState.Pressed || dragging) return;
            var current = args.GetPosition(this);
            if (Math.Abs(current.X - start.X) < SystemParameters.MinimumHorizontalDragDistance &&
                Math.Abs(current.Y - start.Y) < SystemParameters.MinimumVerticalDragDistance) return;
            dragging = true;
            dragStart = null;
            segment.ReleaseMouseCapture();
            detailsPopup.IsOpen = false;
            try
            {
                DragMove();
                FinishMove();
            }
            finally { dragStart = null; dragging = false; dismissedProvider = null; ScheduleCollapse(); }
            args.Handled = true;
        };
        segment.PreviewMouseLeftButtonUp += (_, args) =>
        { dragStart = null; if (dragging) { args.Handled = true; dragging = false; } };
        segment.Click += (_, _) =>
        {
            if (detailsPopup.IsOpen && detailsProvider == id || dismissedProvider == id)
            { detailsPopup.IsOpen = false; dismissedProvider = null; return; }
            detailsPopup.IsOpen = false;
            detailsProvider = id;
            dismissedProvider = null;
            detailsPopup.PlacementTarget = segment;
            UpdateDetails(id);
            detailsPopup.IsOpen = true;
        };
        return segment;
    }
}
