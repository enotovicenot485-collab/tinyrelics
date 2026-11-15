package dev.tinyrelics;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The bone crusher and the soul press, the necromancer's two machines. Both burn gunpowder.
 * The same class serves both blocks: which one it is, the block entity asks the block itself.
 */
public class BoneMachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Working: the fire burns and the rollers turn. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** The press only: the plate is down. */
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");
   public static final MapCodec<BoneMachineBlock> CODEC = simpleCodec(BoneMachineBlock::new);

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
       return CODEC;
   }

    public BoneMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(LIT, false).setValue(PRESSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, PRESSED);
    }

    /** The front with the furnace door looks at whoever places the block. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Blocks with a block entity are invisible unless they say otherwise. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoneMachineBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BoneMachineBlockEntity machine) {
            player.openMenu(machine);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Broken: whatever was inside falls out. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BoneMachineBlockEntity machine) {
            Containers.dropContents(level, pos, machine);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), machine.onTheBelt());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TinyRelics.BONE_MACHINE_ENTITY.get(), BoneMachineBlockEntity::serverTick);
    }
}
