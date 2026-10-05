using System.IO;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Data.Sqlite;

namespace AgentMeter.Windows.Providers.Gemini;

/// <summary>Reads only Gemini's two session cookies from the standalone app profile.</summary>
internal static class GeminiAppCookieReader
{
    private const string SessionCookie = "__Secure-1PSID";
    private const string SessionTimeCookie = "__Secure-1PSIDTS";

    public static bool TryRead(out string cookieHeader, out string reason)
    {
        cookieHeader = "";
        reason = "Gemini app session cookies are unavailable. Sign in to Gemini, then refresh.";
        var foundDatabase = false;
        var unreadable = false;
        foreach (var (database, localState) in Profiles())
        {
            if (!File.Exists(database) || !File.Exists(localState)) continue;
            foundDatabase = true;
            if (TryReadProfile(database, localState, out cookieHeader, out var failed)) return true;
            unreadable |= failed;
        }
        if (foundDatabase && unreadable)
            reason = "Gemini local session store is temporarily unavailable. AgentMeter will retry automatically.";
        else if (foundDatabase)
            reason = "No Gemini session cookie was found in the Gemini app or Edge profile. Sign in to Gemini, then refresh.";
        return false;
    }

    // Read one profile at a time, so rejected app cookies cannot hide a working Edge session.
    internal static IEnumerable<string> AvailableSessions()
    {
        foreach (var (database, localState) in Profiles())
            if (File.Exists(database) && File.Exists(localState) &&
                TryReadProfile(database, localState, out var cookie, out _))
                yield return cookie;
    }

    private static IEnumerable<(string Profile, string LocalState)> Profiles()
    {
        var candidates = new List<(string Profile, string LocalState)>();
        var geminiProfile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Gemini");
        candidates.Add((Path.Combine(geminiProfile, "Network", "Cookies"), Path.Combine(geminiProfile, "Local State")));
        var edgeRoot = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Microsoft", "Edge", "User Data");
        if (Directory.Exists(edgeRoot))
        {
            foreach (var profile in Directory.EnumerateDirectories(edgeRoot)
                         .Where(path => Path.GetFileName(path) == "Default" || Path.GetFileName(path).StartsWith("Profile ", StringComparison.Ordinal))
                         .OrderBy(path => Path.GetFileName(path) == "Default" ? 0 : 1).Take(10))
                candidates.Add((Path.Combine(profile, "Network", "Cookies"), Path.Combine(edgeRoot, "Local State")));
        }

        return candidates;
    }

    private static bool TryReadProfile(string database, string localState, out string cookieHeader, out bool unreadable)
    {
        cookieHeader = "";
        unreadable = false;
        byte[]? key = null;
        try
        {
            key = ReadEncryptionKey(localState);
            var values = new Dictionary<string, string>(StringComparer.Ordinal);
            using var store = new GeminiCookieDatabase(database);
            var connection = store.Connection;
            using var command = connection.CreateCommand();
            command.CommandText = "SELECT host_key, name, encrypted_value, expires_utc FROM cookies " +
                                  "WHERE name IN ($session, $time) AND " +
                                  "host_key IN ('google.com', '.google.com', 'gemini.google.com', '.gemini.google.com')";
            command.Parameters.AddWithValue("$session", SessionCookie);
            command.Parameters.AddWithValue("$time", SessionTimeCookie);
            using var reader = command.ExecuteReader();
            while (reader.Read())
            {
                var hostKey = reader.GetString(0);
                var name = reader.GetString(1);
                // Chromium stores expires_utc as microseconds since 1601, while FromFileTime expects 100 ns ticks.
                if (IsExpired(reader.GetInt64(3))) continue;
                var encrypted = (byte[])reader[2];
                var value = DecryptCookie(encrypted, key, hostKey);
                if (!string.IsNullOrWhiteSpace(value)) values[name] = value;
            }

            if (!values.TryGetValue(SessionCookie, out var session)) return false;
            cookieHeader = values.TryGetValue(SessionTimeCookie, out var sessionTime)
                ? $"{SessionCookie}={session}; {SessionTimeCookie}={sessionTime}"
                : $"{SessionCookie}={session}";
            return true;
        }
        catch (Exception error) when (error is IOException or UnauthorizedAccessException or SqliteException or
                                      JsonException or CryptographicException or InvalidOperationException or ArgumentException)
        {
            // A browser may hold its database open. Do not expose database errors or cookie material.
            unreadable = true;
            return false;
        }
        finally
        {
            if (key != null) CryptographicOperations.ZeroMemory(key);
        }
    }

