package net.unfamily.skb_res_gen_custom.block.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import net.unfamily.skb_res_gen_custom.block.entity.ResourceGeneratorTileEntity;

public class ResourceGeneratorRenderer extends GeoBlockRenderer<ResourceGeneratorTileEntity> {
    public ResourceGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        super(new ResourceGeneratorModel());
    }
}


