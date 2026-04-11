package xaero.map.biome;

import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_6880;
import net.minecraft.class_7924;

public class BiomeGetter {
   public final class_5321<class_1959> PLACEHOLDER_BIOME;
   public final class_5321<class_1959> UNKNOWN_BIOME;

   public BiomeGetter() {
      this.UNKNOWN_BIOME = class_5321.method_29179(class_7924.field_41236, class_2960.method_60654("xaeroworldmap:unknown_biome"));
      this.PLACEHOLDER_BIOME = class_5321.method_29179(class_7924.field_41236, class_2960.method_60654("xaeroworldmap:placeholder_biome"));
   }

   public class_5321<class_1959> getBiome(class_1937 world, class_2338 pos, class_2378<class_1959> biomeRegistry) {
      class_6880<class_1959> biomeHolder = world.method_23753(pos);
      class_1959 biome = biomeHolder == null ? null : (class_1959)biomeHolder.comp_349();
      return (class_5321)biomeRegistry.method_29113(biome).orElse(this.UNKNOWN_BIOME);
   }
}
