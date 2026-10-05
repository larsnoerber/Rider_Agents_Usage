using System.IO;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using Microsoft.Data.Sqlite;

namespace AgentMeter.Windows.Providers.Gemini;

// Owns a direct read-only connection, or a short-lived encrypted snapshot entirely in RAM.
internal sealed class GeminiCookieDatabase : IDisposable
{
    internal SqliteConnection Connection { get; private set; }
    private IntPtr snapshot;
    private int snapshotLength;

    internal GeminiCookieDatabase(string path)
    {
        Connection = new SqliteConnection(new SqliteConnectionStringBuilder
        { DataSource = path, Mode = SqliteOpenMode.ReadOnly, Cache = SqliteCacheMode.Private, Pooling = false }.ToString());
        try { Connection.Open(); }
        catch (SqliteException error) when (error.SqliteErrorCode is 14 or 5 or 6)
        {
            Connection.Dispose();
            Connection = new SqliteConnection("Data Source=:memory:;Pooling=False");
            byte[]? bytes = null;
            try
            {
                bytes = GeminiCookieFile.ReadLockedDatabase(path);
                snapshotLength = bytes.Length;
                snapshot = Marshal.AllocHGlobal(snapshotLength);
                Marshal.Copy(bytes, 0, snapshot, snapshotLength);
                Connection.Open();
                if (SQLitePCL.raw.sqlite3_deserialize(Connection.Handle, "main", snapshot, snapshotLength,
                        snapshotLength, 4) != 0) // SQLITE_DESERIALIZE_READONLY; we own and clear the buffer.
                    throw new IOException("Session store snapshot unavailable.");
                using var check = Connection.CreateCommand();
                check.CommandText = "PRAGMA quick_check";
                if (!string.Equals(check.ExecuteScalar() as string, "ok", StringComparison.Ordinal))
                    throw new IOException("Session store snapshot inconsistent.");
            }
            catch { Dispose(); throw; }
            finally { if (bytes != null) CryptographicOperations.ZeroMemory(bytes); }
        }
        catch { Connection.Dispose(); throw; }
    }

    public void Dispose()
    {
        Connection.Dispose();
        if (snapshot == IntPtr.Zero) return;
        var zeros = new byte[Math.Min(8192, snapshotLength)];
        for (var offset = 0; offset < snapshotLength; offset += zeros.Length)
            Marshal.Copy(zeros, 0, snapshot + offset, Math.Min(zeros.Length, snapshotLength - offset));
        Marshal.FreeHGlobal(snapshot);
        snapshot = IntPtr.Zero;
    }
}
