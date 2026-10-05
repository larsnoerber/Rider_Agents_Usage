import "./quota-display.js";

export const percentText = value => `${Math.round(value)}%`;

export function quotaColor(quota, warning = 25) {
    return `var(--${globalThis.__agentMeterDisplay.level(quota, warning)})`;
}

export function ageText(value) {
    return globalThis.__agentMeterDisplay.age(value);
}

export function resetText(value) {
    return globalThis.__agentMeterDisplay.reset(value);
}
