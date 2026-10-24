package dev.lumina.gui;

import dev.lumina.Anim;
import dev.lumina.Backdrop;
import dev.lumina.Gfx;
import dev.lumina.Theme;
import dev.lumina.modules.Module;
import dev.lumina.modules.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/** Glass module menu: tabs, toggle cards, theme settings. ESC closes. Input is polled via LWJGL. */
public class ClickGui extends Screen {
    public enum Tab {
        HUD("HUD", Module.Cat.HUD), INFO("INFO", Module.Cat.INFO), VISUAL("VISUAL", Module.Cat.VISUAL),
        UTILITY("UTILITY", Module.Cat.UTILITY), SETTINGS("SETTINGS", null);

        final String label;
        final Module.Cat cat;

        Tab(String label, Module.Cat cat) { this.label = label; this.cat = cat; }
    }

    private static final int PW = 340, PH = 232;
    private Tab tab;
    private boolean prevDown;
    private Module dragging;
    private int dragDx, dragDy;
    private float open;

    public ClickGui(Tab tab) { this.tab = tab; }

    @Override
    public void render(int mx, int my, float delta) {
        int[] sc = Gfx.scaled();
        int width = sc[0], height = sc[1];
        boolean down = Mouse.isButtonDown(0);
        boolean click = down && !prevDown, released = !down && prevDown;
        prevDown = down;
        open += (1f - open) * 0.2f;
        if (open > 0.995f) open = 1f;

        boolean inWorld = MinecraftClient.getInstance().player != null;
        Gfx.alpha = open;
        if (inWorld) {
            Gfx.fill(0, 0, width, height, 0x66000000);
            Backdrop.blobs(width, height, mx, my, 0.8f);
        } else {
            Backdrop.draw(width, height, mx, my);
        }

        int px = (width - PW) / 2, py = (height - PH) / 2 + Math.round((1f - open) * 12f);
        boolean inPanel = Widgets.hit(px, py, PW, PH, mx, my);

        if (inWorld) { // draggable HUD previews
            for (Module m : ModuleManager.MODULES) {
                if (!m.enabled || !m.isHud()) continue;
                int w = m.width(), h = m.height();
                m.x = Math.max(0, Math.min(width - w, m.x));
                m.y = Math.max(0, Math.min(height - h, m.y));
                m.draw(m.x, m.y);
                boolean over = Widgets.hit(m.x, m.y, w, h, mx, my);
                if (over) Gfx.fill(m.x, m.y + h - 1, w, 1, Theme.c1);
                if (click && over && dragging == null && !inPanel) {
                    dragging = m;
                    dragDx = mx - m.x;
                    dragDy = my - m.y;
                }
            }
            if (!down) dragging = null;
            if (dragging != null) {
                dragging.x = Math.max(0, Math.min(width - dragging.width(), mx - dragDx));
                dragging.y = Math.max(0, Math.min(height - dragging.height(), my - dragDy));
            }
            if (released) ModuleManager.save();
        }

        Gfx.glass(px, py, PW, PH, 8);
        Gfx.ui("LUMINA", px + 14, py + 12, Theme.c1);
        Gfx.ui("CLIENT", px + 14 + Gfx.width("LUMINA") + 4, py + 12, Theme.dim());
        Gfx.ui("ESC", px + PW - 14 - Gfx.width("ESC"), py + 12, Theme.dim());

        Tab[] tabs = Tab.values();
        int tw = (PW - 28) / tabs.length, ty = py + 32;
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            int tx = px + 14 + i * tw;
            boolean hov = Widgets.hit(tx, ty, tw, 18, mx, my);
            float ht = Anim.to("tab:" + t, hov ? 1f : 0f, 0.2f);
            Gfx.ui(t.label, tx + (tw - Gfx.width(t.label)) / 2f, ty + 4, Gfx.lerp(Theme.dim(), Theme.text(), t == tab ? 1f : ht));
            if (click && hov && dragging == null) tab = t;
        }
        float ux = Anim.to("tabx", tab.ordinal() * tw, 0.25f);
        Gfx.fill(px + 14, ty + 19, PW - 28, 1, Theme.withAlpha(Theme.text(), 0x22));
        Gfx.gradBar(px + 14 + Math.round(ux) + 6, ty + 18, tw - 12, 2, Theme.c1, Theme.c2);

