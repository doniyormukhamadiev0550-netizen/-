package dev.lumina;

/** Animated background: drifting color blobs that lean slightly toward the mouse. */
public final class Backdrop {
    public static void draw(int w, int h, int mx, int my) {
        int top = Theme.dark ? 0xFF0A0A12 : 0xFFEAEFFA, bot = Theme.dark ? 0xFF181428 : 0xFFD2D9F0;
        Gfx.gradV(0, 0, w, h, top, bot);
        blobs(w, h, mx, my, 1f);
    }

    public static void blobs(int w, int h, int mx, int my, float k) {
        float t = (System.currentTimeMillis() % 3600000L) / 1000f;
        float px = (mx - w / 2f) * 0.035f, py = (my - h / 2f) * 0.035f;
        blob(w * 0.22f + (float) Math.sin(t * 0.30) * 28 + px, h * 0.30f + (float) Math.cos(t * 0.23) * 20 + py, h * 0.36f, Theme.c1, k);
        blob(w * 0.80f + (float) Math.cos(t * 0.21) * 30 - px * 1.5f, h * 0.72f + (float) Math.sin(t * 0.33) * 24 - py * 1.5f, h * 0.42f, Theme.c2, k);
        blob(w * 0.55f + (float) Math.sin(t * 0.17) * 40 + px * 0.6f, h * 0.15f + (float) Math.cos(t * 0.29) * 14 + py * 0.6f, h * 0.24f,
                Gfx.lerp(Theme.c1, Theme.c2, 0.5f), k);
    }

    private static void blob(float cx, float cy, float r, int color, float k) {
        int alpha = Math.round(0x16 * k);
        for (int i = 0; i < 3; i++) Gfx.circle(cx, cy, r * (1f - i * 0.28f), Theme.withAlpha(color, alpha));
    }
}
