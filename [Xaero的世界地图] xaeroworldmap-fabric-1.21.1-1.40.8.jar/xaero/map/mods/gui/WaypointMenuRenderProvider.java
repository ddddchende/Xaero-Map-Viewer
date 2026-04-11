package xaero.map.mods.gui;

import java.util.ArrayList;
import java.util.Iterator;
import xaero.map.element.MapElementRenderProvider;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.mods.SupportXaeroMinimap;

public class WaypointMenuRenderProvider extends MapElementRenderProvider<Waypoint, WaypointMenuRenderContext> {
   private final SupportXaeroMinimap minimap;
   private Iterator<Waypoint> iterator;

   public WaypointMenuRenderProvider(SupportXaeroMinimap minimap) {
      this.minimap = minimap;
   }

   public void begin(ElementRenderLocation location, WaypointMenuRenderContext context) {
      ArrayList<Waypoint> sortedList = this.minimap.getWaypointsSorted();
      if (sortedList == null) {
         this.iterator = null;
      } else {
         this.iterator = this.minimap.getWaypointsSorted().iterator();
      }

   }

   public boolean hasNext(ElementRenderLocation location, WaypointMenuRenderContext context) {
      return this.iterator != null && this.iterator.hasNext();
   }

   public Waypoint getNext(ElementRenderLocation location, WaypointMenuRenderContext context) {
      return (Waypoint)this.iterator.next();
   }

   public void end(ElementRenderLocation location, WaypointMenuRenderContext context) {
   }

   /** @deprecated */
   @Deprecated
   public void begin(int location, WaypointMenuRenderContext context) {
      this.begin(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public boolean hasNext(int location, WaypointMenuRenderContext context) {
      return this.hasNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public Waypoint getNext(int location, WaypointMenuRenderContext context) {
      return this.getNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public Waypoint setupContextAndGetNext(ElementRenderLocation location, WaypointMenuRenderContext context) {
      return this.getNext(location, context);
   }

   /** @deprecated */
   @Deprecated
   public Waypoint setupContextAndGetNext(int location, WaypointMenuRenderContext context) {
      return this.setupContextAndGetNext(ElementRenderLocation.fromIndex(location), context);
   }

   /** @deprecated */
   @Deprecated
   public void end(int location, WaypointMenuRenderContext context) {
      this.end(ElementRenderLocation.fromIndex(location), context);
   }
}
