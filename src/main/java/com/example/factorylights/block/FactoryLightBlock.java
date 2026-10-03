package com.example.factorylights.block;

import com.example.factorylights.ModRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FactoryLightBlock extends Block implements EntityBlock {
    public static final MapCodec<FactoryLightBlock> CODEC = simpleCodec(FactoryLightBlock::new);

    // 0 single, 1/2/3 north edge/center/south edge, 4/5/6 west edge/center/east edge.
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 6);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape SHAPE_SINGLE = box(2, 8, 2, 14, 16, 14);
    private static final VoxelShape SHAPE_NORTH_EDGE = box(2, 8, 2, 14, 16, 16);
    private static final VoxelShape SHAPE_SOUTH_EDGE = box(2, 8, 0, 14, 16, 14);
    private static final VoxelShape SHAPE_WEST_EDGE = box(2, 8, 2, 16, 16, 14);
    private static final VoxelShape SHAPE_EAST_EDGE = box(0, 8, 2, 14, 16, 14);
    private static final VoxelShape SHAPE_CENTER_NS = box(2, 8, 0, 14, 16, 16);
    private static final VoxelShape SHAPE_CENTER_EW = box(0, 8, 2, 16, 16, 14);

    public FactoryLightBlock(Properties properties) {
        super(properties.noOcclusion().lightLevel(state -> state.getValue(LIT) ? 15 : 0));
        registerDefaultState(stateDefinition.any()
                .setValue(PART, 0)
                .setValue(AXIS, Direction.Axis.Z)
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, AXIS, LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case 0 -> SHAPE_SINGLE;
            case 1 -> SHAPE_NORTH_EDGE;
            case 2 -> SHAPE_CENTER_NS;
            case 3 -> SHAPE_SOUTH_EDGE;
            case 4 -> SHAPE_WEST_EDGE;
            case 5 -> SHAPE_CENTER_EW;
            case 6 -> SHAPE_EAST_EDGE;
            default -> throw new IllegalStateException();
        };
    }

    public static boolean isCenter(int part) {
        return part == 2 || part == 5;
    }

    public static boolean isNegativeEdge(int part) {
        return part == 1 || part == 4;
    }

    public static boolean isPositiveEdge(int part) {
        return part == 3 || part == 6;
    }

    /** The first block of a row, i.e. the one that owns the row's energy buffer. */
    public static boolean isLeader(int part) {
        return part == 0 || isNegativeEdge(part);
    }

    private static int center(Direction.Axis axis) {
        return axis == Direction.Axis.Z ? 2 : 5;
    }

    private static int negativeEdge(Direction.Axis axis) {
        return axis == Direction.Axis.Z ? 1 : 4;
    }

    private static int positiveEdge(Direction.Axis axis) {
        return axis == Direction.Axis.Z ? 3 : 6;
    }

    private static boolean canExtend(Direction dir, BlockState clicked) {
        int part = clicked.getValue(PART);
        if (part == 0) {
            return true;
        }
        if (clicked.getValue(AXIS) != dir.getAxis() || isCenter(part)) {
            return false;
        }
        // An edge can only grow away from the rest of its row.
        boolean positive = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        return isNegativeEdge(part) != positive;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        if (face.getAxis().isHorizontal()) {
            BlockState clicked = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(face.getOpposite()));
            if (clicked.is(this) && canExtend(face, clicked)) {
                boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                var axis = face.getAxis();
                return defaultBlockState()
                        .setValue(AXIS, axis)
                        .setValue(PART, positive ? positiveEdge(axis) : negativeEdge(axis));
            }
        }
        return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getAxis());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (!dir.getAxis().isHorizontal()) {
            return state;
        }
        var axis = dir.getAxis();
        int part = state.getValue(PART);
        if (part != 0 && state.getValue(AXIS) != axis) {
            return state;
        }

        boolean positive = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        boolean linked = false;
        if (neighbor.is(this) && neighbor.getValue(AXIS) == axis) {
            int nPart = neighbor.getValue(PART);
            // The neighbor links back to us if its row extends toward us.
            linked = isCenter(nPart) || (positive ? isPositiveEdge(nPart) : isNegativeEdge(nPart));
        }

        if (linked) {
            if (part == 0) {
                return state.setValue(AXIS, axis).setValue(PART, positive ? negativeEdge(axis) : positiveEdge(axis));
            }
            if (positive ? isPositiveEdge(part) : isNegativeEdge(part)) {
                return state.setValue(PART, center(axis));
            }
        } else if (isCenter(part)) {
            return state.setValue(PART, positive ? positiveEdge(axis) : negativeEdge(axis));
        } else if (positive ? isNegativeEdge(part) : isPositiveEdge(part)) {
            return state.setValue(PART, 0);
        }
        return state;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            FactoryLightBlockEntity.removeBeam(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FactoryLightBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                           BlockEntityType<T> type) {
        if (level.isClientSide || type != ModRegistry.FACTORY_LIGHT_BE.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<FactoryLightBlockEntity>) FactoryLightBlockEntity::serverTick;
    }
}