        if (tab == Tab.SETTINGS) settings(px, py, mx, my, click);
        else modules(px, py, mx, my, click);
        Gfx.alpha = 1f;
    }

    private void modules(int px, int py, int mx, int my, boolean click) {
        List<Module> list = new ArrayList<Module>();
        for (Module m : ModuleManager.MODULES) if (m.cat == tab.cat) list.add(m);
        int cw = (PW - 36) / 2, ch = 20;
        String hint = null;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int cx = px + 14 + (i % 2) * (cw + 8), cy = py + 66 + (i / 2) * (ch + 5);
            boolean hov = dragging == null && Widgets.hit(cx, cy, cw, ch, mx, my);
            float t = Anim.to("card:" + m.name, hov ? 1f : 0f, 0.2f);
            int lift = Math.round(t);
            Gfx.rrect(cx, cy - lift, cw, ch, 5, Gfx.lerp(Theme.card(), Theme.cardHover(), t));
            if (t > 0.05f) {
                Gfx.gradBar(cx + 4, cy - lift, cw - 8, 1, Theme.withAlpha(Theme.c1, Math.round(0xCC * t)), Theme.withAlpha(Theme.c2, Math.round(0xCC * t)));
            }
            Gfx.ui(m.name, cx + 8, cy - lift + 6, Theme.text());
            m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.25f;
            int sx = cx + cw - 8 - 24, sy = cy - lift + 5;
            Gfx.rrect(sx, sy, 24, 10, 5, Gfx.lerp(Theme.off(), Gfx.lerp(Theme.c1, Theme.c2, 0.5f), m.anim));
            Gfx.rrect(sx + 2 + Math.round(m.anim * 14), sy + 2, 6, 6, 3, 0xFFFFFFFF);
            if (hov) {
                hint = m.desc;
                if (click) { m.toggle(); ModuleManager.save(); }
            }
        }
        if (hint != null) Gfx.ui(hint, px + 14, py + PH - 16, Theme.dim());
        else if (list.isEmpty()) Gfx.ui("Nothing here yet", px + 14, py + 70, Theme.dim());
    }

    private void settings(int px, int py, int mx, int my, boolean click) {
        Gfx.ui("PRIMARY COLOR", px + 14, py + 62, Theme.dim());
        swatches("c1", px + 14, py + 74, mx, my, click, true);
        Gfx.ui("SECONDARY COLOR", px + 14, py + 100, Theme.dim());
        swatches("c2", px + 14, py + 112, mx, my, click, false);
        Gfx.ui("BACKGROUND", px + 14, py + 138, Theme.dim());
        if (Widgets.pill("dark", "Dark", px + 14, py + 150, 80, 20, Theme.dark, mx, my, click)) { Theme.dark = true; ModuleManager.save(); }
        if (Widgets.pill("light", "Light", px + 100, py + 150, 80, 20, !Theme.dark, mx, my, click)) { Theme.dark = false; ModuleManager.save(); }
        Gfx.ui("Tip: with a world open, drag HUD elements anywhere.", px + 14, py + 186, Theme.dim());
        Gfx.ui("Right Shift opens this menu in game.", px + 14, py + 198, Theme.dim());
    }

    private void swatches(String id, int x, int y, int mx, int my, boolean click, boolean primary) {
        for (int i = 0; i < Theme.PALETTE.length; i++) {
            int sx = x + i * 31, col = Theme.PALETTE[i];
            boolean hov = Widgets.hit(sx, y, 24, 14, mx, my);
            float t = Anim.to("sw:" + id + i, hov ? 1f : 0f, 0.25f);
            int lift = Math.round(t * 2);
            Gfx.rrect(sx, y - lift, 24, 14, 4, col);
            if (col == (primary ? Theme.c1 : Theme.c2)) Gfx.outline(sx - 2, y - lift - 2, 28, 18, Theme.text());
            if (hov && click) {
                if (primary) Theme.c1 = col; else Theme.c2 = col;
                ModuleManager.save();
            }
        }
    }
}
