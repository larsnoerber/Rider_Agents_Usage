using System.Runtime.InteropServices;
using System.Security;
using System.Security.Cryptography;

namespace AgentMeter.Windows.Providers.OpenRouter;

// Explicitly entered keys persist only in the current user's Windows credential vault.
internal static class OpenRouterKeyStore
{
    private const string Target = "AgentMeter/OpenRouter";
    private const uint Generic = 1;
    private const int MaximumBytes = 2560;

    internal static bool Save(SecureString key)
    {
        if (key.Length == 0 || key.Length * 2 > MaximumBytes) return false;
        var pointer = Marshal.SecureStringToGlobalAllocUnicode(key);
        try
        {
            var credential = new Credential
            {
                Type = Generic, TargetName = Target, UserName = "OpenRouter",
                CredentialBlob = pointer, CredentialBlobSize = (uint)(key.Length * 2), Persist = 2
            };
            return CredWrite(ref credential, 0);
        }
        finally { Marshal.ZeroFreeGlobalAllocUnicode(pointer); }
    }

    internal static string? Read()
    {
        if (!CredRead(Target, Generic, 0, out var pointer)) return null;
        try
        {
            var credential = Marshal.PtrToStructure<Credential>(pointer);
            var size = credential.CredentialBlobSize;
            if (size == 0 || size > MaximumBytes || size % 2 != 0 || credential.CredentialBlob == IntPtr.Zero) return null;
            try { return Marshal.PtrToStringUni(credential.CredentialBlob, (int)size / 2); }
            finally
            {
                var zero = new byte[size];
                Marshal.Copy(zero, 0, credential.CredentialBlob, zero.Length);
                CryptographicOperations.ZeroMemory(zero);
            }
        }
        finally { CredFree(pointer); }
    }

    internal static bool Delete() => CredDelete(Target, Generic, 0) || Marshal.GetLastWin32Error() == 1168;

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    private struct Credential
    {
        public uint Flags, Type;
        public string? TargetName, Comment;
        public System.Runtime.InteropServices.ComTypes.FILETIME LastWritten;
        public uint CredentialBlobSize;
        public IntPtr CredentialBlob;
        public uint Persist, AttributeCount;
        public IntPtr Attributes;
        public string? TargetAlias, UserName;
    }

    [DllImport("advapi32.dll", EntryPoint = "CredWriteW", CharSet = CharSet.Unicode, SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CredWrite(ref Credential credential, uint flags);

    [DllImport("advapi32.dll", EntryPoint = "CredReadW", CharSet = CharSet.Unicode, SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CredRead(string target, uint type, uint flags, out IntPtr credential);

    [DllImport("advapi32.dll", EntryPoint = "CredDeleteW", CharSet = CharSet.Unicode, SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CredDelete(string target, uint type, uint flags);

    [DllImport("advapi32.dll")]
    private static extern void CredFree(IntPtr buffer);
}
