package net.unfamily.skb_res_gen_custom.block.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import net.unfamily.skb_res_gen_custom.block.entity.BedrockGeneratorTileEntity;

public class BedrockGeneratorRenderer extends GeoBlockRenderer<BedrockGeneratorTileEntity> {
    public BedrockGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        super(new BedrockGeneratorModel());
    }
}


