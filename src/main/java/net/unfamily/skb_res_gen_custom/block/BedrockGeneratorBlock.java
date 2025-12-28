package net.unfamily.skb_res_gen_custom.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.skb_res_gen_custom.block.entity.BedrockGeneratorTileEntity;
import net.unfamily.skb_res_gen_custom.init.ModBlockEntities;
import net.unfamily.skb_res_gen_custom.generator.GeneratorLoader;
import net.unfamily.skb_res_gen_custom.generator.GeneratorDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class BedrockGeneratorBlock extends BaseEntityBlock {
    public static final IntegerProperty ANIMATION = IntegerProperty.create("animation", 0, 1);
    public static final MapCodec<BedrockGeneratorBlock> CODEC = simpleCodec(properties -> new BedrockGeneratorBlock());
    
    public MapCodec<BedrockGeneratorBlock> codec() {
        return CODEC;
    }
    
    public BedrockGeneratorBlock() {
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
        return Collections.singletonList(new net.minecraft.world.item.ItemStack(this, 1));
    }

    @Override
    public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(blockstate, world, pos, oldState, moving);
        
        // Configure tile entity only if not already configured by updateCustomBlockEntityTag
        if (!world.isClientSide) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BedrockGeneratorTileEntity bedrockGen) {
                // Check if already configured (base_id != "bedrock" by default)
                if (bedrockGen.getBaseId().equals("bedrock") && bedrockGen.getOutputItem().equals("minecraft:bedrock")) {
                    // Only if not configured, try to configure from loaded definitions
                    GeneratorDefinition def = GeneratorLoader.getGenerator("bedrock");
                    if (def != null) {
                        bedrockGen.configureFromDefinition(def, "wooden");
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
        if (blockEntity instanceof BedrockGeneratorTileEntity bedrockGen) {
            bedrockGen.tick();
        }
        world.scheduleTick(pos, this, 1);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
        // No GUI: perform extraction behavior server-side.
        if (world.isClientSide) {
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof BedrockGeneratorTileEntity bedrockGen) {
            boolean fullStack = entity.isShiftKeyDown();
            bedrockGen.onPlayerInteract(entity, fullStack);
        }
        return InteractionResult.SUCCESS;
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
            if (blockEntity instanceof BedrockGeneratorTileEntity) {
                BedrockGeneratorTileEntity be = (BedrockGeneratorTileEntity) blockEntity;
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
        if (tileentity instanceof BedrockGeneratorTileEntity) {
            BedrockGeneratorTileEntity be = (BedrockGeneratorTileEntity) tileentity;
            return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(be);
        }
        return 0;
    }
}

