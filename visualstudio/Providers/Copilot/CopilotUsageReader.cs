using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Reflection;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.ServiceHub.Framework;

namespace AgentsUsage.VisualStudio.Providers.Copilot
{
    /// <summary>Optional adapter to the installed Copilot brokered quota service. Reads no authentication data.</summary>
    internal sealed class CopilotUsageReader
    {
        private readonly IServiceBroker broker;
        public CopilotUsageReader(IServiceBroker broker) { this.broker = broker; }

        public async Task<CopilotUsage> ReadAsync(CancellationToken cancellationToken)
        {
            try
            {
                cancellationToken.ThrowIfCancellationRequested();
                var assembly = AppDomain.CurrentDomain.GetAssemblies()
                    .FirstOrDefault(candidate => candidate.GetName().Name == "Microsoft.VisualStudio.Copilot");
                var contract = assembly?.GetType("Microsoft.VisualStudio.Copilot.ICopilotService");
                var descriptor = assembly?.GetType("Microsoft.VisualStudio.Copilot.CopilotDescriptors")
                    ?.GetProperty("CopilotService", BindingFlags.Public | BindingFlags.Static)?.GetValue(null) as ServiceRpcDescriptor;
                var read = contract?.GetMethod("GetQuotasAsync", new[] { typeof(CancellationToken) });
                if (broker == null || descriptor == null || read == null)
                    return new CopilotUsage { Error = "Copilot quota service is unavailable. Open GitHub Copilot and sign in in Visual Studio." };

                using (var deadline = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken))
                {
                    deadline.CancelAfter(TimeSpan.FromSeconds(30));
                    var connect = typeof(CopilotUsageReader).GetMethod(nameof(ReadQuotasAsync), BindingFlags.NonPublic | BindingFlags.Static)
                        .MakeGenericMethod(contract);
                    return await ((Task<CopilotUsage>)connect.Invoke(null, new object[] { broker, descriptor, read, deadline.Token })).ConfigureAwait(false);
                }
            }
            catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested) { throw; }
            catch (Exception error) when (!(error is OutOfMemoryException))
            {
                // Provider API failures are optional. Do not expose provider exception messages or account data.
                cancellationToken.ThrowIfCancellationRequested();
                return new CopilotUsage { Error = "Copilot quota could not be read. Check Copilot sign-in and update Visual Studio if necessary." };
            }
        }

        private static async Task<CopilotUsage> ReadQuotasAsync<T>(IServiceBroker broker, ServiceRpcDescriptor descriptor,
            MethodInfo read, CancellationToken token) where T : class, IDisposable
        {
            using (var proxy = await broker.GetProxyAsync<T>(descriptor, token).ConfigureAwait(false))
            {
                if (proxy == null) return new CopilotUsage { Error = "GitHub Copilot is not available in this Visual Studio installation." };
                var task = (Task)read.Invoke(proxy, new object[] { token });
                await task.ConfigureAwait(false);
                token.ThrowIfCancellationRequested();
                return Parse(task.GetType().GetProperty("Result")?.GetValue(task) as IEnumerable);
            }
        }

        private static CopilotUsage Parse(IEnumerable reported)
        {
            var result = new CopilotUsage();
            var quotas = new List<CopilotQuota>();
            if (reported != null) foreach (var value in reported)
            {
                var category = Get(value, "Type")?.ToString();
                if (category != "Models" && category != "PremiumModels" && category != "CompletionsModels"
                    && category != "RateLimitWeekly" && category != "RateLimitSession") continue;
                var plan = Get(value, "PlanType") as string;
                if (string.IsNullOrWhiteSpace(result.Plan) && !string.IsNullOrWhiteSpace(plan)) result.Plan = plan;
                var unlimited = Get(value, "Unlimited") as bool? == true;
                var used = Number(Get(value, "Usage"));
                var limit = Number(Get(value, "Limit"));
                var percentage = Number(Get(value, "UsagePercentage"));
                // Copilot may expose a premium placeholder (0 / 0) on Free accounts.
                if (!unlimited && limit.HasValue && limit <= 0) continue;
                if (!unlimited && !percentage.HasValue && !(limit > 0 && used.HasValue)) continue;
                var tokenBilling = Get(value, "IsTokenBasedBilling") as bool? == true;
                var rawReset = Get(value, "ResetTime") as DateTime?;
                var title = category == "PremiumModels" ? (tokenBilling ? "AI credits" : "Premium requests")
                    : category == "Models" ? (tokenBilling ? "AI credits" : "Chat requests")
                    : category == "CompletionsModels" ? "Code completions"
                    : category == "RateLimitWeekly" ? "Weekly quota" : "Session quota";
                quotas.Add(new CopilotQuota
                {
                    Category = category, Title = title, Unlimited = unlimited, TokenBilling = tokenBilling,
                    Used = used, Limit = limit,
                    PercentUsed = unlimited ? (int?)null : (int)Math.Round(Math.Max(0, Math.Min(100, percentage ?? used.Value / limit.Value * 100))),
                    Reset = rawReset.HasValue ? new DateTimeOffset(DateTime.SpecifyKind(rawReset.Value, DateTimeKind.Utc)) : (DateTimeOffset?)null
                });
            }
            var free = result.Plan?.IndexOf("free", StringComparison.OrdinalIgnoreCase) >= 0;
            result.Primary = free ? quotas.FirstOrDefault(q => q.Category == "Models")
                : quotas.FirstOrDefault(q => q.Category == "PremiumModels");
            if (result.Primary == null && string.IsNullOrWhiteSpace(result.Plan))
                result.Primary = quotas.FirstOrDefault(q => q.Category == "Models" && !q.Unlimited);
            result.Primary = result.Primary ?? quotas.FirstOrDefault(q => q.Category == "RateLimitWeekly" || q.Category == "RateLimitSession");
            // Free plans do not use the optional premium allowance as their main quota.
            result.Quotas = free ? quotas.Where(q => q.Category != "PremiumModels").ToArray() : quotas.ToArray();
            if (result.Primary == null) result.Error = "Copilot has not reported an included quota yet. Open Copilot in Visual Studio and refresh.";
            return result;
        }

        private static object Get(object value, string name) => value?.GetType().GetProperty(name)?.GetValue(value);
        private static double? Number(object value)
        {
            if (value == null) return null;
            var number = Convert.ToDouble(value, CultureInfo.InvariantCulture);
            return double.IsNaN(number) || double.IsInfinity(number) ? (double?)null : number;
        }
    }
}
