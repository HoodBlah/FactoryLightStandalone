package com.example.factorylights.block;

import com.example.factorylights.FactoryLightsConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible, non-solid block that carries the light projected by a factory light. */
public class FactoryLightLightBlock extends Block {
    public static final MapCodec<FactoryLightLightBlock> CODEC = simpleCodec(FactoryLightLightBlock::new);
    private static final int CHECK_INTERVAL = 10;

    public FactoryLightLightBlock(Properties properties) {
        super(properties.lightLevel(state -> 15));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, CHECK_INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (hasSource(level, pos)) {
            level.scheduleTick(pos, this, CHECK_INTERVAL);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean hasSource(ServerLevel level, BlockPos pos) {
        var cursor = pos.mutable();
        for (int i = 1; i <= FactoryLightsConfig.PROJECTION_RANGE.get(); i++) {
            cursor.move(Direction.UP);
            if (!level.hasChunkAt(cursor)) {
                return true;
            }
            BlockState above = level.getBlockState(cursor);
            if (above.getBlock() instanceof FactoryLightLightBlock) {
                continue;
            }
            return above.getBlock() instanceof FactoryLightBlock && above.getValue(FactoryLightBlock.LIT);
        }
        return false;
    }
}
