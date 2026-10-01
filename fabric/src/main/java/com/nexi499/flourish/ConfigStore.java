package com.nexi499.flourish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

public final class ConfigStore<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final Class<T> dataType;
    private final Supplier<T> defaults;
    private T value;

    public ConfigStore(String modId, Class<T> dataType) {
        this.path = Path.of("config", modId + ".json");
        this.dataType = dataType;
        this.defaults = () -> {
            try {
                return dataType.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not create default config", exception);
            }
        };
        this.value = defaults.get();
        load();
    }

    public T get() {
        return value;
    }

    public void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(value), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            FlourishMod.LOG.error("Could not save Flourish config", exception);
        }
    }

    private void load() {
        if (!Files.isRegularFile(path)) {
            save();
            return;
        }

        try {
            T loadedValue = GSON.fromJson(Files.readString(path), dataType);
            if (loadedValue != null) {
                value = loadedValue;
            }
        } catch (IOException | RuntimeException exception) {
            FlourishMod.LOG.error("Could not load Flourish config; using defaults", exception);
            value = defaults.get();
        }
    }
}