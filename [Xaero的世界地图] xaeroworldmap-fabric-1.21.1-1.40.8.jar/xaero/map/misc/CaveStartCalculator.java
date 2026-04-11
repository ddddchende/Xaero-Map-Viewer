package xaero.map.misc;

import net.minecraft.class_1937;
import net.minecraft.class_1944;
import net.minecraft.class_2246;
import net.minecraft.class_2404;
import net.minecraft.class_2680;
import net.minecraft.class_2688;
import net.minecraft.class_2791;
import net.minecraft.class_2826;
import net.minecraft.class_3481;
import net.minecraft.class_3532;
import net.minecraft.class_3619;
import net.minecraft.class_2338.class_2339;
import net.minecraft.class_2902.class_2903;
import xaero.lib.client.config.ClientConfigManager;
import xaero.map.MapWriter;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;

public class CaveStartCalculator {
   private final class_2339 mutableBlockPos = new class_2339();
   private final CachedFunction<class_2688<?, ?>, Boolean> transparentCache;
   private final MapWriter mapWriter;

   public CaveStartCalculator(MapWriter mapWriter) {
      this.mapWriter = mapWriter;
      this.transparentCache = new CachedFunction((state) -> {
         return mapWriter.shouldOverlay(state);
      });
   }

   public int getCaving(double playerX, double playerY, double playerZ, class_1937 world) {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      int autoCaveModeConfig = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.AUTO_CAVE_MODE);
      if (autoCaveModeConfig == 0) {
         return Integer.MAX_VALUE;
      } else {
         int worldBottomY = world.method_31607();
         int worldTopY = world.method_31600() - 1;
         int y = (int)playerY + 1;
         int defaultCaveStart = y + 3;
         int defaultResult = Integer.MAX_VALUE;
         if (y <= worldTopY && y >= worldBottomY) {
            int x = class_3532.method_15357(playerX);
            int z = class_3532.method_15357(playerZ);
            int roofRadius = autoCaveModeConfig < 0 ? 1 : autoCaveModeConfig - 1;
            int roofDiameter = 1 + roofRadius * 2;
            int startX = x - roofRadius;
            int startZ = z - roofRadius;
            boolean ignoringHeightmaps = this.mapWriter.getMapProcessor().getMapWorld().isIgnoreHeightmaps();
            int bottom = y;
            int top = Integer.MAX_VALUE;
            class_2791 prevBChunk = null;
            int potentialResult = defaultCaveStart;

            for(int o = 0; o < roofDiameter; ++o) {
               label108:
               for(int p = 0; p < roofDiameter; ++p) {
                  int currentX = startX + o;
                  int currentZ = startZ + p;
                  this.mutableBlockPos.method_10103(currentX, y, currentZ);
                  class_2791 bchunk = world.method_8497(currentX >> 4, currentZ >> 4);
                  if (bchunk == null) {
                     return defaultResult;
                  }

                  int skyLight = world.method_8314(class_1944.field_9284, this.mutableBlockPos);
                  int playerSection;
                  int i;
                  if (!ignoringHeightmaps) {
                     if (skyLight >= 15) {
                        return defaultResult;
                     }

                     i = currentX & 15;
                     playerSection = currentZ & 15;
                     top = bchunk.method_12005(class_2903.field_13202, i, playerSection);
                  } else if (bchunk != prevBChunk) {
                     class_2826[] sections = bchunk.method_12006();
                     if (sections.length == 0) {
                        return defaultResult;
                     }

                     playerSection = y - worldBottomY >> 4;
                     boolean foundSomething = false;

                     for(int i = playerSection; i < sections.length; ++i) {
                        class_2826 searchedSection = sections[i];
                        if (!searchedSection.method_38292()) {
                           if (!foundSomething) {
                              bottom = Math.max(bottom, worldBottomY + (i << 4));
                              foundSomething = true;
                           }

                           top = worldBottomY + (i << 4) + 15;
                        }
                     }

                     if (!foundSomething) {
                        return defaultResult;
                     }

                     prevBChunk = bchunk;
                  }

                  if (top < worldBottomY) {
                     return defaultResult;
                  }

                  if (top > worldTopY) {
                     top = worldTopY;
                  }

                  for(i = bottom; i <= top; ++i) {
                     this.mutableBlockPos.method_33098(i);
                     class_2680 state = world.method_8320(this.mutableBlockPos);
                     if (!state.method_26215() && state.method_26223() != class_3619.field_15971 && !(state.method_26204() instanceof class_2404) && !state.method_26164(class_3481.field_15503) && !(Boolean)this.transparentCache.apply(state) && state.method_26204() != class_2246.field_10499) {
                        if (o == p && o == roofRadius) {
                           potentialResult = Math.min(i, defaultCaveStart);
                        }
                        continue label108;
                     }
                  }

                  return defaultResult;
               }
            }

            return potentialResult;
         } else {
            return defaultResult;
         }
      }
   }
}
