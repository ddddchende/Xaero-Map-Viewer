package xaero.map.entity.util;

import net.minecraft.class_1297;
import net.minecraft.class_243;

public class EntityUtil {
   public static double getEntityX(class_1297 e, float partial) {
      double xOld = e.field_6012 > 0 ? e.field_6038 : e.method_23317();
      return xOld + (e.method_23317() - xOld) * (double)partial;
   }

   public static double getEntityY(class_1297 e, float partial) {
      double yOld = e.field_6012 > 0 ? e.field_5971 : e.method_23318();
      return yOld + (e.method_23318() - yOld) * (double)partial;
   }

   public static double getEntityZ(class_1297 e, float partial) {
      double zOld = e.field_6012 > 0 ? e.field_5989 : e.method_23321();
      return zOld + (e.method_23321() - zOld) * (double)partial;
   }

   public static class_243 getEntityPos(class_1297 e, float partial) {
      return new class_243(getEntityX(e, partial), getEntityY(e, partial), getEntityZ(e, partial));
   }
}
