package net.unfamily.skb_res_gen_custom.block.renderer;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.skb_res_gen_custom.block.entity.BedrockGeneratorTileEntity;
import software.bernie.geckolib.model.GeoModel;

public class BedrockGeneratorModel extends GeoModel<BedrockGeneratorTileEntity> {
    @Override
    public ResourceLocation getModelResource(BedrockGeneratorTileEntity object) {
        return ResourceLocation.parse("skyblock_resources:geo/resource_casing.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BedrockGeneratorTileEntity object) {
        // Use complete texture based on base_id + tier
        String baseId = object.getBaseId();
        String tier = object.getTier();
        
        // If no configuration, use default texture
        if ((baseId == null || baseId.isEmpty() || baseId.equals("bedrock")) && 
            (tier == null || tier.isEmpty())) {
            return ResourceLocation.fromNamespaceAndPath(
                "skb_res_gen_custom",
                "textures/block/default_texture.png"
            );
        }
        
        if (baseId == null || baseId.isEmpty()) {
            baseId = "cobblestone"; // default
        }
        if (tier == null || tier.isEmpty()) {
            tier = "wooden"; // default
        }
        
        // Format: block/{base_id}_{tier}.png
        return ResourceLocation.fromNamespaceAndPath(
            "skb_res_gen_custom",
            "textures/block/" + baseId + "_" + tier + ".png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(BedrockGeneratorTileEntity object) {
        return ResourceLocation.parse("skyblock_resources:animations/resource_casing.animation.json");
    }
}



