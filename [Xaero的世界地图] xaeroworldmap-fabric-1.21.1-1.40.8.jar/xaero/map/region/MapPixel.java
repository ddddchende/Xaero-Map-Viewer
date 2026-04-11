package xaero.map.region;

import java.util.ArrayList;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2189;
import net.minecraft.class_2248;
import net.minecraft.class_2378;
import net.minecraft.class_2386;
import net.minecraft.class_2404;
import net.minecraft.class_2504;
import net.minecraft.class_2506;
import net.minecraft.class_2680;
import net.minecraft.class_2874;
import net.minecraft.class_2338.class_2339;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.world.MapDimension;

public class MapPixel {
   private static final int VOID_COLOR = -16121833;
   private static final float DEFAULT_AMBIENT_LIGHT = 0.7F;
   private static final float DEFAULT_AMBIENT_LIGHT_COLORED = 0.2F;
   private static final float DEFAULT_AMBIENT_LIGHT_WHITE = 0.5F;
   private static final float DEFAULT_MAX_DIRECT_LIGHT = 0.6666667F;
   private static final float GLOWING_MAX_DIRECT_LIGHT = 0.22222224F;
   protected class_2680 state;
   protected byte light = 0;
   protected boolean glowing = false;

   private int getVanillaTransparency(class_2248 b) {
      return b instanceof class_2404 ? 191 : (b instanceof class_2386 ? 216 : 127);
   }

