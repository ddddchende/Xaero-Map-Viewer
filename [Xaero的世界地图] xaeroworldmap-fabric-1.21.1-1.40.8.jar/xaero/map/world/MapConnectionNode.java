package xaero.map.world;

import net.minecraft.class_1937;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_7924;

public class MapConnectionNode {
   private final class_5321<class_1937> dimId;
   private final String mw;
   private String cachedString;

   public MapConnectionNode(class_5321<class_1937> dimId, String mw) {
      this.dimId = dimId;
      this.mw = mw;
   }

   public String toString() {
      if (this.cachedString == null) {
         String var10001 = this.dimId.method_29177().toString().replace(':', '$');
         this.cachedString = var10001 + "/" + this.mw;
      }

      return this.cachedString;
   }

   public String getNamedString(MapWorld mapWorld) {
      MapDimension dim = mapWorld.getDimension(this.dimId);
      String var10000 = String.valueOf(this.dimId.method_29177());
      return var10000 + "/" + dim.getMultiworldName(this.mw);
   }

   public static MapConnectionNode fromString(String s) {
      int dividerIndex = s.lastIndexOf(47);
      if (dividerIndex == -1) {
         return null;
      } else {
         String dimString = s.substring(0, dividerIndex);

         class_2960 dimLocation;
         try {
            if (dimString.equals("0")) {
               dimLocation = class_1937.field_25179.method_29177();
            } else if (dimString.equals("-1")) {
               dimLocation = class_1937.field_25180.method_29177();
            } else if (dimString.equals("1")) {
               dimLocation = class_1937.field_25181.method_29177();
            } else {
               dimLocation = class_2960.method_60654(dimString.replace('$', ':'));
            }
         } catch (Throwable var5) {
            return null;
         }

         String mwString = s.substring(dividerIndex + 1);
         return new MapConnectionNode(class_5321.method_29179(class_7924.field_41223, dimLocation), mwString);
      }
   }

   public boolean equals(Object another) {
      if (this == another) {
         return true;
      } else if (another != null && another instanceof MapConnectionNode) {
         MapConnectionNode anotherNode = (MapConnectionNode)another;
         return this.dimId.equals(anotherNode.dimId) && this.mw.equals(anotherNode.mw);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.toString().hashCode();
   }

   public class_5321<class_1937> getDimId() {
      return this.dimId;
   }

   public String getMw() {
      return this.mw;
   }
}
