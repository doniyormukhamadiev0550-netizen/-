package dev.lumina.gui;

import dev.lumina.Backdrop;
import dev.lumina.Gfx;
import dev.lumina.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.File;

/** Skins for offline accounts are edited in the launcher; this screen points there and opens the folder. */
public class SkinScreen extends Screen {
    private final Screen parent;
    private boolean prevDown;
    private float open;

    public SkinScreen(Screen parent) {
        super(Text.literal("Skin"));
        this.parent = parent;
    }

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

    @Override public void close() { Gfx.alpha = 1f; client.setScreen(parent); }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean click = down && !prevDown;
        prevDown = down;
        open += (1f - open) * 0.2f;
        Gfx.alpha = 1f;
        Backdrop.draw(g, width, height, mx, my);
        Gfx.alpha = open;

        int pw = 330, ph = 124, px = (width - pw) / 2, py = (height - ph) / 2;
        Gfx.glass(g, px, py, pw, ph, 8);
        Gfx.ui(g, "SKIN", px + 16, py + 14, Theme.c1);
        Gfx.ui(g, "Offline skins are edited in Lumina Launcher:", px + 16, py + 34, Theme.text());
        Gfx.ui(g, "pick your account and press \"Skin...\". The skin is", px + 16, py + 48, Theme.dim());
        Gfx.ui(g, "applied the next time you start the game.", px + 16, py + 60, Theme.dim());
        if (Widgets.pill(g, "skin_open", "Open skins folder", px + 16, py + ph - 34, 150, 20, true, mx, my, click)) {
            File d = skinsDir();
            d.mkdirs();
            Util.getOperatingSystem().open(d.toURI());
        }
        if (Widgets.pill(g, "skin_back", "Back", px + pw - 16 - 90, py + ph - 34, 90, 20, false, mx, my, click)) close();
        Gfx.alpha = 1f;
    }
}
