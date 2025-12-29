package net.unfamily.skb_res_gen_custom.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.skb_res_gen_custom.SkbResGenCustom;
import net.unfamily.skb_res_gen_custom.generator.GeneratorDefinition;
import net.unfamily.skb_res_gen_custom.generator.GeneratorLoader;
import net.unfamily.skb_res_gen_custom.init.ModBlocks;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Handles conversion of native empty generators to custom generators
 * when right-clicked with the specified recipe item
 */
@EventBusSubscriber(modid = SkbResGenCustom.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class GeneratorConversionHandler {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static final String[] TIERS = {"wooden", "stone", "iron", "gold", "diamond", "netherite"};
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        InteractionHand hand = event.getHand();
        
        // Server-side only
        if (level.isClientSide()) {
            return;
        }
        
        // Get the block clicked
        Block clickedBlock = level.getBlockState(pos).getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(clickedBlock);
        
        // Check if it's an empty generator from the native mod
        if (!blockId.getNamespace().equals("skyblock_resources") || !blockId.getPath().endsWith("_empty_generator")) {
            return;
        }
        
        // Extract tier from block ID (e.g., "wooden_empty_generator" -> "wooden")
        String blockPath = blockId.getPath();
        String tier = blockPath.replace("_empty_generator", "");
        
        // Verify it's a valid tier
        boolean validTier = false;
        for (String t : TIERS) {
            if (t.equals(tier)) {
                validTier = true;
                break;
            }
        }
        
        if (!validTier) {
            return;
        }
        
        // Get the item in hand
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) {
            return;
        }
        
        ResourceLocation heldItemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
        String heldItemIdString = heldItemId.toString();
        
        // Check all generator definitions to see if this item matches a recipe
        for (GeneratorDefinition def : GeneratorLoader.getAllGenerators().values()) {
            String recipeItem = def.getRecipe();
            if (recipeItem != null && recipeItem.equals(heldItemIdString)) {
                // Found a match! Convert the empty generator to our custom generator
                
                // Place our custom generator block
                level.setBlock(pos, ModBlocks.RESOURCE_GENERATOR.get().defaultBlockState(), 3);
                
                // Configure the block entity
                net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof net.unfamily.skb_res_gen_custom.block.entity.ResourceGeneratorTileEntity tileEntity) {
                    tileEntity.configureFromDefinition(def, tier);
                    tileEntity.setChanged();
                }
                
                // Consume the recipe item
                if (!player.isCreative()) {
                    heldItem.shrink(1);
                }
                
                // Play sound effect (same as native mod's empty generator conversion)
                level.playSound(null, pos, SoundEvents.ANVIL_PLACE, 
                              SoundSource.NEUTRAL, 1.0F, 0.5F);
                
                // Swing arm animation
                player.swing(hand, true);
                
                // Cancel the event to prevent default behavior
                event.setCanceled(true);
                
                LOGGER.debug("Converted {} empty generator to {} custom generator (tier: {}) at {}", 
                           tier, def.getBaseId(), tier, pos);
                
                return;
            }
        }
    }
}

