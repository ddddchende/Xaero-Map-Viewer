package xaero.map.mods.gui;

import java.util.ArrayList;
import net.minecraft.class_310;
import net.minecraft.class_437;
import xaero.lib.client.config.ClientConfigManager;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.element.MapElementReader;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.SupportMods;

public class WaypointReader extends MapElementReader<Waypoint, WaypointRenderContext, WaypointRenderer> {
   public boolean waypointIsGood(Waypoint w, WaypointRenderContext context) {
      return (w.getType() != 1 && w.getType() != 2 || context.deathpoints) && (w.isGlobal() || context.userScale >= context.minZoomForLocalWaypoints);
   }

   public boolean isHidden(Waypoint element, WaypointRenderContext context) {
      return !this.waypointIsGood(element, context) || !context.showDisabledWaypoints && element.isDisabled();
   }

   /** @deprecated */
   @Deprecated
   public boolean isInteractable(int location, Waypoint element) {
      return this.isInteractable(ElementRenderLocation.fromIndex(location), element);
   }

   public boolean isInteractable(ElementRenderLocation location, Waypoint element) {
      return true;
   }

   /** @deprecated */
   @Deprecated
   public float getBoxScale(int location, Waypoint element, WaypointRenderContext context) {
      return this.getBoxScale(ElementRenderLocation.fromIndex(location), element, context);
   }

   public float getBoxScale(ElementRenderLocation location, Waypoint element, WaypointRenderContext context) {
      return context.worldmapWaypointsScale;
   }

   public double getRenderX(Waypoint element, WaypointRenderContext context, float partialTicks) {
      return element.getRenderX();
   }

   public double getRenderZ(Waypoint element, WaypointRenderContext context, float partialTicks) {
      return element.getRenderZ();
   }

   public int getInteractionBoxLeft(Waypoint element, WaypointRenderContext context, float partialTicks) {
      return -this.getInteractionBoxRight(element, context, partialTicks);
   }

   public int getInteractionBoxRight(Waypoint element, WaypointRenderContext context, float partialTicks) {
      return element.getSymbol().length() > 1 ? 21 : 14;
   }

   public int getInteractionBoxTop(Waypoint element, WaypointRenderContext context, float partialTicks) {
      return context.waypointBackgrounds ? -41 : -12;
   }

   public int getInteractionBoxBottom(Waypoint element, WaypointRenderContext context, float partialTicks) {
      return context.waypointBackgrounds ? 0 : 12;
   }

   public int getLeftSideLength(Waypoint element, class_310 mc) {
      return 9 + element.getCachedNameLength();
   }

   public String getMenuName(Waypoint element) {
      String name = element.getName();
      if (element.isGlobal()) {
         name = "* " + name;
      }

      return name;
   }

   public int getMenuTextFillLeftPadding(Waypoint element) {
      return (element.isDisabled() ? 11 : 0) + (element.isTemporary() ? 10 : 0);
   }

   public String getFilterName(Waypoint element) {
      String var10000 = this.getMenuName(element);
      return var10000 + " " + element.getSymbol();
   }

