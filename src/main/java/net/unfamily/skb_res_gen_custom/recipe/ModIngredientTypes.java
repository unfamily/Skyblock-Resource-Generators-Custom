package net.unfamily.skb_res_gen_custom.recipe;

import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.unfamily.skb_res_gen_custom.SkbResGenCustom;

/**
 * Registry for custom ingredient types.
 * These ingredients can be used in vanilla recipe types (shapeless, shaped, etc.)
 */
public class ModIngredientTypes {
    
    public static final DeferredRegister<IngredientType<?>> REGISTRY = 
        DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, SkbResGenCustom.MOD_ID);
    
    /**
     * Ingredient type that matches generators by tier only.
     * 
     * Usage in recipe JSON:
     * {
     *   "type": "minecraft:crafting_shapeless",
     *   "ingredients": [
     *     {
     *       "type": "skb_res_gen_custom:generator_tier",
     *       "tier": "netherite"
     *     }
     *   ],
     *   "result": {
     *     "id": "skyblock_resources:netherite_empty_generator"
     *   }
     * }
     */
    public static final DeferredHolder<IngredientType<?>, IngredientType<GeneratorTierIngredient>> GENERATOR_TIER =
        REGISTRY.register("generator_tier", () -> new IngredientType<>(GeneratorTierIngredient.CODEC));
}

