package net.unfamily.skb_res_gen_custom.integration;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Compatibility layer for Skyblock Resources (native mod).
 * - Re-registers IItemHandler capability with side-aware handler (fixes hoppers/pipes)
 * - Pushes items into IItemHandler-only inventories below (fixes modded chests)
 */
public class NativeModCompat {
    private static final String NATIVE_MOD_ID = "skyblock_resources";
    private static final int PUSH_INTERVAL_TICKS = 20; // Once per second
    private static long tickCounter = 0;

    /**
     * Register capabilities with HIGHEST priority so we run before the native mod.
     * Provides side-aware IItemHandler for all skyblock_resources generators.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        BuiltInRegistries.BLOCK_ENTITY_TYPE.stream()
                .filter(type -> {
                    var key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
                    return key != null && key.getNamespace().equals(NATIVE_MOD_ID);
                })
                .forEach(type -> event.registerBlockEntity(
                        Capabilities.ItemHandler.BLOCK,
                        type,
                        NativeModCompat::getItemHandler
                ));
    }

    @Nullable
    private static IItemHandler getItemHandler(BlockEntity blockEntity, Direction side) {
        if (blockEntity instanceof WorldlyContainer worldly) {
            return new SidedInvWrapper(worldly, side);
        }
        return null;
    }

    /**
     * Periodically try to push items from native generators into IItemHandler-only blocks below.
     * The native mod's procedures only push into BaseContainerBlockEntity; we handle the rest.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || serverLevel.isClientSide()) {
            return;
        }
        tickCounter++;
        if (tickCounter % PUSH_INTERVAL_TICKS != 0) {
            return;
        }

        if (!(serverLevel.getChunkSource() instanceof ServerChunkCache chunkCache)) return;
        ChunkMap chunkMap = chunkCache.chunkMap;
        Iterable<ChunkHolder> chunks = getChunks(chunkMap);
        if (chunks == null) return;
        for (ChunkHolder holder : chunks) {
            var chunkResult = holder.getTickingChunkFuture().getNow(null);
            if (chunkResult == null) continue;
            LevelChunk levelChunk = chunkResult.orElse(null);
            if (levelChunk == null) continue;
            levelChunk.getBlockEntities().forEach((pos, blockEntity) -> {
                BlockEntityType<?> type = blockEntity.getType();
                var typeKey = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
                if (typeKey == null || !typeKey.getNamespace().equals(NATIVE_MOD_ID)) {
                    return;
                }
                if (!(blockEntity instanceof Container container)) return;
                ItemStack stack = container.getItem(0);
                if (stack.isEmpty()) return;

                BlockPos belowPos = pos.below();
                IItemHandler handler = serverLevel.getCapability(Capabilities.ItemHandler.BLOCK, belowPos, Direction.UP);
                if (handler == null) return;

                // Push into IItemHandler below (modded chests, storage drawers, etc.)
                for (int i = 0; i < handler.getSlots() && !stack.isEmpty(); i++) {
                    stack = handler.insertItem(i, stack, false);
                }
                container.setItem(0, stack.isEmpty() ? ItemStack.EMPTY : stack);
                blockEntity.setChanged();
                BlockEntity belowBe = serverLevel.getBlockEntity(belowPos);
                if (belowBe != null) belowBe.setChanged();
            });
        }
    }

    /**
     * Get max items buffer for a tier, inheriting from native mod config when available.
     * Fallback defaults: wooden 128, stone 256, iron 512, gold 1024, diamond 2048, netherite 4096.
     */
    public static int getMaxItemsForTier(String tier) {
        try {
            Class<?> configClass = Class.forName("net.drakma.skyblockresources.configuration.SkyblockResourcesConfigConfiguration");
            String fieldName = switch (tier != null ? tier.toLowerCase() : "wooden") {
                case "stone" -> "STONE_MAX_ITEMS";
                case "iron" -> "IRON_MAX_ITEMS";
                case "gold" -> "GOLD_MAX_ITEMS";
                case "diamond" -> "DIAMOND_MAX_ITEMS";
                case "netherite" -> "NETHERITE_MAX_ITEMS";
                default -> "WOODEN_MAX_ITEMS";
            };
            var field = configClass.getField(fieldName);
            Object configValue = field.get(null);
            if (configValue != null && configValue.getClass().getMethod("get").invoke(configValue) instanceof Number n) {
                return (int) Math.round(n.doubleValue());
            }
        } catch (Exception ignored) {}
        return getDefaultMaxItemsForTier(tier);
    }

    private static int getDefaultMaxItemsForTier(String tier) {
        return switch (tier != null ? tier.toLowerCase() : "wooden") {
            case "stone" -> 256;
            case "iron" -> 512;
            case "gold" -> 1024;
            case "diamond" -> 2048;
            case "netherite" -> 4096;
            default -> 128;
        };
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Iterable<ChunkHolder> getChunks(ChunkMap chunkMap) {
        try {
            Method m = ChunkMap.class.getDeclaredMethod("getChunks");
            m.setAccessible(true);
            return (Iterable<ChunkHolder>) m.invoke(chunkMap);
        } catch (Exception e) {
            return null;
        }
    }
}
