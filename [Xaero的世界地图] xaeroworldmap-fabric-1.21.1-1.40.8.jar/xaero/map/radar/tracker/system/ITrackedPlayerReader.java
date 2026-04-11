package xaero.map.radar.tracker.system;

import java.util.UUID;
import net.minecraft.class_1937;
import net.minecraft.class_5321;

public interface ITrackedPlayerReader<P> {
   UUID getId(P var1);

   double getX(P var1);

   double getY(P var1);

   double getZ(P var1);

   class_5321<class_1937> getDimension(P var1);
}
