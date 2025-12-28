package net.unfamily.skb_res_gen_custom.init;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.skb_res_gen_custom.SkbResGenCustom;
import net.unfamily.skb_res_gen_custom.block.BedrockGeneratorBlock;

public class ModBlocks {
    public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(SkbResGenCustom.MOD_ID);
    
    public static final DeferredBlock<Block> RESOURCE_GENERATOR = REGISTRY.register("resource_generator", BedrockGeneratorBlock::new);
}

