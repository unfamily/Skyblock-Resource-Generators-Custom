package net.unfamily.skb_res_gen_custom.block.display;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import java.util.function.Consumer;
import java.util.List;
import net.unfamily.skb_res_gen_custom.client.renderer.ResourceGeneratorDisplayItemRenderer;
import net.unfamily.skb_res_gen_custom.block.entity.ResourceGeneratorTileEntity;
import net.unfamily.skb_res_gen_custom.generator.GeneratorLoader;
import net.unfamily.skb_res_gen_custom.generator.GeneratorDefinition;
import net.unfamily.skb_res_gen_custom.init.ModDataComponents;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.network.chat.MutableComponent;

public class ResourceGeneratorDisplayItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ResourceGeneratorDisplayItem(Block block, Item.Properties properties) {
        super(block, properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    
    /**
     * Returns true to indicate that each ItemStack with different NBT should be treated as unique
     */
    @Override
    public boolean canFitInsideContainerItems() {
        return true; // This helps with stack uniqueness
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No controller needed for static rendering
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public Component getName(ItemStack stack) {
        // Read NBT data to show custom name
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                              net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        
        if (tag.contains("GeneratorData")) {
            CompoundTag genData = tag.getCompound("GeneratorData");
            String tier = genData.getString("tier");
            String baseId = genData.getString("base_id");
            
            if (!tier.isEmpty() && !baseId.isEmpty()) {
                GeneratorDefinition def = GeneratorLoader.getGenerator(baseId);
                if (def != null) {
                    String nameKey = def.getName().getString();
                    
                    // If name starts with "generator.", use translation key: name.tier
                    if (nameKey.startsWith("generator.")) {
                        String translationKey = nameKey + "." + tier.toLowerCase();
                        return Component.translatable(translationKey);
                    } else {
                        // Fallback: "Resource" + name + "Generator" + tier (e.g., "Resource Obsidian Generator Wooden")
                        String tierCapitalized = tier.substring(0, 1).toUpperCase() + tier.substring(1);
                        return Component.literal("Resource " + nameKey + " Generator " + tierCapitalized);
                    }
                }
            }
        }
        
        return super.getName(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Add generator information to tooltip
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                              net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        
        if (tag.contains("GeneratorData")) {
            CompoundTag genData = tag.getCompound("GeneratorData");
            
            // Show generator name (from definition)
            if (genData.contains("baseId")) {
                String baseId = genData.getString("baseId");
                GeneratorDefinition def = GeneratorLoader.getGenerator(baseId);
                if (def != null && def.getName() != null) {
                    // Prefix label and append the generator's component name so translations/styles are preserved
                    tooltip.add(Component.literal("§7Type: §f").append(def.getName()));
                }
            }
            
            if (genData.contains("ticks")) {
                int ticks = genData.getInt("ticks");
                double seconds = ticks / 20.0;
                tooltip.add(Component.literal("§7Time: §f" + String.format("%.1f", seconds) + "s"));
            }
            
            if (genData.contains("stack_size")) {
                int stackSize = genData.getInt("stack_size");
                tooltip.add(Component.literal("§7Amount: §f" + stackSize + " per cycle"));
            }
        }
    }
    
    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, net.minecraft.world.level.Level world, 
                                                  net.minecraft.world.entity.player.Player player, 
                                                  ItemStack stack, BlockState state) {
        boolean result = super.updateCustomBlockEntityTag(pos, world, player, stack, state);
        
        // Configure TileEntity with item NBT data
        if (!world.isClientSide) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ResourceGeneratorTileEntity tileEntity) {
                CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                                      net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                
                CompoundTag genData = null;
                
                // Try new format (nested in GeneratorData)
                if (tag.contains("GeneratorData")) {
                    genData = tag.getCompound("GeneratorData");
                }
                // Try direct format (from recipe)
                else if (tag.contains("tier") && tag.contains("base_id")) {
                    genData = tag;
                }
                
                if (genData != null) {
                    String tier = genData.getString("tier");
                    String baseId = genData.getString("base_id");
                    
                    if (!tier.isEmpty() && !baseId.isEmpty()) {
                        // Load definition and configure tile entity
                        GeneratorDefinition def = GeneratorLoader.getGenerator(baseId);
                        if (def != null) {
                            tileEntity.configureFromDefinition(def, tier);
                        } else {
                            // Fallback: use manual data if present
                            if (genData.contains("output") && genData.contains("ticks") && genData.contains("stack_size")) {
                                tileEntity.configure(tier, baseId, 
                                    genData.getString("output"), 
                                    genData.getInt("ticks"), 
                                    genData.getInt("stack_size"));
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ResourceGeneratorDisplayItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ResourceGeneratorDisplayItemRenderer();
                }
                return this.renderer;
            }
        });
    }
    
    /**
     * Helper method to create a configured ItemStack with tier and base_id using DataComponents
     */
    public static ItemStack createConfiguredStack(Block block, String tier, String baseId) {
        ItemStack stack = new ItemStack(block);
        
        // Use DataComponent instead of NBT for proper stack distinction
        stack.set(ModDataComponents.GENERATOR_DATA.get(), 
                  new ModDataComponents.GeneratorData(tier, baseId));
        
        // Also keep NBT for backward compatibility and additional data
        CompoundTag genData = new CompoundTag();
        genData.putString("tier", tier);
        genData.putString("base_id", baseId);
        
        // Load definition to add complete data
        GeneratorDefinition def = GeneratorLoader.getGenerator(baseId);
        if (def != null) {
            genData.putString("output", def.getOutput());
            int tierIndex = getTierIndex(tier);
            if (tierIndex >= 0 && tierIndex < 6) {
                genData.putInt("ticks", def.getTimes()[tierIndex]);
                genData.putInt("stack_size", def.getStacks()[tierIndex]);
            }
        }
        
        CompoundTag customData = new CompoundTag();
        customData.put("GeneratorData", genData);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                  net.minecraft.world.item.component.CustomData.of(customData));
        return stack;
    }
    
    private static int getTierIndex(String tier) {
        return switch (tier.toLowerCase()) {
            case "wooden" -> 0;
            case "stone" -> 1;
            case "iron" -> 2;
            case "gold" -> 3;
            case "diamond" -> 4;
            case "netherite" -> 5;
            default -> 0;
        };
    }
}


