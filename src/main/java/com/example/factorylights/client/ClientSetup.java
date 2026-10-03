package com.example.factorylights.client;

import com.example.factorylights.FactoryLights;
import com.example.factorylights.ModRegistry;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class ClientSetup {
    static final ModelResourceLocation RAYS_SINGLE = rays("godrayssingular");
    static final ModelResourceLocation RAYS_FRONT = rays("godraysedgefront");
    static final ModelResourceLocation RAYS_CENTER = rays("godrayscenter");
    static final ModelResourceLocation RAYS_BACK = rays("godraysedgeback");

    private ClientSetup() {}

    private static ModelResourceLocation rays(String name) {
        return ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(FactoryLights.MODID, "block/factory_light/" + name));
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerRenderers);
        modBus.addListener(ClientSetup::registerModels);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistry.FACTORY_LIGHT_BE.get(), FactoryLightRenderer::new);
    }

    private static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(RAYS_SINGLE);
        event.register(RAYS_FRONT);
        event.register(RAYS_CENTER);
        event.register(RAYS_BACK);
    }
}
