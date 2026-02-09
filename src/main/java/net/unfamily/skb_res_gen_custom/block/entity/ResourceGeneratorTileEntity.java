package net.unfamily.skb_res_gen_custom.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import net.unfamily.skb_res_gen_custom.init.ModBlockEntities;
import net.unfamily.skb_res_gen_custom.integration.NativeModCompat;
import net.unfamily.skb_res_gen_custom.block.ResourceGeneratorBlock;
import net.unfamily.skb_res_gen_custom.generator.GeneratorDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

public class ResourceGeneratorTileEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, GeoBlockEntity {
    private NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);

    private static final int DEFAULT_TICKS_PER_GENERATION = 100; // 5 seconds (default)
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable) this);
    private String prevAnim = "0";
    
    // Dynamic generator data
    private String tier = "wooden";
    private String baseId = "bedrock";
    private String outputItem = "minecraft:bedrock";
    private int ticksPerGeneration = DEFAULT_TICKS_PER_GENERATION;
    private int stackSize = 1;

    public ResourceGeneratorTileEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType<?>) ModBlockEntities.RESOURCE_GENERATOR.get(), pos, state);
        if (getPersistentData().getDouble("ticks_remaining") == 0.0D) {
            getPersistentData().putDouble("ticks_remaining", getTicksPerGeneration());
        }
    }
    
    /**
     * Configures this generator from a definition and tier
     */
    public void configureFromDefinition(GeneratorDefinition definition, String tier) {
        if (definition == null || tier == null) return;
        
        this.tier = tier;
        this.baseId = definition.getBaseId();
        this.outputItem = definition.getOutput();
        
        // Determine tier index
        int tierIndex = getTierIndex(tier);
        if (tierIndex >= 0 && tierIndex < 6) {
            this.ticksPerGeneration = definition.getTimes()[tierIndex];
            this.stackSize = definition.getStacks()[tierIndex];
        }
        
        // Reset timer with new value
        getPersistentData().putDouble("ticks_remaining", this.ticksPerGeneration);
        setChanged();
    }
    
    /**
     * Manually configures the generator (for compatibility or testing)
     */
    public void configure(String tier, String baseId, String outputItem, int ticksPerGeneration, int stackSize) {
        this.tier = tier;
        this.baseId = baseId;
        this.outputItem = outputItem;
        this.ticksPerGeneration = ticksPerGeneration;
        this.stackSize = stackSize;
        
        getPersistentData().putDouble("ticks_remaining", this.ticksPerGeneration);
        setChanged();
    }
    
    /**
     * Returns the tier index (0-5)
     */
    private int getTierIndex(String tier) {
        return switch (tier.toLowerCase()) {
            case "wooden" -> 0;
            case "stone" -> 1;
            case "iron" -> 2;
            case "gold" -> 3;
            case "diamond" -> 4;
            case "netherite" -> 5;
            default -> 0;
        };
    }
    
    // Public getters
    public String getTier() { return tier; }
    public String getBaseId() { return baseId; }
    public String getOutputItem() { return outputItem; }
    public int getTicksPerGeneration() { return ticksPerGeneration; }
    public int getGenerationStackSize() { return stackSize; }

    public void tick() {
        if (level == null || level.isClientSide) return;

        double ticksRemaining = getPersistentData().getDouble("ticks_remaining");
        if (ticksRemaining > 0) {
            getPersistentData().putDouble("ticks_remaining", ticksRemaining - 1);
            setChanged();
        } else {
            generateItem();
            getPersistentData().putDouble("ticks_remaining", getTicksPerGeneration());
            setChanged();
        }
        // Always try to push when we have items (handles: generator placed first, storage below added later)
        if (!this.stacks.get(0).isEmpty()) {
            try { tryInsertIntoInventoryBelow(); } catch (Exception ignored) {}
        }
    }

    private void generateItem() {
        if (level == null || level.isClientSide) return;
        
        // Parse output item from ResourceLocation
        ResourceLocation itemLocation = ResourceLocation.tryParse(outputItem);
        if (itemLocation == null) {
            return; // Invalid item ID
        }
        
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(itemLocation);
        if (item == null || item == Items.AIR) {
            return; // Item not found
        }
        
        // Write to internal inventory slot 0
        ItemStack current = this.stacks.get(0);
        if (current.isEmpty()) {
            this.stacks.set(0, new ItemStack(item, stackSize));
        } else if (current.getItem() == item && current.getCount() + stackSize <= getMaxStackSize()) {
            current.grow(stackSize);
            this.stacks.set(0, current);
        } else {
            return; // Inventory full or different item
        }

        // Trigger animation by setting blockstate animation to 1
        try {
            if (this.level != null) {
                BlockState bs = this.level.getBlockState(this.getBlockPos());
                if (bs.getBlock() instanceof ResourceGeneratorBlock) {
                    this.level.setBlock(this.getBlockPos(), bs.setValue(ResourceGeneratorBlock.ANIMATION, 1), 3);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Called when a player interacts (right click) the generator.
     * If fullStack is true (shift-click) extract the whole stack, otherwise extract one item.
     */
    public void onPlayerInteract(net.minecraft.world.entity.player.Player player, boolean fullStack) {
        if (level == null || level.isClientSide) return;
        ItemStack extracted = extractFromInternal(fullStack ? Integer.MAX_VALUE : 1);
        if (extracted.isEmpty()) return;
        boolean added = player.addItem(extracted);
        if (!added) {
            // drop near player if couldn't add to inventory
            Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), extracted);
        }
        setChanged();
    }

    /**
     * Extract up to 'count' items from internal slot 0 and return them.
     * If count is Integer.MAX_VALUE, extract the full stack.
     */
    private ItemStack extractFromInternal(int count) {
        ItemStack current = this.stacks.get(0);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack result;
        if (count >= current.getCount()) {
            result = current.copy();
            this.stacks.set(0, ItemStack.EMPTY);
        } else {
            result = current.split(count);
            // split already modified current; update storage
            this.stacks.set(0, current.isEmpty() ? ItemStack.EMPTY : current);
        }
        return result;
    }

    /**
     * Try to insert items from internal storage into the block directly below.
     * Supports both vanilla Container and modded IItemHandler capability.
     */
    private void tryInsertIntoInventoryBelow() {
        if (this.level == null || this.level.isClientSide) return;
        BlockPos belowPos = this.getBlockPos().below();
        net.minecraft.world.level.block.entity.BlockEntity be = this.level.getBlockEntity(belowPos);
        if (be == null) return;

        ItemStack toMove = this.stacks.get(0);
        if (toMove.isEmpty()) return;

        // Try vanilla Container first
        if (be instanceof Container container) {
            tryInsertIntoContainer(container, toMove);
            return;
        }

        // Try IItemHandler capability (modded inventories)
        IItemHandler handler = this.level.getCapability(Capabilities.ItemHandler.BLOCK, belowPos, Direction.UP);
        if (handler != null) {
            tryInsertIntoItemHandler(handler, toMove);
            be.setChanged();
            return;
        }
    }

    /**
     * Insert into vanilla Container: merge into existing stacks, then fill empty slots.
     */
    private void tryInsertIntoContainer(Container container, ItemStack toMove) {
        // First try to merge into existing compatible stacks
        for (int i = 0; i < container.getContainerSize() && !toMove.isEmpty(); i++) {
            ItemStack target = container.getItem(i);
            if (!target.isEmpty() && target.getItem() == toMove.getItem() && target.getCount() < target.getMaxStackSize()) {
                int space = target.getMaxStackSize() - target.getCount();
                int move = Math.min(space, toMove.getCount());
                ItemStack copy = target.copy();
                copy.grow(move);
                container.setItem(i, copy);
                toMove.shrink(move);
            }
        }

        // Then put into empty slots
        for (int i = 0; i < container.getContainerSize() && !toMove.isEmpty(); i++) {
            ItemStack target = container.getItem(i);
            if (target.isEmpty()) {
                int move = Math.min(toMove.getCount(), toMove.getMaxStackSize());
                ItemStack set = toMove.copy();
                set.setCount(move);
                container.setItem(i, set);
                toMove.shrink(move);
            }
        }

        this.stacks.set(0, toMove.isEmpty() ? ItemStack.EMPTY : toMove);
        setChanged();
        if (container instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
    }

    /**
     * Insert into IItemHandler (modded chests, barrels, storage drawers, etc.).
     */
    private void tryInsertIntoItemHandler(IItemHandler handler, ItemStack toMove) {
        for (int i = 0; i < handler.getSlots() && !toMove.isEmpty(); i++) {
            toMove = handler.insertItem(i, toMove, false);
        }
        this.stacks.set(0, toMove.isEmpty() ? ItemStack.EMPTY : toMove);
        setChanged();
    }

    @Override
    public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(compound, lookupProvider);
        if (!tryLoadLootTable(compound))
            this.stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
        
        // Load dynamic generator data
        if (compound.contains("GeneratorTier")) {
            this.tier = compound.getString("GeneratorTier");
        }
        if (compound.contains("GeneratorBaseId")) {
            this.baseId = compound.getString("GeneratorBaseId");
        }
        if (compound.contains("GeneratorOutput")) {
            this.outputItem = compound.getString("GeneratorOutput");
        }
        if (compound.contains("GeneratorTicks")) {
            this.ticksPerGeneration = compound.getInt("GeneratorTicks");
        }
        if (compound.contains("GeneratorStackSize")) {
            this.stackSize = compound.getInt("GeneratorStackSize");
        }
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
        super.saveAdditional(compound, lookupProvider);
        if (!trySaveLootTable(compound)) {
            ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
        }
        
        // Save dynamic generator data
        compound.putString("GeneratorTier", this.tier);
        compound.putString("GeneratorBaseId", this.baseId);
        compound.putString("GeneratorOutput", this.outputItem);
        compound.putInt("GeneratorTicks", this.ticksPerGeneration);
        compound.putInt("GeneratorStackSize", this.stackSize);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
        return saveWithFullMetadata(lookupProvider);
    }

    @Override
    public int getContainerSize() {
        return this.stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.stacks) {
            if (!itemstack.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public Component getDefaultName() {
        return Component.literal(tier + "_" + baseId + "_generator");
    }

    @Override
    public int getMaxStackSize() {
        return NativeModCompat.getMaxItemsForTier(tier);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory);
    }

    @Override
    public Component getDisplayName() {
        // Format display name nicely: "Wooden Obsidian Generator"
        String tierCapitalized = tier.substring(0, 1).toUpperCase() + tier.substring(1);
        String baseIdFormatted = baseId.replace("-", " ").replace("_", " ");
        String[] words = baseIdFormatted.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(word.substring(0, 1).toUpperCase())
                         .append(word.substring(1))
                         .append(" ");
            }
        }
        return Component.literal(tierCapitalized + " " + formatted.toString().trim() + " Generator");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.stacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return index != 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    /**
     * Returns IItemHandler for capability queries (hoppers, pipes, etc.).
     * Side-aware: uses getSlotsForFace / canTakeItemThroughFace / canPlaceItemThroughFace.
     */
    public IItemHandler getItemHandler(@Nullable Direction side) {
        return new SidedInvWrapper(this, side);
    }

    /* GeckoLib: minimal animatable implementation so renderer accepts this tile entity */
    private PlayState predicate(AnimationState<ResourceGeneratorTileEntity> event) {
        String animationprocedure = String.valueOf(getBlockState().getValue(ResourceGeneratorBlock.ANIMATION));
        if (animationprocedure.equals("0")) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("0"));
        }
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "controller", 0, this::predicate));
        data.add(new AnimationController<>(this, "procedurecontroller", 0, this::procedurePredicate));
    }

    private PlayState procedurePredicate(AnimationState<ResourceGeneratorTileEntity> event) {
        String animationprocedure = String.valueOf(getBlockState().getValue(ResourceGeneratorBlock.ANIMATION));
        if ((!animationprocedure.equals("0") && event.getController().getAnimationState() == AnimationController.State.STOPPED)
                || (!animationprocedure.equals(this.prevAnim) && !animationprocedure.equals("0"))) {
            if (!animationprocedure.equals(this.prevAnim))
                event.getController().forceAnimationReset();
            event.getController().setAnimation(RawAnimation.begin().thenPlay(animationprocedure));
            if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                // reset blockstate animation to 0
                try {
                    BlockState bs = this.level.getBlockState(this.getBlockPos());
                    if (bs.getBlock() instanceof ResourceGeneratorBlock) {
                        this.level.setBlock(this.getBlockPos(), bs.setValue(ResourceGeneratorBlock.ANIMATION, 0), 3);
                    }
                } catch (Exception ignored) {}
                event.getController().forceAnimationReset();
            }
        } else if (animationprocedure.equals("0")) {
            this.prevAnim = "0";
            return PlayState.STOP;
        }
        this.prevAnim = animationprocedure;
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

