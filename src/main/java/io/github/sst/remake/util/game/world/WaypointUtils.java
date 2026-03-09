package io.github.sst.remake.util.game.world;

import io.github.sst.remake.Client;
import io.github.sst.remake.util.IMinecraft;
import io.github.sst.remake.util.client.ConfigUtils;
import io.github.sst.remake.util.game.world.data.Chunk;
import io.github.sst.remake.util.game.world.data.RegionPos;
import io.github.sst.remake.util.game.world.data.MapRegion;
import io.github.sst.remake.util.math.color.ColorHelper;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WaypointUtils implements IMinecraft {
    public static final ConcurrentHashMap<Long, MapRegion> regionCache = new ConcurrentHashMap<>();

    public static final List<ChunkPos> processedChunks = new CopyOnWriteArrayList<>();
    public static final List<ChunkPos> borderChunks = new CopyOnWriteArrayList<>();
    public static final List<RegionPos> missingRegionFiles = new CopyOnWriteArrayList<>();
    private static final AtomicLong mapDataVersion = new AtomicLong(0L);

    public static ByteBuffer defaultChunkBuffer = BufferUtils.createByteBuffer(10 * 16 * 10 * 16 * 3);

    public static void markMapDataDirty() {
        mapDataVersion.incrementAndGet();
    }

    public static long getMapDataVersion() {
        return mapDataVersion.get();
    }

    public static String getRegionFilePath(String baseDir, WorldChunk chunk) {
        RegionPos regionPos = RegionPos.fromChunkPos(chunk.getPos());
        return baseDir + "/" + regionPos.x + "c" + regionPos.z + ConfigUtils.WAYPOINT_EXTENSION;
    }

    public static String getRegionFilePath(String baseDir, MapRegion region) {
        return baseDir + "/" + region.regionX + "c" + region.regionZ + ConfigUtils.WAYPOINT_EXTENSION;
    }

    public static String getRegionFilePath(String baseDir, RegionPos regionPos) {
        return baseDir + "/" + regionPos.x + "c" + regionPos.z + ConfigUtils.WAYPOINT_EXTENSION;
    }

    public static String getWorldIdentifier() {
        String identifier = "local/local";

        if (client.getServer() == null && client.getCurrentServerEntry() != null) {
            identifier = "server/" + client.getCurrentServerEntry().address.replace("/", ":");
        } else if (client.getServer() != null) {
            identifier = "local/" + client.getServer().getSaveProperties().getLevelName();
        }

        return identifier;
    }

    public static boolean areNeighborsLoaded(WorldChunk var1) {
        WorldChunk var4 = client.world.getChunk(var1.getPos().x, var1.getPos().z + 1);
        WorldChunk var5 = client.world.getChunk(var1.getPos().x, var1.getPos().z - 1);
        return var4 != null && !var4.isEmpty() && var5 != null && !var5.isEmpty();
    }


    public static ByteBuffer generateChunkMap(WorldChunk var1, boolean var2) {
        ByteBuffer var5 = BufferUtils.createByteBuffer(768);
        int var6 = var1.getPos().x * 16;
        int var7 = var1.getPos().z * 16;

        for (int var8 = 0; var8 < 16; var8++) {
            for (int var9 = 0; var9 < 16; var9++) {
                BlockPos var10 = new BlockPos(var6 + var8, 64, var7 + var9);
                int var11 = getWaypointHeight(
                        new BlockPos(var10.getX(), var1.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(var8, var9) - 1, var10.getZ()), var2
                );
                var5.put((byte) (var11 >> 16 & 0xFF));
                var5.put((byte) (var11 >> 8 & 0xFF));
                var5.put((byte) (var11 & 0xFF));
            }
        }

        ((Buffer) var5).flip();
        return var5;
    }

    public static Chunk createMapTexture(ChunkPos chunkPos, int size) {
        List<ChunkPos> chunkPositions = new ArrayList<>();

        for (int i = -size / 2; i < size / 2; i++) {
            for (int j = -size / 2; j < size / 2; j++) {
                chunkPositions.add(new ChunkPos(chunkPos.x + i, chunkPos.z + j));
            }
        }

        ByteBuffer buffer = BufferUtils.createByteBuffer(size * 16 * size * 16 * 3);
        int i = 0;
        int bufferPos = buffer.position();

        for (ChunkPos pos : chunkPositions) {
            ByteBuffer chunkBuffer = defaultChunkBuffer.duplicate();
            ((Buffer) chunkBuffer).position(0);
            RegionPos regionPos = RegionPos.fromChunkPos(pos);
            MapRegion mapRegion = regionCache.get(regionPos.toLong());
            if (mapRegion != null) {
                ByteBuffer data = mapRegion.getChunkData(pos);
                if (data != null) {
                    chunkBuffer = data.duplicate();
                }
            } else {
                try {
                    if (loadRegionFromFile(regionPos)) {
                        mapRegion = regionCache.get(regionPos.toLong());
                        ByteBuffer data = mapRegion.getChunkData(pos);
                        if (data != null) {
                            chunkBuffer = data.duplicate();
                        }
                    }
                } catch (IOException e) {
                    Client.LOGGER.warn("Failed to create map texture", e);
                }
            }

            int currentPos = buffer.position();
            int pos2 = buffer.position();

            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    buffer.put(chunkBuffer.get());
                    buffer.put(chunkBuffer.get());
                    buffer.put(chunkBuffer.get());
                }

                currentPos += 16 * size * 3;
                if (currentPos < buffer.limit()) {
                    ((Buffer) buffer).position(currentPos);
                }
            }

            bufferPos += 48;
            if (pos2 + 48 < buffer.limit()) {
                ((Buffer) buffer).position(pos2 + 48);
            }

            if (i != bufferPos / (48 * size)) {
                i = bufferPos / (48 * size);
                if (256 * size * 3 * i < buffer.limit()) {
                    ((Buffer) buffer).position(256 * size * 3 * i);
                }
            }

            ((Buffer) chunkBuffer).position(0);
        }

        ((Buffer) buffer).position(16 * size * 16 * size * 3);
        ((Buffer) buffer).flip();
        return new Chunk(buffer, 16 * size, 16 * size);
    }

    public static boolean loadRegionFromFile(final RegionPos regionPos) throws IOException {
        if (missingRegionFiles.contains(regionPos)) {
            return false;
        }

        final String identifier = Client.INSTANCE.waypointTracker.mapRegionIdentifier;
        final File file = new File(getRegionFilePath(identifier, regionPos));

        if (!file.exists()) {
            missingRegionFiles.add(regionPos);
            return false;
        }

        final long key = regionPos.toLong();

        MapRegion existing = regionCache.get(key);
        if (existing != null) {
            return true;
        }

        MapRegion loaded = regionCache.computeIfAbsent(key, k -> {
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {

                MapRegion region = new MapRegion(regionPos.x, regionPos.z);
                synchronized (region) {
                    region.read(ois);
                }
                return region;

            } catch (IOException e) {
                Client.LOGGER.warn("Failed to load region file", e);
                missingRegionFiles.add(regionPos);
                return null;
            }
        });

        return loaded != null;
    }

    /**
     * Get the color for a waypoint map at the given block position.
     *
     * In 1.20+, Material class was removed. We now use direct block/fluid state checks:
     * - Material.AIR -> state.isAir()
     * - Material.SNOW_BLOCK -> state.isOf(Blocks.SNOW_BLOCK) || state.isOf(Blocks.SNOW)
     * - Material.LAVA -> state.getFluidState().isOf(Fluids.LAVA)
     * - Material.WATER -> state.getFluidState().isOf(Fluids.WATER)
     * - material.getColor() -> state.getMapColor(world, pos)
     */
    public static int getWaypointHeight(BlockPos var1, boolean var2) {
        if (client.world.getBlockState(var1).isAir()) {
            var1 = var1.down();
        }

        MapColor var5 = client.world.getBlockState(var1).getMapColor(client.world, var1);
        int var6 = var5.color;

        // Check block above for snow/lava
        net.minecraft.block.BlockState aboveState = client.world.getBlockState(var1.up());
        boolean isSnow = aboveState.isOf(Blocks.SNOW_BLOCK) || aboveState.isOf(Blocks.SNOW);
        boolean isLava = aboveState.getFluidState().isOf(Fluids.LAVA);

        if (!isSnow) {
            if (isLava) {
                var6 = aboveState.getMapColor(client.world, var1.up()).color;
            }
        } else {
            var6 = -1;
        }

        if (client.world.getBlockState(var1).contains(Properties.WATERLOGGED)) {
            // Water map color
            var6 = MapColor.WATER_BLUE.color;
        }

        int var8 = (var6 & 0xFF0000) >> 16;
        int var9 = (var6 & 0xFF00) >> 8;
        int var10 = var6 & 0xFF;
        var6 = new Color(var8, var9, var10).getRGB();
        boolean var11 = Math.abs(var1.getZ() % 16) != 15 && Math.abs(var1.getZ() % 16) != 0;
        if (var1.getZ() < 0) {
            var11 = Math.abs(var1.getZ() % 16) != 16 && Math.abs(var1.getZ() % 16) != 0;
        }

        if (var2 || var11) {
            // Check north/south blocks for shading
            net.minecraft.block.BlockState northState = client.world.getBlockState(var1.north());
            net.minecraft.block.BlockState southState = client.world.getBlockState(var1.south());
            boolean northIsAirOrSnow = northState.isAir() || northState.isOf(Blocks.SNOW_BLOCK) || northState.isOf(Blocks.SNOW);
            boolean southIsAirOrSnow = southState.isAir() || southState.isOf(Blocks.SNOW_BLOCK) || southState.isOf(Blocks.SNOW);

            if (northIsAirOrSnow) {
                var6 = ColorHelper.blendColor(new Color(var6, true), Color.BLACK, 0.6F).getRGB();
            } else if (southIsAirOrSnow) {
                var6 = ColorHelper.blendColor(new Color(var6, true), Color.WHITE, 0.6F).getRGB();
            }
        }

        if (var6 != -16777216) {
            var6 = new Color(var6, true).getRGB();
        } else {
            var6 = -7687425;
        }

        return var6;
    }
}
