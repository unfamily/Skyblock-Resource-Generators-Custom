package net.unfamily.skb_res_gen_custom.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.skb_res_gen_custom.block.entity.ResourceGeneratorTileEntity;
import net.unfamily.skb_res_gen_custom.init.ModBlockEntities;
import net.unfamily.skb_res_gen_custom.init.ModDataComponents;
import net.unfamily.skb_res_gen_custom.generator.GeneratorLoader;
import net.unfamily.skb_res_gen_custom.generator.GeneratorDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ResourceGeneratorBlock extends BaseEntityBlock {
    public static final IntegerProperty ANIMATION = IntegerProperty.create("animation", 0, 1);
    public static final MapCodec<ResourceGeneratorBlock> CODEC = simpleCodec(properties -> new ResourceGeneratorBlock());
    
    public MapCodec<ResourceGeneratorBlock> codec() {
        return CODEC;
    }
    
    public ResourceGeneratorBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(1.0F, 10.0F)
                .noOcclusion()
                .isRedstoneConductor((bs, br, bp) -> false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ModBlockEntities.RESOURCE_GENERATOR.get().create(blockPos, blockState);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ANIMATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        List<net.minecraft.world.item.ItemStack> dropsOriginal = super.getDrops(state, builder);
        if (!dropsOriginal.isEmpty())
            return dropsOriginal;
        
        // Get tile entity from loot context
        BlockEntity blockEntity = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof ResourceGeneratorTileEntity gen) {
            // Use createConfiguredStack to create a clean item like /give command
            ItemStack drop = net.unfamily.skb_res_gen_custom.block.display.ResourceGeneratorDisplayItem.createConfiguredStack(
                this, gen.getTier(), gen.getBaseId()
            );
            return Collections.singletonList(drop);
        }
        
        // Fallback: return basic item
        return Collections.singletonList(new ItemStack(this, 1));
    }
    
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, 
                                      net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        // Middle-click: return item with NBT
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ResourceGeneratorTileEntity gen) {
            // Use createConfiguredStack to create a clean item like /give command
            return net.unfamily.skb_res_gen_custom.block.display.ResourceGeneratorDisplayItem.createConfiguredStack(
                this, gen.getTier(), gen.getBaseId()
            );
        }
        
        // Fallback: return basic item
        return new ItemStack(this);
    }

    @Override
    public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(blockstate, world, pos, oldState, moving);
        
        // Configure tile entity only if not already configured by updateCustomBlockEntityTag
        if (!world.isClientSide) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ResourceGeneratorTileEntity gen) {
                // Check if already configured (base_id != "bedrock" by default)
                if (gen.getBaseId().equals("bedrock") && gen.getOutputItem().equals("minecraft:bedrock")) {
                    // Only if not configured, try to configure from loaded definitions
                    GeneratorDefinition def = GeneratorLoader.getGenerator("bedrock");
                    if (def != null) {
                        gen.configureFromDefinition(def, "wooden");
                    }
                    // If no definition for "bedrock", keeps tile entity default values
                }
            }
        }
        
        world.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
        super.tick(blockstate, world, pos, random);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof ResourceGeneratorTileEntity gen) {
            gen.tick();
        }
        world.scheduleTick(pos, this, 1);
    }

    @Override
    protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Check if player is using an upgrade card from the native mod
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        
        if (itemId.startsWith("skyblock_resources:") && itemId.endsWith("_upgrade_card")) {
            InteractionResult result = handleUpgradeCard(stack, level, pos, player, itemId);
            return result == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : 
                   result == InteractionResult.FAIL ? ItemInteractionResult.FAIL : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
        // No GUI: perform extraction behavior server-side.
        if (world.isClientSide) {
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ResourceGeneratorTileEntity gen) {
            boolean fullStack = entity.isShiftKeyDown();
            gen.onPlayerInteract(entity, fullStack);
        }
        return InteractionResult.SUCCESS;
    }
    
    private InteractionResult handleUpgradeCard(net.minecraft.world.item.ItemStack stack, Level level, BlockPos pos, Player player, String cardId) {
        if (player == null) {
            return InteractionResult.PASS;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ResourceGeneratorTileEntity gen)) {
            return InteractionResult.PASS;
        }
        
        // Map upgrade cards to tier transitions
        String currentTier = gen.getTier();
        String newTier = null;
        String requiredTier = null;
        
        switch (cardId) {
            case "skyblock_resources:stone_upgrade_card":
                requiredTier = "wooden";
                newTier = "stone";
                break;
            case "skyblock_resources:iron_upgrade_card":
                requiredTier = "stone";
                newTier = "iron";
                break;
            case "skyblock_resources:gold_upgrade_card":
                requiredTier = "iron";
                newTier = "gold";
                break;
            case "skyblock_resources:diamond_upgrade_card":
                requiredTier = "gold";
                newTier = "diamond";
                break;
            case "skyblock_resources:netherite_upgrade_card":
                requiredTier = "diamond";
                newTier = "netherite";
                break;
        }
        
        if (requiredTier == null || newTier == null) {
            return InteractionResult.PASS;
        }
        
        // Check if generator is at the correct tier
        if (!requiredTier.equals(currentTier)) {
            // Wrong tier, consume event but don't do anything (silent fail)
            return InteractionResult.CONSUME;
        }
        
        // Apply upgrade
        if (!level.isClientSide) {
            String baseId = gen.getBaseId();
            GeneratorDefinition def = GeneratorLoader.getGenerator(baseId);
            
            if (def == null) {
                return InteractionResult.CONSUME;
            }
            
            // Upgrade the generator
            gen.configureFromDefinition(def, newTier);
            gen.setChanged();
            
            // Force block update to refresh texture immediately (like native mod)
            BlockState currentState = level.getBlockState(pos);
            level.sendBlockUpdated(pos, currentState, currentState, 3);
            
            // Consume the upgrade card (like native mod)
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            
            // Play upgrade sound (same as native mod)
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), 
                          net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 6.0F);
            
            // Animation like native mod
                player.swing(InteractionHand.MAIN_HAND, true);
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
        // This generator has no GUI — always return null
        return null;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
        super.triggerEvent(state, world, pos, eventID, eventParam);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(eventID, eventParam);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ResourceGeneratorTileEntity) {
                ResourceGeneratorTileEntity be = (ResourceGeneratorTileEntity) blockEntity;
                Containers.dropContents(world, pos, be);
                world.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, world, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
        BlockEntity tileentity = world.getBlockEntity(pos);
        if (tileentity instanceof ResourceGeneratorTileEntity) {
            ResourceGeneratorTileEntity be = (ResourceGeneratorTileEntity) tileentity;
            return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(be);
        }
        return 0;
    }
}

