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
import net.unfamily.skb_res_gen_custom.command.ReloadGeneratorsCommand;
import net.unfamily.skb_res_gen_custom.command.GiveGeneratorCommand;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SkbResGenCustom.MOD_ID)
public class SkbResGenCustom {
    public static final String MOD_ID = "skb_res_gen_custom";
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public SkbResGenCustom(IEventBus modEventBus, ModContainer modContainer) {
        // Register blocks, items and block entities
        ModBlocks.REGISTRY.register(modEventBus);
        ModItems.REGISTRY.register(modEventBus);
        ModBlockEntities.REGISTRY.register(modEventBus);
        
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Carica le definizioni dei generatori custom
        event.enqueueWork(() -> {
            LOGGER.info("Loading custom generator definitions...");
            net.unfamily.skb_res_gen_custom.generator.GeneratorLoader.scanConfigDirectory();
        });
    }

    // Add item to the Skyblock Resources creative tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Check if this is the skyblock_resources tab
        if (event.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS) ||
            event.getTabKey().location().toString().equals("skyblock_resources:skyblock_resources")) {
            try {
                event.accept(ModItems.RESOURCE_GENERATOR);
            } catch (IllegalArgumentException ignored) {
                // Some setups may already register the same ItemStack in the tab; ignore duplicates
            }
        }
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