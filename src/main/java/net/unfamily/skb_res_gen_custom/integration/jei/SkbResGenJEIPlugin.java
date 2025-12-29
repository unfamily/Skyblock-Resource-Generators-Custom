package net.unfamily.skb_res_gen_custom.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.unfamily.skb_res_gen_custom.SkbResGenCustom;
import net.unfamily.skb_res_gen_custom.init.ModDataComponents;
import net.unfamily.skb_res_gen_custom.init.ModItems;

import java.util.Optional;

@JeiPlugin
public class SkbResGenJEIPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(
        SkbResGenCustom.MOD_ID, 
        "jei_plugin"
    );

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Register subtype interpreter for resource generators
        // This tells JEI how to distinguish different generator variants
        registration.registerSubtypeInterpreter(ModItems.RESOURCE_GENERATOR.get(), GENERATOR_ITEM_INTERPRETER);
    }

    /**
     * Subtype interpreter that distinguishes generators based on their DataComponent (tier + base_id)
     */
    public static final ISubtypeInterpreter<ItemStack> GENERATOR_ITEM_INTERPRETER = new ISubtypeInterpreter<>() {
        @Override
        public Object getSubtypeData(ItemStack ingredient, UidContext context) {
            return Optional.ofNullable(ingredient.get(ModDataComponents.GENERATOR_DATA.get()))
                .map(data -> data.tier() + "_" + data.baseId())
                .orElse("default");
        }

        @Override
        public String getLegacyStringSubtypeInfo(ItemStack ingredient, UidContext context) {
            return Optional.ofNullable(ingredient.get(ModDataComponents.GENERATOR_DATA.get()))
                .map(data -> data.tier() + "_" + data.baseId())
                .orElse("default");
        }
    };
}

