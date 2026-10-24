package dev.lumina.gui;

import dev.lumina.Anim;
import dev.lumina.Gfx;
import dev.lumina.Theme;

/** Animated buttons shared by the menus. */
final class Widgets {
    static boolean hit(float x, float y, float w, float h, int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    /** Black "Lunar-style" button; grows and glows with the theme colors on hover. Returns true on click. */
    static boolean black(String id, String label, int x, int y, int w, int h, int mx, int my, boolean click, boolean red) {
        boolean hov = hit(x, y, w, h, mx, my);
        float t = Anim.to("b:" + id, hov ? 1f : 0f, 0.2f);
        int grow = Math.round(t * 3);
        int bx = x - grow, bw = w + grow * 2;
        int glow = red ? 0xFFEF4444 : Gfx.lerp(Theme.c1, Theme.c2, 0.5f);
        if (t > 0.02f) Gfx.rrect(bx - 2, y - 2, bw + 4, h + 4, 6, Theme.withAlpha(glow, Math.round(0x40 * t)));
        int base = red ? 0xE6A81C1C : 0xE6070707, hover = red ? 0xF2D32F2F : 0xF2141414;
        Gfx.rrect(bx, y, bw, h, 4, Gfx.lerp(base, hover, t));
        int r1 = red ? Gfx.lerp(0x55FFFFFF, 0xFFFF8A80, t) : Gfx.lerp(0x40FFFFFF, Theme.c1 | 0xFF000000, t);
        int r2 = red ? r1 : Gfx.lerp(0x40FFFFFF, Theme.c2 | 0xFF000000, t);
        Gfx.gradBar(bx + 4, y, bw - 8, 1, r1, r2);
        Gfx.gradBar(bx + 4, y + h - 1, bw - 8, 1, r2, r1);
        Gfx.fill(bx, y + 4, 1, h - 8, r1);
        Gfx.fill(bx + bw - 1, y + 4, 1, h - 8, r2);
        Gfx.spaced(label, x + w / 2, y + (h - 8) / 2, Gfx.lerp(0xFFC8C8C8, 0xFFFFFFFF, t), h > 24 ? 2 : 1);
        return hov && click;
    }

    /** Glass pill used inside panels. */
    static boolean pill(String id, String label, int x, int y, int w, int h, boolean active, int mx, int my, boolean click) {
        boolean hov = hit(x, y, w, h, mx, my);
        float t = Anim.to("p:" + id, hov ? 1f : 0f, 0.2f);
        int lift = Math.round(t);
        Gfx.rrect(x, y - lift, w, h, 5, Gfx.lerp(Theme.card(), Theme.cardHover(), t));
        if (active) {
            Gfx.gradBar(x + 4, y - lift, w - 8, 1, Theme.c1, Theme.c2);
            Gfx.gradBar(x + 4, y - lift + h - 1, w - 8, 1, Theme.c2, Theme.c1);
        }
        Gfx.ui(label, x + (w - Gfx.width(label)) / 2f, y + (h - 8) / 2f - lift, active ? Theme.text() : Theme.dim());
        return hov && click;
    }
}
