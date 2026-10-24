package dev.lumina;

import net.minecraft.client.gui.DrawContext;

/** "Lumina Client" tag shown in the bottom-right corner of inventory screens. */
public final class Badge {
    public static void draw(DrawContext g, int sw, int sh) {
        Gfx.alpha = 1f;
        String s = "Lumina Client";
        int w = Gfx.width(s) + 22, h = 16, x = sw - w - 6, y = sh - h - 6;
        Gfx.glass(g, x, y, w, h, 4);
        Gfx.circle(g, x + 8, y + 8, 3, Theme.c1);
        Gfx.ui(g, s, x + 15, y + 4, Theme.text());
    }
}
