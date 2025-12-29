package net.unfamily.skb_res_gen_custom.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.skb_res_gen_custom.SkbResGenCustom;
import net.unfamily.skb_res_gen_custom.block.display.ResourceGeneratorDisplayItem;
import net.unfamily.skb_res_gen_custom.generator.GeneratorLoader;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRY = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkbResGenCustom.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SKB_RES_GEN_CUSTOM_TAB = 
        REGISTRY.register("skb_res_gen_custom", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.skb_res_gen_custom.tab"))
            .icon(() -> new ItemStack(ModItems.RESOURCE_GENERATOR.get()))
            .displayItems((params, output) -> {
                // Tiers in order
                String[] tiers = {"wooden", "stone", "iron", "gold", "diamond", "netherite"};
                
                // Add all configured generators with creative_tab = true
                GeneratorLoader.getAllGenerators().values().forEach(def -> {
                    if (def.isCreativeTab()) {
                        // Add one item for each tier
                        for (String tier : tiers) {
                            try {
                                ItemStack stack = ResourceGeneratorDisplayItem.createConfiguredStack(
                                    ModBlocks.RESOURCE_GENERATOR.get(), 
                                    tier, 
                                    def.getBaseId()
                                );
                                output.accept(stack);
                            } catch (Exception e) {
                                // Silently skip if there's an error
                            }
                        }
                    }
                });
            })
            .build()
        );
}

