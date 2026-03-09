package io.github.sst.remake.util.render;

import org.joml.Vector3f;

/**
 * Rendering helper utilities updated for 1.21.
 * In 1.17+ the fixed-function pipeline lighting methods were removed from RenderSystem.
 * In 1.21, DiffuseLighting was restructured to use Type enum (LEVEL, ITEMS_FLAT, ITEMS_3D, etc.)
 * with setShaderLights(Type). Since this mod uses immediate-mode GL for most rendering,
 * these are effectively no-ops — the mod doesn't rely on Minecraft's diffuse lighting system.
 */
public class RenderHelper {
    private static final Vector3f DEFAULT_LIGHTING = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
    private static final Vector3f DIFFUSE_LIGHTING = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();
    private static final Vector3f GUI_FLAT_DIFFUSE_LIGHTING = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
    private static final Vector3f GUI_3D_DIFFUSE_LIGHTING = new Vector3f(-0.2F, -1.0F, 0.7F).normalize();

    /**
     * Previously enabled standard item lighting via fixed-function OpenGL.
     * In 1.21, DiffuseLighting uses Type-based API. No-op for immediate-mode rendering.
     */
    public static void enableStandardItemLighting() {
        // No-op: DiffuseLighting in 1.21 uses Type enum, not applicable for immediate-mode GL
    }

    /**
     * Previously disabled standard item lighting.
     * No-op for immediate-mode rendering.
     */
    public static void disableStandardItemLighting() {
        // No-op
    }

    public static void setupDiffuseGuiLighting(org.joml.Matrix4f matrix) {
        // No-op: DiffuseLighting.setShaderLights(Type) in 1.21, not applicable here
    }

    public static void setupLevelDiffuseLighting(org.joml.Matrix4f matrixIn) {
        // No-op
    }

    public static void setupGuiFlatDiffuseLighting() {
        // No-op
    }

    public static void setupGui3DDiffuseLighting() {
        // No-op
    }
}
