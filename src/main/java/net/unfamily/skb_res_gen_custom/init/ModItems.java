package net.unfamily.skb_res_gen_custom.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.skb_res_gen_custom.SkbResGenCustom;
import net.unfamily.skb_res_gen_custom.block.display.ResourceGeneratorDisplayItem;

public class ModItems {
    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(SkbResGenCustom.MOD_ID);

    public static final DeferredItem<Item> RESOURCE_GENERATOR = REGISTRY.register(
            ModBlocks.RESOURCE_GENERATOR.getId().getPath(),
            () -> new ResourceGeneratorDisplayItem(ModBlocks.RESOURCE_GENERATOR.get(), new Item.Properties())
    );
}


