package net.unfamily.skb_res_gen_custom.integration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
 *
 * NOTE: The native mod's push logic only pushes into BaseContainerBlockEntity (vanilla containers).
 * This class supplements that by pushing into any IItemHandler (modded storage, drawers, etc.).
 *
 * The chunk iteration uses only public NeoForge APIs (ServerChunkCache.getChunkNow + getForcedChunks)
 * to avoid reflection issues with the Java module system in production environments.
 * Each dimension uses its own game time to avoid cross-dimension tick counter interference.
 */
public class NativeModCompat {
    private static final String NATIVE_MOD_ID = "skyblock_resources";
    private static final int PUSH_INTERVAL_TICKS = 20; // Once per second
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean loggedOnce = false;

    /**
     * Register capabilities with HIGHEST priority so we run before the native mod.
     * Provides side-aware IItemHandler for all skyblock_resources generators.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        int count = 0;
        for (var type : BuiltInRegistries.BLOCK_ENTITY_TYPE.stream()
                .filter(t -> {
                    var key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(t);
                    return key != null && key.getNamespace().equals(NATIVE_MOD_ID);
                }).toList()) {
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    type,
                    NativeModCompat::getItemHandler
            );
            count++;
        }
        LOGGER.info("[NativeModCompat] Registered side-aware IItemHandler for {} native block entity types", count);
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
     *
     * Uses the level's own game time so each dimension ticks independently,
     * avoiding cross-dimension tick counter interference.
     * Only processes levels that have players or force-loaded chunks.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || serverLevel.isClientSide()) {
            return;
        }

        // Use the level's own game time so each dimension ticks independently
        if (serverLevel.getGameTime() % PUSH_INTERVAL_TICKS != 0) {
            return;
        }

        // Skip dimensions with no players and no forced chunks (nothing to process)
        if (serverLevel.players().isEmpty() && serverLevel.getForcedChunks().isEmpty()) {
            return;
        }

        try {
            processLoadedChunks(serverLevel);
        } catch (Exception e) {
            LOGGER.error("[NativeModCompat] Error in processLoadedChunks", e);
        }
    }

    /**
     * Iterate all loaded and ticking chunks (player-area + force-loaded) and push items
     * from native generators into IItemHandler inventories below.
     */
    private static void processLoadedChunks(ServerLevel serverLevel) {
        ServerChunkCache chunkSource = serverLevel.getChunkSource();
        Set<Long> processedChunks = new HashSet<>();
        int generatorsPushed = 0;

        // 1. Process chunks around each player (ticking area)
        int simDist = serverLevel.getServer().getPlayerList().getSimulationDistance();
        for (ServerPlayer player : serverLevel.players()) {
            ChunkPos playerChunk = player.chunkPosition();
            for (int dx = -simDist; dx <= simDist; dx++) {
                for (int dz = -simDist; dz <= simDist; dz++) {
                    int cx = playerChunk.x + dx;
                    int cz = playerChunk.z + dz;
                    if (!processedChunks.add(ChunkPos.asLong(cx, cz))) continue;

                    LevelChunk chunk = chunkSource.getChunkNow(cx, cz);
                    if (chunk == null) continue;

                    generatorsPushed += processChunk(serverLevel, chunk);
                }
            }
        }

        // 2. Process force-loaded chunks (chunk loaders, etc.)
        LongSet forcedChunks = serverLevel.getForcedChunks();
        if (!forcedChunks.isEmpty()) {
            forcedChunks.forEach(chunkPosLong -> {
                if (!processedChunks.add(chunkPosLong)) return;
                int cx = ChunkPos.getX(chunkPosLong);
                int cz = ChunkPos.getZ(chunkPosLong);
                LevelChunk chunk = chunkSource.getChunkNow(cx, cz);
                if (chunk != null) {
                    processChunk(serverLevel, chunk);
                }
            });
        }

        // One-time startup log to confirm the fix is active
        if (!loggedOnce && generatorsPushed > 0) {
            LOGGER.info("[NativeModCompat] Push fix active - pushed items from native generators in {} chunks",
                    processedChunks.size());
            loggedOnce = true;
        }
    }

    /**
     * Process a single chunk: find native generators, push their items into IItemHandler below.
     * @return number of generators that had items pushed
     */
    private static int processChunk(ServerLevel serverLevel, LevelChunk chunk) {
        // Collect generators first to avoid ConcurrentModificationException during iteration
        List<Map.Entry<BlockPos, BlockEntity>> generators = new ArrayList<>();
        for (var entry : chunk.getBlockEntities().entrySet()) {
            BlockEntity blockEntity = entry.getValue();
            var typeKey = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
            if (typeKey != null && typeKey.getNamespace().equals(NATIVE_MOD_ID)) {
                generators.add(Map.entry(entry.getKey(), blockEntity));
            }
        }

        int pushed = 0;
        for (var entry : generators) {
            BlockPos pos = entry.getKey();
            BlockEntity blockEntity = entry.getValue();

            if (!(blockEntity instanceof Container container)) continue;
            ItemStack stack = container.getItem(0);
            if (stack.isEmpty()) continue;

            BlockPos belowPos = pos.below();
            IItemHandler handler = serverLevel.getCapability(Capabilities.ItemHandler.BLOCK, belowPos, Direction.UP);
            if (handler == null) continue;

            // Push into IItemHandler below (modded chests, storage drawers, etc.)
            ItemStack remaining = stack;
            for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
                remaining = handler.insertItem(i, remaining, false);
            }

            if (remaining.getCount() != stack.getCount()) {
                container.setItem(0, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                blockEntity.setChanged();
                BlockEntity belowBe = serverLevel.getBlockEntity(belowPos);
                if (belowBe != null) belowBe.setChanged();
                pushed++;
            }
        }
        return pushed;
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
}
