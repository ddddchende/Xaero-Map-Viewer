package xaero.map.cache;

import java.util.Set;
import net.minecraft.class_2680;

public class BrokenBlockTintCache {
   private final Set<class_2680> brokenStates;

   public BrokenBlockTintCache(Set<class_2680> brokenStates) {
      this.brokenStates = brokenStates;
   }

   public void setBroken(class_2680 state) {
      this.brokenStates.add(state);
   }

   public boolean isBroken(class_2680 state) {
      return this.brokenStates.contains(state);
   }

   public int getSize() {
      return this.brokenStates.size();
   }
}
