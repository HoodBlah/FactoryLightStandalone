package com.example.factorylights;

import com.example.factorylights.block.FactoryLightBlock;
import com.example.factorylights.block.FactoryLightBlockEntity;
import com.example.factorylights.block.FactoryLightLightBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FactoryLights.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FactoryLights.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FactoryLights.MODID);

    public static final DeferredBlock<FactoryLightBlock> FACTORY_LIGHT = BLOCKS.registerBlock(
            "factory_light", FactoryLightBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<FactoryLightLightBlock> FACTORY_LIGHT_LIGHT = BLOCKS.registerBlock(
            "factory_light_light", FactoryLightLightBlock::new,
            BlockBehaviour.Properties.of()
                    .replaceable()
                    .noCollission()
                    .noLootTable()
                    .noOcclusion()
                    .noTerrainParticles()
                    .pushReaction(PushReaction.DESTROY)
                    .air());

    public static final DeferredItem<BlockItem> FACTORY_LIGHT_ITEM = ITEMS.registerSimpleBlockItem("factory_light", FACTORY_LIGHT);

    public static final Supplier<BlockEntityType<FactoryLightBlockEntity>> FACTORY_LIGHT_BE = BLOCK_ENTITIES.register(
            "factory_light",
            () -> BlockEntityType.Builder.of(FactoryLightBlockEntity::new, FACTORY_LIGHT.get()).build(null));

    private ModRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ModRegistry::registerCapabilities);
        modBus.addListener(ModRegistry::buildCreativeTabs);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FACTORY_LIGHT_BE.get(),
                (be, side) -> be.getEnergyProxy());
    }

    private static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(FACTORY_LIGHT_ITEM);
        }
    }
}
