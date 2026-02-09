package net.unfamily.skb_res_gen_custom;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = SkbResGenCustom.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Custom Generators Configuration").push("generators");
    }

    private static final ModConfigSpec.ConfigValue<String> CUSTOM_GENERATORS_PATH = BUILDER
            .comment("Path to the custom generators JSON files directory",
                    "Default: 'kubejs/external_scripts/skb_res_gen_custom/'",
                    "The system will look for JSON files in this directory to generate custom generators")
            .define("000_custom_generators_path", "kubejs/external_scripts/skb_res_gen_custom/");

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static String customGeneratorsPath;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        customGeneratorsPath = CUSTOM_GENERATORS_PATH.get();
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        onLoad(event);
    }
}
