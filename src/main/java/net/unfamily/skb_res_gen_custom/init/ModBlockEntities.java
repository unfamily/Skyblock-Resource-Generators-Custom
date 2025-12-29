package net.unfamily.skb_res_gen_custom.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.skb_res_gen_custom.SkbResGenCustom;
import net.unfamily.skb_res_gen_custom.block.ResourceGeneratorBlock;
import net.unfamily.skb_res_gen_custom.block.entity.ResourceGeneratorTileEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SkbResGenCustom.MOD_ID);
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceGeneratorTileEntity>> RESOURCE_GENERATOR = 
        REGISTRY.register("resource_generator", () -> {
            // Create the type
            BlockEntityType<ResourceGeneratorTileEntity> type = BlockEntityType.Builder.of(
                ResourceGeneratorTileEntity::new,
                ModBlocks.RESOURCE_GENERATOR.get()
            ).build(null);
            
            return type;
        });
}

