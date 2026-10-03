package com.example.factorylights.block;

import com.example.factorylights.FactoryLightsConfig;
import com.example.factorylights.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import static com.example.factorylights.block.FactoryLightBlock.AXIS;
import static com.example.factorylights.block.FactoryLightBlock.LIT;
import static com.example.factorylights.block.FactoryLightBlock.PART;

public class FactoryLightBlockEntity extends BlockEntity {
    private static final int MAX_ROW = 64;
    private static final int ROW_SYNC_INTERVAL = 10;
    private static final int BEAM_RECHECK_INTERVAL = 20;
    // Beam/block updates only need to reach clients; skipping neighbor updates keeps column edits cheap.
    private static final int BEAM_FLAGS = Block.UPDATE_CLIENTS;

    private final IEnergyStorage energyProxy = new EnergyProxy();
    // Only meaningful on the row leader; other pieces forward whatever they hold.
    private int energy;
    private int rowLength = 1;
    private boolean beamLit;
    private int tickCount;

    public FactoryLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.FACTORY_LIGHT_BE.get(), pos, state);
    }

    public IEnergyStorage getEnergyProxy() {
        return energyProxy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FactoryLightBlockEntity be) {
        be.tick(level);
    }

    private void tick(Level level) {
        BlockState state = level.getBlockState(worldPosition);
        boolean sync = tickCount++ % ROW_SYNC_INTERVAL == 0;

        if (FactoryLightBlock.isLeader(state.getValue(PART))) {
            tickLeader(level, state, sync);
        } else if (energy > 0 && sync) {
            var leader = findLeader();
            if (leader != null && leader != this) {
                energy -= leader.receive(energy, false);
                setChanged();
            }
        }

        boolean lit = level.getBlockState(worldPosition).getValue(LIT);
        if (lit != beamLit || tickCount % BEAM_RECHECK_INTERVAL == 0) {
            beamLit = lit;
            projectBeam(level, lit);
        }
    }

    private void tickLeader(Level level, BlockState state, boolean sync) {
        boolean wasLit = state.getValue(LIT);
        if (sync) {
            rowLength = walkRow(level, wasLit);
        }

        int cost = FactoryLightsConfig.FE_PER_TICK.get() * rowLength;
        int capacity = capacity();
        if (energy > capacity) {
            energy = capacity;
        }

        // Starting needs about a second of power so a weak supply doesn't strobe the light.
        boolean lit = wasLit ? energy >= cost : energy >= Math.min(capacity, cost * 20);
        if (lit && cost > 0) {
            energy -= cost;
            setChanged();
        }
        if (lit != wasLit) {
            walkRow(level, lit);
        }
    }

    /** Applies the lit state to every piece of this row (starting at this leader) and returns the row length. */
    private int walkRow(Level level, boolean lit) {
        BlockState state = level.getBlockState(worldPosition);
        var axis = state.getValue(AXIS);
        var step = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        BlockPos pos = worldPosition;
        int count = 0;
        while (true) {
            count++;
            if (state.getValue(LIT) != lit) {
                level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
            }
            int part = state.getValue(PART);
            if (part == 0 || FactoryLightBlock.isPositiveEdge(part) || count >= MAX_ROW) {
                return count;
            }
            pos = pos.relative(step);
            if (!level.hasChunkAt(pos)) {
                return count;
            }
            state = level.getBlockState(pos);
            if (!state.is(getBlockState().getBlock()) || state.getValue(AXIS) != axis
                    || FactoryLightBlock.isLeader(state.getValue(PART))) {
                return count;
            }
        }
    }

    @Nullable
    private FactoryLightBlockEntity findLeader() {
        Level level = this.level;
        if (level == null) {
            return null;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (!state.is(getBlockState().getBlock())) {
            return null;
        }
        if (FactoryLightBlock.isLeader(state.getValue(PART))) {
            return this;
        }
        var axis = state.getValue(AXIS);
        var step = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
        BlockPos pos = worldPosition;
        for (int i = 0; i < MAX_ROW; i++) {
            pos = pos.relative(step);
            if (!level.hasChunkAt(pos)) {
                return null;
            }
            BlockState other = level.getBlockState(pos);
            if (!other.is(state.getBlock()) || other.getValue(AXIS) != axis) {
                return null;
            }
            int part = other.getValue(PART);
            if (FactoryLightBlock.isNegativeEdge(part)) {
                return level.getBlockEntity(pos) instanceof FactoryLightBlockEntity be ? be : null;
            }
            if (part == 0) {
                return null;
            }
        }
        return null;
    }

    private int capacity() {
        int perBlock = Math.max(FactoryLightsConfig.BUFFER_PER_BLOCK.get(), FactoryLightsConfig.FE_PER_TICK.get());
        return perBlock * rowLength;
    }

    private int receive(int amount, boolean simulate) {
        int accepted = Math.max(0, Math.min(amount, capacity() - energy));
        if (accepted > 0 && !simulate) {
            energy += accepted;
            setChanged();
        }
        return accepted;
    }

    private void projectBeam(Level level, boolean lit) {
        int range = FactoryLightsConfig.PROJECTION_RANGE.get();
        BlockState beam = ModRegistry.FACTORY_LIGHT_LIGHT.get().defaultBlockState();
        var cursor = worldPosition.mutable();
        for (int i = 1; i <= range; i++) {
            cursor.move(Direction.DOWN);
            if (!level.hasChunkAt(cursor)) {
                return;
            }
            BlockState below = level.getBlockState(cursor);
            boolean isBeam = below.getBlock() instanceof FactoryLightLightBlock;
            if (lit) {
                if (isBeam) {
                    continue;
                }
                if (!below.isAir()) {
                    return;
                }
                level.setBlock(cursor.immutable(), beam, BEAM_FLAGS);
            } else {
                if (!isBeam) {
                    return;
                }
                level.setBlock(cursor.immutable(), Blocks.AIR.defaultBlockState(), BEAM_FLAGS);
            }
        }
    }

    /** Clears the contiguous beam blocks directly below a light. */
    public static void removeBeam(Level level, BlockPos lightPos) {
        var cursor = lightPos.mutable();
        for (int i = 1; i <= FactoryLightsConfig.PROJECTION_RANGE.get(); i++) {
            cursor.move(Direction.DOWN);
            if (!level.hasChunkAt(cursor) || !(level.getBlockState(cursor).getBlock() instanceof FactoryLightLightBlock)) {
                return;
            }
            level.setBlock(cursor.immutable(), Blocks.AIR.defaultBlockState(), BEAM_FLAGS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getInt("Energy");
    }

    private final class EnergyProxy implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            var leader = findLeader();
            return leader == null ? 0 : leader.receive(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            var leader = findLeader();
            return leader == null ? 0 : leader.energy;
        }

        @Override
        public int getMaxEnergyStored() {
            var leader = findLeader();
            return leader == null ? 0 : leader.capacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
