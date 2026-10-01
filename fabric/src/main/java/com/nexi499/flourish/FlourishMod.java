package com.nexi499.flourish;

import com.nexi499.flourish.options.FlowerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlourishMod {
    public static final String MOD_ID = "flourish";
    public static final String MOD_NAME = "Flourish";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
    public static final ConfigStore<FlowerOptions> SETTINGS = new ConfigStore<>(MOD_ID, FlowerOptions.class);

    private FlourishMod() {
    }

    public static FlowerOptions options() {
        return SETTINGS.get();
    }
}