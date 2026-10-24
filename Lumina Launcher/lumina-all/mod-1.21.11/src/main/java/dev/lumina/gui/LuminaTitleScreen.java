package dev.lumina.gui;

import dev.lumina.Backdrop;
import dev.lumina.Gfx;
import dev.lumina.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Replaces the vanilla title screen: two big black buttons plus settings / skin / exit in the corners. */
public class LuminaTitleScreen extends Screen {
    private boolean prevDown;
    private float fade;

    public LuminaTitleScreen() { super(Text.literal("Lumina")); }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean click = down && !prevDown;
        prevDown = down;
        fade += (1f - fade) * 0.1f;

        Gfx.alpha = 1f;
        Backdrop.draw(g, width, height, mx, my);
        Gfx.alpha = fade;

        int cx = width / 2;
        int bob = Math.round((float) Math.sin(System.currentTimeMillis() / 900.0) * 1.5f);
        Gfx.logo(g, cx, height / 2 - 82 + bob, 4);
        Gfx.spaced(g, "CLIENT", cx, height / 2 - 46 + bob, Theme.dim(), 6);

        int bw = 230, bh = 30, by = height / 2 - 22;
        if (Widgets.black(g, "sp", "SINGLEPLAYER", cx - bw / 2, by, bw, bh, mx, my, click, false)) {
            client.setScreen(new SelectWorldScreen(this));
        }
        if (Widgets.black(g, "mp", "MULTIPLAYER", cx - bw / 2, by + bh + 8, bw, bh, mx, my, click, false)) {
            client.setScreen(new MultiplayerScreen(this));
        }
        if (Widgets.black(g, "set", "SETTINGS", 10, height - 30, 76, 20, mx, my, click, false)) {
            client.setScreen(new ClickGui(this, ClickGui.Tab.SETTINGS));
        }
        if (Widgets.black(g, "skin", "SKIN", width - 86, height - 30, 76, 20, mx, my, click, false)) {
            client.setScreen(new SkinScreen(this));
        }
        if (Widgets.black(g, "exit", "EXIT", width - 62, 10, 52, 20, mx, my, click, true)) {
            client.scheduleStop();
        }

        String foot = "Lumina Client  |  Minecraft 1.21.11";
        Gfx.ui(g, foot, cx - Gfx.width(foot) / 2, height - 14, Theme.dim());
        Gfx.alpha = 1f;
    }
}
