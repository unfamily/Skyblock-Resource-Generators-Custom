package net.unfamily.skb_res_gen_custom.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import net.unfamily.skb_res_gen_custom.block.display.ResourceGeneratorDisplayItem;
import net.unfamily.skb_res_gen_custom.block.renderer.ResourceGeneratorItemModel;

public class ResourceGeneratorDisplayItemRenderer extends GeoItemRenderer<ResourceGeneratorDisplayItem> {
    public ResourceGeneratorDisplayItemRenderer() {
        super(new ResourceGeneratorItemModel());
    }
    
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Read tier and baseId from NBT
        String tier = null;
        String baseId = null;
        
        try {
            CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                                  net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if (tag.contains("GeneratorData")) {
                CompoundTag genData = tag.getCompound("GeneratorData");
                String readTier = genData.getString("tier");
                if (readTier != null && !readTier.isEmpty()) {
                    tier = readTier;
                }
                // IMPORTANT: NBT uses "base_id" (with underscore), not "baseId"
                String readBaseId = genData.getString("base_id");
                if (readBaseId != null && !readBaseId.isEmpty()) {
                    baseId = readBaseId;
                }
            }
        } catch (Exception e) {
            // Fallback - will use default texture
            System.out.println("[DEBUG] Error reading item NBT: " + e.getMessage());
        }
        
        // If no NBT data (creative tab), both will be null and default texture will be used
        System.out.println("[DEBUG] Rendering item with tier=" + tier + " baseId=" + baseId);
        
        // Pass tier and baseId to model via ThreadLocal (null values trigger default texture)
        ResourceGeneratorItemModel.CURRENT_TIER.set(tier);
        ResourceGeneratorItemModel.CURRENT_BASE_ID.set(baseId);
        
        try {
            // Render
            super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
        } finally {
            // Cleanup
            ResourceGeneratorItemModel.CURRENT_TIER.remove();
            ResourceGeneratorItemModel.CURRENT_BASE_ID.remove();
        }
    }
}


