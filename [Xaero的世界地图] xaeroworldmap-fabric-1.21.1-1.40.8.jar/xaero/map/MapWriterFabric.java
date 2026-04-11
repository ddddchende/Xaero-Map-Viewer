package xaero.map;

import java.util.List;
import net.minecraft.class_1058;
import net.minecraft.class_1087;
import net.minecraft.class_1921;
import net.minecraft.class_2350;
import net.minecraft.class_2680;
import net.minecraft.class_4696;
import net.minecraft.class_773;
import net.minecraft.class_777;
import xaero.map.biome.BiomeGetter;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.region.OverlayManager;

public class MapWriterFabric extends MapWriter {
   public MapWriterFabric(OverlayManager overlayManager, BlockStateShortShapeCache blockStateShortShapeCache, BiomeGetter biomeGetter) {
      super(overlayManager, blockStateShortShapeCache, biomeGetter);
   }

   protected boolean blockStateHasTranslucentRenderType(class_2680 blockState) {
      return class_4696.method_23679(blockState) == class_1921.method_23583();
   }

   protected List<class_777> getQuads(class_1087 model, class_2680 state, class_2350 direction) {
      return model.method_4707(state, direction, this.usedRandom);
   }

   protected class_1058 getParticleIcon(class_773 bms, class_1087 model, class_2680 state) {
      return bms.method_3339(state);
   }
}
