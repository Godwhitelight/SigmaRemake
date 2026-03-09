package io.github.sst.remake.util.game.world;

import io.github.sst.remake.util.IMinecraft;
import org.joml.Vector3d;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

@SuppressWarnings("ALL")
public class EntityUtils implements IMinecraft {
    public static double calculateDistanceSquared(BlockPos blockPos) {
        Vector3d pos = getEntityPosition(client.player);
        double deltaX = pos.x - (double) blockPos.getX();
        double deltaY = pos.y - (double) blockPos.getY();
        double deltaZ = pos.z - (double) blockPos.getZ();
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    public static Vector3d getEntityPosition(Entity entity) {
        float tickDelta = client.getRenderTickCounter().getTickDelta(true);
        return new Vector3d(
                entity.lastRenderX + (entity.getX() - entity.lastRenderX) * (double) tickDelta,
                entity.lastRenderY + (entity.getY() - entity.lastRenderY) * (double) tickDelta,
                entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * (double) tickDelta
        );
    }
}
