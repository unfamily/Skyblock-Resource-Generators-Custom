package net.unfamily.skb_res_gen_custom.mixin;

import net.drakma.skyblockresources.init.SkyblockResourcesModJeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.unfamily.skb_res_gen_custom.block.display.ResourceGeneratorDisplayItem;
import net.unfamily.skb_res_gen_custom.generator.GeneratorDefinition;
import net.unfamily.skb_res_gen_custom.generator.GeneratorLoader;
import net.unfamily.skb_res_gen_custom.init.ModBlocks;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mixin to add our custom generator conversions to JEI as if they were native recipes
 * This is ONLY for visualization - actual conversion happens in GeneratorConversionHandler
 */
@Mixin(SkyblockResourcesModJeiPlugin.class)
public class JEIRecipeRegistrationMixin {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] TIERS = {"wooden", "stone", "iron", "gold", "diamond", "netherite"};
    
    @Inject(method = "registerRecipes", at = @At("TAIL"), remap = false)
    private void onRegisterRecipes(IRecipeRegistration registration, CallbackInfo ci) {
        try {
            RecipeManager recipeManager = ((ClientLevel) Objects.requireNonNull(Minecraft.getInstance().level)).getRecipeManager();
            
            // Create fake SetGeneratorGUIRecipe instances for our custom generators
            List<net.drakma.skyblockresources.jei_recipes.SetGeneratorGUIRecipe> customRecipes = new ArrayList<>();
            
            for (GeneratorDefinition def : GeneratorLoader.getAllGenerators().values()) {
                String recipeItem = def.getRecipe();
                if (recipeItem == null || recipeItem.isEmpty()) {
                    continue; // Skip if no recipe defined
                }
                
                // Create a recipe for each tier
                for (String tier : TIERS) {
                    try {
                        // Create output stack
                        ItemStack output = ResourceGeneratorDisplayItem.createConfiguredStack(
                            ModBlocks.RESOURCE_GENERATOR.get(), tier, def.getBaseId()
                        );
                        
                        // Create ingredients: recipe item + empty generator
                        NonNullList<Ingredient> ingredients = NonNullList.create();
                        
                        // Parse recipe item
                        net.minecraft.resources.ResourceLocation recipeItemLoc = net.minecraft.resources.ResourceLocation.parse(recipeItem);
                        net.minecraft.world.item.Item recipeItemObj = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(recipeItemLoc);
                        ingredients.add(Ingredient.of(new ItemStack(recipeItemObj)));
                        
                        // Parse empty generator
                        net.minecraft.resources.ResourceLocation emptyGenLoc = net.minecraft.resources.ResourceLocation.parse("skyblock_resources:" + tier + "_empty_generator");
                        net.minecraft.world.item.Item emptyGenItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(emptyGenLoc);
                        ingredients.add(Ingredient.of(new ItemStack(emptyGenItem)));
                        
                        // Create fake recipe
                        FakeSetGeneratorGUIRecipe fakeRecipe = new FakeSetGeneratorGUIRecipe(output, ingredients);
                        customRecipes.add(fakeRecipe);
                        
                    } catch (Exception e) {
                        LOGGER.error("Failed to create JEI recipe for {} (tier: {}): {}", 
                                   def.getBaseId(), tier, e.getMessage());
                    }
                }
            }
            
            // Register our fake recipes with JEI using the native recipe type
            if (!customRecipes.isEmpty()) {
                registration.addRecipes(SkyblockResourcesModJeiPlugin.SetGeneratorGUI_Type, customRecipes);
                LOGGER.info("Registered {} custom generator recipes in JEI", customRecipes.size());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to register custom generator recipes in JEI: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Fake implementation of SetGeneratorGUIRecipe for JEI visualization only
     */
    private static class FakeSetGeneratorGUIRecipe extends net.drakma.skyblockresources.jei_recipes.SetGeneratorGUIRecipe {
        public FakeSetGeneratorGUIRecipe(ItemStack output, NonNullList<Ingredient> ingredients) {
            super(output, ingredients);
        }
    }
}

