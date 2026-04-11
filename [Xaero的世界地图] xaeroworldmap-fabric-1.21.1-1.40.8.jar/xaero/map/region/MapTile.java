package xaero.map.region;

import xaero.map.pool.PoolUnit;

public class MapTile implements PoolUnit {
   public static final int CURRENT_WORLD_INTERPRETATION_VERSION = 1;
   private boolean loaded;
   private byte signed_worldInterpretationVersion;
   private int chunkX;
   private int chunkZ;
   private MapBlock[][] blocks = new MapBlock[16][16];
   private boolean writtenOnce;
   private int writtenCaveStart;
   private byte writtenCaveDepth;

   public MapTile(Object... args) {
      this.create(args);
   }

   public void create(Object... args) {
      this.chunkX = (Integer)args[1];
      this.chunkZ = (Integer)args[2];
      this.loaded = false;
      this.signed_worldInterpretationVersion = 0;
      this.writtenOnce = false;
      this.writtenCaveStart = Integer.MAX_VALUE;
      this.writtenCaveDepth = 0;
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   public void setLoaded(boolean loaded) {
      this.loaded = loaded;
   }

   public MapBlock getBlock(int x, int z) {
      return this.blocks[x][z];
   }

   public MapBlock[] getBlockColumn(int x) {
      return this.blocks[x];
   }

   public void setBlock(int x, int z, MapBlock block) {
      this.blocks[x][z] = block;
   }

   public int getChunkX() {
      return this.chunkX;
   }

   public int getChunkZ() {
      return this.chunkZ;
   }

   public boolean wasWrittenOnce() {
      return this.writtenOnce;
   }

   public void setWrittenOnce(boolean writtenOnce) {
      this.writtenOnce = writtenOnce;
   }

   public int getWorldInterpretationVersion() {
      return this.signed_worldInterpretationVersion & 255;
   }

   public void setWorldInterpretationVersion(int version) {
      this.signed_worldInterpretationVersion = (byte)version;
   }

   public int getWrittenCaveStart() {
      return this.writtenCaveStart;
   }

   public void setWrittenCave(int writtenCaveStart, int writtenCaveDepth) {
      this.writtenCaveStart = writtenCaveStart;
      this.writtenCaveDepth = (byte)writtenCaveDepth;
   }

   public int getWrittenCaveDepth() {
      return this.writtenCaveDepth & 255;
   }
}
