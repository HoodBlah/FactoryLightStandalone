package com.example.factorylights.client;

import com.example.factorylights.block.FactoryLightBlock;
import com.example.factorylights.block.FactoryLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class FactoryLightRenderer implements BlockEntityRenderer<FactoryLightBlockEntity> {
    public FactoryLightRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(FactoryLightBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!state.getValue(FactoryLightBlock.LIT)) {
            return;
        }

        int part = state.getValue(FactoryLightBlock.PART);
        ModelResourceLocation rays = switch (part) {
            case 0 -> ClientSetup.RAYS_SINGLE;
            case 1, 4 -> ClientSetup.RAYS_FRONT;
            case 2, 5 -> ClientSetup.RAYS_CENTER;
            default -> ClientSetup.RAYS_BACK;
        };
        float rotation = part >= 4 ? 90f : 0f;

        var model = Minecraft.getInstance().getModelManager().getModel(rays);
        VertexConsumer consumer = buffers.getBuffer(ModRenderTypes.additive());
        var random = RandomSource.create(42L);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5, -0.5, -0.5);
        var pose = poseStack.last();
        for (BakedQuad quad : model.getQuads(null, null, random)) {
            consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, packedOverlay);
        }
        for (Direction side : Direction.values()) {
            for (BakedQuad quad : model.getQuads(null, side, random)) {
                consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, packedOverlay);
            }
        }
        poseStack.popPose();
    }
}
