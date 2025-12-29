package net.unfamily.skb_res_gen_custom.block.renderer;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.skb_res_gen_custom.block.entity.ResourceGeneratorTileEntity;
import software.bernie.geckolib.model.GeoModel;

public class ResourceGeneratorModel extends GeoModel<ResourceGeneratorTileEntity> {
    @Override
    public ResourceLocation getModelResource(ResourceGeneratorTileEntity object) {
        return ResourceLocation.parse("skyblock_resources:geo/resource_casing.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ResourceGeneratorTileEntity object) {
        // Use complete texture based on base_id + tier
        String baseId = object.getBaseId();
        String tier = object.getTier();
        
        // If no valid configuration, use default texture (during placement or unconfigured)
        if (baseId == null || baseId.isEmpty() || baseId.equals("bedrock") ||
            tier == null || tier.isEmpty()) {
            return ResourceLocation.fromNamespaceAndPath(
                "skb_res_gen_custom",
                "textures/block/default_texture.png"
            );
        }
        
        // Format: block/{base_id}_{tier}.png
        return ResourceLocation.fromNamespaceAndPath(
            "skb_res_gen_custom",
            "textures/block/" + baseId + "_" + tier + ".png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(ResourceGeneratorTileEntity object) {
        return ResourceLocation.parse("skyblock_resources:animations/resource_casing.animation.json");
    }
}



