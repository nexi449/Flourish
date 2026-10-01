package com.nexi499.flourish.client.gui;

import com.nexi499.flourish.FlourishMod;
import com.nexi499.flourish.options.FlowerOptions;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class FlowerOptionsScreen extends Screen {
    private final Screen parent;

    public FlowerOptionsScreen(Screen parent) {
        super(Component.literal(FlourishMod.MOD_NAME));
        this.parent = parent;
    }

    @Override
    protected void init() {
        FlowerOptions options = FlourishMod.options();
        int left = this.width / 2 - 155;
        int top = this.height / 2 - 60;

        addRenderableWidget(Checkbox.builder(Component.translatable("flourish.options.wither_rose"), this.font)
                .selected(options.witherRose)
                .onValueChange((checkbox, selected) -> options.witherRose = selected)
                .pos(left, top)
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("flourish.options.torchflower"), this.font)
                .selected(options.torchflower)
                .onValueChange((checkbox, selected) -> options.torchflower = selected)
                .pos(left, top + 24)
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("flourish.options.use_tall_flower_behavior"), this.font)
                .selected(options.useTallFlowerBehavior)
                .onValueChange((checkbox, selected) -> options.useTallFlowerBehavior = selected)
                .pos(left, top + 48)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> close())
                .bounds(this.width / 2 - 100, this.height / 2 + 75, 200, 20)
                .build());
    }

    @Override
    public void onClose() {
        FlourishMod.SETTINGS.save();
        super.onClose();
    }

    private void close() {
        FlourishMod.SETTINGS.save();
        this.minecraft.setScreen(this.parent);
    }
}