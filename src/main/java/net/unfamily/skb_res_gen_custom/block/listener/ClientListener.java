package net.unfamily.skb_res_gen_custom.block.listener;

import net.unfamily.skb_res_gen_custom.block.renderer.BedrockGeneratorRenderer;
import net.unfamily.skb_res_gen_custom.init.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "skb_res_gen_custom", value = {Dist.CLIENT}, bus = EventBusSubscriber.Bus.MOD)
public class ClientListener {
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        try {
            event.registerBlockEntityRenderer((net.minecraft.world.level.block.entity.BlockEntityType) ModBlockEntities.RESOURCE_GENERATOR.get(),
                    context -> new BedrockGeneratorRenderer(context));
        } catch (Throwable ignored) {}
    }
}


