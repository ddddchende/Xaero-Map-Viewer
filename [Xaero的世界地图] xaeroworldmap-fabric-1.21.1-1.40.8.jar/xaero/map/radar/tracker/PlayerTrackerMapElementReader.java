package xaero.map.radar.tracker;

import java.util.ArrayList;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_640;
import xaero.lib.client.config.ClientConfigManager;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.element.MapElementReader;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.SupportMods;

public class PlayerTrackerMapElementReader extends MapElementReader<PlayerTrackerMapElement<?>, PlayerTrackerMapElementRenderContext, PlayerTrackerMapElementRenderer> {
   public boolean isHidden(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context) {
      return class_310.method_1551().field_1687.method_27983() != element.getDimension() && context.mapDimId != element.getDimension();
   }

   public double getRenderX(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context, float partialTicks) {
      return class_310.method_1551().field_1687.method_27983() != element.getDimension() ? element.getX() * context.mapDimDiv : element.getX();
   }

   public double getRenderZ(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context, float partialTicks) {
      return class_310.method_1551().field_1687.method_27983() != element.getDimension() ? element.getZ() * context.mapDimDiv : element.getZ();
   }

   public int getInteractionBoxLeft(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context, float partialTicks) {
      return -16;
   }

   public int getInteractionBoxRight(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context, float partialTicks) {
      return 16;
   }

   public int getInteractionBoxTop(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context, float partialTicks) {
      return -16;
   }

   public int getInteractionBoxBottom(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context, float partialTicks) {
      return 16;
   }

   public int getRenderBoxLeft(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context, float partialTicks) {
      return -20;
   }

   public int getRenderBoxRight(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context, float partialTicks) {
      return 20;
   }

   public int getRenderBoxTop(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context, float partialTicks) {
      return -20;
   }

   public int getRenderBoxBottom(PlayerTrackerMapElement<?> element, PlayerTrackerMapElementRenderContext context, float partialTicks) {
      return 20;
   }

   public int getLeftSideLength(PlayerTrackerMapElement<?> element, class_310 mc) {
      class_640 info = class_310.method_1551().method_1562().method_2871(element.getPlayerId());
      return info == null ? 9 : 9 + mc.field_1772.method_1727(info.method_2966().getName());
   }

   public String getMenuName(PlayerTrackerMapElement<?> element) {
      class_640 info = class_310.method_1551().method_1562().method_2871(element.getPlayerId());
      return info == null ? String.valueOf(element.getPlayerId()).makeConcatWithConstants<invokedynamic>(String.valueOf(element.getPlayerId())) : info.method_2966().getName();
   }

   public String getFilterName(PlayerTrackerMapElement<?> element) {
      return this.getMenuName(element);
   }

   public int getMenuTextFillLeftPadding(PlayerTrackerMapElement<?> element) {
      return 0;
   }

   public int getRightClickTitleBackgroundColor(PlayerTrackerMapElement<?> element) {
      return -11184641;
   }

   public boolean shouldScaleBoxWithOptionalScale() {
      return true;
   }

   public boolean isRightClickValid(PlayerTrackerMapElement<?> element) {
      return WorldMap.trackedPlayerRenderer.getCollector().playerExists(element.getPlayerId());
   }

   public ArrayList<RightClickOption> getRightClickOptions(PlayerTrackerMapElement<?> element, IRightClickableElement target) {
      ArrayList<RightClickOption> rightClickOptions = new ArrayList();
      rightClickOptions.add(new RightClickOption(this, this.getMenuName(element), rightClickOptions.size(), target) {
         public void onAction(class_437 screen) {
         }
      });
      rightClickOptions.add(new RightClickOption(this, "", rightClickOptions.size(), target) {
         public String getName() {
            ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
            return !(Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.COORDINATES) ? "hidden" : String.format("X: %d, Y: %s, Z: %d", (int)Math.floor(element.getX()), (int)Math.floor(element.getY()), (int)Math.floor(element.getZ()));
         }

         public void onAction(class_437 screen) {
         }
      });
      rightClickOptions.add((new RightClickOption(this, "gui.xaero_right_click_player_teleport", rightClickOptions.size(), target) {
         public void onAction(class_437 screen) {
            WorldMapSession session = WorldMapSession.getCurrentSession();
            (new PlayerTeleporter()).teleportToPlayer(screen, session.getMapProcessor().getMapWorld(), element);
         }
      }).setNameFormatArgs(new Object[]{"T"}));
      if (SupportMods.pac()) {
         rightClickOptions.add((new RightClickOption(this, "gui.xaero_right_click_player_config", rightClickOptions.size(), target) {
            public void onAction(class_437 screen) {
               SupportMods.xaeroPac.openPlayerConfigScreen(screen, screen, element);
            }

            public boolean isActive() {
               return class_310.method_1551().field_1724.method_5687(2) && class_310.method_1551().method_1562().method_2871(element.getPlayerId()) != null;
            }
         }).setNameFormatArgs(new Object[]{"C"}));
      }

      return rightClickOptions;
   }

   /** @deprecated */
   @Deprecated
   public boolean isInteractable(int location, PlayerTrackerMapElement<?> element) {
      return this.isInteractable(ElementRenderLocation.fromIndex(location), element);
   }

   public boolean isInteractable(ElementRenderLocation location, PlayerTrackerMapElement<?> element) {
      return true;
   }
}
