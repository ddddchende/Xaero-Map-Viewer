package xaero.map.misc;

import net.minecraft.class_4587;
import net.minecraft.class_4587.class_4665;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class OptimizedMath {
   public static final Vector3f XP = new Vector3f(1.0F, 0.0F, 0.0F);
   public static final Vector3f YP = new Vector3f(0.0F, 1.0F, 0.0F);
   public static final Vector3f ZP = new Vector3f(0.0F, 0.0F, 1.0F);

   public static int myFloor(double d) {
      int asInt = (int)d;
      if ((double)asInt != d && d < 0.0D) {
         --asInt;
      }

      return asInt;
   }

   public static void rotatePose(class_4587 poseStack, float degrees, Vector3fc vector) {
      class_4665 pose = poseStack.method_23760();
      pose.method_23761().rotate(degrees * 0.017453292F, vector);
      pose.method_23762().rotate(degrees * 0.017453292F, vector);
   }
}
