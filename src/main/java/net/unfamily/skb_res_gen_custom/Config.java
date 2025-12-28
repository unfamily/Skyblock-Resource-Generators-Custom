package net.unfamily.skb_res_gen_custom;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = SkbResGenCustom.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Custom Generators Configuration
    static {
        BUILDER.comment("Custom Generators Configuration").push("generators");
    }

    private static final ModConfigSpec.ConfigValue<String> CUSTOM_GENERATORS_PATH = BUILDER
            .comment("Path to the custom generators JSON files directory",
                    "Default: 'kubejs/external_scripts/skb_res_gen_custom/'",
                    "The system will look for JSON files in this directory to generate custom generators")
            .define("000_custom_generators_path", "kubejs/external_scripts/skb_res_gen_custom/");

    static {
        BUILDER.pop(); // End of generators category
    }

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    // Public variable to access custom generators path
    public static String customGeneratorsPath;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        customGeneratorsPath = CUSTOM_GENERATORS_PATH.get();
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        onLoad(event);
    }

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
