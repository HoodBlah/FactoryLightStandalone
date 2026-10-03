package com.example.factorylights.test;

import com.example.factorylights.FactoryLights;
import com.example.factorylights.ModRegistry;
import com.example.factorylights.block.FactoryLightBlock;
import com.example.factorylights.block.FactoryLightLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FactoryLights.MODID)
@PrefixGameTestTemplate(false)
public class FactoryLightTests {
    private static BlockState light(int part, Direction.Axis axis) {
        return ModRegistry.FACTORY_LIGHT.get().defaultBlockState()
                .setValue(FactoryLightBlock.PART, part)
                .setValue(FactoryLightBlock.AXIS, axis);
    }

    private static void placeRow(GameTestHelper helper, BlockPos start) {
        helper.setBlock(start, light(1, Direction.Axis.Z));
        helper.setBlock(start.south(), light(2, Direction.Axis.Z));
        helper.setBlock(start.south(2), light(3, Direction.Axis.Z));
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void rowSharesEnergyAndProjectsBeam(GameTestHelper helper) {
        var start = new BlockPos(3, 18, 2);
        placeRow(helper, start);
        var middle = start.south();

        helper.runAfterDelay(2, () -> {
            var storage = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(middle), null);
            if (storage == null) {
                helper.fail("No energy capability on a middle segment");
                return;
            }
            int accepted = storage.receiveEnergy(100_000, false);
            if (accepted != 1500) {
                helper.fail("Expected the 3-block row to accept 1500 FE, got " + accepted);
            }
        });

        helper.succeedWhen(() -> {
            for (int i = 0; i < 3; i++) {
                helper.assertBlockProperty(start.south(i), FactoryLightBlock.LIT, true);
            }
            helper.assertBlock(start.south().below(16), b -> b instanceof FactoryLightLightBlock, () -> "Beam missing at max range");
            helper.assertBlock(start.below(), b -> b instanceof FactoryLightLightBlock, () -> "Beam missing below the leader");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void removingLightClearsBeam(GameTestHelper helper) {
        var start = new BlockPos(3, 18, 2);
        helper.setBlock(start, light(0, Direction.Axis.Z));
        helper.runAfterDelay(2, () -> helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(start), null)
                .receiveEnergy(100_000, false));

        helper.runAfterDelay(40, () -> {
            helper.assertBlock(start.below(3), b -> b instanceof FactoryLightLightBlock, () -> "Beam was not projected");
            helper.setBlock(start, Blocks.AIR);
            helper.assertBlock(start.below(3), b -> b == Blocks.AIR, () -> "Beam was not cleared");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void splittingRowRestoresSingles(GameTestHelper helper) {
        var start = new BlockPos(3, 5, 2);
        placeRow(helper, start);
        helper.setBlock(start.south(), Blocks.AIR);
        helper.runAfterDelay(1, () -> {
            helper.assertBlockProperty(start, FactoryLightBlock.PART, 0);
            helper.assertBlockProperty(start.south(2), FactoryLightBlock.PART, 0);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void neighboringEdgeMergesSingle(GameTestHelper helper) {
        var first = new BlockPos(3, 5, 2);
        helper.setBlock(first, light(0, Direction.Axis.X));
        helper.setBlock(first.south(), light(3, Direction.Axis.Z));
        helper.runAfterDelay(1, () -> {
            helper.assertBlockProperty(first, FactoryLightBlock.PART, 1);
            helper.assertBlockProperty(first, FactoryLightBlock.AXIS, Direction.Axis.Z);
            helper.succeed();
        });
    }
}
