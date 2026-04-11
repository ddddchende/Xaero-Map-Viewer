package xaero.map.highlight;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.nio.ByteBuffer;
import java.util.Iterator;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_5321;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import xaero.map.WorldMap;
import xaero.map.mods.SupportMods;
import xaero.map.pool.buffer.PoolTextureDirectBufferUnit;
import xaero.map.world.MapDimension;

public class DimensionHighlighterHandler {
   private final MapDimension mapDimension;
   private final class_5321<class_1937> dimension;
   private final HighlighterRegistry registry;
   private final Long2ObjectMap<Integer> hashCodeCache;
   private final class_2561 SUBTLE_TOOLTIP_SEPARATOR = class_2561.method_43470(" | ");
   private final class_2561 BLUNT_TOOLTIP_SEPARATOR = class_2561.method_43470(" \n ");

   public DimensionHighlighterHandler(MapDimension mapDimension, class_5321<class_1937> dimension, HighlighterRegistry registry) {
      this.mapDimension = mapDimension;
      this.dimension = dimension;
      this.registry = registry;
      this.hashCodeCache = new Long2ObjectOpenHashMap();
   }

   public int getRegionHash(int regionX, int regionZ) {
      synchronized(this) {
         long key = getKey(regionX, regionZ);
         Integer cachedHash = (Integer)this.hashCodeCache.get(key);
         if (cachedHash == null) {
            cachedHash = this.recalculateHash(regionX, regionZ);
         }

         return cachedHash;
      }
   }

   public boolean shouldApplyRegionHighlights(int regionX, int regionZ, boolean discovered) {
      class_5321<class_1937> dimension = this.dimension;
      Iterator var5 = this.registry.getHighlighters().iterator();

      AbstractHighlighter hl;
      do {
         do {
            if (!var5.hasNext()) {
               return false;
            }

            hl = (AbstractHighlighter)var5.next();
         } while(!discovered && !hl.isCoveringOutsideDiscovered());
      } while(!hl.regionHasHighlights(dimension, regionX, regionZ));

      return true;
   }

   public boolean shouldApplyTileChunkHighlights(int regionX, int regionZ, int insideTileChunkX, int insideTileChunkZ, boolean discovered) {
      int startChunkX = regionX << 5 | insideTileChunkX << 2;
      int startChunkZ = regionZ << 5 | insideTileChunkZ << 2;
      Iterator var8 = this.registry.getHighlighters().iterator();

      AbstractHighlighter hl;
      do {
         if (!var8.hasNext()) {
            return false;
         }

         hl = (AbstractHighlighter)var8.next();
      } while(!this.shouldApplyTileChunkHighlightsHelp(hl, regionX, regionZ, startChunkX, startChunkZ, discovered));

      return true;
   }

   private boolean shouldApplyTileChunkHighlights(AbstractHighlighter hl, int regionX, int regionZ, int insideTileChunkX, int insideTileChunkZ, boolean discovered) {
      int startChunkX = regionX << 5 | insideTileChunkX << 2;
      int startChunkZ = regionZ << 5 | insideTileChunkZ << 2;
      return this.shouldApplyTileChunkHighlightsHelp(hl, regionX, regionZ, startChunkX, startChunkZ, discovered);
   }

