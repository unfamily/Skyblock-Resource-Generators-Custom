package net.unfamily.skb_res_gen_custom.block.renderer;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.skb_res_gen_custom.block.display.ResourceGeneratorDisplayItem;
import software.bernie.geckolib.model.GeoModel;

public class ResourceGeneratorItemModel extends GeoModel<ResourceGeneratorDisplayItem> {
    // ThreadLocal to pass tier and base_id from renderer to model
    // Use null as default to detect when no NBT is present
    public static final ThreadLocal<String> CURRENT_TIER = new ThreadLocal<>();
    public static final ThreadLocal<String> CURRENT_BASE_ID = new ThreadLocal<>();
    
    @Override
    public ResourceLocation getModelResource(ResourceGeneratorDisplayItem object) {
        return ResourceLocation.parse("skyblock_resources:geo/resource_casing.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ResourceGeneratorDisplayItem object) {
        // Use complete texture based on base_id + tier from ThreadLocal
        String baseId = CURRENT_BASE_ID.get();
        String tier = CURRENT_TIER.get();
        
        // If no configuration (creative tab item), use default texture
        if (baseId == null || tier == null) {
            System.out.println("[DEBUG] ItemModel using default texture (no NBT)");
            return ResourceLocation.fromNamespaceAndPath(
                "skb_res_gen_custom",
                "textures/block/default_texture.png"
            );
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
    public ResourceLocation getAnimationResource(ResourceGeneratorDisplayItem object) {
        return ResourceLocation.parse("skyblock_resources:animations/resource_casing.animation.json");
    }
}

