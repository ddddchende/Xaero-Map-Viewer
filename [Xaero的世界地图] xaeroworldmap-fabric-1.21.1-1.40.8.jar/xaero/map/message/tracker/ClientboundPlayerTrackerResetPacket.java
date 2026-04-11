package xaero.map.message.tracker;

import java.util.function.Consumer;
import net.minecraft.class_2540;
import xaero.map.WorldMapSession;

public class ClientboundPlayerTrackerResetPacket {
   public void write(class_2540 buffer) {
   }

   public static ClientboundPlayerTrackerResetPacket read(class_2540 buffer) {
      return new ClientboundPlayerTrackerResetPacket();
   }

   public static class Handler implements Consumer<ClientboundPlayerTrackerResetPacket> {
      public void accept(ClientboundPlayerTrackerResetPacket t) {
         WorldMapSession session = WorldMapSession.getCurrentSession();
         if (session != null) {
            session.getMapProcessor().getClientSyncedTrackedPlayerManager().reset();
         }
      }
   }
}
