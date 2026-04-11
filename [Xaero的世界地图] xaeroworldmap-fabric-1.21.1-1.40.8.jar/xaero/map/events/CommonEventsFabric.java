package xaero.map.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class CommonEventsFabric extends CommonEvents {
   public void register() {
      ServerPlayerEvents.COPY_FROM.register(this::onPlayerClone);
      ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarting);
      ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerStopped);
   }
}
