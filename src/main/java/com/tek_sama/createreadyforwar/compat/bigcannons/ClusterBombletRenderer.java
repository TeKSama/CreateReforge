package com.tek_sama.createreadyforwar.compat.bigcannons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A bomblet is drawn as a miniature cluster shell (same model and textures, 40% of the size),
 * nose first along its trajectory.
 */
public class ClusterBombletRenderer extends EntityRenderer<ClusterBomblet> {

    private static final float SCALE = 0.4f;

    public ClusterBombletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ClusterBomblet bomblet, float entityYaw, float partialTicks, PoseStack poseStack,
        MultiBufferSource buffer, int packedLight) {
        // The shell model points its nose up (+Y); turn it to face the direction of flight.
        Vec3 motion = bomblet.getDeltaMovement();
        float yaw = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(motion.horizontalDistance(), motion.y) * Mth.RAD_TO_DEG);

        poseStack.pushPose();
        poseStack.translate(0, bomblet.getBbHeight() / 2, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(-0.5, -0.5, -0.5);
        BlockState shell = ModShells.block(ShellKind.CLUSTER).defaultBlockState();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(shell, poseStack, buffer, packedLight,
            OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        super.render(bomblet, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ClusterBomblet bomblet) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
