package dev.lumina.modules;

import com.google.gson.*;
import dev.lumina.Theme;
import dev.lumina.gui.ClickGui;
import dev.lumina.gui.LuminaTitleScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import dev.lumina.modules.Module.Cat;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();
    private static final Map<String, Module> BY_NAME = new HashMap<>();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("lumina.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long START = System.currentTimeMillis();
    private static boolean prevMenuKey, applied;

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    private static void add(Module m, boolean defaultOn) {
        m.enabled = defaultOn;
        MODULES.add(m);
        BY_NAME.put(m.name, m);
    }

    private static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    public static void init() {
        // HUD
        add(new Mods.Info("FPS", "Frame rate", Cat.HUD, 6, 6, () -> mc().getCurrentFps() + " FPS"), true);
        add(new Mods.Cps(), true);
        add(new Mods.Info("Coordinates", "XYZ position", Cat.HUD, 6, 46, () -> {
            var p = mc().player;
            return p == null ? "XYZ" : String.format(Locale.ROOT, "XYZ: %.0f / %.0f / %.0f", p.getX(), p.getY(), p.getZ());
        }), true);
        add(new Mods.Info("Ping", "Server latency", Cat.HUD, 6, 66, () -> {
            var m = mc();
            if (m.player == null || m.getNetworkHandler() == null) return "- ms";
            var e = m.getNetworkHandler().getPlayerListEntry(m.player.getUuid());
            return (e == null ? 0 : e.getLatency()) + " ms";
        }), false);
        add(new Mods.Speed(), false);
        add(new Mods.Keystrokes(), true);
        add(new Mods.Armor(), false);
        add(new Mods.Potions(), false);
        // INFO
        add(new Mods.Info("Clock", "Real time", Cat.INFO, 110, 6,
                () -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))), false);
        add(new Mods.Info("Direction", "Facing and yaw", Cat.INFO, 110, 26, () -> {
            var p = mc().player;
            return p == null ? "-" : cap(p.getHorizontalFacing().asString()) + " (" + Math.round(MathHelper.wrapDegrees(p.getYaw())) + ")";
        }), false);
        add(new Mods.Info("Memory", "Java RAM usage", Cat.INFO, 110, 46, () -> {
            Runtime r = Runtime.getRuntime();
            return "RAM " + ((r.totalMemory() - r.freeMemory()) >> 20) + "/" + (r.maxMemory() >> 20) + " MB";
        }), false);
        add(new Mods.Info("Server Address", "Current server", Cat.INFO, 110, 66, () -> {
            var s = mc().getCurrentServerEntry();
            return s == null ? "Singleplayer" : s.address;
        }), false);
        add(new Mods.Info("Session Timer", "Time since game start", Cat.INFO, 110, 86, () -> {
            long s = (System.currentTimeMillis() - START) / 1000;
            return String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
        }), false);
        add(new Mods.Info("Light Level", "Light at your feet", Cat.INFO, 110, 106, () -> {
            var m = mc();
            return m.world == null || m.player == null ? "Light -" : "Light " + m.world.getLightLevel(m.player.getBlockPos());
        }), false);
        add(new Mods.Info("Totem Counter", "Totems in inventory", Cat.INFO, 110, 126, () -> {
            var p = mc().player;
            if (p == null) return "Totems 0";
            PlayerInventory inv = p.getInventory();
            int n = 0;
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (s.isOf(Items.TOTEM_OF_UNDYING)) n += s.getCount();
            }
            return "Totems " + n;
        }), false);
        add(new Mods.Info("Day Counter", "World day", Cat.INFO, 110, 146, () -> {
            var w = mc().world;
            return w == null ? "Day -" : "Day " + (w.getTimeOfDay() / 24000L + 1);
        }), false);
        // VISUAL
        add(new Mods.Fullbright(), false);
        add(new Mods.Zoom(), true);
        add(new Mods.Flag("No Fire", "Hides the fire overlay"), false);
        add(new Mods.Flag("No Smoke", "Removes smoke particles"), false);
        add(new Mods.Flag("No Fog", "Pushes terrain fog away"), false);
        add(new Mods.Flag("Low Shield", "Lowers the held shield"), false);
        add(new Mods.Flag("No Hurt Cam", "No camera shake when hit"), false);
        add(new Mods.NoBob(), false);
        add(new Mods.ReduceParticles(), false);
        add(new Mods.NoWeather(), false);
        add(new Mods.Crosshair(), false);
        // UTILITY
        add(new Mods.ToggleSprint(), true);
        add(new Mods.ToggleSneak(), false);
        add(new Mods.AutoRespawn(), false);
        load();
        for (Module m : MODULES) m.anim = m.enabled ? 1f : 0f;
        Runtime.getRuntime().addShutdownHook(new Thread(ModuleManager::save));
    }

    /** Used by mixins. */
    public static boolean on(String name) {
        Module m = BY_NAME.get(name);
        return m != null && m.enabled;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.getWindow() == null || mc.options == null) return;
        if (!applied) { // apply persisted state once the options exist
            applied = true;
            for (Module m : MODULES) if (m.enabled) m.onEnable();
        }
        if (mc.currentScreen instanceof TitleScreen) { // swap in our main menu
            mc.setScreen(new LuminaTitleScreen());
            return;
        }
        boolean key = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (key && !prevMenuKey && mc.currentScreen == null && mc.world != null) {
            mc.setScreen(new ClickGui(null, ClickGui.Tab.HUD));
        }
        prevMenuKey = key;
        for (Module m : MODULES) if (m.enabled || m.alwaysTick()) m.tick();
    }

    public static void renderHud(DrawContext g) {
        var mc = mc();
        if (mc.currentScreen instanceof ClickGui || mc.options.hudHidden) return;
        dev.lumina.Gfx.alpha = 1f;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        for (Module m : MODULES) {
            if (!m.enabled) continue;
            if (m.isHud()) {
                int x = Math.max(0, Math.min(sw - m.width(), m.x)), y = Math.max(0, Math.min(sh - m.height(), m.y));
                m.draw(g, x, y);
            }
            m.overlay(g, sw, sh);
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
            Files.writeString(FILE, GSON.toJson(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void load() {
        try {
            if (!Files.exists(FILE)) return;
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
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
        } catch (Exception e) { e.printStackTrace(); }
    }
}
