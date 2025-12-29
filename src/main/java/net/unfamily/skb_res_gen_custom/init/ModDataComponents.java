package net.unfamily.skb_res_gen_custom.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.skb_res_gen_custom.SkbResGenCustom;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTRY = DeferredRegister.createDataComponents(SkbResGenCustom.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GeneratorData>> GENERATOR_DATA = 
        REGISTRY.registerComponentType("generator_data", builder -> builder
            .persistent(GeneratorData.CODEC)
            .networkSynchronized(GeneratorData.STREAM_CODEC)
        );

    /**
     * Data class to store generator configuration in ItemStack
     */
    public record GeneratorData(String tier, String baseId) {
        public static final Codec<GeneratorData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.fieldOf("tier").forGetter(GeneratorData::tier),
                Codec.STRING.fieldOf("base_id").forGetter(GeneratorData::baseId)
            ).apply(instance, GeneratorData::new)
        );

        public static final StreamCodec<ByteBuf, GeneratorData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GeneratorData::tier,
            ByteBufCodecs.STRING_UTF8, GeneratorData::baseId,
            GeneratorData::new
        );
    }
}

