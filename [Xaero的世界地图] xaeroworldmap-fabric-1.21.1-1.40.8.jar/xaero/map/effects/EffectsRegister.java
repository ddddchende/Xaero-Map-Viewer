package xaero.map.effects;

import java.util.function.Function;
import net.minecraft.class_1291;
import net.minecraft.class_6880;

public class EffectsRegister {
   public void registerEffects(Function<WorldMapStatusEffect, class_6880<class_1291>> registry) {
      Effects.init();
      Effects.NO_WORLD_MAP = (class_6880)registry.apply(Effects.NO_WORLD_MAP_UNHELD);
      Effects.NO_WORLD_MAP_HARMFUL = (class_6880)registry.apply(Effects.NO_WORLD_MAP_HARMFUL_UNHELD);
      Effects.NO_CAVE_MAPS = (class_6880)registry.apply(Effects.NO_CAVE_MAPS_UNHELD);
      Effects.NO_CAVE_MAPS_HARMFUL = (class_6880)registry.apply(Effects.NO_CAVE_MAPS_HARMFUL_UNHELD);
   }
}
