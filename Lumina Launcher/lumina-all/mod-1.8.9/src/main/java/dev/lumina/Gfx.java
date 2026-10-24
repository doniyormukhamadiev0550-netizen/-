package dev.lumina;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/** Raw LWJGL2 drawing: keeps the number of Minecraft-specific names tiny. */
public final class Gfx {
    public static float alpha = 1f;
    private static final FloatBuffer BUF = BufferUtils.createFloatBuffer(16);

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

    /** GUI-scaled screen size, read from the current ortho projection. */
    public static int[] scaled() {
        BUF.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, BUF);
        float sx = BUF.get(0), sy = BUF.get(5);
        if (sx == 0 || sy == 0) return new int[]{427, 240};
        return new int[]{Math.round(2f / sx), Math.round(-2f / sy)};
    }

    private static void begin() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void end() { GL11.glPopAttrib(); }

    private static void col(int c) {
        c = a(c);
        GL11.glColor4f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, ((c >>> 24) & 255) / 255f);
    }

    // ---------------------------------------------------------------- text
    public static int width(String s) { return MinecraftClient.getInstance().textRenderer.getStringWidth(s); }

    public static void text(String s, float x, float y, int c) {
        int ac = a(c);
        if (((ac >>> 24) & 255) < 8) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        MinecraftClient.getInstance().textRenderer.drawWithShadow(s, x, y, ac);
        GL11.glPopAttrib();
    }

    public static void ui(String s, float x, float y, int c) { text(s, x, y, c); }

    public static void spaced(String s, int cx, int y, int c, int sp) {
        int w = -sp;
        for (int i = 0; i < s.length(); i++) w += width(String.valueOf(s.charAt(i))) + sp;
        int x = cx - w / 2;
        for (int i = 0; i < s.length(); i++) {
            String t = String.valueOf(s.charAt(i));
            text(t, x, y, c);
            x += width(t) + sp;
        }
    }

    // ---------------------------------------------------------------- shapes
    public static void fill(float x, float y, float w, float h, int c) {
        begin();
        col(c);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        end();
    }

    public static void gradV(float x, float y, float w, float h, int top, int bottom) {
        begin();
        GL11.glBegin(GL11.GL_QUADS);
        col(top);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        col(bottom);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
        end();
    }

    public static void gradBar(float x, float y, float w, float h, int left, int right) {
        begin();
        GL11.glBegin(GL11.GL_QUADS);
        col(left);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        col(right);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        end();
    }

    public static void rrect(float x, float y, float w, float h, float r, int c) {
        r = Math.min(r, Math.min(w, h) / 2f);
        begin();
        col(c);
        GL11.glBegin(GL11.GL_POLYGON);
        float[][] cs = {{x + w - r, y + r, -90}, {x + w - r, y + h - r, 0}, {x + r, y + h - r, 90}, {x + r, y + r, 180}};
        for (float[] k : cs) {
            for (int i = 0; i <= 6; i++) {
                double an = Math.toRadians(k[2] + i * 15);
                GL11.glVertex2f(k[0] + (float) Math.cos(an) * r, k[1] + (float) Math.sin(an) * r);
            }
        }
        GL11.glEnd();
        end();
    }

    public static void circle(float cx, float cy, float r, int c) {
        begin();
        col(c);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 40; i++) {
            double an = Math.PI * 2 * i / 40;
            GL11.glVertex2f(cx + (float) Math.cos(an) * r, cy + (float) Math.sin(an) * r);
        }
        GL11.glEnd();
        end();
    }

    public static void outline(float x, float y, float w, float h, int c) {
        fill(x, y, w, 1, c);
        fill(x, y + h - 1, w, 1, c);
        fill(x, y + 1, 1, h - 2, c);
        fill(x + w - 1, y + 1, 1, h - 2, c);
    }

    /** "Glass" panel: translucent body, soft top sheen and a two-color rim. */
    public static void glass(float x, float y, float w, float h, float r) {
        rrect(x, y, w, h, r, Theme.glass());
        gradV(x + r, y + 1, w - 2 * r, Math.max(3, h / 2), Theme.glassHi(), 0x00FFFFFF);
        int b1 = Theme.withAlpha(Theme.c1, 0x99), b2 = Theme.withAlpha(Theme.c2, 0x99);
        gradBar(x + r, y, w - 2 * r, 1, b1, b2);
        gradBar(x + r, y + h - 1, w - 2 * r, 1, Theme.withAlpha(Theme.c2, 0x55), Theme.withAlpha(Theme.c1, 0x55));
        fill(x, y + r, 1, h - 2 * r, Theme.withAlpha(Theme.c1, 0x55));
        fill(x + w - 1, y + r, 1, h - 2 * r, Theme.withAlpha(Theme.c2, 0x55));
    }

    public static void hud(float x, float y, float w, float h) {
        rrect(x, y, w, h, 3, 0x99101018);
        gradV(x + 1, y + 3, 2, h - 6, Theme.c1, Theme.c2);
    }

    // ---------------------------------------------------------------- block-font logo
    private static final String[] L = {"X....", "X....", "X....", "X....", "X....", "X....", "XXXXX"};
    private static final String[] U = {"X...X", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX."};
    private static final String[] M = {"X...X", "XX.XX", "X.X.X", "X...X", "X...X", "X...X", "X...X"};
    private static final String[] I = {"XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "XXXXX"};
    private static final String[] N = {"X...X", "XX..X", "X.X.X", "X..XX", "X...X", "X...X", "X...X"};
    private static final String[] A = {".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"};

    public static void logo(int cx, int y, int px) {
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
                        if (pass == 0) fill(gx + 1, y + row * px + 2, px, px, 0x66000000);
                        else fill(gx, y + row * px, px, px, lerp(Theme.c1, Theme.c2, t) | 0xFF000000);
                    }
                }
            }
        }
    }
}
