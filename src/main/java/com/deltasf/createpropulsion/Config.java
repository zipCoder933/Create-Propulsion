package com.deltasf.createpropulsion;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Float> THRUSTER_THRUST_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Integer> OPTICAL_SENSOR_TICKS_PER_UPDATE;
    public static final ForgeConfigSpec.ConfigValue<Integer> INLINE_OPTICAL_SENSOR_MAX_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<Integer> OPTICAL_SENSOR_MAX_DISTANCE;

    static {
        BUILDER.push("Configuration");
        
        THRUSTER_THRUST_MULTIPLIER = BUILDER.comment("Thruster thrust is multiplied by that.").define("Thrust multiplier", 1.0f);
        OPTICAL_SENSOR_TICKS_PER_UPDATE = BUILDER.comment("How many ticks between casting a ray. Lower values are more precise, but can have negative effect on performance.")
            .defineInRange("Optical sensor tick rate", 5, 1, 100);
        INLINE_OPTICAL_SENSOR_MAX_DISTANCE = BUILDER.comment("Length of the raycast ray.")
            .defineInRange("Inline optical sensor max raycast distance", 16, 4, 32);
        OPTICAL_SENSOR_MAX_DISTANCE = BUILDER.comment("Length of the raycast ray. Very high values may degrade performance. Change with caution!")
            .defineInRange("Optical sensor max raycast distance", 64, 8, 128);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
