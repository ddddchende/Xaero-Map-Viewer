package xaero.map.region;

import java.util.ArrayList;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2378;
import net.minecraft.class_2680;
import net.minecraft.class_2874;
import net.minecraft.class_5321;
import net.minecraft.class_2338.class_2339;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.world.MapDimension;

public class MapBlock extends MapPixel {
   protected boolean slopeUnknown = true;
   private byte verticalSlope;
   private byte diagonalSlope;
   private short height;
   private short topHeight;
   private ArrayList<Overlay> overlays;
   private class_5321<class_1959> biome = null;

   public boolean isGrass() {
      return this.state.method_26204() == class_2246.field_10219;
   }

   public int getParametres() {
      int parametres = 0;
      int parametres = parametres | (!this.isGrass() ? 1 : 0);
      parametres |= this.getNumberOfOverlays() != 0 ? 2 : 0;
      parametres |= this.light << 8;
      parametres |= (this.getHeight() & 255) << 12;
      parametres |= this.biome != null ? 1048576 : 0;
      parametres |= this.height != this.topHeight ? 16777216 : 0;
      parametres |= (this.getHeight() >> 8 & 15) << 25;
      return parametres;
   }

   public void getPixelColour(int[] result_dest, MapWriter mapWriter, class_1937 world, MapDimension dim, class_2378<class_2248> blockRegistry, MapTileChunk tileChunk, MapTileChunk prevChunk, MapTileChunk prevChunkDiagonal, MapTileChunk prevChunkHorisontal, MapTile mapTile, int x, int z, int caveStart, int caveDepth, class_2339 mutableGlobalPos, class_2378<class_1959> biomeRegistry, class_2378<class_2874> dimensionTypes, float shadowR, float shadowG, float shadowB, BlockTintProvider blockTintProvider, MapProcessor mapProcessor, OverlayManager overlayManager, int effectiveHeight, int effectiveTopHeight, BlockStateShortShapeCache blockStateShortShapeCache, MapUpdateFastConfig config) {
      super.getPixelColours(result_dest, mapWriter, world, dim, blockRegistry, tileChunk, prevChunk, prevChunkDiagonal, prevChunkHorisontal, mapTile, x, z, this, effectiveHeight, effectiveTopHeight, caveStart, caveDepth, this.overlays, mutableGlobalPos, biomeRegistry, dimensionTypes, shadowR, shadowG, shadowB, blockTintProvider, mapProcessor, overlayManager, blockStateShortShapeCache, config);
   }

   public String toRenderString(class_2378<class_1959> biomeRegistry) {
      boolean var10000 = class_2248.field_10651.method_10200(class_2248.field_10651.method_10206(this.getState())) == this.getState();
      return var10000 + " S: " + String.valueOf(this.getState()) + ", VS: " + this.verticalSlope + ", DS: " + this.diagonalSlope + ", SU: " + this.slopeUnknown + ", H: " + this.getHeight() + ", B: " + String.valueOf(this.biome == null ? "null" : this.biome.method_29177()) + ", L: " + this.light + ", G: " + this.glowing + ", O: " + this.getNumberOfOverlays();
   }

   public boolean equalsSlopesExcluded(MapBlock p) {
      boolean equal = p != null && this.state == p.state && this.light == p.light && this.height == p.height && this.topHeight == p.topHeight && this.getNumberOfOverlays() == p.getNumberOfOverlays() && this.biome == p.biome;
      if (equal && this.getNumberOfOverlays() != 0) {
         for(int i = 0; i < this.overlays.size(); ++i) {
            if (!((Overlay)this.overlays.get(i)).equals((Overlay)p.overlays.get(i))) {
               return false;
            }
         }
      }

      return equal;
   }

   public boolean equals(MapBlock p, boolean equalsSlopesExcluded) {
      return p != null && this.verticalSlope == p.verticalSlope && this.diagonalSlope == p.diagonalSlope && this.slopeUnknown == p.slopeUnknown && equalsSlopesExcluded;
   }

