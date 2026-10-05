using System.Globalization;
using System.Text.Json.Nodes;

namespace AgentMeter.Windows.Core;

internal static class JsonFields
{
    public static JsonObject? Object(this JsonNode? node, string key) => (node as JsonObject)?[key] as JsonObject;
    public static string? Text(this JsonNode? node, string key) =>
        (node as JsonObject)?[key] is JsonValue value && value.TryGetValue<string>(out var text)
            && !string.IsNullOrWhiteSpace(text) ? text : null;
    public static double? Number(this JsonNode? node, string key)
    {
        var value = (node as JsonObject)?[key] as JsonValue;
        if (value == null) return null;
        if (!value.TryGetValue<double>(out var number) &&
            !(value.TryGetValue<string>(out var text) && double.TryParse(text, NumberStyles.Float,
                CultureInfo.InvariantCulture, out number))) return null;
        return double.IsFinite(number) && number >= 0 ? number : null;
    }
    public static bool Flag(this JsonNode? node, string key) =>
        (node as JsonObject)?[key] is JsonValue value && value.TryGetValue<bool>(out var flag) && flag;
    public static DateTimeOffset? Date(string? value) => DateTimeOffset.TryParse(value,
        CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal, out var date) ? date : null;
    public static DateTimeOffset? Unix(double? seconds) => seconds is >= 0 and <= 253402300799
        ? DateTimeOffset.FromUnixTimeSeconds((long)seconds.Value) : null;
    public static double Clamp(double value) => Math.Clamp(value, 0, 100);
}
