// Position handling reads only the overlay's geometry, never website content.
(() => {
    globalThis.__agentMeterPosition = (host, handle, settings, save, reportError) => {
        let drag = null;
        const style = (name, value) => host.style.setProperty(name, value, "important");
        const bounds = () => ({
            x: Math.max(0, innerWidth - host.offsetWidth - 16),
            y: Math.max(0, innerHeight - host.offsetHeight - 16)
        });
        const clamp = (value, max) => Math.max(8, Math.min(max + 8, value));
        const place = (x, y) => {
            const max = bounds();
            style("left", `${clamp(x, max.x)}px`);
            style("top", `${clamp(y, max.y)}px`);
            style("right", "auto");
            style("bottom", "auto");
        };
        const apply = () => {
            if (drag || !host.isConnected) return;
            const value = settings();
            if (value.barAnchor) {
                const max = bounds();
                place(8 + value.barAnchor.x * max.x, 8 + value.barAnchor.y * max.y);
            } else {
                style("left", "auto");
                style("right", "16px");
                style("top", value.pageBarPosition === "top" ? "12px" : "auto");
                style("bottom", value.pageBarPosition === "bottom" ? "12px" : "auto");
            }
        };
        const persist = () => {
            const rect = host.getBoundingClientRect();
            const max = bounds();
            const position = {x: max.x ? (rect.left - 8) / max.x : 0, y: max.y ? (rect.top - 8) / max.y : 0};
            settings().barAnchor = position;
            void save(position).catch(reportError);
        };
        const down = event => {
            if (!event.isTrusted || event.button !== 0) return;
            const rect = host.getBoundingClientRect();
            drag = {id: event.pointerId, x: event.clientX, y: event.clientY, left: rect.left, top: rect.top};
            handle.setPointerCapture(event.pointerId);
            event.preventDefault();
            event.stopPropagation();
        };
        const move = event => {
            if (!drag || event.pointerId !== drag.id) return;
            place(drag.left + event.clientX - drag.x, drag.top + event.clientY - drag.y);
            event.stopPropagation();
        };
        const up = event => {
            if (!drag || event.pointerId !== drag.id) return;
            move(event);
            drag = null;
            persist();
            if (handle.hasPointerCapture(event.pointerId)) handle.releasePointerCapture(event.pointerId);
        };
        const cancel = () => {
            drag = null;
            apply();
        };
        const key = event => {
            if (!event.isTrusted || !["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown"].includes(event.key)) return;
            event.preventDefault();
            const rect = host.getBoundingClientRect();
            const step = event.shiftKey ? 50 : 10;
            place(rect.left + (event.key === "ArrowLeft" ? -step : event.key === "ArrowRight" ? step : 0),
                rect.top + (event.key === "ArrowUp" ? -step : event.key === "ArrowDown" ? step : 0));
            persist();
        };
        handle.addEventListener("pointerdown", down);
        handle.addEventListener("pointermove", move);
        handle.addEventListener("pointerup", up);
        handle.addEventListener("pointercancel", cancel);
        handle.addEventListener("lostpointercapture", cancel);
        handle.addEventListener("keydown", key);
        window.addEventListener("resize", apply);
        return {
            apply, dragging: () => Boolean(drag),
            dispose() {
                drag = null;
                handle.removeEventListener("pointerdown", down);
                handle.removeEventListener("pointermove", move);
                handle.removeEventListener("pointerup", up);
                handle.removeEventListener("pointercancel", cancel);
                handle.removeEventListener("lostpointercapture", cancel);
                handle.removeEventListener("keydown", key);
                window.removeEventListener("resize", apply);
            }
        };
    };
})();
