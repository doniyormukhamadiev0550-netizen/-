package dev.lumina.modules;

import dev.lumina.Gfx;
import dev.lumina.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.client.option.ParticlesMode;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Every module implementation. */
public final class Mods {
    /** True while the menu draws HUD previews (so empty modules can show a placeholder). */
    public static boolean preview;

    private static long win() { return Module.mc().getWindow().getHandle(); }

    private static boolean keyDown(int key) { return GLFW.glfwGetKey(win(), key) == GLFW.GLFW_PRESS; }

    // ------------------------------------------------------------------ HUD / INFO
    public static class Info extends Module {
        private final Supplier<String> supplier;

        public Info(String n, String d, Cat c, int x, int y, Supplier<String> s) {
            super(n, d, c, x, y);
            supplier = s;
        }

        protected String text() { return supplier.get(); }
        @Override public boolean isHud() { return true; }
        @Override public int width() { return Gfx.width(text()) + 14; }
        @Override public int height() { return 16; }

        @Override
        public void draw(DrawContext g, int x, int y) {
            Gfx.hud(g, x, y, width(), height());
            Gfx.text(g, text(), x + 8, y + 4, 0xFFFFFFFF);
        }
    }

    public static class Cps extends Info {
        private final ArrayDeque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();
        private boolean pl, pr;

        public Cps() { super("CPS", "Clicks per second", Cat.HUD, 6, 26, () -> ""); }

        @Override public boolean alwaysTick() { return true; }

        @Override
        public void tick() {
            if (mc().getWindow() == null) return;
            long w = win();
            boolean l = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean r = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            long now = System.currentTimeMillis();
            if (mc().currentScreen == null) {
                if (l && !pl) left.add(now);
                if (r && !pr) right.add(now);
            }
            pl = l;
            pr = r;
            while (!left.isEmpty() && now - left.peek() > 1000) left.poll();
            while (!right.isEmpty() && now - right.peek() > 1000) right.poll();
        }

        @Override protected String text() { return left.size() + " | " + right.size() + " CPS"; }
    }

    public static class Speed extends Info {
        private double px, pz, bps;

        public Speed() { super("Speed", "Blocks per second", Cat.HUD, 6, 86, () -> ""); }

        @Override
        public void tick() {
            var p = mc().player;
            if (p == null) return;
            bps = Math.hypot(p.getX() - px, p.getZ() - pz) * 20.0;
            px = p.getX();
            pz = p.getZ();
        }

        @Override protected String text() { return String.format(java.util.Locale.ROOT, "%.1f BPS", bps); }
    }

    public static class Keystrokes extends Module {
        private static final int S = 22, G = 2;

        public Keystrokes() { super("Keystrokes", "WASD and mouse buttons", Cat.HUD, 6, 110); }

        @Override public boolean isHud() { return true; }
        @Override public int width() { return S * 3 + G * 2; }
        @Override public int height() { return S * 2 + G * 2 + 16; }

        private void key(DrawContext g, int x, int y, int w, int h, String label, boolean down) {
            Gfx.rrect(g, x, y, w, h, 3, down ? Theme.withAlpha(Theme.c1, 0xCC) : 0x99101018);
            Gfx.text(g, label, x + (w - Gfx.width(label)) / 2, y + (h - 8) / 2, 0xFFFFFFFF);
        }

        @Override
        public void draw(DrawContext g, int x, int y) {
            var o = mc().options;
            key(g, x + S + G, y, S, S, "W", o.forwardKey.isPressed());
            key(g, x, y + S + G, S, S, "A", o.leftKey.isPressed());
            key(g, x + S + G, y + S + G, S, S, "S", o.backKey.isPressed());
            key(g, x + 2 * (S + G), y + S + G, S, S, "D", o.rightKey.isPressed());
            int by = y + 2 * (S + G), bw = (width() - G) / 2;
            key(g, x, by, bw, 16, "LMB", o.attackKey.isPressed());
            key(g, x + bw + G, by, bw, 16, "RMB", o.useKey.isPressed());
        }
    }

    public static class Armor extends Module {
        private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        public Armor() { super("Armor Status", "Armor pieces and durability", Cat.HUD, 6, 190); }

        @Override public boolean isHud() { return true; }
        @Override public int width() { return 58; }
        @Override public int height() { return 4 * 18 + 2; }

        @Override
        public void draw(DrawContext g, int x, int y) {
            Gfx.hud(g, x, y, width(), height());
            var p = mc().player;
            for (int i = 0; i < 4; i++) {
                ItemStack s = p == null ? ItemStack.EMPTY : p.getEquippedStack(SLOTS[i]);
                if (s.isEmpty()) continue;
                g.drawItem(s, x + 6, y + 1 + i * 18);
                if (s.isDamageable()) {
                    int left = s.getMaxDamage() - s.getDamage();
                    float f = (float) left / s.getMaxDamage();
                    Gfx.text(g, String.valueOf(left), x + 25, y + 6 + i * 18, Gfx.lerp(0xFFFF4444, 0xFF55FF55, f));
                }
            }
        }
    }

    public static class Potions extends Module {
        public Potions() { super("Potion Status", "Active effects and time left", Cat.HUD, 110, 190); }

        private List<String> lines() {
            List<String> out = new ArrayList<>();
            if (mc().player == null) return out;
            for (StatusEffectInstance e : mc().player.getStatusEffects()) {
                int s = e.getDuration() / 20;
                String t = e.isInfinite() ? "**" : (s / 60) + ":" + String.format("%02d", s % 60);
                out.add(e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1) + "  " + t);
            }
            if (out.isEmpty() && preview) out.add("No effects");
            return out;
        }

