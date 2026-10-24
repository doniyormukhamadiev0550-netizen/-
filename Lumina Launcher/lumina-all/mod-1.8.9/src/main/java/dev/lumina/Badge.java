package dev.lumina;

/** "Lumina Client" tag in the bottom-right corner of inventory screens. */
public final class Badge {
    public static void draw() {
        Gfx.alpha = 1f;
        int[] sc = Gfx.scaled();
        String s = "Lumina Client";
        int w = Gfx.width(s) + 22, h = 16, x = sc[0] - w - 6, y = sc[1] - h - 6;
        Gfx.glass(x, y, w, h, 4);
        Gfx.circle(x + 8, y + 8, 3, Theme.c1);
        Gfx.ui(s, x + 15, y + 4, Theme.text());
    }
}
