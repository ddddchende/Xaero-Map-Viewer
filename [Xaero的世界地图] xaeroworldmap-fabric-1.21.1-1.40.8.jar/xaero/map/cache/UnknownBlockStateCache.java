package xaero.map.cache;

import java.util.HashMap;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2487;
import net.minecraft.class_2512;
import net.minecraft.class_2680;
import net.minecraft.class_7225;
import xaero.map.region.state.UnknownBlockState;

public class UnknownBlockStateCache {
   private HashMap<String, class_2680> unknownBlockStates = new HashMap();

   public class_2680 getBlockStateFromNBT(class_7225<class_2248> blockLookup, class_2487 nbt) {
      Object blockState;
      try {
         blockState = class_2512.method_10681(blockLookup, nbt);
      } catch (IllegalArgumentException var5) {
         blockState = class_2246.field_10124.method_9564();
      }

      if (!nbt.method_10558("Name").equals("minecraft:air") && ((class_2680)blockState).method_26204() == class_2246.field_10124) {
         String nbtString = nbt.toString();
         blockState = (class_2680)this.unknownBlockStates.get(nbtString);
         if (blockState == null) {
            blockState = new UnknownBlockState(nbt);
            this.unknownBlockStates.put(nbtString, blockState);
         }
      }

      return (class_2680)blockState;
   }
}
