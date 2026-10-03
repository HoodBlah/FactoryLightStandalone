package com.example.factorylights;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FactoryLightsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue FE_PER_TICK;
    public static final ModConfigSpec.IntValue BUFFER_PER_BLOCK;
    public static final ModConfigSpec.IntValue PROJECTION_RANGE;

    static {
        var builder = new ModConfigSpec.Builder();
        FE_PER_TICK = builder
                .comment("FE consumed per tick by each block of a lit factory light.")
                .defineInRange("fePerTickPerBlock", 4, 0, 100_000);
        BUFFER_PER_BLOCK = builder
                .comment("FE buffer added by each block of a factory light row.")
                .defineInRange("bufferPerBlock", 500, 1, 10_000_000);
        PROJECTION_RANGE = builder
                .comment("Maximum number of blocks below the light that receive projected light.")
                .defineInRange("projectionRange", 16, 1, 64);
        SPEC = builder.build();
    }

    private FactoryLightsConfig() {}
}
