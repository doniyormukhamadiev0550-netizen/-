package dev.lumina;

import java.util.HashMap;
import java.util.Map;

/** Tiny keyed easing helper: value glides toward its target every frame. */
public final class Anim {
    private static final Map<String, Float> VALUES = new HashMap<>();

    public static float to(String key, float target, float speed) {
        float v = VALUES.getOrDefault(key, target);
        v += (target - v) * speed;
        if (Math.abs(target - v) < 0.002f) v = target;
        VALUES.put(key, v);
        return v;
    }
}
