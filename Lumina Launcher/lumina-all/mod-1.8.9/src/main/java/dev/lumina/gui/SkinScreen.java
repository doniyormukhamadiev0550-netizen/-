package dev.lumina.gui;

import dev.lumina.Backdrop;
import dev.lumina.Gfx;
import dev.lumina.Theme;
import dev.lumina.modules.ModuleManager;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;

import java.awt.Desktop;
import java.io.File;

/** Skins for offline accounts are edited in the launcher; this screen points there and opens the folder. */
public class SkinScreen extends Screen {
    private boolean prevDown;
    private float open;

    private static File skinsDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", ".");
        File base;
        if (os.contains("win")) {
            String app = System.getenv("APPDATA");
            base = new File(app != null ? app : home, "LuminaLauncher");
        } else if (os.contains("mac")) {
            base = new File(home, "Library/Application Support/LuminaLauncher");
        } else {
            base = new File(home, ".local/share/LuminaLauncher");
        }
        return new File(base, "skins");
    }

    @Override
    public void render(int mx, int my, float delta) {
        int[] sc = Gfx.scaled();
        int w = sc[0], h = sc[1];
        boolean down = Mouse.isButtonDown(0);
        boolean click = down && !prevDown;
        prevDown = down;
        open += (1f - open) * 0.2f;
        Gfx.alpha = 1f;
        Backdrop.draw(w, h, mx, my);
        Gfx.alpha = open;

        int pw = 330, ph = 124, px = (w - pw) / 2, py = (h - ph) / 2;
        Gfx.glass(px, py, pw, ph, 8);
        Gfx.ui("SKIN", px + 16, py + 14, Theme.c1);
        Gfx.ui("Offline skins are edited in Lumina Launcher:", px + 16, py + 34, Theme.text());
        Gfx.ui("pick your account and press \"Skin...\". The skin is", px + 16, py + 48, Theme.dim());
        Gfx.ui("applied the next time you start the game.", px + 16, py + 60, Theme.dim());
        if (Widgets.pill("skin_open", "Open skins folder", px + 16, py + ph - 34, 150, 20, true, mx, my, click)) {
            try {
                File d = skinsDir();
                d.mkdirs();
                Desktop.getDesktop().open(d);
            } catch (Throwable ignored) {
            }
        }
        if (Widgets.pill("skin_back", "Back", px + pw - 16 - 90, py + ph - 34, 90, 20, false, mx, my, click)) {
            ModuleManager.openScreen(null); // back to the title screen (or the game)
        }
        Gfx.alpha = 1f;
    }
}
