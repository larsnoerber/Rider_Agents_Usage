using System.Windows;
using System.Windows.Controls;
using AgentMeter.Windows.Settings;

namespace AgentMeter.Windows.UI.Components;

internal sealed class WidgetOptionsPanel : StackPanel
{
    private readonly Dictionary<string, CheckBox> toggles = new();
    private readonly ListBox order = new()
    {
        Height = 126,
        Width = 230,
        HorizontalAlignment = HorizontalAlignment.Left,
        Background = System.Windows.Media.Brushes.Transparent,
        Foreground = System.Windows.Media.Brushes.White,
        DisplayMemberPath = "Label"
    };
    private bool applying;
    public event Action<Func<UsageSettings, UsageSettings>>? Changed;

    public WidgetOptionsPanel()
    {
        AddToggle("snap", "Snap to screen edges", s => s.WidgetSnapToEdges, (s, v) => s with { WidgetSnapToEdges = v });
        AddToggle("lock", "Lock position", s => s.WidgetPositionLocked, (s, v) => s with { WidgetPositionLocked = v });
        AddToggle("resets", "Show reset countdowns", s => s.WidgetShowResets, (s, v) => s with { WidgetShowResets = v });
        AddToggle("freshness", "Show data freshness", s => s.WidgetShowFreshness, (s, v) => s with { WidgetShowFreshness = v });
        AddToggle("collapse", "Collapse when not in use", s => s.WidgetAutoCollapse, (s, v) => s with { WidgetAutoCollapse = v });
        Children.Add(new TextBlock { Text = "Provider order", Margin = new Thickness(0, 10, 0, 4) });
        Children.Add(order);
        var buttons = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 5, 0, 8) };
        foreach (var (label, delta) in new[] { ("Move up", -1), ("Move down", 1) })
        {
            var button = new Button { Content = label, Margin = new Thickness(0, 0, 6, 0) };
            button.Click += (_, _) => Move(delta);
            buttons.Children.Add(button);
        }
        Children.Add(buttons);
    }

    private void AddToggle(string id, string label, Func<UsageSettings, bool> read,
        Func<UsageSettings, bool, UsageSettings> write)
    {
        var toggle = new CheckBox { Content = label, Tag = read, Margin = new Thickness(0, 3, 0, 3) };
        toggles[id] = toggle;
        RoutedEventHandler handler = (_, _) =>
        {
            if (!applying) Changed?.Invoke(s => write(s, toggle.IsChecked == true));
        };
        toggle.Checked += handler;
        toggle.Unchecked += handler;
        Children.Add(toggle);
    }

    public void Apply(UsageSettings settings)
    {
        applying = true;
        foreach (var toggle in toggles.Values) toggle.IsChecked = ((Func<UsageSettings, bool>)toggle.Tag)(settings);
        var selected = (order.SelectedItem as WidgetProviderChoice)?.Id;
        order.ItemsSource = settings.WidgetProviderOrder.Select(id => new WidgetProviderChoice(id, ProviderCatalog.Get(id).Name)).ToArray();
        order.SelectedItem = order.Items.Cast<WidgetProviderChoice>().FirstOrDefault(p => p.Id == selected);
        if (order.SelectedIndex < 0) order.SelectedIndex = 0;
        applying = false;
    }

    private void Move(int delta)
    {
        var index = order.SelectedIndex;
        var target = index + delta;
        if (index < 0 || target < 0 || target >= order.Items.Count) return;
        var items = order.Items.Cast<WidgetProviderChoice>().ToArray();
        (items[index], items[target]) = (items[target], items[index]);
        order.ItemsSource = items;
        order.SelectedIndex = target;
        Changed?.Invoke(s => s with { WidgetProviderOrder = items.Select(p => p.Id).ToArray() });
    }
}

internal sealed record WidgetProviderChoice(string Id, string Label);
