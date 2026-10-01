package com.nexi499.flourish.client;

import com.nexi499.flourish.client.gui.FlowerOptionsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class FlourishMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FlowerOptionsScreen::new;
    }
}