package xaero.map.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xaero.map.server.events.ServerEvents;

public abstract class WorldMapServer {
   public static Logger LOGGER = LogManager.getLogger();
   protected ServerEvents serverEvents;

   public void load() {
      LOGGER.info("Loading Xaero's World Map - Stage 1/2 (Server)");
   }

   public void loadLater() {
      LOGGER.info("Loading Xaero's World Map - Stage 2/2 (Server)");
   }

   public ServerEvents getServerEvents() {
      return this.serverEvents;
   }
}
