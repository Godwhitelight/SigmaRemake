package io.github.sst.remake.module.impl.gui;

import io.github.sst.remake.Client;
import io.github.sst.remake.data.bus.Subscribe;
import io.github.sst.remake.event.impl.client.RenderClient2DEvent;
import io.github.sst.remake.event.impl.game.player.ClientPlayerTickEvent;
import io.github.sst.remake.event.impl.game.render.RenderLevelEvent;
import io.github.sst.remake.module.Category;
import io.github.sst.remake.module.Module;
import io.github.sst.remake.setting.impl.BooleanSetting;
import io.github.sst.remake.setting.impl.SliderSetting;
import io.github.sst.remake.util.game.combat.RotationUtils;
import io.github.sst.remake.util.math.anim.AnimationUtils;
import io.github.sst.remake.util.math.vec.VecUtils;
import io.github.sst.remake.util.render.RenderCompat;
import io.github.sst.remake.util.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class RearViewModule extends Module {
    public static boolean RENDERING_REAR_VIEW = false;

    private final AnimationUtils visibilityAnimation = new AnimationUtils(230, 200, AnimationUtils.Direction.BACKWARDS);
    private Framebuffer rearViewFramebuffer;
    private int smartVisibilityTicks = 0;

    private final BooleanSetting showInGui = new BooleanSetting("Show in GUI", "Makes the Rear View visible in guis", false);
    private final BooleanSetting smartVisibility = new BooleanSetting("Smart visibility", "Only pops up when a player is behind you", false);
    private final SliderSetting size = new SliderSetting("Size", "Width", 400, 120, 1000, 1);

    public RearViewModule() {
        super("RearView", "Lets you see what's going on behind you", Category.GUI);
    }

    @Override
    public void onEnable() {
        rebuild();
    }

    @Override
    public void onDisable() {
        visibilityAnimation.changeDirection(AnimationUtils.Direction.FORWARDS);
    }

    @Subscribe
    public void onTick(ClientPlayerTickEvent event) {
        if (rearViewFramebuffer != null
                && (rearViewFramebuffer.textureWidth != client.getWindow().getFramebufferWidth()
                || rearViewFramebuffer.textureHeight != client.getWindow().getFramebufferHeight())) {
            rebuild();
        }

        if (!smartVisibility.value) {
            return;
        }

        List<PlayerEntity> nearbyPlayersBehindYou = client.world.getEntitiesByClass(
                PlayerEntity.class,
                client.player.getBoundingBox().expand(14.0),
                player -> player.distanceTo(client.player) < 12.0F
                        && !isEntityWithinViewAngle(player)
                        && client.player != player
        );

        if (nearbyPlayersBehindYou.isEmpty()) {
            if (smartVisibilityTicks > 0) {
                smartVisibilityTicks--;
            }
        } else {
            smartVisibilityTicks = 5;
        }
    }

    @Subscribe
    public void onRender(RenderClient2DEvent event) {
        if (rearViewFramebuffer == null) return;
        if (client.options.hudHidden) return;

        if (!smartVisibility.value) {
            boolean inScreenAndNotAllowed = client.currentScreen != null && !showInGui.value;
            visibilityAnimation.changeDirection(inScreenAndNotAllowed
                    ? AnimationUtils.Direction.FORWARDS
                    : AnimationUtils.Direction.BACKWARDS);
        } else {
            visibilityAnimation.changeDirection(smartVisibilityTicks <= 0
                    ? AnimationUtils.Direction.FORWARDS
                    : AnimationUtils.Direction.BACKWARDS);
        }

        float aspectRatio = (float) client.getWindow().getWidth() / (float) client.getWindow().getHeight();

        int rearViewWidth = size.value.intValue();
        int rearViewHeight = (int) ((float) rearViewWidth / aspectRatio);

        int padding = 10;
        int yOffset = -padding - rearViewHeight;

        float anim = visibilityAnimation.calcPercent();

        if (anim == 0.0F) {
            return;
        }

        if (visibilityAnimation.getDirection() != AnimationUtils.Direction.BACKWARDS) {
            yOffset = (int) ((float) yOffset * VecUtils.interpolate(anim, 0.49, 0.59, 0.16, 1.04));
        } else {
            yOffset = (int) ((float) yOffset * VecUtils.interpolate(anim, 0.3, 0.88, 0.47, 1.0));
        }

        RenderUtils.drawRoundedRect(
                (float) (client.getWindow().getWidth() - padding - rearViewWidth),
                (float) (client.getWindow().getHeight() + yOffset),
                (float) rearViewWidth,
                (float) (rearViewHeight - 1),
                14.0F,
                anim
        );

        int scaledWidth = (int) (rearViewWidth * Client.INSTANCE.screenManager.scaleFactor);
        int scaledHeight = (int) (rearViewHeight * Client.INSTANCE.screenManager.scaleFactor);
        int scaledPadding = (int) (padding * Client.INSTANCE.screenManager.scaleFactor);
        int scaledYOffset = (int) (yOffset * Client.INSTANCE.screenManager.scaleFactor);

        RenderCompat.pushMatrix();
        blitFramebufferToScreen(
                rearViewFramebuffer,
                scaledWidth,
                scaledHeight,
                client.getWindow().getFramebufferWidth() - scaledPadding - scaledWidth,
                client.getWindow().getFramebufferHeight() + scaledYOffset
        );
        RenderCompat.popMatrix();

        // Restore UI projection after custom ortho/viewport.
        RenderCompat.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        RenderCompat.matrixMode(5889);
        RenderCompat.loadIdentity();
        RenderCompat.ortho(
                0.0,
                (double) client.getWindow().getFramebufferWidth() / client.getWindow().getScaleFactor(),
                (double) client.getWindow().getFramebufferHeight() / client.getWindow().getScaleFactor(),
                0.0,
                1000.0,
                3000.0
        );
        RenderCompat.matrixMode(5888);
        RenderCompat.loadIdentity();
        RenderCompat.translatef(0.0F, 0.0F, -2000.0F);
        GL11.glScaled(
                1.0 / client.getWindow().getScaleFactor() * (double) Client.INSTANCE.screenManager.scaleFactor,
                1.0 / client.getWindow().getScaleFactor() * (double) Client.INSTANCE.screenManager.scaleFactor,
                1.0
        );

        RenderCompat.viewport(0, 0, client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
        // In 1.21, Framebuffer.beginWrite(boolean) is removed.
        // The main framebuffer is automatically bound by the rendering pipeline.
        // We use blitToScreen() to ensure our framebuffer is displayed.
        rearViewFramebuffer.blitToScreen(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
    }

    @Subscribe
    public void onRenderLevel(RenderLevelEvent event) {
        if (rearViewFramebuffer == null) {
            rebuild();
            return;
        }
        if (client.world == null || client.player == null) {
            return;
        }

        if (client.currentScreen != null
                && !showInGui.value
                && smartVisibilityTicks == 0) {
            return;
        }

        // In 1.21, Framebuffer.beginWrite()/endWrite() are removed.
        // The rendering pipeline manages framebuffer binding differently now.
        // We clear the framebuffer and set up GL state manually.
        GL11.glBindFramebuffer(GL11.GL_FRAMEBUFFER, 0); // Will be rebound by render call
        RenderCompat.clear(16640, false);
        RenderCompat.enableDepthTest();
        GL11.glAlphaFunc(519, 0.0F);

        float originalYaw = client.player.getYaw();
        int originalFov = client.options.getFov().getValue();
        boolean originalRenderHand = client.gameRenderer.renderHand;
        Framebuffer originalOutlineFbo = client.worldRenderer.entityOutlinesFramebuffer;

        try {
            RENDERING_REAR_VIEW = true;
            client.player.setYaw(client.player.getYaw() + 180.0F);
            client.options.getFov().setValue(114);
            client.gameRenderer.renderHand = false;
            client.worldRenderer.entityOutlinesFramebuffer = null;

            // In 1.21, renderWorld takes RenderTickCounter instead of (float, long, MatrixStack)
            client.gameRenderer.renderWorld(client.getRenderTickCounter());
        } finally {
            RENDERING_REAR_VIEW = false;
            client.worldRenderer.entityOutlinesFramebuffer = originalOutlineFbo;
            client.gameRenderer.renderHand = originalRenderHand;
            client.options.getFov().setValue(originalFov);
            client.player.setYaw(originalYaw);
        }

        // In 1.21, no need to call beginWrite on main framebuffer — handled by pipeline
    }

    private boolean isEntityWithinViewAngle(LivingEntity targetEntity) {
        float yawToTarget = RotationUtils.getRotationsToEntityFrom(
                targetEntity,
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
        )[0];

        return RotationUtils.getWrappedAngleDifference(client.player.getYaw(), yawToTarget) <= 90.0F;
    }

    private void blitFramebufferToScreen(Framebuffer source, int width, int height, double posX, double posY) {
        posY = posY - (double) client.getWindow().getFramebufferHeight() + (double) height;

        RenderCompat.colorMask(true, true, true, false);
        RenderCompat.disableDepthTest();
        RenderCompat.depthMask(false);

        RenderCompat.matrixMode(5889);
        RenderCompat.loadIdentity();
        RenderCompat.ortho(0.0, (double) width + posX, height, 0.0, 1000.0, 3000.0);

        RenderCompat.matrixMode(5888);
        RenderCompat.loadIdentity();
        RenderCompat.translatef(0.0F, 0.0F, -2000.0F);

        RenderCompat.viewport(0, 0, width + (int) posX, height - (int) posY);

        RenderCompat.enableTexture();
        RenderCompat.disableLighting();
        RenderCompat.disableAlphaTest();
        RenderCompat.disableBlend();
        RenderCompat.enableColorMaterial();
        RenderCompat.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        // In 1.21, Framebuffer.beginRead()/endRead() are removed.
        // Instead, we bind the texture directly via GL.
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, source.getColorAttachment());

        float w = (float) width;
        float h = (float) height;
        float uMax = (float) source.textureWidth / (float) source.textureWidth; // 1.0
        float vMax = (float) source.textureHeight / (float) source.textureHeight; // 1.0

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex3d(posX, (double) h + posY, 0.0);
        GL11.glTexCoord2f(uMax, 0.0F);
        GL11.glVertex3d((double) w + posX, (double) h + posY, 0.0);
        GL11.glTexCoord2f(uMax, vMax);
        GL11.glVertex3d((double) w + posX, posY, 0.0);
        GL11.glTexCoord2f(0.0F, vMax);
        GL11.glVertex3d(posX, posY, 0.0);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        RenderCompat.depthMask(true);
        RenderCompat.colorMask(true, true, true, true);
        RenderCompat.enableAlphaTest();
        RenderCompat.enableBlend();
    }

    private void rebuild() {
        // In 1.21, Framebuffer constructor changed.
        // Use SimpleFramebuffer(String name, int width, int height, boolean useDepthAttachment)
        rearViewFramebuffer = new SimpleFramebuffer(
                "rearview",
                client.getWindow().getFramebufferWidth(),
                client.getWindow().getFramebufferHeight(),
                true
        );
        rearViewFramebuffer.setClearColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

}
