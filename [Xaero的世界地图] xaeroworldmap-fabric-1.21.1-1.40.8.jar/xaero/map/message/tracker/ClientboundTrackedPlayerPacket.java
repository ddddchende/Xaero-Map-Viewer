package xaero.map.message.tracker;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.class_2487;
import net.minecraft.class_2505;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_7924;
import xaero.map.WorldMapSession;

public class ClientboundTrackedPlayerPacket {
   private final boolean remove;
   private final UUID id;
   private final double x;
   private final double y;
   private final double z;
   private final class_2960 dimension;

   public ClientboundTrackedPlayerPacket(boolean remove, UUID id, double x, double y, double z, class_2960 dimension) {
      this.remove = remove;
      this.id = id;
      this.x = x;
      this.y = y;
      this.z = z;
      this.dimension = dimension;
   }

   public void write(class_2540 buffer) {
      class_2487 nbt = new class_2487();
      nbt.method_10556("r", this.remove);
      nbt.method_25927("i", this.id);
      if (!this.remove) {
         nbt.method_10549("x", this.x);
         nbt.method_10549("y", this.y);
         nbt.method_10549("z", this.z);
         nbt.method_10582("d", this.dimension.toString());
      }

      buffer.method_10794(nbt);
   }

   public static ClientboundTrackedPlayerPacket read(class_2540 buffer) {
      class_2487 nbt = (class_2487)buffer.method_30616(class_2505.method_53898());
      boolean remove = nbt.method_10577("r");
      UUID id = nbt.method_25926("i");
      double x = remove ? 0.0D : nbt.method_10574("x");
      double y = remove ? 0.0D : nbt.method_10574("y");
      double z = remove ? 0.0D : nbt.method_10574("z");
      String dimensionString = remove ? null : nbt.method_10558("d");
      class_2960 dimension = dimensionString == null ? null : class_2960.method_60654(dimensionString);
      return new ClientboundTrackedPlayerPacket(remove, id, x, y, z, dimension);
   }

   public static class Handler implements Consumer<ClientboundTrackedPlayerPacket> {
      public void accept(ClientboundTrackedPlayerPacket t) {
         WorldMapSession session = WorldMapSession.getCurrentSession();
         if (session != null) {
            if (t.remove) {
               session.getMapProcessor().getClientSyncedTrackedPlayerManager().remove(t.id);
            } else {
               session.getMapProcessor().getClientSyncedTrackedPlayerManager().update(t.id, t.x, t.y, t.z, class_5321.method_29179(class_7924.field_41223, t.dimension));
            }
         }
      }
   }
}
