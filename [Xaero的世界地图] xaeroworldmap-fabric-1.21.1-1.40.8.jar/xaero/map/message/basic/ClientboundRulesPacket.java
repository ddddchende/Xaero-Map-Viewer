package xaero.map.message.basic;

import java.util.function.Consumer;
import net.minecraft.class_2487;
import net.minecraft.class_2540;
import xaero.map.mcworld.WorldMapClientWorldDataHelper;

public class ClientboundRulesPacket {
   public final boolean allowCaveModeOnServer;
   public final boolean allowNetherCaveModeOnServer;

   public ClientboundRulesPacket(boolean allowCaveModeOnServer, boolean allowNetherCaveModeOnServer) {
      this.allowCaveModeOnServer = allowCaveModeOnServer;
      this.allowNetherCaveModeOnServer = allowNetherCaveModeOnServer;
   }

   public void write(class_2540 u) {
      class_2487 nbt = new class_2487();
      nbt.method_10556("cm", this.allowCaveModeOnServer);
      nbt.method_10556("ncm", this.allowNetherCaveModeOnServer);
      u.method_10794(nbt);
   }

   public static ClientboundRulesPacket read(class_2540 buffer) {
      class_2487 nbt = buffer.method_10798();
      return new ClientboundRulesPacket(nbt.method_10577("cm"), nbt.method_10577("ncm"));
   }

   public static class ClientHandler implements Consumer<ClientboundRulesPacket> {
      public void accept(ClientboundRulesPacket message) {
         WorldMapClientWorldDataHelper.getCurrentWorldData().setSyncedRules(message);
      }
   }
}
