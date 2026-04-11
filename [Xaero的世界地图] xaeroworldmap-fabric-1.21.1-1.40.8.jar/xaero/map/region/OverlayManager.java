package xaero.map.region;

import java.util.HashMap;
import net.minecraft.class_2680;

public class OverlayManager {
   private HashMap<class_2680, HashMap<Byte, HashMap<Short, Overlay>>> overlayMap = new HashMap();
   private int numberOfUniques = 0;
   private Object[] keyHolder = new Object[5];

   public synchronized Overlay getOriginal(Overlay o) {
      o.fillManagerKeyHolder(this.keyHolder);
      return this.getOriginal(this.overlayMap, o, 0);
   }

   private Overlay getOriginal(HashMap map, Overlay o, int index) {
      Object byKey = map.get(this.keyHolder[index]);
      if (index == this.keyHolder.length - 1) {
         if (byKey == null) {
            ++this.numberOfUniques;
            map.put(this.keyHolder[index], o);
            return o;
         } else {
            return (Overlay)byKey;
         }
      } else {
         if (byKey == null) {
            byKey = new HashMap();
            map.put(this.keyHolder[index], byKey);
         }

         HashMap var10001 = (HashMap)byKey;
         ++index;
         return this.getOriginal(var10001, o, index);
      }
   }

   public int getNumberOfUniqueOverlays() {
      return this.numberOfUniques;
   }
}