    private static bool IsExpired(long chromiumExpiration)
    {
        if (chromiumExpiration <= 0) return false; // Session cookie.
        try { return DateTimeOffset.FromFileTime(checked(chromiumExpiration * 10)) <= DateTimeOffset.UtcNow; }
        catch (Exception error) when (error is ArgumentOutOfRangeException or OverflowException) { return true; }
    }

    private static byte[] ReadEncryptionKey(string localState)
    {
        using var document = JsonDocument.Parse(File.ReadAllText(localState));
        var encoded = document.RootElement.GetProperty("os_crypt").GetProperty("encrypted_key").GetString();
        if (string.IsNullOrWhiteSpace(encoded)) throw new CryptographicException();
        var wrapped = Convert.FromBase64String(encoded);
        try
        {
            if (wrapped.Length <= 5 || Encoding.ASCII.GetString(wrapped, 0, 5) != "DPAPI")
                throw new CryptographicException();
            return Unprotect(wrapped.AsSpan(5).ToArray());
        }
        finally { CryptographicOperations.ZeroMemory(wrapped); }
    }

    private static string DecryptCookie(byte[] encrypted, byte[] key, string hostKey)
    {
        if (encrypted.Length >= 3 && encrypted[0] == 'v' && encrypted[1] == '1' && encrypted[2] == '0')
        {
            if (encrypted.Length < 3 + 12 + 16) throw new CryptographicException();
            var plaintext = new byte[encrypted.Length - 3 - 12 - 16];
            using var aes = new AesGcm(key, 16);
            aes.Decrypt(encrypted.AsSpan(3, 12), encrypted.AsSpan(15, plaintext.Length),
                encrypted.AsSpan(encrypted.Length - 16, 16), plaintext);
            try { return DecodeCookie(plaintext, hostKey); }
            finally { CryptographicOperations.ZeroMemory(plaintext); }
        }
        // Chromium app-bound v20 cookies require a platform broker; do not bypass it.
        if (encrypted.Length >= 3 && encrypted[0] == 'v' && encrypted[1] == '2' && encrypted[2] == '0')
            throw new CryptographicException();
        var legacy = Unprotect(encrypted);
        try { return DecodeCookie(legacy, hostKey); }
        finally { CryptographicOperations.ZeroMemory(legacy); }
    }

    private static string DecodeCookie(byte[] plaintext, string hostKey)
    {
        var hostHash = SHA256.HashData(Encoding.UTF8.GetBytes(hostKey));
        var offset = plaintext.Length >= hostHash.Length &&
                     CryptographicOperations.FixedTimeEquals(plaintext.AsSpan(0, hostHash.Length), hostHash)
            ? hostHash.Length : 0;
        CryptographicOperations.ZeroMemory(hostHash);
        var value = Encoding.UTF8.GetString(plaintext.AsSpan(offset));
        // Cookie values must be printable ASCII and cannot contain header separators.
        if (value.Length == 0 || value.Any(character => character < 0x21 || character > 0x7e || character is ';' or ',' or '"'))
            throw new CryptographicException();
        return value;
    }

    private static byte[] Unprotect(byte[] input)
    {
        var inputBlob = new DataBlob(input.Length, Marshal.AllocHGlobal(input.Length));
        var output = default(DataBlob);
        try
        {
            Marshal.Copy(input, 0, inputBlob.Data, input.Length);
            if (!CryptUnprotectData(ref inputBlob, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero, 0, out output))
                throw new CryptographicException();
            var result = new byte[output.Length];
            Marshal.Copy(output.Data, result, 0, result.Length);
            return result;
        }
        finally
        {
            Marshal.FreeHGlobal(inputBlob.Data);
            if (output.Data != IntPtr.Zero) LocalFree(output.Data);
            CryptographicOperations.ZeroMemory(input);
        }
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct DataBlob(int length, IntPtr data)
    {
        public int Length = length;
        public IntPtr Data = data;
    }

    [DllImport("crypt32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CryptUnprotectData(ref DataBlob input, IntPtr description, IntPtr entropy,
        IntPtr reserved, IntPtr prompt, int flags, out DataBlob output);

    [DllImport("kernel32.dll")]
    private static extern IntPtr LocalFree(IntPtr memory);
}
