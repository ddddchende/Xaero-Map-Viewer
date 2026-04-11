package xaero.map.biome;

import net.minecraft.class_1920;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2378;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3568;
import net.minecraft.class_3610;
import net.minecraft.class_6539;
import net.minecraft.class_2338.class_2339;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.WorldMap;
import xaero.map.cache.BrokenBlockTintCache;
import xaero.map.region.MapTile;

public class BlockTintProvider implements class_1920 {
   private final class_2378<class_1959> biomeRegistry;
   private final BiomeColorCalculator calculator;
   private final class_2339 mutablePos;
   private final MapProcessor mapProcessor;
   private final BrokenBlockTintCache brokenBlockTintCache;
   private final MapWriter mapWriter;
   private class_2680 state;
   private boolean overlay;
   private MapTile tile;
   private int caveLayer;

   public BlockTintProvider(class_2378<class_1959> biomeRegistry, BiomeColorCalculator calculator, MapProcessor mapProcessor, BrokenBlockTintCache brokenBlockTintCache, MapWriter mapWriter) {
      this.biomeRegistry = biomeRegistry;
      this.calculator = calculator;
      this.mutablePos = new class_2339();
      this.mapProcessor = mapProcessor;
      this.brokenBlockTintCache = brokenBlockTintCache;
      this.mapWriter = mapWriter;
   }

   public int method_31605() {
      return 16;
   }

   public int method_31607() {
      return this.mutablePos.method_10264() >> 4 << 4;
   }

   public class_2586 method_8321(class_2338 blockPos) {
      return null;
   }

   public class_2680 method_8320(class_2338 blockPos) {
      return this.state;
   }

   public class_3610 method_8316(class_2338 blockPos) {
      return this.state == null ? null : this.state.method_26227();
   }

   public float method_24852(class_2350 direction, boolean bl) {
      return 1.0F;
   }

   public class_3568 method_22336() {
      return null;
   }

   public int method_23752(class_2338 blockPos, class_6539 colorResolver) {
      this.mutablePos.method_10101(blockPos);
      return this.calculator.getBiomeColor(colorResolver, this.overlay, this.mutablePos, this.tile, this.caveLayer, this.biomeRegistry, this.mapProcessor);
   }

   public int getBiomeColor(class_2338 blockPos, class_2680 state, boolean overlay, MapTile tile, int caveLayer) {
      if (this.brokenBlockTintCache.isBroken(state)) {
         return -1;
      } else {
         this.mutablePos.method_10101(blockPos);
         this.state = state;
         this.overlay = overlay;
         this.tile = tile;
         this.caveLayer = caveLayer;

         try {
            return class_310.method_1551().method_1505().method_1697(state, this, blockPos, this.mapWriter.getBlockTintIndex(state));
         } catch (Throwable var7) {
            this.brokenBlockTintCache.setBroken(state);
            WorldMap.LOGGER.error("Error calculating block tint for block state " + String.valueOf(state) + "! Total: " + this.brokenBlockTintCache.getSize(), var7);
            return -1;
         }
      }
   }
}
