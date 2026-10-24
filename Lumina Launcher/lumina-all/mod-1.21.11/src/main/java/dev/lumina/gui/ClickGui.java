package dev.lumina.gui;

import dev.lumina.Anim;
import dev.lumina.Backdrop;
import dev.lumina.Gfx;
import dev.lumina.Theme;
import dev.lumina.modules.Mods;
import dev.lumina.modules.Module;
import dev.lumina.modules.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Glass module menu: tabs, toggle cards, theme settings. Input is polled so it survives API changes. */
public class ClickGui extends Screen {
    public enum Tab {
        HUD("HUD", Module.Cat.HUD), INFO("INFO", Module.Cat.INFO), VISUAL("VISUAL", Module.Cat.VISUAL),
        UTILITY("UTILITY", Module.Cat.UTILITY), SETTINGS("SETTINGS", null);

        final String label;
        final Module.Cat cat;

        Tab(String label, Module.Cat cat) { this.label = label; this.cat = cat; }
    }

    private static final int PW = 340, PH = 232;
    private final Screen parent;
    private Tab tab;
    private boolean prevDown;
    private Module dragging;
    private int dragDx, dragDy;
    private float open;

    public ClickGui(Screen parent, Tab tab) {
        super(Text.literal("Lumina"));
        this.parent = parent;
        this.tab = tab;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        Gfx.alpha = 1f;
        ModuleManager.save();
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean click = down && !prevDown, released = !down && prevDown;
        prevDown = down;
        open += (1f - open) * 0.2f;
        if (open > 0.995f) open = 1f;

        boolean inWorld = client.world != null;
        Gfx.alpha = open;
        if (inWorld) {
            g.fill(0, 0, width, height, Gfx.a(0x66000000));
            Backdrop.blobs(g, width, height, mx, my, 0.8f);
        } else {
            Backdrop.draw(g, width, height, mx, my);
        }

        int px = (width - PW) / 2, py = (height - PH) / 2 + Math.round((1f - open) * 12f);
        boolean inPanel = Widgets.hit(px, py, PW, PH, mx, my);

        if (inWorld) { // draggable HUD previews
            Mods.preview = true;
            for (Module m : ModuleManager.MODULES) {
                if (!m.enabled || !m.isHud()) continue;
                int w = m.width(), h = m.height();
                m.x = Math.max(0, Math.min(width - w, m.x));
                m.y = Math.max(0, Math.min(height - h, m.y));
                m.draw(g, m.x, m.y);
                boolean over = Widgets.hit(m.x, m.y, w, h, mx, my);
                if (over) Gfx.fill(g, m.x, m.y + h - 1, w, 1, Theme.c1);
                if (click && over && dragging == null && !inPanel) {
                    dragging = m;
                    dragDx = mx - m.x;
                    dragDy = my - m.y;
                }
            }
            Mods.preview = false;
            if (!down) dragging = null;
            if (dragging != null) {
                dragging.x = Math.max(0, Math.min(width - dragging.width(), mx - dragDx));
                dragging.y = Math.max(0, Math.min(height - dragging.height(), my - dragDy));
            }
            if (released) ModuleManager.save();
        }

        Gfx.glass(g, px, py, PW, PH, 8);
        Gfx.ui(g, "LUMINA", px + 14, py + 12, Theme.c1);
        Gfx.ui(g, "CLIENT", px + 14 + Gfx.width("LUMINA") + 4, py + 12, Theme.dim());
        Gfx.ui(g, "ESC", px + PW - 14 - Gfx.width("ESC"), py + 12, Theme.dim());

        // tabs with a sliding underline
        Tab[] tabs = Tab.values();
        int tw = (PW - 28) / tabs.length, ty = py + 32;
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            int tx = px + 14 + i * tw;
            boolean hov = Widgets.hit(tx, ty, tw, 18, mx, my);
            float ht = Anim.to("tab:" + t, hov ? 1f : 0f, 0.2f);
            Gfx.ui(g, t.label, tx + (tw - Gfx.width(t.label)) / 2, ty + 4, Gfx.lerp(Theme.dim(), Theme.text(), t == tab ? 1f : ht));
            if (click && hov && dragging == null) tab = t;
        }
        float ux = Anim.to("tabx", tab.ordinal() * tw, 0.25f);
        Gfx.fill(g, px + 14, ty + 19, PW - 28, 1, Theme.withAlpha(Theme.text(), 0x22));
        Gfx.gradBar(g, px + 14 + Math.round(ux) + 6, ty + 18, tw - 12, 2, Theme.c1, Theme.c2);

