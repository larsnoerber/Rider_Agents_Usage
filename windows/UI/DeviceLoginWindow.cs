using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace AgentMeter.Windows.UI;

internal sealed class DeviceLoginWindow : Window
{
    public DeviceLoginWindow(string code)
    {
        Title = "Sign in to GitHub Copilot";
        Width = 460;
        SizeToContent = SizeToContent.Height;
        ResizeMode = ResizeMode.NoResize;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        Background = new SolidColorBrush(Color.FromRgb(22, 33, 55));
        var panel = new StackPanel { Margin = new Thickness(24) };
        panel.Children.Add(new TextBlock { Text = "Use this one-time code on GitHub's device sign-in page. The official Copilot agent will open your browser.", TextWrapping = TextWrapping.Wrap });
        panel.Children.Add(new TextBox { Text = code, IsReadOnly = true, FontSize = 24, FontWeight = FontWeights.SemiBold,
            HorizontalContentAlignment = HorizontalAlignment.Center, Margin = new Thickness(0, 18, 0, 18) });
        panel.Children.Add(new TextBlock { Text = "Select and copy the code, then continue. Authentication is stored by the official agent.", TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 0, 0, 18) });
        var buttons = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right };
        var cancel = new Button { Content = "Cancel", IsCancel = true };
        var next = new Button { Content = "Open GitHub sign-in", IsDefault = true };
        next.Click += (_, _) => DialogResult = true;
        buttons.Children.Add(cancel);
        buttons.Children.Add(next);
        panel.Children.Add(buttons);
        Content = panel;
    }
}
