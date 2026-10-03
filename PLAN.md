# Factory Lights: standalone FE-powered factory light (NeoForge 1.21.1)

Extracts the Factory Light from [Create: Power Grid](https://github.com/patryk3211/PowerGrid) (branch `architectury-1.21.1/dev`, Apache-2.0)
into a standalone NeoForge mod powered by Forge Energy (FE).

## Decisions

- No bulb item and no thermal/burnout simulation. A light is on whenever it has FE; unpowered uses the "empty" model.
- Connected segments in a row share one FE buffer. The leader is the single piece or negative-edge piece (part 0, 1 or 4).
- 4 FE/t per block and a 500 FE buffer per block, both configurable. No redstone control.
- Mod id `factorylights`, name "Factory Lights", package `com.example.factorylights`.
- Recipe: 3 iron ingots (top row), 1 glass (middle), 1 copper ingot (bottom), yields 1 light.
- Projection range defaults to 16 (as upstream), configurable up to 64.
- License Apache-2.0, with a NOTICE crediting PowerGrid and listing modifications.

## Upstream sources

Base: `https://raw.githubusercontent.com/patryk3211/PowerGrid/architectury-1.21.1/dev/`

- `src/main/java/org/patryk3211/powergrid/electricity/light/factorylight/`
  `FactoryLightBlock`, `FactoryLightBlockEntity`, `FactoryLightLightBlock`, `FactoryLightLightBlockEntity`, `FactoryLightRenderer`
- `src/main/resources/assets/powergrid/models/block/factory_light/` (models, including `empty/`)
- `src/main/resources/assets/powergrid/textures/block/factory_light/` (`factorylight`, `factorylightempty`, `godrays`)
- `.../collections/ModdedRenderLayers.java` (additive render type)

Part numbering: 0 single; 1 / 2 / 3 = north edge / center NS / south edge; 4 / 5 / 6 = west edge / center EW / east edge.

## Phases

1. **Scaffold**: NeoForge MDK (ModDevGradle, Java 21), `gradle.properties`, `neoforge.mods.toml`, `LICENSE`, `NOTICE`.
2. **Assets**: download models and textures, rewrite namespace to `factorylights`, replace the `zinc_plate` particle texture. Handwrite blockstates, item model, loot table, recipe, lang and tags.
3. **Code**:
   - `FactoryLightBlock`: part merging, shapes, `LIT` property.
   - `FactoryLightBlockEntity`: leader/row logic, FE buffer, light projection.
   - Energy capability: any segment forwards received FE to the row leader.
   - `FactoryLightLightBlock`: invisible light block, self-cleaning via scheduled ticks (no block entity).
   - Client: additive-blended ray renderer.
   - Config, registries, creative tab.

## Verification

1. `gradlew build` produces a jar without errors.
2. `gradlew runClient`: place a row of 3 and check that segments merge into edge/center/edge and split again when one is broken.
3. Feed FE into one segment: the whole row lights, rays render, light blocks extend down to the configured range.
4. Remove FE: the row goes dark and the light column vanishes within ~20 ticks.
5. Break a light: its column vanishes, the others keep working.
6. Reload the world: energy and lit state persist.

## Status

- Implemented and built: `build/libs/factorylights-1.0.0.jar`.
- Verified by gametests (`gradlew runGameTestServer`, sources in `src/main/java/com/example/factorylights/test`): row energy sharing, beam projection, beam removal, row splitting and merging.
- Not yet verified: client rendering (additive rays, models). Run `gradlew runClient` and check visually.
- Building requires JDK 21 (`JAVA_HOME`).

## Out of scope

Bulbs, Create, Flywheel, Ponder, wires, ceiling tiles, fixtures, datagen, Fabric/Forge loaders.
Shader packs may break the additive rays (a known upstream issue).
