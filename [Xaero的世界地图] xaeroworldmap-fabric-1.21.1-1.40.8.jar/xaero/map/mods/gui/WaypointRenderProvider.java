package xaero.map.mods.gui;

import java.util.Iterator;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.element.MapElementRenderProvider;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.mods.SupportXaeroMinimap;

public class WaypointRenderProvider extends MapElementRenderProvider<Waypoint, WaypointRenderContext> {
   private final SupportXaeroMinimap minimap;
   private Iterator<Waypoint> iterator;

   public WaypointRenderProvider(SupportXaeroMinimap minimap) {
      this.minimap = minimap;
   }

   public void begin(ElementRenderLocation location, WaypointRenderContext context) {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.WAYPOINTS) && this.minimap.getWaypoints() != null) {
         this.iterator = this.minimap.getWaypoints().iterator();
         context.worldmapWaypointsScale = (float)(Double)configManager.getEffective(WorldMapProfiledConfigOptions.WAYPOINT_SCALE);
         context.waypointBackgrounds = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.WAYPOINT_BACKGROUNDS);
         SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
         context.showDisabledWaypoints = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DISPLAY_DISABLED_WAYPOINTS);
      } else {
         this.iterator = null;
      }
   }

   public boolean hasNext(ElementRenderLocation location, WaypointRenderContext context) {
      return this.iterator != null && this.iterator.hasNext();
   }

   public Waypoint getNext(ElementRenderLocation location, WaypointRenderContext context) {
      return (Waypoint)this.iterator.next();
   }

   public void end(ElementRenderLocation location, WaypointRenderContext context) {
   }

   /** @deprecated */
   @Deprecated
   public void begin(int location, WaypointRenderContext context) {
      this.begin(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public boolean hasNext(int location, WaypointRenderContext context) {
      return this.hasNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public Waypoint getNext(int location, WaypointRenderContext context) {
      return this.getNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public Waypoint setupContextAndGetNext(ElementRenderLocation location, WaypointRenderContext context) {
      return this.getNext(location, context);
   }

   /** @deprecated */
   @Deprecated
   public Waypoint setupContextAndGetNext(int location, WaypointRenderContext context) {
      return this.setupContextAndGetNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public void end(int location, WaypointRenderContext context) {
      this.end(ElementRenderLocation.fromIndex(location), context);
   }
}
