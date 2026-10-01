# Armor Visibility Documentation

## 1. Purpose

Armor Visibility is a client-side Minecraft mod that controls which armor-related visual layers are rendered. It does not modify inventory, equipment, player stats, or combat mechanics. The mod changes only the render layer and therefore acts as a purely cosmetic visibility control.

The key idea is simple: the game keeps the item and its equipment state, but the mod cancels the render call before the model is drawn.

## 2. Functional scope

The mod supports:

- global armor visibility on/off
- per-slot control for helmet, chestplate, leggings, boots
- cape visibility toggle
- elytra visibility toggle
- local-player-only logic
- optional behavior for other players

This means the project is not a gameplay modification. It is a client-side rendering filter.

## 3. Configuration architecture

The central class is `ArmorVisibilityConfig`.

### 3.1 Singleton pattern

The config is exposed as a static singleton:

- `ArmorVisibilityConfig.INSTANCE`

This avoids passing config objects through many subsystems and makes the render classes simple.

### 3.2 Internal data model

The internal `Data` container keeps the actual config values:

- `armorVisible`
- `hideHelmet`
- `hideChestplate`
- `hideLeggings`
- `hideBoots`
- `keepCapeVisible`
- `keepElytraVisible`
- `hideForOtherPlayers`
- `playerOnly`

Default values:

- armor visible = true
- each armor slot visible
- cape visible = true
- elytra visible = true
- other players unaffected by default
- playerOnly = false

### 3.3 File persistence

The config is saved as:

- `armorvisibility.json`

The location is resolved via the loader config directory. The loader API is used to place the file in the proper Minecraft config folder.

The persistence flow is:

1. `load()` checks whether the config file exists.
2. If missing, it creates a new default object and immediately writes it.
3. If present, it reads the JSON with Gson.
4. If parsing fails, it falls back to the default config.
5. `save()` writes the current state with pretty formatting.

This protects the mod from missing or corrupted configuration files and ensures consistent default behavior.

### 3.4 Slot mapping

The enum `ArmorPart` maps friendly names to Minecraft equipment slots:

- `HELMET -> HEAD`
- `CHESTPLATE -> CHEST`
- `LEGGINGS -> LEGS`
- `BOOTS -> FEET`

This is important because the UI labels are human-readable, but the render logic operates on `EquipmentSlot` values from the engine.

### 3.5 Important methods

Core accessors and setters include:

- `load()`
- `save()`
- `isArmorVisible()`
- `setArmorVisible(boolean)`
- `shouldKeepCapeVisible()`
- `setKeepCapeVisible(boolean)`
- `shouldKeepElytraVisible()`
- `setKeepElytraVisible(boolean)`
- `shouldHideForOtherPlayers()`
- `setHideForOtherPlayers(boolean)`
- `isPlayerOnly()`
- `setPlayerOnly(boolean)`
- `isArmorPartHidden(EquipmentSlot slot)`
- `setArmorPartHidden(EquipmentSlot slot, boolean hidden)`

The most important behavior is inside `isArmorPartHidden(...)`:

- if `armorVisible` is false, the method reports the slot as hidden
- otherwise it checks the slot-specific boolean for the requested equipment slot

This creates a layered decision system: global state first, then per-slot state.

## 4. Client initialization

`ArmorVisibilityClient` is the client entry point and implements `ClientModInitializer`.

### 4.1 Startup flow

When the client initializes, it does this:

1. loads settings from disk
2. hooks into the screen lifecycle
3. locates the pause-screen statistics button
4. injects a custom icon button beside it
5. opens the config screen when pressed

### 4.2 Screen event hook

The mod registers a listener with `ScreenEvents.AFTER_INIT`.

For every screen created after initialization, it checks whether the screen is a `PauseScreen`. If it is not, it exits early. If it is, it continues by searching for the statistics button and placing the custom button next to it.

This makes the configuration visible from the standard pause menu without needing a separate in-game menu page.

### 4.3 Button discovery

The static method `findStatisticsButton(...)` searches widgets and compares each text label against:

- `gui.stats`

When it finds the right button, it stores the widget reference and positions the new icon button offset by a small margin.

### 4.4 Custom icon button

The class `ArmorIconButton` extends `Button` and overrides the default render extraction method.

It draws a custom texture from a resource path:

- `armorvisibility:textures/gui/armoricon.png`

This is how the mod exposes a minimal UI element that integrates visually with the vanilla pause menu.

### 4.5 Compatibility screen opening

The method `showScreen(Minecraft client, Screen screen)` uses reflection:

- first tries `setScreenAndShow(Screen)`
- falls back to `setScreen(Screen)`

This allows compatibility across Minecraft versions that may have changed the method signature or screen API.

## 5. Config screen design

`ArmorVisibilityConfigScreen` is the user-facing config screen.

### 5.1 Screen lifecycle

The screen stores a reference to the parent screen and the title of the config screen is set to "Armor Visibility".

The `init()` method builds the UI every time the screen is reopened or rebuilt.

### 5.2 Layout behavior

The layout is manually calculated:

- `contentWidth` is limited to the screen size
- the buttons are placed in a two-column layout
- slot controls are stacked vertically
- global toggles are placed further down on the screen

This keeps the interface compact and readable without generating complex UI logic.

### 5.3 Armor slot controls

The code creates a button for each part via `addArmorToggle(...)`:

- Helmet
- Chestplate
- Leggings
- Boots

Each button toggles the hidden state of that exact part.

When clicked:

- `isArmorPartHidden(part.getSlot())` reads the current state
- the opposite value is written back via `setArmorPartHidden(...)`
- the widget list is rebuilt so the label updates immediately

### 5.4 Global toggles