   public void fixHeightType(int x, int z, MapTile mapTile, MapTileChunk tileChunk, MapTileChunk prevChunk, MapTileChunk prevChunkDiagonal, MapTileChunk prevChunkHorisontal, int height, boolean useSourceData, BlockStateShortShapeCache blockStateShortShapeCache, MapUpdateFastConfig updateConfig) {
      int prevHeight = 32767;
      int prevHeightDiagonal = 32767;
      if (useSourceData && z > 0) {
         prevHeight = mapTile.getBlock(x, z - 1).getEffectiveHeight(blockStateShortShapeCache, updateConfig);
         if (x > 0) {
            prevHeightDiagonal = mapTile.getBlock(x - 1, z - 1).getEffectiveHeight(blockStateShortShapeCache, updateConfig);
         }
      }

      if (prevHeight == 32767 || prevHeightDiagonal == 32767) {
         int inTileChunkX = ((mapTile.getChunkX() & 3) << 4) + x;
         int inTileChunkZ = ((mapTile.getChunkZ() & 3) << 4) + z;
         int inTileChunkXPrev = inTileChunkX - 1;
         int inTileChunkZPrev = inTileChunkZ - 1;
         MapTileChunk verticalSlopeSrc = tileChunk;
         MapTileChunk diagonalSlopeSrc = tileChunk;
         boolean verticalEdge = inTileChunkZPrev < 0;
         boolean horisontalEdge = inTileChunkXPrev < 0;
         if (verticalEdge) {
            diagonalSlopeSrc = prevChunk;
            verticalSlopeSrc = prevChunk;
            inTileChunkZPrev = 63;
         }

         if (horisontalEdge) {
            inTileChunkXPrev = 63;
            diagonalSlopeSrc = verticalEdge ? prevChunkDiagonal : prevChunkHorisontal;
         }

         if (prevHeight == 32767 && verticalSlopeSrc != null && verticalSlopeSrc.getLoadState() >= 2) {
            prevHeight = verticalSlopeSrc.getLeafTexture().getHeight(inTileChunkX, inTileChunkZPrev);
         }

         if (prevHeightDiagonal == 32767 && diagonalSlopeSrc != null && diagonalSlopeSrc.getLoadState() >= 2) {
            prevHeightDiagonal = diagonalSlopeSrc.getLeafTexture().getHeight(inTileChunkXPrev, inTileChunkZPrev);
         }

         if (prevHeight == 32767 || prevHeightDiagonal == 32767) {
            if (useSourceData) {
               return;
            } else {
               int reX = x < 15 ? x + 1 : x;
               int reZ = z < 15 ? z + 1 : z;
               if (reX == x && reZ == z) {
                  this.verticalSlope = 0;
                  this.diagonalSlope = 0;
                  this.slopeUnknown = false;
                  return;
               } else {
                  int inTileChunkReX = ((mapTile.getChunkX() & 3) << 4) + reX;
                  int inTileChunkReZ = ((mapTile.getChunkZ() & 3) << 4) + reZ;
                  int reHeight = tileChunk.getLeafTexture().getHeight(inTileChunkReX, inTileChunkReZ);
                  if (reHeight != 32767) {
                     this.fixHeightType(reX, reZ, mapTile, tileChunk, prevChunk, prevChunkDiagonal, prevChunkHorisontal, reHeight, useSourceData, blockStateShortShapeCache, updateConfig);
                  }

                  return;
               }
            }
         }
      }

      this.verticalSlope = (byte)Math.max(-128, Math.min(127, height - prevHeight));
      this.diagonalSlope = (byte)Math.max(-128, Math.min(127, height - prevHeightDiagonal));
      this.slopeUnknown = false;
   }

   public void prepareForWriting(int defaultHeight) {
      if (this.overlays != null) {
         this.overlays.clear();
      }

      this.biome = null;
      this.state = class_2246.field_10124.method_9564();
      this.slopeUnknown = true;
      this.light = 0;
      this.glowing = false;
      this.height = (short)defaultHeight;
      this.topHeight = (short)defaultHeight;
   }

   public void write(class_2680 state, int height, int topHeight, class_5321<class_1959> biomeIn, byte light, boolean glowing, boolean cave) {
      this.state = state;
      this.setHeight(height);
      this.setTopHeight(topHeight);
      if (biomeIn != null) {
         this.biome = biomeIn;
      }

      this.light = light;
      this.glowing = glowing;
      if (this.overlays != null && this.overlays.isEmpty()) {
         this.overlays = null;
      }

   }

   public void addOverlay(Overlay o) {
      if (this.overlays == null) {
         this.overlays = new ArrayList();
      }

      this.overlays.add(o);
   }

   public int getHeight() {
      return this.height;
   }

   public void setHeight(int h) {
      this.height = (short)h;
   }

   public int getTopHeight() {
      return this.topHeight;
   }

   public void setTopHeight(int h) {
      this.topHeight = (short)h;
   }

   public int getEffectiveHeight(BlockStateShortShapeCache blockStateShortShapeCache, MapUpdateFastConfig updateConfig) {
      return this.getEffectiveHeight(updateConfig.adjustHeightForShortBlocks && blockStateShortShapeCache.isShort(this.state));
   }

   public int getEffectiveHeight(boolean subtractOne) {
      int height = this.getHeight();
      if (subtractOne) {
         --height;
      }

      return height;
   }

   public int getEffectiveTopHeight(boolean subtractOne) {
      int topHeight = this.getTopHeight();
      if (subtractOne && topHeight == this.getHeight()) {
         --topHeight;
      }

      return topHeight;
   }

   public class_5321<class_1959> getBiome() {
      return this.biome;
   }

   public void setBiome(class_5321<class_1959> biome) {
      this.biome = biome;
   }

   public ArrayList<Overlay> getOverlays() {
      return this.overlays;
   }

   public byte getVerticalSlope() {
      return this.verticalSlope;
   }

   public void setVerticalSlope(byte slope) {
      this.verticalSlope = slope;
   }

   public byte getDiagonalSlope() {
      return this.diagonalSlope;
   }

   public void setDiagonalSlope(byte slope) {
      this.diagonalSlope = slope;
   }

   public void setSlopeUnknown(boolean slopeUnknown) {
      this.slopeUnknown = slopeUnknown;
   }

   public int getNumberOfOverlays() {
      return this.overlays == null ? 0 : this.overlays.size();
   }
}
