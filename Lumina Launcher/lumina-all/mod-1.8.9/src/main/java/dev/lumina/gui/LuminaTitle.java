package dev.lumina.gui;

import dev.lumina.Backdrop;
import dev.lumina.Gfx;
import dev.lumina.Theme;
import dev.lumina.modules.ModuleManager;
import org.lwjgl.input.Mouse;

/**
 * Draws the Lumina main menu over the vanilla title screen (called from a mixin).
 * The two big buttons sit exactly on the vanilla Singleplayer / Multiplayer buttons, so vanilla still handles
 * those clicks; the corner buttons are handled here.
 */
public final class LuminaTitle {
    private static boolean prevDown;
    private static float fade;
    private static long lastRender;

    public static void render(int mx, int my) {
        long now = System.currentTimeMillis();
        if (now - lastRender > 500) fade = 0f;
        lastRender = now;
        int[] sc = Gfx.scaled();
        int w = sc[0], h = sc[1];
        boolean down = Mouse.isButtonDown(0);
        boolean click = down && !prevDown;
        prevDown = down;
        fade += (1f - fade) * 0.1f;

        Gfx.alpha = 1f;
        Backdrop.draw(w, h, mx, my);
        Gfx.alpha = fade;

        int cx = w / 2, j = h / 4 + 48; // vanilla layout: Singleplayer at j, Multiplayer at j + 24
        int bob = Math.round((float) Math.sin(now / 900.0) * 1.5f);
        Gfx.logo(cx, j - 72 + bob, 4);
        Gfx.spaced("CLIENT", cx, j - 36 + bob, Theme.dim(), 6);

        Widgets.black("sp", "SINGLEPLAYER", cx - 100, j, 200, 20, mx, my, false, false);
        Widgets.black("mp", "MULTIPLAYER", cx - 100, j + 24, 200, 20, mx, my, false, false);

        if (Widgets.black("set", "SETTINGS", 10, h - 30, 76, 20, mx, my, click, false)) {
            ModuleManager.openScreen(new ClickGui(ClickGui.Tab.SETTINGS));
        }
        if (Widgets.black("skin", "SKIN", w - 86, h - 30, 76, 20, mx, my, click, false)) {
            ModuleManager.openScreen(new SkinScreen());
        }
        if (Widgets.black("exit", "EXIT", w - 62, 10, 52, 20, mx, my, click, true)) {
            System.exit(0); // shutdown hook saves the config
        }

        String foot = "Lumina Client  |  Minecraft 1.8.9";
        Gfx.ui(foot, cx - Gfx.width(foot) / 2f, h - 14, Theme.dim());
        Gfx.alpha = 1f;
    }
}
