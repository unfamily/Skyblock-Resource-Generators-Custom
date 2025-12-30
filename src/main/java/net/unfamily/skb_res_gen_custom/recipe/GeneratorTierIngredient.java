package net.unfamily.skb_res_gen_custom.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.unfamily.skb_res_gen_custom.init.ModDataComponents;
import net.unfamily.skb_res_gen_custom.init.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.unfamily.skb_res_gen_custom.generator.GeneratorLoader;
import net.unfamily.skb_res_gen_custom.generator.GeneratorDefinition;

/**
 * Custom ingredient that matches generator items based only on their tier,
 * ignoring the base_id. This allows recipes like:
 * "any netherite generator" -> "netherite_empty_generator"
 */
public class GeneratorTierIngredient implements ICustomIngredient {
    
    public static final MapCodec<GeneratorTierIngredient> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            com.mojang.serialization.Codec.STRING.fieldOf("tier").forGetter(i -> i.tier)
        ).apply(instance, GeneratorTierIngredient::new)
    );
    
    private final String tier;
    
    public GeneratorTierIngredient(String tier) {
        this.tier = tier.toLowerCase();
    }
    
    /**
     * Tests if the given ItemStack matches this ingredient.
     * Matches if the item is a resource generator with the specified tier,
     * regardless of the base_id.
     */
    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        // Check if it's our resource generator item
        if (!stack.is(ModItems.RESOURCE_GENERATOR.get())) {
            return false;
        }
        
        // Check the tier from the DataComponent
        ModDataComponents.GeneratorData data = stack.get(ModDataComponents.GENERATOR_DATA.get());
        if (data != null) {
            return tier.equalsIgnoreCase(data.tier());
        }
        
        // Fallback: check NBT data (for backward compatibility)
        net.minecraft.nbt.CompoundTag customData = stack.getOrDefault(
            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY
        ).copyTag();
        
        if (customData.contains("GeneratorData")) {
            net.minecraft.nbt.CompoundTag genData = customData.getCompound("GeneratorData");
            String itemTier = genData.getString("tier");
            return tier.equalsIgnoreCase(itemTier);
        }
        
        return false;
    }
    
    /**
     * Returns all matching item stacks for display in JEI/recipe viewers.
     * Returns all registered generators with the specified tier so JEI shows
     * real textures instead of missing textures.
     */
    @Override
    public Stream<ItemStack> getItems() {
        List<ItemStack> stacks = new ArrayList<>();
        
        // Get all registered custom generators and create a stack for each with this tier
        for (GeneratorDefinition def : GeneratorLoader.getAllGenerators().values()) {
            ItemStack stack = net.unfamily.skb_res_gen_custom.block.display.ResourceGeneratorDisplayItem
                .createConfiguredStack(
                    net.unfamily.skb_res_gen_custom.init.ModBlocks.RESOURCE_GENERATOR.get(),
                    tier,
                    def.getBaseId()
                );
            stacks.add(stack);
        }
        
        // If no custom generators are registered, return an empty stream
        // (JEI will handle this gracefully)
        if (stacks.isEmpty()) {
            return Stream.empty();
        }
        
        return stacks.stream();
    }
    
    @Override
    public boolean isSimple() {
        return false; // We have custom matching logic
    }
    
    @Override
    public IngredientType<?> getType() {
        return ModIngredientTypes.GENERATOR_TIER.get();
    }
    
    /**
     * Creates a new Ingredient from this custom ingredient
     */
    public static Ingredient of(String tier) {
        return new GeneratorTierIngredient(tier).toVanilla();
    }
    
    public String getTier() {
        return tier;
    }
}

