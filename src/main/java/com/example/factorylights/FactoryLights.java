package com.example.factorylights;

import com.example.factorylights.client.ClientSetup;
import com.example.factorylights.test.FactoryLightTests;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(FactoryLights.MODID)
public class FactoryLights {
    public static final String MODID = "factorylights";

    public FactoryLights(IEventBus modBus, ModContainer container) {
        ModRegistry.register(modBus);
        modBus.addListener(RegisterGameTestsEvent.class, event -> event.register(FactoryLightTests.class));
        container.registerConfig(ModConfig.Type.SERVER, FactoryLightsConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.register(modBus);
        }
    }
}