   public void getPixelColours(int[] result_dest, MapWriter mapWriter, class_1937 world, MapDimension dim, class_2378<class_2248> blockRegistry, MapTileChunk tileChunk, MapTileChunk prevChunk, MapTileChunk prevChunkDiagonal, MapTileChunk prevChunkHorisontal, MapTile mapTile, int x, int z, MapBlock block, int height, int topHeight, int caveStart, int caveDepth, ArrayList<Overlay> overlays, class_2339 mutableGlobalPos, class_2378<class_1959> biomeRegistry, class_2378<class_2874> dimensionTypes, float shadowR, float shadowG, float shadowB, BlockTintProvider blockTintProvider, MapProcessor mapProcessor, OverlayManager overlayManager, BlockStateShortShapeCache blockStateShortShapeCache, MapUpdateFastConfig updateConfig) {
      int colour = block != null && caveStart != Integer.MAX_VALUE ? 0 : -16121833;
      int topLightValue = this.light;
      int lightMin = 9;
      float brightnessR = 1.0F;
      float brightnessG = 1.0F;
      float brightnessB = 1.0F;
      mutableGlobalPos.method_10103(mapTile.getChunkX() * 16 + x, height, mapTile.getChunkZ() * 16 + z);
      class_2680 state = this.state;
      boolean isAir = state.method_26204() instanceof class_2189;
      boolean isFinalBlock = this instanceof MapBlock;
      int g;
      if (!isAir) {
         if (updateConfig.blockColors == 0) {
            colour = mapWriter.loadBlockColourFromTexture(state, true, world, blockRegistry, mutableGlobalPos);
         } else {
            try {
               class_2248 b = state.method_26204();
               g = this.getVanillaTransparency(b);
               colour = state.method_26205(world, mutableGlobalPos).field_16011;
               if (!isFinalBlock && colour == 0) {
                  result_dest[0] = -1;
                  return;
               }

               colour = g << 24 | colour & 16777215;
            } catch (Exception var60) {
            }
         }

         if (!isFinalBlock && !updateConfig.stainedGlass && (state.method_26204() instanceof class_2506 || state.method_26204() instanceof class_2504)) {
            result_dest[0] = -1;
            return;
         }
      }

      int r = colour >> 16 & 255;
      g = colour >> 8 & 255;
      int b = colour & 255;
      int overlayRed;
      float minBrightness;
      float brightener;
      float currentTransparencyMultiplier;
      if (updateConfig.biomeColorsInVanilla || updateConfig.blockColors == 0) {
         overlayRed = blockTintProvider.getBiomeColor(mutableGlobalPos, state, !isFinalBlock, mapTile, tileChunk.getInRegion().getCaveLayer());
         minBrightness = (float)r / 255.0F;
         brightener = (float)g / 255.0F;
         currentTransparencyMultiplier = (float)b / 255.0F;
         r = (int)((float)(overlayRed >> 16 & 255) * minBrightness);
         g = (int)((float)(overlayRed >> 8 & 255) * brightener);
         b = (int)((float)(overlayRed & 255) * currentTransparencyMultiplier);
      }

      if (this.glowing) {
         overlayRed = r + g + b;
         minBrightness = 407.0F;
         brightener = Math.max(1.0F, minBrightness / (float)overlayRed);
         r = (int)((float)r * brightener);
         g = (int)((float)g * brightener);
         b = (int)((float)b * brightener);
         topLightValue = 15;
      }

      overlayRed = 0;
      int overlayGreen = 0;
      int overlayBlue = 0;
      currentTransparencyMultiplier = 1.0F;
      boolean legibleCaveMaps = updateConfig.legibleCaveMaps && caveStart != Integer.MAX_VALUE;
      boolean hasValidOverlay = false;
      int slopes;
      float min;
      float ambientLightColored;
      if (overlays != null && !overlays.isEmpty()) {
         int sun = 15;

         for(slopes = 0; slopes < overlays.size(); ++slopes) {
            Overlay o = (Overlay)overlays.get(slopes);
            o.getPixelColour(block, result_dest, mapWriter, world, dim, blockRegistry, tileChunk, prevChunk, prevChunkDiagonal, prevChunkHorisontal, mapTile, x, z, caveStart, caveDepth, mutableGlobalPos, biomeRegistry, dimensionTypes, shadowR, shadowG, shadowB, blockTintProvider, mapProcessor, overlayManager, updateConfig);
            if (result_dest[0] != -1) {
               hasValidOverlay = true;
               if (slopes == 0) {
                  topLightValue = o.light;
               }

               min = (float)result_dest[3] / 255.0F;
               ambientLightColored = this.getBlockBrightness((float)lightMin, o.light, sun) * min * currentTransparencyMultiplier;
               overlayRed = (int)((float)overlayRed + (float)result_dest[0] * ambientLightColored);
               overlayGreen = (int)((float)overlayGreen + (float)result_dest[1] * ambientLightColored);
               overlayBlue = (int)((float)overlayBlue + (float)result_dest[2] * ambientLightColored);
               sun -= o.getOpacity();
               if (sun < 0) {
                  sun = 0;
               }

               currentTransparencyMultiplier *= 1.0F - min;
            }
         }

         if (!legibleCaveMaps && hasValidOverlay && !this.glowing && !isAir) {
            brightnessR = brightnessG = brightnessB = this.getBlockBrightness((float)lightMin, this.light, sun);
         }
      }

      if (isFinalBlock) {
         if (block.slopeUnknown) {
            if (!isAir) {
               block.fixHeightType(x, z, mapTile, tileChunk, prevChunk, prevChunkDiagonal, prevChunkHorisontal, height, false, blockStateShortShapeCache, updateConfig);
            } else {
               block.setVerticalSlope((byte)0);
               block.setDiagonalSlope((byte)0);
               block.slopeUnknown = false;
            }
         }

         float depthBrightness = 1.0F;
         slopes = updateConfig.terrainSlopes;
         if (legibleCaveMaps) {
            topLightValue = 15;
         }

         float cos;
         if (height != 32767) {
            if (!legibleCaveMaps || isAir && !hasValidOverlay) {
               if (!isAir && !this.glowing && updateConfig.terrainDepth) {
                  if (caveStart == Integer.MAX_VALUE) {
                     depthBrightness = (float)height / 63.0F;
                  } else if (caveStart == Integer.MIN_VALUE) {
                     depthBrightness = 0.7F + 0.3F * (float)height / (float)dim.getDimensionType(dimensionTypes).comp_653();
                  } else {
                     int caveBottom = caveStart - caveDepth;
                     depthBrightness = 0.7F + 0.3F * (float)(height - caveBottom) / (float)caveDepth;
                  }

                  float max = slopes >= 2 ? 1.0F : 1.15F;
                  min = slopes >= 2 ? 0.9F : 0.7F;
                  if (depthBrightness > max) {
                     depthBrightness = max;
                  } else if (depthBrightness < min) {
                     depthBrightness = min;
                  }
               }
            } else {
               int depthCalculationBase = 0;
               int depthCalculationHeight = height;
               int depthCalculationBottom = caveStart + 1 - caveDepth;
               int depthCalculationTop = caveStart;
               int caveRange;
               if (caveStart == Integer.MIN_VALUE) {
                  depthCalculationBottom = 0;
                  depthCalculationTop = 63;
                  caveRange = height >> 6 & 1;
                  depthCalculationHeight = 63 * caveRange + (1 - 2 * caveRange) * (height & 63);
                  depthCalculationBase = 16;
               }

               caveRange = 1 + depthCalculationTop - depthCalculationBottom;
               if (!isAir && !this.glowing) {
                  cos = (1.0F + (float)depthCalculationBase + (float)depthCalculationHeight - (float)depthCalculationBottom) / (float)(depthCalculationBase + caveRange);
                  brightnessR *= cos;
                  brightnessG *= cos;
                  brightnessB *= cos;
               }

               if (hasValidOverlay) {
                  depthCalculationHeight = topHeight;
                  if (caveStart == Integer.MIN_VALUE) {
                     int odd = topHeight >> 6 & 1;
                     depthCalculationHeight = 63 * odd + (1 - 2 * odd) * (topHeight & 63);
                  }

                  cos = (1.0F + (float)depthCalculationBase + (float)depthCalculationHeight - (float)depthCalculationBottom) / (float)(depthCalculationBase + caveRange);
                  overlayRed = (int)((float)overlayRed * cos);
                  overlayGreen = (int)((float)overlayGreen * cos);
                  overlayBlue = (int)((float)overlayBlue * cos);
               }
            }
         }

         if (!isAir && slopes > 0 && !block.slopeUnknown) {
            int verticalSlope = block.getVerticalSlope();
            if (slopes == 1) {
               if (verticalSlope > 0) {
                  depthBrightness = (float)((double)depthBrightness * 1.15D);
               } else if (verticalSlope < 0) {
                  depthBrightness = (float)((double)depthBrightness * 0.85D);
               }
            } else {
               int diagonalSlope = block.getDiagonalSlope();
               ambientLightColored = 0.2F;
               float ambientLightWhite = 0.5F;
               float maxDirectLight = 0.6666667F;
               if (this.glowing) {
                  ambientLightColored = 0.0F;
                  ambientLightWhite = 1.0F;
                  maxDirectLight = 0.22222224F;
               }

               cos = 0.0F;
               float whiteLight;
               float directLightClamped;
               if (slopes == 2) {
                  directLightClamped = (float)(-verticalSlope);
                  if (directLightClamped < 1.0F) {
                     if (verticalSlope == 1 && diagonalSlope == 1) {
                        cos = 1.0F;
                     } else {
                        whiteLight = (float)(verticalSlope - diagonalSlope);
                        float cast = 1.0F - directLightClamped;
                        float crossMagnitude = (float)Math.sqrt((double)(whiteLight * whiteLight + 1.0F + directLightClamped * directLightClamped));
                        cos = (float)((double)(cast / crossMagnitude) / Math.sqrt(2.0D));
                     }
                  }
               } else if (verticalSlope >= 0) {
                  if (verticalSlope == 1) {
                     cos = 1.0F;
                  } else {
                     directLightClamped = (float)Math.sqrt((double)(verticalSlope * verticalSlope + 1));
                     whiteLight = (float)(verticalSlope + 1);
                     cos = (float)((double)(whiteLight / directLightClamped) / Math.sqrt(2.0D));
                  }
               }

               directLightClamped = 0.0F;
               if (cos == 1.0F) {
                  directLightClamped = maxDirectLight;
               } else if (cos > 0.0F) {
                  directLightClamped = (float)Math.ceil((double)(cos * 10.0F)) / 10.0F * maxDirectLight * 0.88388F;
               }

               whiteLight = ambientLightWhite + directLightClamped;
               brightnessR *= shadowR * ambientLightColored + whiteLight;
               brightnessG *= shadowG * ambientLightColored + whiteLight;
               brightnessB *= shadowB * ambientLightColored + whiteLight;
            }
         }

         brightnessR *= depthBrightness;
         brightnessG *= depthBrightness;
         brightnessB *= depthBrightness;
         result_dest[3] = (int)(this.getPixelLight((float)lightMin, topLightValue) * 255.0F);
      } else {
         result_dest[3] = colour >> 24 & 255;
         if (result_dest[3] == 0) {
            result_dest[3] = this.getVanillaTransparency(state.method_26204());
         }
      }

      result_dest[0] = (int)((float)r * brightnessR * currentTransparencyMultiplier + (float)overlayRed);
      if (result_dest[0] > 255) {
         result_dest[0] = 255;
      }

      result_dest[1] = (int)((float)g * brightnessG * currentTransparencyMultiplier + (float)overlayGreen);
      if (result_dest[1] > 255) {
         result_dest[1] = 255;
      }

      result_dest[2] = (int)((float)b * brightnessB * currentTransparencyMultiplier + (float)overlayBlue);
      if (result_dest[2] > 255) {
         result_dest[2] = 255;
      }

   }

   public float getBlockBrightness(float min, int l, int sun) {
      return (min + (float)Math.max(sun, l)) / (15.0F + min);
   }

   private float getPixelLight(float min, int topLightValue) {
      return topLightValue == 0 ? 0.0F : this.getBlockBrightness(min, topLightValue, 0);
   }

   public class_2680 getState() {
      return this.state;
   }

   public void setState(class_2680 state) {
      this.state = state;
   }

   public void setLight(byte light) {
      this.light = light;
   }

   public void setGlowing(boolean glowing) {
      this.glowing = glowing;
   }
}
