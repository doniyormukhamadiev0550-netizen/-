package dev.lumina;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class Gfx {
    /** Global fade multiplier used for open/close animations. */
    public static float alpha = 1f;

    public static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static int a(int c) {
        int al = Math.round(((c >>> 24) & 255) * alpha);
        return (al << 24) | (c & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int width(String s) { return mc().textRenderer.getWidth(s); }

    public static void text(DrawContext g, String s, int x, int y, int c) {
        g.drawText(mc().textRenderer, s, x, y, a(c), true);
    }

    /** Text for themed panels (no shadow on the light theme). */
    public static void ui(DrawContext g, String s, int x, int y, int c) {
        g.drawText(mc().textRenderer, s, x, y, a(c), Theme.dark);
    }

    public static void spaced(DrawContext g, String s, int cx, int y, int c, int sp) {
        int w = -sp;
        for (char ch : s.toCharArray()) w += width(String.valueOf(ch)) + sp;
        int x = cx - w / 2;
        for (char ch : s.toCharArray()) {
            String t = String.valueOf(ch);
            text(g, t, x, y, c);
            x += width(t) + sp;
        }
    }

    public static void fill(DrawContext g, int x, int y, int w, int h, int c) { g.fill(x, y, x + w, y + h, a(c)); }

    public static void rrect(DrawContext g, int x, int y, int w, int h, int r, int c) {
        r = Math.min(r, Math.min(w, h) / 2);
        c = a(c);
        g.fill(x, y + r, x + w, y + h - r, c);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int d = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            g.fill(x + d, y + i, x + w - d, y + i + 1, c);
            g.fill(x + d, y + h - i - 1, x + w - d, y + h - i, c);
        }
    }

    public static void circle(DrawContext g, int cx, int cy, int r, int c) {
        c = a(c);
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, c);
        }
    }

    public static void outline(DrawContext g, int x, int y, int w, int h, int c) {
        c = a(c);
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    /** Horizontal gradient bar made of short segments. */
    public static void gradBar(DrawContext g, int x, int y, int w, int h, int c1, int c2) {
        int n = Math.max(1, Math.min(32, w / 4));
        for (int i = 0; i < n; i++) {
            int x0 = x + w * i / n, x1 = x + w * (i + 1) / n;
            g.fill(x0, y, x1, y + h, a(lerp(c1, c2, n == 1 ? 0f : (float) i / (n - 1))));
        }
    }

    /** "Glass" panel: translucent body, soft top sheen and a two-color rim. */
    public static void glass(DrawContext g, int x, int y, int w, int h, int r) {
        rrect(g, x, y, w, h, r, Theme.glass());
        g.fillGradient(x + r, y + 1, x + w - r, y + Math.max(3, h / 2), a(Theme.glassHi()), a(0x00FFFFFF));
        int b1 = Theme.withAlpha(Theme.c1, 0x99), b2 = Theme.withAlpha(Theme.c2, 0x99);
        gradBar(g, x + r, y, w - 2 * r, 1, b1, b2);
        gradBar(g, x + r, y + h - 1, w - 2 * r, 1, Theme.withAlpha(Theme.c2, 0x55), Theme.withAlpha(Theme.c1, 0x55));
        g.fill(x, y + r, x + 1, y + h - r, a(Theme.withAlpha(Theme.c1, 0x55)));
        g.fill(x + w - 1, y + r, x + w, y + h - r, a(Theme.withAlpha(Theme.c2, 0x55)));
    }

    /** Small box behind HUD elements. */
    public static void hud(DrawContext g, int x, int y, int w, int h) {
        rrect(g, x, y, w, h, 3, 0x99101018);
        g.fillGradient(x + 1, y + 3, x + 3, y + h - 3, a(Theme.c1), a(Theme.c2));
    }

    // --- 5x7 block font for the logo ---
    private static final String[] L = {"X....", "X....", "X....", "X....", "X....", "X....", "XXXXX"};
    private static final String[] U = {"X...X", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX."};
    private static final String[] M = {"X...X", "XX.XX", "X.X.X", "X...X", "X...X", "X...X", "X...X"};
    private static final String[] I = {"XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "XXXXX"};
    private static final String[] N = {"X...X", "XX..X", "X.X.X", "X..XX", "X...X", "X...X", "X...X"};
    private static final String[] A = {".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"};

    public static void logo(DrawContext g, int cx, int y, int px) {
        String[][] letters = {L, U, M, I, N, A};
        int lw = 5 * px, gap = px, total = letters.length * lw + (letters.length - 1) * gap;
        int x0 = cx - total / 2;
        for (int pass = 0; pass < 2; pass++) {
            for (int li = 0; li < letters.length; li++) {
                for (int row = 0; row < 7; row++) {
                    for (int col = 0; col < 5; col++) {
                        if (letters[li][row].charAt(col) != 'X') continue;
                        int gx = x0 + li * (lw + gap) + col * px;
                        float t = (float) (li * 5 + col) / (letters.length * 5 - 1);
                        if (pass == 0) g.fill(gx + 1, y + row * px + 2, gx + px + 1, y + row * px + px + 2, a(0x66000000));
                        else g.fill(gx, y + row * px, gx + px, y + row * px + px, a(lerp(Theme.c1, Theme.c2, t) | 0xFF000000));
                    }
                }
            }
        }
    }
}
