package dev.lumina.modules;

import net.minecraft.client.MinecraftClient;

public abstract class Module {
    public enum Cat { HUD, INFO, VISUAL, UTILITY }

    public final String name, desc;
    public final Cat cat;
    public boolean enabled;
    public float anim;
    public int x, y;

    protected Module(String name, String desc, Cat cat, int x, int y) {
        this.name = name;
        this.desc = desc;
        this.cat = cat;
        this.x = x;
        this.y = y;
    }

    protected static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean v) {
        if (enabled == v) return;
        enabled = v;
        if (v) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void tick() {}               // once per rendered frame
    public boolean alwaysTick() { return false; }
    public boolean isHud() { return false; }
    public int width() { return 0; }
    public int height() { return 0; }
    public void draw(int x, int y) {}
    public void overlay(int sw, int sh) {}
}
