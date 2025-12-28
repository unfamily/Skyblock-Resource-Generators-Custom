package net.unfamily.skb_res_gen_custom.block.renderer;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.skb_res_gen_custom.block.display.BedrockGeneratorDisplayItem;
import software.bernie.geckolib.model.GeoModel;

public class BedrockGeneratorItemModel extends GeoModel<BedrockGeneratorDisplayItem> {
    // ThreadLocal per passare tier e base_id dal renderer al model
    public static final ThreadLocal<String> CURRENT_TIER = ThreadLocal.withInitial(() -> "gold");
    public static final ThreadLocal<String> CURRENT_BASE_ID = ThreadLocal.withInitial(() -> "cobblestone");
    
    @Override
    public ResourceLocation getModelResource(BedrockGeneratorDisplayItem object) {
        return ResourceLocation.parse("skyblock_resources:geo/resource_casing.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BedrockGeneratorDisplayItem object) {
        // Use complete texture based on base_id + tier from ThreadLocal
        String baseId = CURRENT_BASE_ID.get();
        String tier = CURRENT_TIER.get();
        
        // If no configuration, use default texture
        if ((baseId == null || baseId.isEmpty() || baseId.equals("cobblestone")) &&
            (tier == null || tier.isEmpty() || tier.equals("gold"))) {
            return ResourceLocation.fromNamespaceAndPath(
                "skb_res_gen_custom",
                "textures/block/default_texture.png"
            );
        }
        
        if (baseId == null || baseId.isEmpty()) {
            baseId = "obsidian"; // default changed for test
        }
        if (tier == null || tier.isEmpty()) {
            tier = "gold"; // default
        }
        
        System.out.println("[DEBUG] ItemModel getTextureResource: tier=" + tier + " baseId=" + baseId);
        
        // Format: block/{base_id}_{tier}.png
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
            "skb_res_gen_custom",
            "textures/block/" + baseId + "_" + tier + ".png"
        );
        
        System.out.println("[DEBUG] Texture path: " + texture);
        
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(BedrockGeneratorDisplayItem object) {
        return ResourceLocation.parse("skyblock_resources:animations/resource_casing.animation.json");
    }
}

