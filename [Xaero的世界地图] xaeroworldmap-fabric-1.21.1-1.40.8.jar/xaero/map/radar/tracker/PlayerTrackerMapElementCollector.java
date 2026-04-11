package xaero.map.radar.tracker;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import xaero.map.radar.tracker.system.IPlayerTrackerSystem;
import xaero.map.radar.tracker.system.ITrackedPlayerReader;
import xaero.map.radar.tracker.system.PlayerTrackerSystemManager;

public class PlayerTrackerMapElementCollector {
   private Map<UUID, PlayerTrackerMapElement<?>> elements = new HashMap();
   private final PlayerTrackerSystemManager systemManager;
   private final Runnable onElementsChange;

   public PlayerTrackerMapElementCollector(PlayerTrackerSystemManager systemManager, Runnable onElementsChange) {
      this.systemManager = systemManager;
      this.onElementsChange = onElementsChange;
   }

   public void update(class_310 mc) {
      if (this.elements == null) {
         this.elements = new HashMap();
      }

      Map<UUID, PlayerTrackerMapElement<?>> updatedMap = new HashMap();
      boolean hasNewPlayer = false;

      IPlayerTrackerSystem system;
      for(Iterator var4 = this.systemManager.getSystems().iterator(); var4.hasNext(); hasNewPlayer = this.updateForSystem(system, updatedMap, this.elements) || hasNewPlayer) {
         system = (IPlayerTrackerSystem)var4.next();
      }

      if (hasNewPlayer || updatedMap.size() != this.elements.size()) {
         this.elements = updatedMap;
         this.onElementsChange.run();
      }

   }

   private <P> boolean updateForSystem(IPlayerTrackerSystem<P> system, Map<UUID, PlayerTrackerMapElement<?>> destination, Map<UUID, PlayerTrackerMapElement<?>> current) {
      Iterator<P> playerIterator = system.getTrackedPlayerIterator();
      if (playerIterator == null) {
         return false;
      } else {
         ITrackedPlayerReader<P> reader = system.getReader();
         boolean hasNewPlayer = false;

         while(true) {
            Object player;
            UUID playerId;
            PlayerTrackerMapElement element;
            do {
               if (!playerIterator.hasNext()) {
                  return hasNewPlayer;
               }

               player = playerIterator.next();
               playerId = reader.getId(player);
               element = (PlayerTrackerMapElement)current.get(playerId);
            } while(destination.containsKey(playerId));

            if (element == null || element.getPlayer() != player) {
               element = new PlayerTrackerMapElement(player, system);
               hasNewPlayer = true;
            }

            destination.put(element.getPlayerId(), element);
         }
      }
   }

   public boolean playerExists(UUID id) {
      return this.elements != null && this.elements.containsKey(id);
   }

   public Iterable<PlayerTrackerMapElement<?>> getElements() {
      return this.elements.values();
   }

   public void resetRenderedOnRadarFlags() {
      Iterator var1 = this.elements.values().iterator();

      while(var1.hasNext()) {
         PlayerTrackerMapElement<?> e = (PlayerTrackerMapElement)var1.next();
         e.setRenderedOnRadar(false);
      }

   }

   public void confirmPlayerRadarRender(class_1657 p) {
      ((PlayerTrackerMapElement)this.elements.get(p.method_5667())).setRenderedOnRadar(true);
   }
}