        if (tab == Tab.SETTINGS) settings(g, px, py, mx, my, click);
        else modules(g, px, py, mx, my, click);
        Gfx.alpha = 1f;
    }

    private void modules(DrawContext g, int px, int py, int mx, int my, boolean click) {
        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) if (m.cat == tab.cat) list.add(m);
        int cw = (PW - 36) / 2, ch = 20;
        String hint = null;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int cx = px + 14 + (i % 2) * (cw + 8), cy = py + 66 + (i / 2) * (ch + 5);
            boolean hov = dragging == null && Widgets.hit(cx, cy, cw, ch, mx, my);
            float t = Anim.to("card:" + m.name, hov ? 1f : 0f, 0.2f);
            int lift = Math.round(t);
            Gfx.rrect(g, cx, cy - lift, cw, ch, 5, Gfx.lerp(Theme.card(), Theme.cardHover(), t));
            if (t > 0.05f) Gfx.gradBar(g, cx + 4, cy - lift, cw - 8, 1, Theme.withAlpha(Theme.c1, Math.round(0xCC * t)), Theme.withAlpha(Theme.c2, Math.round(0xCC * t)));
            Gfx.ui(g, m.name, cx + 8, cy - lift + 6, Theme.text());
            m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.25f;
            int sx = cx + cw - 8 - 24, sy = cy - lift + 5;
            Gfx.rrect(g, sx, sy, 24, 10, 5, Gfx.lerp(Theme.off(), Gfx.lerp(Theme.c1, Theme.c2, 0.5f), m.anim));
            Gfx.rrect(g, sx + 2 + Math.round(m.anim * 14), sy + 2, 6, 6, 3, 0xFFFFFFFF);
            if (hov) {
                hint = m.desc;
                if (click) { m.toggle(); ModuleManager.save(); }
            }
        }
        if (hint != null) Gfx.ui(g, hint, px + 14, py + PH - 16, Theme.dim());
        else if (list.isEmpty()) Gfx.ui(g, "Nothing here yet", px + 14, py + 70, Theme.dim());
    }

    private void settings(DrawContext g, int px, int py, int mx, int my, boolean click) {
        Gfx.ui(g, "PRIMARY COLOR", px + 14, py + 62, Theme.dim());
        swatches(g, "c1", px + 14, py + 74, mx, my, click, true);
        Gfx.ui(g, "SECONDARY COLOR", px + 14, py + 100, Theme.dim());
        swatches(g, "c2", px + 14, py + 112, mx, my, click, false);
        Gfx.ui(g, "BACKGROUND", px + 14, py + 138, Theme.dim());
        if (Widgets.pill(g, "dark", "Dark", px + 14, py + 150, 80, 20, Theme.dark, mx, my, click)) { Theme.dark = true; ModuleManager.save(); }
        if (Widgets.pill(g, "light", "Light", px + 100, py + 150, 80, 20, !Theme.dark, mx, my, click)) { Theme.dark = false; ModuleManager.save(); }
        Gfx.ui(g, "Tip: with a world open, drag HUD elements anywhere.", px + 14, py + 186, Theme.dim());
        Gfx.ui(g, "Right Shift opens this menu in game.", px + 14, py + 198, Theme.dim());
    }

    private void swatches(DrawContext g, String id, int x, int y, int mx, int my, boolean click, boolean primary) {
        for (int i = 0; i < Theme.PALETTE.length; i++) {
            int sx = x + i * 31, col = Theme.PALETTE[i];
            boolean hov = Widgets.hit(sx, y, 24, 14, mx, my);
            float t = Anim.to("sw:" + id + i, hov ? 1f : 0f, 0.25f);
            int lift = Math.round(t * 2);
            Gfx.rrect(g, sx, y - lift, 24, 14, 4, col);
            if (col == (primary ? Theme.c1 : Theme.c2)) Gfx.outline(g, sx - 2, y - lift - 2, 28, 18, Theme.text());
            if (hov && click) {
                if (primary) Theme.c1 = col; else Theme.c2 = col;
                ModuleManager.save();
            }
        }
    }
}
