package net.unfamily.skb_res_gen_custom;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.unfamily.skb_res_gen_custom.init.ModItems;
import net.unfamily.skb_res_gen_custom.init.ModBlocks;
import net.unfamily.skb_res_gen_custom.init.ModBlockEntities;
import net.unfamily.skb_res_gen_custom.init.ModDataComponents;
import net.unfamily.skb_res_gen_custom.init.ModCreativeTabs;
import net.unfamily.skb_res_gen_custom.recipe.ModIngredientTypes;
import net.unfamily.skb_res_gen_custom.command.ReloadGeneratorsCommand;
import net.unfamily.skb_res_gen_custom.command.GiveGeneratorCommand;
import net.unfamily.skb_res_gen_custom.generator.GeneratorLoader;
import net.unfamily.skb_res_gen_custom.generator.GeneratorDefinition;
import net.unfamily.skb_res_gen_custom.block.display.ResourceGeneratorDisplayItem;
import net.minecraft.world.item.ItemStack;
import net.unfamily.skb_res_gen_custom.util.ReadmeGenerator;
import java.nio.file.Path;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SkbResGenCustom.MOD_ID)
public class SkbResGenCustom {
    public static final String MOD_ID = "skb_res_gen_custom";
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public SkbResGenCustom(IEventBus modEventBus, ModContainer modContainer) {
        // Register blocks, items, block entities, data components, ingredient types and creative tabs
        ModBlocks.REGISTRY.register(modEventBus);
        ModItems.REGISTRY.register(modEventBus);
        ModBlockEntities.REGISTRY.register(modEventBus);
        ModDataComponents.REGISTRY.register(modEventBus);
        ModIngredientTypes.REGISTRY.register(modEventBus);
        ModCreativeTabs.REGISTRY.register(modEventBus);
        
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Creative tab is now handled by ModCreativeTabs.java
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Carica le definizioni dei generatori custom
        event.enqueueWork(() -> {
            LOGGER.info("Loading custom generator definitions...");
            net.unfamily.skb_res_gen_custom.generator.GeneratorLoader.scanConfigDirectory();
            // Generate README in the same directory the loader scans (Modpack/kubejs/external_scripts/skb_res_gen_custom/)
            try {
                Path readmeDir = Path.of("Modpack", "kubejs", "external_scripts", "skb_res_gen_custom");
                ReadmeGenerator.writeReadme(readmeDir, true); // force overwrite to apply latest template
            } catch (Exception e) {
                LOGGER.debug("Failed to generate README for custom generators: {}", e.getMessage());
            }
        });
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ReloadGeneratorsCommand.register(event.getDispatcher());
        GiveGeneratorCommand.register(event.getDispatcher());
        LOGGER.info("Registered /skb_res_gen commands (reload, give)");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // BlockEntity renderer is registered in `ClientListener.registerRenderers`
        }
    }
}