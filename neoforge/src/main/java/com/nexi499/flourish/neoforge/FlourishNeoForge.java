package com.nexi499.flourish.neoforge;

import com.nexi499.flourish.FlourishMod;
import com.nexi499.flourish.client.gui.FlowerOptionsScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(FlourishMod.MOD_ID)
public final class FlourishNeoForge {
    public FlourishNeoForge(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> new FlowerOptionsScreen(parent)
        );
    }
}