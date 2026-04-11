package xaero.map.mods.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import net.minecraft.class_1044;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_5250;
import xaero.common.HudMod;
import xaero.common.IXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.gui.GuiNewSet;
import xaero.common.gui.GuiWaypointSets;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.settings.ModSettings;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.controls.util.KeyMappingUtils;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;
import xaero.lib.client.gui.widget.dropdown.IDropDownWidgetCallback;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget.Builder;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.element.MapElementMenuRenderer;
import xaero.map.element.render.ElementRenderer;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiTexturedButton;
import xaero.map.mods.SupportMods;

public class WaypointMenuRenderer extends MapElementMenuRenderer<Waypoint, WaypointMenuRenderContext> {
   private final WaypointRenderer renderer;
   private class_4185 renderWaypointsButton;
   private class_4185 showDisabledButton;
   private class_4185 closeMenuWhenHoppingButton;
   private class_4185 currentMapWaypointsButton;
   private class_4185 renderAllSetsButton;

   public WaypointMenuRenderer(WaypointMenuRenderContext context, WaypointMenuRenderProvider provider, WaypointRenderer renderer) {
      super(context, provider);
      this.renderer = renderer;
   }

   public void onMapInit(GuiMap screen, class_310 mc, int width, int height, WaypointWorld waypointWorld, IXaeroMinimap modMain, XaeroMinimapSession minimapSession) {
      super.onMapInit(screen, mc, width, height);
      GuiWaypointSets sets = waypointWorld != null ? new GuiWaypointSets(true, waypointWorld) : null;
      IDropDownWidgetCallback setsDropdownCallback = null;
      if (sets != null) {
         setsDropdownCallback = (menu, selected) -> {
            if (selected == menu.size() - 1) {
               GuiNewSet guiNewSet = new GuiNewSet(modMain, minimapSession, screen, screen, waypointWorld);
               class_310.method_1551().method_1507(guiNewSet);
               return false;
            } else {
               sets.setCurrentSet(selected);
               waypointWorld.setCurrent(sets.getCurrentSetKey());
               minimapSession.getWaypointsManager().updateWaypoints();

               try {
                  modMain.getSettings().saveWaypoints(waypointWorld);
               } catch (IOException var8) {
                  WorldMap.LOGGER.error("suppressed exception", var8);
               }

               return true;
            }
         };
      }

      DropDownWidget setsDropdown = sets == null ? null : Builder.begin().setOptions(sets.getOptions()).setX(width - 173).setY(height - 56).setW(151).setSelected(sets.getCurrentSet()).setCallback(setsDropdownCallback).setContainer(screen).setOpeningUp(true).setNarrationTitle(class_2561.method_43471("gui.xaero_dropdown_waypoint_set")).build();
      if (setsDropdown != null) {
         screen.method_25429(setsDropdown);
      }

      class_5250 fullWaypointMenuTooltipText = class_2561.method_43469("gui.xaero_box_full_waypoints_menu", new Object[]{class_2561.method_43470(KeyMappingUtils.getKeyName(ModSettings.keyWaypoints)).method_27692(class_124.field_1077)});
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      boolean renderWaypoints = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.RENDER_WAYPOINTS);
      SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
      boolean onlyCurrentMapWaypoints = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.ONLY_CURRENT_MAP_WAYPOINTS);
      boolean showDisabledWaypoints = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DISPLAY_DISABLED_WAYPOINTS);
      boolean closeWaypointsWhenHopping = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.CLOSE_WAYPOINTS_AFTER_HOP);
      Tooltip fullWaypointMenuTooltip = new Tooltip(fullWaypointMenuTooltipText, true);
      Tooltip onlyCurrentMapWaypointsTooltip = new Tooltip(onlyCurrentMapWaypoints ? "gui.xaero_box_only_current_map_waypoints" : "gui.xaero_box_waypoints_selected_by_minimap", class_2583.field_24360, true);
      Tooltip renderingWaypointsTooltip = new Tooltip(class_2561.method_43469(renderWaypoints ? "gui.xaero_box_rendering_waypoints" : "gui.xaero_box_not_rendering_waypoints", new Object[]{class_2561.method_43470(KeyMappingUtils.getKeyName(ModSettings.keyToggleMapWaypoints)).method_27692(class_124.field_1077)}), true);
      ClientConfigManager minimapConfigManager = HudMod.INSTANCE.getHudConfigs().getClientConfigManager();
      boolean renderAllSetsConfig = (Boolean)minimapConfigManager.getEffective(MinimapProfiledConfigOptions.WAYPOINTS_ALL_SETS);
      Tooltip renderAllSetsTooltip = new Tooltip(class_2561.method_43469(!renderAllSetsConfig ? "gui.xaero_box_rendering_current_set" : "gui.xaero_box_rendering_all_sets", new Object[]{class_2561.method_43470(KeyMappingUtils.getKeyName(ModSettings.keyAllSets)).method_27692(class_124.field_1077)}), true);
      Tooltip showingDisabledTooltip = new Tooltip(showDisabledWaypoints ? "gui.xaero_box_showing_disabled" : "gui.xaero_box_hiding_disabled", class_2583.field_24360, true);
      Tooltip closeWhenHoppingTooltip = new Tooltip(closeWaypointsWhenHopping ? "gui.xaero_box_closing_menu_when_hopping" : "gui.xaero_box_not_closing_menu_when_hopping", class_2583.field_24360, true);
      screen.addButton(new GuiTexturedButton(width - 173, height - 20, 20, 20, 229, 0, 16, 16, WorldMap.guiTextures, (b) -> {
         this.onFullMenuButton(b, screen);
      }, () -> {
         return fullWaypointMenuTooltip;
      }));
      screen.addButton(this.currentMapWaypointsButton = new GuiTexturedButton(width - 153, height - 20, 20, 20, onlyCurrentMapWaypoints ? 213 : 229, 16, 16, 16, WorldMap.guiTextures, (b) -> {
         this.onCurrentMapWaypointsButton(b, screen, width, height);
      }, () -> {
         return onlyCurrentMapWaypointsTooltip;
      }));
      screen.addButton(this.renderWaypointsButton = new GuiTexturedButton(width - 133, height - 20, 20, 20, renderWaypoints ? 229 : 213, 48, 16, 16, WorldMap.guiTextures, (b) -> {
         this.onRenderWaypointsButton(screen, width, height);
      }, () -> {
         return renderingWaypointsTooltip;
      }));
      screen.addButton(this.renderAllSetsButton = new GuiTexturedButton(width - 113, height - 20, 20, 20, !renderAllSetsConfig ? 81 : 97, 16, 16, 16, WorldMap.guiTextures, (b) -> {
         this.onRenderAllSetsButton(b, screen, width, height);
      }, () -> {
         return renderAllSetsTooltip;
      }));
      screen.addButton(this.showDisabledButton = new GuiTexturedButton(width - 93, height - 20, 20, 20, showDisabledWaypoints ? 133 : 149, 16, 16, 16, WorldMap.guiTextures, (b) -> {
         this.onShowDisabledButton(b, screen, width, height);
      }, () -> {
         return showingDisabledTooltip;
      }));
      screen.addButton(this.closeMenuWhenHoppingButton = new GuiTexturedButton(width - 73, height - 20, 20, 20, closeWaypointsWhenHopping ? 181 : 197, 16, 16, 16, WorldMap.guiTextures, (b) -> {
         this.onCloseMenuWhenHoppingButton(b, screen, width, height);
      }, () -> {
         return closeWhenHoppingTooltip;
      }));
      this.renderWaypointsButton.field_22763 = !WorldMapClientConfigUtils.isOptionServerEnforced(WorldMapProfiledConfigOptions.RENDER_WAYPOINTS);
   }

   public void onRenderWaypointsButton(GuiMap screen, int width, int height) {
      WorldMapClientConfigUtils.tryTogglingCurrentProfileOption(WorldMapProfiledConfigOptions.RENDER_WAYPOINTS);
      screen.method_25423(this.mc, width, height);
      screen.method_25395(this.renderWaypointsButton);
   }

   private void onFullMenuButton(class_4185 b, GuiMap screen) {
      SupportMods.xaeroMinimap.openWaypointsMenu(this.mc, screen);
   }

   private void onRenderAllSetsButton(class_4185 b, GuiMap screen, int width, int height) {
      SupportMods.xaeroMinimap.handleMinimapKeyBinding(ModSettings.keyAllSets, screen);
      screen.method_25395(this.renderAllSetsButton);
   }

   private void onShowDisabledButton(class_4185 b, GuiMap screen, int width, int height) {
      WorldMapClientConfigUtils.togglePrimaryOption(WorldMapPrimaryClientConfigOptions.DISPLAY_DISABLED_WAYPOINTS);
      screen.method_25423(this.mc, width, height);
      screen.method_25395(this.showDisabledButton);
   }

   private void onCloseMenuWhenHoppingButton(class_4185 b, GuiMap screen, int width, int height) {
      WorldMapClientConfigUtils.togglePrimaryOption(WorldMapPrimaryClientConfigOptions.CLOSE_WAYPOINTS_AFTER_HOP);
      screen.method_25423(this.mc, width, height);
      screen.method_25395(this.closeMenuWhenHoppingButton);
   }

   private void onCurrentMapWaypointsButton(class_4185 b, GuiMap screen, int width, int height) {
      WorldMapClientConfigUtils.togglePrimaryOption(WorldMapPrimaryClientConfigOptions.ONLY_CURRENT_MAP_WAYPOINTS);
      screen.method_25423(this.mc, width, height);
      screen.method_25395(this.currentMapWaypointsButton);
   }

   public void renderInMenu(Waypoint element, class_332 guiGraphics, class_437 gui, int mouseX, int mouseY, double scale, boolean enabled, boolean hovered, class_310 mc, boolean pressed, int textX) {
      class_4587 matrixStack = guiGraphics.method_51448();
      boolean disabled = element.isDisabled();
      boolean temporary = element.isTemporary();
      int type = element.getType();
      int color = element.getColor();
      String symbol = element.getSymbol();
      matrixStack.method_46416(-4.0F, -4.0F, 0.0F);
      RenderSystem.enableBlend();
      if (type == 1) {
         guiGraphics.method_25294(0, 0, 9, 9, color);
         class_1044 texture = mc.method_1531().method_4619(Waypoint.minimapTextures);
         texture.method_4527(false, false);
         RenderSystem.setShaderColor(0.2431F, 0.2431F, 0.2431F, 1.0F);
         guiGraphics.method_25302(Waypoint.minimapTextures, 1, 1, 0, 78, 9, 9);
         RenderSystem.setShaderColor(0.9882F, 0.9882F, 0.9882F, 1.0F);
         guiGraphics.method_25302(Waypoint.minimapTextures, 0, 0, 0, 78, 9, 9);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         guiGraphics.method_25294(0, 0, 9, 9, color);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (type != 1) {
         guiGraphics.method_25303(mc.field_1772, symbol, 5 - mc.field_1772.method_1727(symbol) / 2, 1, -1);
      }

      RenderSystem.enableBlend();
      int infoIconOffset = 10;
      if (disabled) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 0.0F, 1.0F);
         guiGraphics.method_25302(WorldMap.guiTextures, textX - 1 - infoIconOffset, 0, 173, 16, 8, 8);
         infoIconOffset += 10;
      }

      if (temporary) {
         RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
         guiGraphics.method_25302(WorldMap.guiTextures, textX - 1 - infoIconOffset, 0, 165, 16, 8, 8);
         infoIconOffset += 10;
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public int menuStartPos(int height) {
      return height - 59;
   }

   public int menuSearchPadding() {
      return 14;
   }

   protected String getFilterPlaceholder() {
      return "gui.xaero_filter_waypoints_by_name";
   }

   protected ElementRenderer<? super Waypoint, ?, ?> getRenderer(Waypoint element) {
      return this.renderer;
   }

   protected void beforeFiltering() {
   }

   protected void beforeMenuRender() {
   }

   protected void afterMenuRender() {
   }
}
