using System;
using System.Diagnostics;
using System.Reflection;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using AgentsUsage.VisualStudio.Providers.Codex;
using AgentsUsage.VisualStudio.Settings;
using Microsoft.VisualStudio.PlatformUI;
using Microsoft.VisualStudio.Shell;

namespace AgentsUsage.VisualStudio.UI
{
    internal sealed class UsageControl : UserControl, IDisposable
    {
        private readonly UsageOptions options;
        private readonly CodexUsageReader reader = new CodexUsageReader();
        private readonly CancellationTokenSource lifetime = new CancellationTokenSource();
        private readonly DispatcherTimer timer;
        private readonly StackPanel content = new StackPanel();
        private readonly Button refreshButton = new Button { Content = "Refresh", Padding = new Thickness(10, 4, 10, 4) };
        private bool refreshing;
        private bool disposed;
        private static readonly Brush Green = new SolidColorBrush(Color.FromRgb(100, 205, 135));
        private static readonly Brush Amber = new SolidColorBrush(Color.FromRgb(255, 185, 75));
        private static readonly Brush Red = new SolidColorBrush(Color.FromRgb(255, 110, 110));

        public UsageControl(UsageOptions options, Action openSettings)
        {
            this.options = options;
            SetResourceReference(BackgroundProperty, EnvironmentColors.ToolWindowBackgroundBrushKey);
            SetResourceReference(ForegroundProperty, EnvironmentColors.ToolWindowTextBrushKey);
            var root = new DockPanel { Margin = new Thickness(12) };
            var header = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 0, 0, 12) };
            header.Children.Add(new Image
            {
                Source = new BitmapImage(new Uri("pack://application:,,,/AgentsUsage.VisualStudio;component/Resources/icon.png")),
                Width = 28, Height = 28, Margin = new Thickness(0, 0, 8, 0)
            });
            header.Children.Add(Label("Agents Usage", 16, true));
            DockPanel.SetDock(header, Dock.Top);
            root.Children.Add(header);
            var toolbar = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 0, 0, 12) };
            toolbar.Children.Add(refreshButton);
            var settings = new Button { Content = "Settings", Padding = new Thickness(10, 4, 10, 4), Margin = new Thickness(8, 0, 0, 0) };
            settings.Click += (sender, args) => openSettings();
            toolbar.Children.Add(settings);
            var repository = new Button { Content = "GitHub", Padding = new Thickness(10, 4, 10, 4), Margin = new Thickness(8, 0, 0, 0) };
            repository.Click += (sender, args) => Process.Start(new ProcessStartInfo("https://github.com/larsnoerber/Rider_Agents_Usage") { UseShellExecute = true });
            toolbar.Children.Add(repository);
            DockPanel.SetDock(toolbar, Dock.Top);
            root.Children.Add(toolbar);
            var version = typeof(UsageControl).Assembly.GetCustomAttribute<AssemblyInformationalVersionAttribute>()?.InformationalVersion.Split('+')[0];
            var footer = Label("Version " + (version ?? "Unknown"), 11);
            footer.Margin = new Thickness(0, 10, 0, 0);
            DockPanel.SetDock(footer, Dock.Bottom);
            root.Children.Add(footer);
            root.Children.Add(new ScrollViewer { Content = content, VerticalScrollBarVisibility = ScrollBarVisibility.Auto });
            Content = root;
            timer = new DispatcherTimer();
            timer.Tick += OnTick;
            options.Changed += OnOptionsChanged;
            Loaded += OnLoaded;
            Unloaded += OnUnloaded;
            refreshButton.Click += OnRefresh;
            ShowMessage("Open Usage to read the reported Codex quota.");
        }

        private void OnLoaded(object sender, RoutedEventArgs args) { ConfigureTimer(); _ = RefreshAsync(); }
        private void OnUnloaded(object sender, RoutedEventArgs args) { timer.Stop(); }
        private void OnTick(object sender, EventArgs args) { _ = RefreshAsync(); }
        private void OnRefresh(object sender, RoutedEventArgs args) { _ = RefreshAsync(); }
        private void OnOptionsChanged(object sender, EventArgs args)
        {
            if (disposed) return;
            ConfigureTimer();
            if (!refreshing) _ = RefreshAsync();
        }

        private void ConfigureTimer()
        {
            timer.Stop();
            timer.Interval = TimeSpan.FromSeconds(Math.Max(10, Math.Min(3600, options.RefreshIntervalSeconds)));
            if (IsLoaded && options.CodexEnabled && !disposed) timer.Start();
        }

        private async Task RefreshAsync()
        {
            if (disposed || refreshing) return;
            if (!options.CodexEnabled) { ShowMessage("Enable OpenAI Codex in Settings to see its quota."); return; }
            refreshing = true;
            refreshButton.IsEnabled = false;
            var path = options.CodexPath;
            var cancellationToken = lifetime.Token;
            try
            {
                var usage = await Task.Run(() => reader.ReadAsync(path, cancellationToken));
                if (disposed) return;
                if (options.CodexEnabled) Render(usage);
                else ShowMessage("Enable OpenAI Codex in Settings to see its quota.");
            }
            catch (OperationCanceledException) { }
            finally
            {
                refreshing = false;
                if (!disposed) refreshButton.IsEnabled = true;
                else lifetime.Dispose();
            }
        }

        private void Render(CodexUsage usage)
        {
            content.Children.Clear();
            var card = new StackPanel { Margin = new Thickness(12) };
            card.Children.Add(Label("OpenAI Codex", 15, true));
            card.Children.Add(Label("Plan: " + (string.IsNullOrWhiteSpace(usage.Plan) ? "Unknown" : usage.Plan), 12));
            AddQuota(card, "5-hour quota", usage.FiveHourLeft, usage.FiveHourReset);
            AddQuota(card, "Weekly quota", usage.WeeklyLeft, usage.WeeklyReset);
            if (usage.Credits.HasValue) card.Children.Add(Label("Credits: " + usage.Credits.Value.ToString("N2"), 12));
            if (usage.Error != null) card.Children.Add(Label(usage.Error, 12));
            card.Children.Add(Label("Updated: " + usage.UpdatedAt.ToLocalTime().ToString("g"), 11));
            var surface = new Border { Child = card, BorderThickness = new Thickness(1), CornerRadius = new CornerRadius(6) };
            surface.SetResourceReference(Border.BorderBrushProperty, EnvironmentColors.ToolWindowTextBrushKey);
            content.Children.Add(surface);
        }

        private static void AddQuota(Panel panel, string title, int? remaining, DateTimeOffset? reset)
        {
            if (!remaining.HasValue) return;
            var color = remaining >= 50 ? Green : remaining >= 20 ? Amber : Red;
            var row = new DockPanel { Margin = new Thickness(0, 14, 0, 6) };
            var value = Label(remaining.Value + "% remaining", 13, true);
            value.Foreground = color;
            DockPanel.SetDock(value, Dock.Right);
            row.Children.Add(value);
            row.Children.Add(Label(title, 13));
            panel.Children.Add(row);
            panel.Children.Add(new ProgressBar { Minimum = 0, Maximum = 100, Value = remaining.Value, Height = 7, Foreground = color });
            if (reset.HasValue) panel.Children.Add(Label("Resets: " + reset.Value.ToLocalTime().ToString("g"), 11));
        }

        private void ShowMessage(string message) { content.Children.Clear(); content.Children.Add(Label(message, 13)); }
        private static TextBlock Label(string text, double size, bool bold = false) => new TextBlock
        {
            Text = text, FontSize = size, FontWeight = bold ? FontWeights.SemiBold : FontWeights.Normal,
            TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 2, 0, 2)
        };

        public void Dispose()
        {
            if (disposed) return;
            disposed = true;
            timer.Stop();
            timer.Tick -= OnTick;
            options.Changed -= OnOptionsChanged;
            Loaded -= OnLoaded;
            Unloaded -= OnUnloaded;
            refreshButton.Click -= OnRefresh;
            lifetime.Cancel();
            reader.Dispose();
            if (!refreshing) lifetime.Dispose();
        }
    }
}
