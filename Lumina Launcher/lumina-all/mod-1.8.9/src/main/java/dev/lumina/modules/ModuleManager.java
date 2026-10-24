package dev.lumina.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumina.Gfx;
import dev.lumina.Theme;
import dev.lumina.gui.ClickGui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<Module>();
    private static final Map<String, Module> BY_NAME = new HashMap<String, Module>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long START = System.currentTimeMillis();
    private static boolean prevMenuKey, applied;
    private static Path file;

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    private static void add(Module m, boolean defaultOn) {
        m.enabled = defaultOn;
        MODULES.add(m);
        BY_NAME.put(m.name, m);
    }

    private static Mods.Info info(String n, String d, Module.Cat c, int x, int y, Supplier<String> s) {
        return new Mods.Info(n, d, c, x, y, s);
    }

    public static void init() {
        add(new Mods.Fps(), true);
        add(new Mods.Cps(), true);
        add(info("Coordinates", "XYZ position", Module.Cat.HUD, 6, 46, new Supplier<String>() {
            public String get() {
                if (mc().player == null) return "XYZ";
                return String.format(Locale.ROOT, "XYZ: %.0f / %.0f / %.0f", mc().player.x, mc().player.y, mc().player.z);
            }
        }), true);
        add(new Mods.Speed(), false);
        add(new Mods.Keystrokes(), true);
        add(info("Clock", "Real time", Module.Cat.INFO, 110, 6, new Supplier<String>() {
            public String get() { return new SimpleDateFormat("HH:mm").format(new Date()); }
        }), false);
        add(info("Direction", "Facing direction", Module.Cat.INFO, 110, 26, new Supplier<String>() {
            public String get() {
                if (mc().player == null) return "-";
                float yaw = ((mc().player.yaw % 360f) + 360f) % 360f;
                String[] d = {"South", "West", "North", "East"};
                return d[Math.round(yaw / 90f) % 4] + " (" + Math.round(yaw) + ")";
            }
        }), false);
        add(info("Memory", "Java RAM usage", Module.Cat.INFO, 110, 46, new Supplier<String>() {
            public String get() {
                Runtime r = Runtime.getRuntime();
                return "RAM " + ((r.totalMemory() - r.freeMemory()) >> 20) + "/" + (r.maxMemory() >> 20) + " MB";
            }
        }), false);
        add(info("Session Timer", "Time since game start", Module.Cat.INFO, 110, 66, new Supplier<String>() {
            public String get() {
                long s = (System.currentTimeMillis() - START) / 1000;
                return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
            }
        }), false);
        add(info("Date", "Today's date", Module.Cat.INFO, 110, 86, new Supplier<String>() {
            public String get() { return new SimpleDateFormat("dd.MM.yyyy").format(new Date()); }
        }), false);
        add(new Mods.Stopwatch(), false);
        add(new Mods.Fullbright(), false);
        add(new Mods.Zoom(), true);
        add(new Mods.Crosshair(), false);
        add(new Mods.NoBob(), false);
        add(new Mods.ReduceParticles(), false);
        add(new Mods.ToggleSprint(), true);
        file = FabricLoader.getInstance().getConfigDir().resolve("lumina.json");
        load();
        for (Module m : MODULES) m.anim = m.enabled ? 1f : 0f;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() { public void run() { save(); } }));
    }

    public static boolean on(String name) {
        Module m = BY_NAME.get(name);
        return m != null && m.enabled;
    }

    /** Called once per rendered frame from the HUD mixin. */
    public static void frame(MinecraftClient mc) {
        if (mc.player == null || mc.options == null) return;
        if (!applied) {
            applied = true;
            for (Module m : MODULES) if (m.enabled) m.onEnable();
        }
        boolean key = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (key && !prevMenuKey && mc.currentScreen == null) openScreen(new ClickGui(ClickGui.Tab.HUD));
        prevMenuKey = key;
        for (Module m : MODULES) if (m.enabled || m.alwaysTick()) m.tick();
        if (mc.currentScreen instanceof ClickGui) return; // the menu draws its own previews
        Gfx.alpha = 1f;
        int[] sc = Gfx.scaled();
        for (Module m : MODULES) {
            if (!m.enabled) continue;
            if (m.isHud()) {
                int x = Math.max(0, Math.min(sc[0] - m.width(), m.x)), y = Math.max(0, Math.min(sc[1] - m.height(), m.y));
                m.draw(x, y);
            }
            m.overlay(sc[0], sc[1]);
        }
    }

    /** Finds "void method(Screen)" by type, so it works with any mapping. */
    public static void openScreen(Screen s) {
        try {
            for (Method m : MinecraftClient.class.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == Screen.class && m.getReturnType() == void.class) {
                    m.setAccessible(true);
                    m.invoke(MinecraftClient.getInstance(), s);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            JsonObject t = new JsonObject();
            t.addProperty("c1", Theme.c1);
            t.addProperty("c2", Theme.c2);
            t.addProperty("dark", Theme.dark);
            root.add("theme", t);
            for (Module m : MODULES) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled);
                if (m.isHud()) { o.addProperty("x", m.x); o.addProperty("y", m.y); }
                root.add(m.name, o);
            }
            Files.write(file, GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void load() {
        try {
            if (!Files.exists(file)) return;
            JsonObject root = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("theme")) {
                JsonObject t = root.getAsJsonObject("theme");
                if (t.has("c1")) Theme.c1 = t.get("c1").getAsInt();
                if (t.has("c2")) Theme.c2 = t.get("c2").getAsInt();
                if (t.has("dark")) Theme.dark = t.get("dark").getAsBoolean();
            }
            for (Module m : MODULES) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean();
                if (m.isHud() && o.has("x")) { m.x = o.get("x").getAsInt(); m.y = o.get("y").getAsInt(); }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
