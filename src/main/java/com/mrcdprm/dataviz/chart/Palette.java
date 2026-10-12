package com.mrcdprm.dataviz.chart;

import java.util.List;

/**
 * Renk paletleri. Renk körlüğüne uygun palet Okabe-Ito renkleridir. Her paletin 8. rengi gridir: pastadaki
 * "Diğer" dilimi (7 dilim + Diğer) ve 8. seri nötr renkte görünür.
 */
public enum Palette {
    DEFAULT(List.of("#2f6fdd", "#f59e0b", "#10b981", "#ef4444", "#8b5cf6", "#06b6d4", "#ec4899", "#9ca3af",
            "#f97316", "#64748b")),
    COLORBLIND(List.of("#0072B2", "#E69F00", "#009E73", "#D55E00", "#56B4E9", "#CC79A7", "#F0E442", "#999999",
            "#000000", "#882255")),
    WARM(List.of("#b91c1c", "#ea580c", "#f59e0b", "#facc15", "#dc2626", "#fb923c", "#fbbf24", "#a8a29e",
            "#9a3412", "#fde047")),
    COOL(List.of("#1d4ed8", "#0891b2", "#059669", "#7c3aed", "#2563eb", "#0d9488", "#4f46e5", "#94a3b8",
            "#16a34a", "#6d28d9")),
    MONO(List.of("#1e3a8a", "#1d4ed8", "#3b82f6", "#60a5fa", "#93c5fd", "#172554", "#2563eb", "#94a3b8",
            "#1e40af", "#dbeafe"));

    private final List<String> colors;

    Palette(List<String> colors) {
        this.colors = colors;
    }

    public List<String> colors() {
        return colors;
    }

    /** n'inci serinin rengi; renkler biterse baştan döner. */
    public String color(int index) {
        return colors.get(Math.floorMod(index, colors.size()));
    }
}