   private boolean shouldApplyTileChunkHighlightsHelp(AbstractHighlighter hl, int regionX, int regionZ, int startChunkX, int startChunkZ, boolean discovered) {
      if (!discovered && !hl.isCoveringOutsideDiscovered()) {
         return false;
      } else {
         class_5321<class_1937> dimension = this.dimension;
         if (!hl.regionHasHighlights(dimension, regionX, regionZ)) {
            return false;
         } else {
            for(int i = 0; i < 4; ++i) {
               for(int j = 0; j < 4; ++j) {
                  if (hl.chunkIsHighlit(dimension, startChunkX | i, startChunkZ | j)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   public PoolTextureDirectBufferUnit applyChunkHighlightColors(int chunkX, int chunkZ, int innerChunkX, int innerChunkZ, PoolTextureDirectBufferUnit buffer, PoolTextureDirectBufferUnit highlitColorBuffer, boolean highlitBufferPrepared, boolean discovered, boolean separateBuffer) {
      boolean hasSomething = false;
      class_5321<class_1937> dimension = this.dimension;
      if (!separateBuffer) {
         highlitBufferPrepared = true;
         highlitColorBuffer = buffer;
      }

      ByteBuffer highlitColorBufferDirect = highlitColorBuffer == null ? null : highlitColorBuffer.getDirectBuffer();
      Iterator var13 = this.registry.getHighlighters().iterator();

      while(true) {
         int[] highlightColors;
         do {
            AbstractHighlighter hl;
            do {
               if (!var13.hasNext()) {
                  if (!hasSomething) {
                     return null;
                  }

                  return highlitColorBuffer;
               }

               hl = (AbstractHighlighter)var13.next();
            } while(!discovered && !hl.isCoveringOutsideDiscovered());

            highlightColors = hl.getChunkHighlitColor(dimension, chunkX, chunkZ);
         } while(highlightColors == null);

         if (!hasSomething && !highlitBufferPrepared) {
            highlitColorBuffer = WorldMap.textureDirectBufferPool.get(buffer == null);
            highlitColorBufferDirect = highlitColorBuffer.getDirectBuffer();
            if (buffer != null) {
               highlitColorBufferDirect.put(buffer.getDirectBuffer());
            }

            highlitColorBufferDirect.position(0);
            if (buffer != null) {
               buffer.getDirectBuffer().position(0);
            }
         }

         hasSomething = true;
         int textureOffset = innerChunkZ << 4 << 6 | innerChunkX << 4;

         for(int i = 0; i < highlightColors.length; ++i) {
            int highlightColor = highlightColors[i];
            int hlAlpha = highlightColor & 255;
            float hlAlphaFloat = (float)hlAlpha / 255.0F;
            float oneMinusHlAlpha = 1.0F - hlAlphaFloat;
            int hlRed = highlightColor >> 8 & 255;
            int hlGreen = highlightColor >> 16 & 255;
            int hlBlue = highlightColor >> 24 & 255;
            int index = textureOffset | i >> 4 << 6 | i & 15;
            int originalColor = highlitColorBufferDirect.getInt(index * 4);
            int red = originalColor >> 8 & 255;
            int green = originalColor >> 16 & 255;
            int blue = originalColor >> 24 & 255;
            int alpha = originalColor & 255;
            red = (int)((float)red * oneMinusHlAlpha + (float)hlRed * hlAlphaFloat);
            green = (int)((float)green * oneMinusHlAlpha + (float)hlGreen * hlAlphaFloat);
            blue = (int)((float)blue * oneMinusHlAlpha + (float)hlBlue * hlAlphaFloat);
            if (red > 255) {
               red = 255;
            }

            if (green > 255) {
               green = 255;
            }

            if (blue > 255) {
               blue = 255;
            }

            highlitColorBufferDirect.putInt(index * 4, blue << 24 | green << 16 | red << 8 | alpha);
         }
      }
   }

   private int recalculateHash(int regionX, int regionZ) {
      HashCodeBuilder hashcodeBuilder = new HashCodeBuilder();
      Iterator var4 = this.registry.getHighlighters().iterator();

      while(var4.hasNext()) {
         AbstractHighlighter hl = (AbstractHighlighter)var4.next();
         hashcodeBuilder.append(hl.calculateRegionHash(this.dimension, regionX, regionZ));
         hashcodeBuilder.append(hl.isCoveringOutsideDiscovered());
      }

      int builtHash = hashcodeBuilder.build();
      long key = getKey(regionX, regionZ);
      this.hashCodeCache.put(key, builtHash);
      return builtHash;
   }

   public void clearCachedHash(int regionX, int regionZ) {
      long key = getKey(regionX, regionZ);
      this.hashCodeCache.remove(key);
      this.mapDimension.onClearCachedHighlightHash(regionX, regionZ);
      if (SupportMods.minimap()) {
         SupportMods.xaeroMinimap.onClearHighlightHash(regionX, regionZ);
      }

   }

   public void clearCachedHashes() {
      this.hashCodeCache.clear();
      this.mapDimension.onClearCachedHighlightHashes();
      if (SupportMods.minimap()) {
         SupportMods.xaeroMinimap.onClearHighlightHashes();
      }

   }

   public class_2561 getBlockHighlightSubtleTooltip(int blockX, int blockZ, boolean discovered) {
      return this.getBlockHighlightTooltip(blockX, blockZ, discovered, true);
   }

   public class_2561 getBlockHighlightBluntTooltip(int blockX, int blockZ, boolean discovered) {
      return this.getBlockHighlightTooltip(blockX, blockZ, discovered, false);
   }

   private class_2561 getBlockHighlightTooltip(int blockX, int blockZ, boolean discovered, boolean subtle) {
      class_5321<class_1937> dimension = this.dimension;
      int tileChunkX = blockX >> 6;
      int tileChunkZ = blockZ >> 6;
      int regionX = tileChunkX >> 3;
      int regionZ = tileChunkZ >> 3;
      if (!this.shouldApplyRegionHighlights(regionX, regionZ, discovered)) {
         return null;
      } else {
         int localTileChunkX = tileChunkX & 7;
         int localTileChunkZ = tileChunkZ & 7;
         class_2561 result = null;
         Iterator var13 = this.registry.getHighlighters().iterator();

         while(var13.hasNext()) {
            AbstractHighlighter hl = (AbstractHighlighter)var13.next();
            if (this.shouldApplyTileChunkHighlights(hl, regionX, regionZ, localTileChunkX, localTileChunkZ, discovered)) {
               class_2561 hlTooltip = subtle ? hl.getBlockHighlightSubtleTooltip(dimension, blockX, blockZ) : hl.getBlockHighlightBluntTooltip(dimension, blockX, blockZ);
               if (hlTooltip != null) {
                  if (result == null) {
                     result = class_2561.method_43470("");
                  } else {
                     result.method_10855().add(subtle ? this.SUBTLE_TOOLTIP_SEPARATOR : this.BLUNT_TOOLTIP_SEPARATOR);
                  }

                  result.method_10855().add(hlTooltip);
               }
            }
         }

         return result;
      }
   }

   public static long getKey(int regionX, int regionZ) {
      return (long)regionZ << 32 | (long)regionX & 4294967295L;
   }

   public static int getXFromKey(long key) {
      return (int)(key & -1L);
   }

   public static int getZFromKey(long key) {
      return (int)(key >> 32);
   }
}
