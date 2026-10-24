package dev.lumina;

import dev.lumina.modules.ModuleManager;
import net.fabricmc.api.ModInitializer;

public class LuminaMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ModuleManager.init();
    }
}