        @Override public boolean isHud() { return true; }

        @Override
        public int width() {
            int w = 40;
            for (String s : lines()) w = Math.max(w, Gfx.width(s) + 14);
            return w;
        }

        @Override public int height() { return Math.max(1, lines().size()) * 14 + 2; }

        @Override
        public void draw(DrawContext g, int x, int y) {
            List<String> l = lines();
            if (l.isEmpty()) return;
            Gfx.hud(g, x, y, width(), height());
            for (int i = 0; i < l.size(); i++) Gfx.text(g, l.get(i), x + 8, y + 4 + i * 14, 0xFFFFFFFF);
        }
    }

    // ------------------------------------------------------------------ VISUAL
    /** Pure switch: the matching mixin reads {@code enabled}. */
    public static class Flag extends Module {
        public Flag(String n, String d) { super(n, d, Cat.VISUAL, 0, 0); }
    }

    public static class Fullbright extends Module {
        private boolean applied;

        public Fullbright() { super("Fullbright", "See clearly in the dark", Cat.VISUAL, 0, 0); }

        @Override
        public void tick() {
            var p = mc().player;
            if (p == null) return;
            var cur = p.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (cur == null || (applied && cur.getDuration() < 300)) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false));
                applied = true;
            }
        }

        @Override
        public void onDisable() {
            if (mc().player != null && applied) mc().player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            applied = false;
        }
    }

    public static class Zoom extends Module {
        private boolean zooming;
        private int saved;

        public Zoom() { super("Zoom", "Hold C to zoom in", Cat.VISUAL, 0, 0); }

        @Override
        public void tick() {
            if (mc().getWindow() == null) return;
            boolean held = mc().currentScreen == null && keyDown(GLFW.GLFW_KEY_C);
            if (held && !zooming) { saved = mc().options.getFov().getValue(); zooming = true; }
            if (held) mc().options.getFov().setValue(30);
            else if (zooming) reset();
        }

        private void reset() { mc().options.getFov().setValue(saved); zooming = false; }

        @Override public void onDisable() { if (zooming) reset(); }
    }

    public static class NoBob extends Module {
        private boolean saved = true;

        public NoBob() { super("No View Bobbing", "Removes camera bobbing while walking", Cat.VISUAL, 0, 0); }

        @Override public void onEnable() { saved = mc().options.getBobView().getValue(); mc().options.getBobView().setValue(false); }
        @Override public void onDisable() { mc().options.getBobView().setValue(saved); }
    }

    public static class ReduceParticles extends Module {
        private ParticlesMode saved = ParticlesMode.ALL;

        public ReduceParticles() { super("Reduce Particles", "Minimal particle mode", Cat.VISUAL, 0, 0); }

        @Override public void onEnable() { saved = mc().options.getParticles().getValue(); mc().options.getParticles().setValue(ParticlesMode.MINIMAL); }
        @Override public void onDisable() { mc().options.getParticles().setValue(saved); }
    }

    public static class NoWeather extends Module {
        public NoWeather() { super("No Weather", "Hides rain and thunder", Cat.VISUAL, 0, 0); }

        @Override
        public void tick() {
            var w = mc().world;
            if (w == null) return;
            w.setRainGradient(0f);
            w.setThunderGradient(0f);
        }
    }

    public static class Crosshair extends Module {
        public Crosshair() { super("Crosshair", "Extra colored crosshair", Cat.VISUAL, 0, 0); }

        @Override
        public void overlay(DrawContext g, int sw, int sh) {
            if (mc().currentScreen != null) return;
            int cx = sw / 2, cy = sh / 2, c = Theme.c1;
            Gfx.fill(g, cx - 6, cy, 4, 1, c);
            Gfx.fill(g, cx + 3, cy, 4, 1, c);
            Gfx.fill(g, cx, cy - 6, 1, 4, c);
            Gfx.fill(g, cx, cy + 3, 1, 4, c);
        }
    }

    // ------------------------------------------------------------------ UTILITY
    public static class ToggleSprint extends Module {
        public ToggleSprint() { super("Toggle Sprint", "Sprint automatically while moving", Cat.UTILITY, 0, 0); }

        @Override
        public void tick() {
            var p = mc().player;
            if (p == null || mc().currentScreen != null) return;
            if (mc().options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision
                    && p.getHungerManager().getFoodLevel() > 6) p.setSprinting(true);
        }
    }

    public static class ToggleSneak extends Module {
        private boolean prev, state;

        public ToggleSneak() { super("Toggle Sneak", "Left Shift toggles sneaking", Cat.UTILITY, 0, 0); }

        @Override
        public void tick() {
            if (mc().getWindow() == null || mc().player == null) return;
            boolean k = mc().currentScreen == null && keyDown(GLFW.GLFW_KEY_LEFT_SHIFT);
            if (k && !prev) state = !state;
            prev = k;
            if (mc().currentScreen == null) mc().options.sneakKey.setPressed(state);
        }

        @Override
        public void onDisable() {
            state = false;
            mc().options.sneakKey.setPressed(false);
        }
    }

    public static class AutoRespawn extends Module {
        public AutoRespawn() { super("Auto Respawn", "Respawns instantly after dying", Cat.UTILITY, 0, 0); }

        @Override
        public void tick() {
            var p = mc().player;
            if (p != null && p.isDead()) p.requestRespawn();
        }
    }
}
