package xaero.map.radar.tracker.system;

import java.util.HashMap;
import java.util.Map;
import xaero.map.WorldMap;

public class PlayerTrackerSystemManager {
   private final Map<String, IPlayerTrackerSystem<?>> systems = new HashMap();

   public void register(String name, IPlayerTrackerSystem<?> system) {
      if (this.systems.containsKey(name)) {
         WorldMap.LOGGER.error("Player tracker system with the name " + name + " has already been registered!");
      } else {
         this.systems.put(name, system);
         WorldMap.LOGGER.info("Registered player tracker system: " + name);
      }
   }

   public Iterable<IPlayerTrackerSystem<?>> getSystems() {
      return this.systems.values();
   }
}