The screen adds buttons for:

- Global Armor
- Cape
- Elytra
- Player Only
- Other Armor

These are generic boolean toggles created through `createToggleButton(...)`, which reads the current state from a boolean supplier and writes back through a setter function.

### 5.5 Text generation

String generation is handled by helper methods:

- `armorPartText(...)`
- `toggleText(...)`

The labels are designed to say either:

- `Hidden` / `Shown`
- `On` / `Off`
- `Visible` / `Hidden` for the other-player setting

This keeps the UI readable while reflecting the underlying boolean values precisely.

### 5.6 Done action

The final button calls:

- `ArmorVisibilityClient.showScreen(Minecraft.getInstance(), parent)`

This returns the player to the previous menu after configuration is done.

## 6. Mod Menu integration

`ArmorVisibilityModMenuApi` is a tiny compatibility adapter for Mod Menu.

It implements:

- `getModConfigScreenFactory()`

and returns a factory that creates the config screen.

This means the mod can be config-driven from both the pause menu and the standard mod loader menu.

## 7. Render cancellation as the core mechanism

The project relies on Mixin injection. That is the actual behavior driver.

The mod does not replace textures or manipulate equipment state. Instead, it cancels the render method before the model is submitted to the renderer.

The critical operation is:

- `ci.cancel();`

This cancels the callback, stopping the draw flow at its source.

## 8. Armor render hook

### 8.1 Target class

The mod targets `HumanoidArmorLayer`.

The mixin class is:

- `ArmorFeatureRendererMixin`

### 8.2 Injection site

The injection is:

- `@Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)`

Going at `HEAD` is important because it prevents the rest of the method from executing when the mod decides the armor should be hidden.

### 8.3 Input parameters and conditions

The method receives:

- `PoseStack matrices`
- `SubmitNodeCollector queue`
- `ItemStack stack`
- `EquipmentSlot slot`
- `int light`
- `HumanoidRenderState state`
- `CallbackInfo ci`

The method checks multiple conditions in sequence:

1. `slot == null` -> return immediately
2. stack is elytra -> temporarily skip and let the elytra-specific logic decide
3. `playerOnly` enabled and the current render target is not the local player -> do nothing
4. global armor disabled -> cancel render
5. slot-specific hide rule active and the target is either not another player or `hideForOtherPlayers` is enabled -> cancel render

This structure keeps the logic deterministic and prevents accidental hiding of the wrong entity.

### 8.4 Player-only logic

The check uses:

- `Minecraft.getInstance().player`

If `playerOnly` is enabled and either the local player is missing or the entity type does not match the local player type, the render is allowed to continue.

This means the feature is intentionally restricted to the local player model when configured that way.

### 8.5 Other-player detection

The helper `isOtherPlayer(...)` compares the rendered entity state to the local player state.

It only returns true for a different rendered entity that matches the same entity type but is not the current local player.

Its purpose is to decide whether a hide rule should be applied to another player, depending on the `hideForOtherPlayers` config value.

## 9. Cape render hook

The class:

- `CapeFeatureRendererMixin`

targets the cape render pipeline and injects into:

- `CapeLayer.submit`

The injection is:

- `@Inject(method = "submit", at = @At("HEAD"), cancellable = true)`

Then it checks:

- `!ArmorVisibilityConfig.INSTANCE.shouldKeepCapeVisible()`

If the flag is false, it cancels the render invocation.

This makes cape visibility independent from the normal armor slot logic.

## 10. Elytra render hook

The class:

- `ElytraFeatureRendererMixin`

targets the wings/elytra layer and injects into:

- `WingsLayer.submit`

The logic is even simpler than the armor logic:

- if `shouldKeepElytraVisible()` is false, cancel render

This mirrors the cape behavior and ensures the elytra is hidden without affecting the actual equipment item.

## 11. Full runtime sequence

The full behavior path is:

1. The user opens the config screen.
2. A toggle is clicked.
3. The UI setter calls into `ArmorVisibilityConfig`.
4. The value is persisted to `armorvisibility.json` with `save()`.
5. On the next render pass, the relevant mixin executes.
6. The mixin reads the config value at `HEAD` before drawing begins.
7. If the condition matches, it calls `ci.cancel()`.
8. The render pipeline exits and the armor, cape, or elytra does not appear on screen.

This is the central architecture of the mod: state in config, UI in screen, enforcement in mixins.

## 12. Behavior semantics

### 12.1 Global armor off

When `armorVisible` is false, all armor is hidden regardless of per-slot state.

This is the strongest mode because it globally disables the render of armor pieces on the player model.

### 12.2 Per-slot hiding

When global armor remains enabled, each slot can be controlled individually.

Example:

- helmet hidden
- chestplate visible
- leggings visible
- boots visible

This is useful when the player wants a custom look without removing gear from equipment slots.

### 12.3 Player-only behavior

When `playerOnly` is true, the hide rules are applied only to the local player.

This means the mod does not change how other players are rendered unless separate rules are added.

### 12.4 Other-player visibility toggle

If `hideForOtherPlayers` is true, other players are also subject to the hide logic.

This turns the mod from a local-only cosmetic filter into a more global visual effect.

## 13. Why the mod is stable

The implementation is stable because it is deliberately narrow in scope:

- configuration is centralized
- UI code is isolated to the screen class
- rendering logic is isolated to mixins
- no gameplay systems are rewritten

It targets the render phase only, which is the safest place to perform visual toggling without affecting game logic.

## 14. Limitations

This mod does not:

- remove the item from the equipment slot
- modify armor item data or stats
- affect combat mechanics
- alter enchantments or item durability
- change world state or entity state

It only hides the model representation, not the underlying equipment.
