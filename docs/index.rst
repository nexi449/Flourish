# Documentation

Flourish adds configurable bone-meal behavior to Minecraft small flowers. The same gameplay and configuration code is compiled for Fabric, NeoForge, and Quilt. Mod metadata currently declares Minecraft 1.21.1 through 1.21.11. Build checks were run at 1.21.1 and 1.21.11; intermediate versions were not individually build-tested.

## Project Layout

- `fabric/src/main/java/com/nexi499/flourish` contains the shared Java implementation used by all three loader modules.
- `fabric/src/main/resources` contains Fabric metadata and the shared asset resources.
- `neoforge/src/main/java` contains the NeoForge mod entrypoint. Its build also compiles the shared Java sources from the Fabric tree, except for the Fabric/Mod Menu entrypoint.
- `neoforge/src/main/resources` contains NeoForge metadata and its Mixin configuration.
- `quilt/src/main/resources` contains Quilt metadata and its Mixin configuration. Quilt also compiles the shared Java sources from the Fabric tree.

Keep shared gameplay, options, and config changes in the shared Fabric source tree. Keep loader-specific startup metadata and Mixin registration in the corresponding loader module. Avoid copying shared classes into the other modules, since the build already includes the shared source tree.

## Runtime Flow

1. The loader reads its own metadata and registers `flourish.mixins.json`.
2. When Minecraft loads `FlowerBlock`, `FlowerGrowthMixin` adds the `BonemealableBlock` behavior to it.
3. The first access to `FlourishMod` creates the singleton settings store. It loads `config/flourish.json`, or creates that file from Java's default boolean values when it does not exist.
4. When bone meal is used, the Mixin checks whether the flower is eligible and which configured behavior to run.
5. In the client options screen, checkbox changes update the in-memory `FlowerOptions`; pressing Done or closing the screen saves those values to disk.

## Java Code Reference

### `FlourishMod`

This is the shared access point for mod-wide state. `MOD_ID` is used to derive the config filename (`flourish.json`), `MOD_NAME` names the logger and screen, and `SETTINGS` owns the `ConfigStore<FlowerOptions>`. `options()` returns the currently loaded options object. It does not make a copy, so UI changes are visible to gameplay code immediately.

### `FlowerOptions`

This is the Gson data model. Its three public primitive booleans default to `false` in Java:

- `witherRose`, serialized as `wither_rose`, allows Wither Roses to be bone-meal targets when enabled.
- `torchflower`, serialized using its field name `torchflower`, allows Torchflowers to be targets when enabled.
- `useTallFlowerBehavior`, serialized as `use_tall_flower_behavior`, selects item-drop behavior instead of nearby flower spreading when enabled.

The exact JSON defaults and their user-facing effects are documented separately in [defaultconfig.md](defaultconfig.md).

### `ConfigStore<T>`

This generic class reads and writes one JSON config model. Its constructor builds the path `config/<modId>.json`, stores the model type, creates a default instance through its no-argument constructor, and then calls `load()`.

- `get()` returns the active in-memory model.
- `save()` creates the parent directory if needed and writes pretty-printed JSON, creating or truncating the file.
- `load()` writes defaults when no file exists. Otherwise it deserializes the file with Gson. A null result leaves the default model active. An I/O or runtime parsing error is logged and resets the in-memory value to defaults.

The error fallback does not rewrite a malformed existing file. A developer investigating a config parse failure should check the log and the JSON file itself.

### `FlowerGrowthMixin`

`@Mixin(FlowerBlock.class)` applies the class to Minecraft's flower block type. The Mixin implements `BonemealableBlock`, adding the callbacks vanilla uses to decide whether bone meal can act on a block and what happens after it succeeds. `@Unique` marks Flourish's helper methods so they do not collide with methods on the target class.

- `isValidBonemealTarget(...)` requires the current state to be in `BlockTags.SMALL_FLOWERS`. It rejects Wither Roses unless `wither_rose` is enabled and rejects Torchflowers unless `torchflower` is enabled. Other small flowers remain eligible.
- `isBonemealSuccess(...)` always returns `true`; the target check is handled separately by `isValidBonemealTarget(...)`.
- `getType()` reports `Type.NEIGHBOR_SPREADER` for the spreading mode and `Type.GROWER` for tall-flower item-drop mode. This tells vanilla which bonemeal behavior category is active.
- `performBonemeal(...)` dispatches to the selected behavior on the server. With tall-flower behavior enabled, `flourish$dropFlower(...)` spawns one item of the same block at the flower's position. Otherwise, `flourish$spreadFlowers(...)` attempts to place more blocks of the same flower nearby.

The spread algorithm chooses a target of 1 to 7 successful placements. It makes at most 64 attempts. The horizontal search radius increases after each group of 22 attempts; each candidate also gets a vertical offset from -1 to 1. A candidate is placed only when the block above the ground is air and the ground is in the DIRT tag or is farmland. The new block uses the source flower's default state and update flags `1 | 2`.

### `FlowerOptionsScreen`

This is the shared client settings screen. `init()` reads the active options and creates one checkbox for each boolean. Each checkbox callback updates the same options object returned by `FlourishMod.options()`. Both the Done button and `onClose()` save through `FlourishMod.SETTINGS`, so changes persist when the screen closes.

### `FlourishMenu`

This implements Mod Menu's `ModMenuApi` and returns a factory for `FlowerOptionsScreen`. Fabric and Quilt expose this class through their loader metadata's `modmenu` entrypoint. Mod Menu is an optional integration on Fabric; the screen can still be registered by another loader-specific entrypoint.

### `FlourishNeoForge`

The `@Mod("flourish")` annotation makes this the NeoForge entrypoint. Its constructor registers `FlowerOptionsScreen` as NeoForge's config screen extension point, using the supplied parent screen for navigation.

## Mixin and Metadata Resources

Each loader module registers a `flourish.mixins.json` file. The config identifies `com.nexi499.flourish.mixin.FlowerGrowthMixin`, sets the minimum Mixin version and Java compatibility level, and names the refmap used for remapping. Keep the class name and package synchronized between the Java source and all three Mixin configs. Loader metadata is separate per platform and controls dependencies, entrypoints, supported Minecraft versions, and the Mixin config path.

## Configuration File

The runtime path is `config/flourish.json`. It is generated on first launch and saved when the settings screen closes. See [defaultconfig.md](defaultconfig.md) for a copyable default file.

## Build

In a full development checkout with the Gradle build files and wrapper, build all loader variants with:

```powershell
.\gradlew.bat :fabric:build :neoforge:build :quilt:build
```

Each module writes its artifact to its own `build/libs/` directory; all three release JARs use the name `flourish-<version>.jar`. The public source repository does not include the Gradle build files, so a source-only clone cannot run this command as-is.
