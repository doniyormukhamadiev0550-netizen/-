package dev.lumina;

import dev.lumina.modules.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.util.Identifier;

public class LuminaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        ClientTickEvents.END_CLIENT_TICK.register(ModuleManager::tick);
        HudElementRegistry.addLast(Identifier.of("lumina", "hud"), (ctx, tickCounter) -> ModuleManager.renderHud(ctx));
        // "Lumina Client" tag in the corner of every inventory-style screen
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof HandledScreen<?>) {
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> Badge.draw(ctx, s.width, s.height));
            }
        });
    }
}