   public ArrayList<RightClickOption> getRightClickOptions(Waypoint element, IRightClickableElement target) {
      ArrayList<RightClickOption> rightClickOptions = new ArrayList();
      rightClickOptions.add(new RightClickOption(this, element.getName(), rightClickOptions.size(), target) {
         public void onAction(class_437 screen) {
            SupportMods.xaeroMinimap.openWaypoint((GuiMap)screen, element);
         }
      });
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.COORDINATES) && !SupportMods.xaeroMinimap.hidingWaypointCoordinates()) {
         rightClickOptions.add(new RightClickOption(this, String.format("X: %d, Y: %s, Z: %d", element.getX(), element.isyIncluded() ? element.getY().makeConcatWithConstants<invokedynamic>(element.getY()) : "~", element.getZ()), rightClickOptions.size(), target) {
            public void onAction(class_437 screen) {
               SupportMods.xaeroMinimap.openWaypoint((GuiMap)screen, element);
            }
         });
      }

      rightClickOptions.add((new RightClickOption(this, "gui.xaero_right_click_waypoint_edit", rightClickOptions.size(), target) {
         public void onAction(class_437 screen) {
            SupportMods.xaeroMinimap.openWaypoint((GuiMap)screen, element);
         }
      }).setNameFormatArgs(new Object[]{"E"}));
      rightClickOptions.add((new RightClickOption(this, "gui.xaero_right_click_waypoint_teleport", rightClickOptions.size(), target) {
         public void onAction(class_437 screen) {
            SupportMods.xaeroMinimap.teleportToWaypoint(screen, element);
         }

         public boolean isActive() {
            return SupportMods.xaeroMinimap.canTeleport(SupportMods.xaeroMinimap.getWaypointWorld());
         }
      }).setNameFormatArgs(new Object[]{"T"}));
      rightClickOptions.add(new RightClickOption(this, "gui.xaero_right_click_waypoint_share", rightClickOptions.size(), target) {
         public void onAction(class_437 screen) {
            SupportMods.xaeroMinimap.shareWaypoint(element, (GuiMap)screen, SupportMods.xaeroMinimap.getWaypointWorld());
         }
      });
      rightClickOptions.add((new RightClickOption(this, "", rightClickOptions.size(), target) {
         public String getName() {
            return element.isTemporary() ? "gui.xaero_right_click_waypoint_restore" : (element.isDisabled() ? "gui.xaero_right_click_waypoint_enable" : "gui.xaero_right_click_waypoint_disable");
         }

         public void onAction(class_437 screen) {
            if (element.isTemporary()) {
               SupportMods.xaeroMinimap.toggleTemporaryWaypoint(element);
            } else {
               SupportMods.xaeroMinimap.disableWaypoint(element);
            }

         }
      }).setNameFormatArgs(new Object[]{"H"}));
      rightClickOptions.add((new RightClickOption(this, "", rightClickOptions.size(), target) {
         public String getName() {
            return element.isTemporary() ? "gui.xaero_right_click_waypoint_delete_confirm" : "gui.xaero_right_click_waypoint_delete";
         }

         public void onAction(class_437 screen) {
            if (element.isTemporary()) {
               SupportMods.xaeroMinimap.deleteWaypoint(element);
            } else {
               SupportMods.xaeroMinimap.toggleTemporaryWaypoint(element);
            }

         }
      }).setNameFormatArgs(new Object[]{"DEL"}));
      return rightClickOptions;
   }

   public boolean isRightClickValid(Waypoint element) {
      return SupportMods.xaeroMinimap.waypointExists(element);
   }

   public int getRightClickTitleBackgroundColor(Waypoint element) {
      return element.getColor();
   }

   public boolean shouldScaleBoxWithOptionalScale() {
      return true;
   }

   public int getRenderBoxLeft(Waypoint element, WaypointRenderContext context, float partialTicks) {
      int left = this.getInteractionBoxLeft(element, context, partialTicks);
      return element.getAlpha() <= 0.0F ? left : Math.min(left, -element.getCachedNameLength() * 3 / 2);
   }

   public int getRenderBoxRight(Waypoint element, WaypointRenderContext context, float partialTicks) {
      int right = this.getInteractionBoxRight(element, context, partialTicks) + 12;
      return element.getAlpha() <= 0.0F ? right : Math.max(right, element.getCachedNameLength() * 3 / 2);
   }

   public int getRenderBoxTop(Waypoint element, WaypointRenderContext context, float partialTicks) {
      return this.getInteractionBoxTop(element, context, partialTicks);
   }

   public int getRenderBoxBottom(Waypoint element, WaypointRenderContext context, float partialTicks) {
      return this.getInteractionBoxBottom(element, context, partialTicks);
   }
}
