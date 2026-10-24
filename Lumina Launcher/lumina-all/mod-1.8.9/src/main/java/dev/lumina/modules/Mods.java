package dev.lumina.modules;

import dev.lumina.Gfx;
import dev.lumina.Theme;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.function.Supplier;

/** Every module of the 1.8.9 build. */
public final class Mods {
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
        public void draw(int x, int y) {
            Gfx.hud(x, y, width(), height());
            Gfx.text(text(), x + 8, y + 4, 0xFFFFFFFF);
        }
    }

    public static class Fps extends Info {
        private int frames, fps;
        private long last = System.currentTimeMillis();

        public Fps() { super("FPS", "Frame rate", Cat.HUD, 6, 6, new Supplier<String>() { public String get() { return ""; } }); }

        @Override public boolean alwaysTick() { return true; }

        @Override
        public void tick() {
            frames++;
            long now = System.currentTimeMillis();
            if (now - last >= 1000) { fps = frames; frames = 0; last = now; }
        }

        @Override protected String text() { return fps + " FPS"; }
    }

    public static class Cps extends Info {
        private final ArrayDeque<Long> left = new ArrayDeque<Long>(), right = new ArrayDeque<Long>();
        private boolean pl, pr;

        public Cps() { super("CPS", "Clicks per second", Cat.HUD, 6, 26, new Supplier<String>() { public String get() { return ""; } }); }

        @Override public boolean alwaysTick() { return true; }

        @Override
        public void tick() {
            boolean l = Mouse.isButtonDown(0), r = Mouse.isButtonDown(1);
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
        private long lastT = System.nanoTime();

        public Speed() { super("Speed", "Blocks per second", Cat.HUD, 6, 66, new Supplier<String>() { public String get() { return ""; } }); }

        @Override
        public void tick() {
            if (mc().player == null) return;
            long now = System.nanoTime();
            double dt = (now - lastT) / 1.0e9;
            if (dt >= 0.25) { // sample 4x per second
                bps = Math.hypot(mc().player.x - px, mc().player.z - pz) / dt;
                px = mc().player.x;
                pz = mc().player.z;
                lastT = now;
            }
        }

        @Override protected String text() { return String.format(Locale.ROOT, "%.1f BPS", bps); }
    }

    public static class Keystrokes extends Module {
        private static final int S = 22, G = 2;

        public Keystrokes() { super("Keystrokes", "WASD and mouse buttons", Cat.HUD, 6, 90); }

        @Override public boolean isHud() { return true; }
        @Override public int width() { return S * 3 + G * 2; }
        @Override public int height() { return S * 2 + G * 2 + 16; }

        private void key(int x, int y, int w, int h, String label, boolean down) {
            Gfx.rrect(x, y, w, h, 3, down ? Theme.withAlpha(Theme.c1, 0xCC) : 0x99101018);
            Gfx.text(label, x + (w - Gfx.width(label)) / 2f, y + (h - 8) / 2f, 0xFFFFFFFF);
        }

        @Override
        public void draw(int x, int y) {
            key(x + S + G, y, S, S, "W", Keyboard.isKeyDown(Keyboard.KEY_W));
            key(x, y + S + G, S, S, "A", Keyboard.isKeyDown(Keyboard.KEY_A));
            key(x + S + G, y + S + G, S, S, "S", Keyboard.isKeyDown(Keyboard.KEY_S));
            key(x + 2 * (S + G), y + S + G, S, S, "D", Keyboard.isKeyDown(Keyboard.KEY_D));
            int by = y + 2 * (S + G), bw = (width() - G) / 2;
            key(x, by, bw, 16, "LMB", Mouse.isButtonDown(0));
            key(x + bw + G, by, bw, 16, "RMB", Mouse.isButtonDown(1));
        }
    }

    /** Hold-to-run stopwatch: press N to start / stop, M to reset. */
    public static class Stopwatch extends Info {
        private long start, acc;
        private boolean running, pn, pm;

        public Stopwatch() { super("Stopwatch", "N start/stop, M reset", Cat.INFO, 110, 106, new Supplier<String>() { public String get() { return ""; } }); }

        @Override
        public void tick() {
            boolean n = mc().currentScreen == null && Keyboard.isKeyDown(Keyboard.KEY_N);
            boolean m = mc().currentScreen == null && Keyboard.isKeyDown(Keyboard.KEY_M);
            if (n && !pn) {
                if (running) acc += System.currentTimeMillis() - start; else start = System.currentTimeMillis();
                running = !running;
            }
            if (m && !pm) { running = false; acc = 0; }
            pn = n;
            pm = m;
        }

        @Override
        protected String text() {
            long ms = acc + (running ? System.currentTimeMillis() - start : 0);
            long s = ms / 1000;
            return String.format(Locale.ROOT, "%02d:%02d.%d", s / 60, s % 60, (ms / 100) % 10);
        }
    }

    public static class Fullbright extends Module {
        private float saved = 1f;

        public Fullbright() { super("Fullbright", "See clearly in the dark", Cat.VISUAL, 0, 0); }

        @Override public void onEnable() { if (mc().options.gamma < 5f) saved = mc().options.gamma; }
        @Override public void tick() { mc().options.gamma = 100f; }
        @Override public void onDisable() { mc().options.gamma = saved; }
    }

    public static class Zoom extends Module {
        private boolean zooming;
        private float saved;

        public Zoom() { super("Zoom", "Hold C to zoom in", Cat.VISUAL, 0, 0); }

        @Override
        public void tick() {
            boolean held = mc().currentScreen == null && Keyboard.isKeyDown(Keyboard.KEY_C);
            if (held && !zooming) { saved = mc().options.fov; zooming = true; }
            if (held) mc().options.fov = 30f;
            else if (zooming) reset();
        }

        private void reset() { mc().options.fov = saved; zooming = false; }

        @Override public void onDisable() { if (zooming) reset(); }
    }

    public static class Crosshair extends Module {
        public Crosshair() { super("Crosshair", "Extra colored crosshair", Cat.VISUAL, 0, 0); }

        @Override
        public void overlay(int sw, int sh) {
            if (mc().currentScreen != null) return;
            int cx = sw / 2, cy = sh / 2, c = Theme.c1;
            Gfx.fill(cx - 6, cy, 4, 1, c);
            Gfx.fill(cx + 3, cy, 4, 1, c);
            Gfx.fill(cx, cy - 6, 1, 4, c);
            Gfx.fill(cx, cy + 3, 1, 4, c);
        }
    }

    public static class NoBob extends Module {
        private boolean saved = true;

        public NoBob() { super("No View Bobbing", "Removes camera bobbing while walking", Cat.VISUAL, 0, 0); }

        @Override public void onEnable() { saved = mc().options.bobView; mc().options.bobView = false; }
        @Override public void onDisable() { mc().options.bobView = saved; }
    }

    public static class ReduceParticles extends Module {
        private int saved;

        public ReduceParticles() { super("Reduce Particles", "Minimal particle mode", Cat.VISUAL, 0, 0); }

        @Override public void onEnable() { saved = mc().options.particles; mc().options.particles = 2; }
        @Override public void onDisable() { mc().options.particles = saved; }
    }

    public static class ToggleSprint extends Module {
        public ToggleSprint() { super("Toggle Sprint", "Sprint automatically while moving", Cat.UTILITY, 0, 0); }

        @Override
        public void tick() {
            if (mc().player == null || mc().currentScreen != null) return;
            if (Keyboard.isKeyDown(Keyboard.KEY_W) && !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) mc().player.setSprinting(true);
        }
    }
}
