package xaero.map.mods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import net.minecraft.class_1074;
import net.minecraft.class_1937;
import net.minecraft.class_2378;
import net.minecraft.class_2874;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5321;
import net.minecraft.class_3675.class_307;
import xaero.common.HudMod;
import xaero.common.IXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.core.XaeroMinimapCore;
import xaero.common.effect.Effects;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.gui.GuiWaypoints;
import xaero.common.gui.GuiWorldTpCommand;
import xaero.common.minimap.highlight.DimensionHighlighterHandler;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointWorldRootContainer;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.common.settings.ModSettings;
import xaero.hud.minimap.common.config.MinimapConfigConstants;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.config.util.MinimapConfigClientUtils;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.controls.util.KeyMappingUtils;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.lib.common.util.KeySortableByOther;
import xaero.map.WorldMap;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.gui.GuiMap;
import xaero.map.misc.Misc;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointMenuRenderContext;
import xaero.map.mods.gui.WaypointMenuRenderProvider;
import xaero.map.mods.gui.WaypointMenuRenderer;
import xaero.map.mods.gui.WaypointRenderer;
import xaero.map.mods.minimap.element.RadarRendererWrapperHelper;
import xaero.map.mods.minimap.tracker.system.MinimapSyncedPlayerTrackerSystem;
import xaero.map.radar.tracker.system.IPlayerTrackerSystem;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

public class SupportXaeroMinimap {
   IXaeroMinimap modMain;
   public int compatibilityVersion;
   private boolean deathpoints = true;
   private boolean refreshWaypoints = true;
   private WaypointWorld waypointWorld;
   private WaypointWorld mapWaypointWorld;
   private class_5321<class_1937> mapDimId;
   private double dimDiv;
   private WaypointSet waypointSet;
   private boolean allSets;
   private ArrayList<Waypoint> waypoints;
   private ArrayList<Waypoint> waypointsSorted;
   private WaypointMenuRenderer waypointMenuRenderer;
   private final WaypointRenderer waypointRenderer;
   private IPlayerTrackerSystem<?> minimapSyncedPlayerTrackerSystem;
   private WaypointWorld mouseBlockWaypointWorld;
   private WaypointWorld rightClickWaypointWorld;

   public SupportXaeroMinimap() {
      try {
         Class mmClassTest = Class.forName("xaero.pvp.BetterPVP");
         this.modMain = XaeroMinimapCore.modMain;
         WorldMap.LOGGER.info("Xaero's WorldMap Mod: Better PVP found!");
      } catch (ClassNotFoundException var5) {
         try {
            Class mmClassTest = Class.forName("xaero.minimap.XaeroMinimap");
            this.modMain = XaeroMinimapCore.modMain;
            WorldMap.LOGGER.info("Xaero's WorldMap Mod: Xaero's minimap found!");
         } catch (ClassNotFoundException var4) {
         }
      }

      if (this.modMain != null) {
         try {
            this.compatibilityVersion = SupportXaeroWorldmap.WORLDMAP_COMPATIBILITY_VERSION;
         } catch (NoSuchFieldError var3) {
         }

         if (this.compatibilityVersion < 3) {
            throw new RuntimeException("Xaero's Minimap 20.23.0 or newer required!");
         }
      }

      this.waypointRenderer = WaypointRenderer.Builder.begin().setMinimap(this).setSymbolCreator(WorldMap.waypointSymbolCreator).build();
   }

   public void register() {
      WorldMap.playerTrackerSystemManager.register("minimap_synced", this.getMinimapSyncedPlayerTrackerSystem());
   }

   public ArrayList<Waypoint> convertWaypoints(double dimDiv) {
      if (this.waypointSet == null) {
         return null;
      } else {
         ArrayList<Waypoint> result = new ArrayList();
         if (!this.allSets) {
            this.convertSet(this.waypointSet, result, dimDiv);
         } else {
            HashMap<String, WaypointSet> sets = this.waypointWorld.getSets();
            Iterator var5 = sets.values().iterator();

            while(var5.hasNext()) {
               WaypointSet set = (WaypointSet)var5.next();
               this.convertSet(set, result, dimDiv);
            }
         }

         ClientConfigManager minimapConfigManager = HudMod.INSTANCE.getHudConfigs().getClientConfigManager();
         this.deathpoints = (Boolean)minimapConfigManager.getEffective(MinimapProfiledConfigOptions.DEATHPOINTS);
         return result;
      }
   }

   private void convertSet(WaypointSet set, ArrayList<Waypoint> result, double dimDiv) {
      ArrayList<xaero.common.minimap.waypoints.Waypoint> list = set.getList();
      String setName = set.getName();
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
      boolean showingDisabled = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DISPLAY_DISABLED_WAYPOINTS);

      for(int i = 0; i < list.size(); ++i) {
         xaero.common.minimap.waypoints.Waypoint w = (xaero.common.minimap.waypoints.Waypoint)list.get(i);
         if (showingDisabled || !w.isDisabled()) {
            result.add(this.convertWaypoint(w, true, setName, dimDiv));
         }
      }

   }

   public Waypoint convertWaypoint(xaero.common.minimap.waypoints.Waypoint w, boolean editable, String setName, double dimDiv) {
      int waypointType = w.getWaypointType();
      Waypoint converted = new Waypoint(w, w.getX(), w.getY(), w.getZ(), w.getName(), w.getSymbol(), MinimapConfigConstants.COLORS[w.getColor()], waypointType, editable, setName, w.isYIncluded(), dimDiv);
      converted.setDisabled(w.isDisabled());
      converted.setYaw(w.getYaw());
      converted.setRotation(w.isRotation());
      converted.setTemporary(w.isTemporary());
      converted.setGlobal(w.isGlobal());
      return converted;
   }

   public void openWaypoint(GuiMap parent, Waypoint waypoint) {
      if (waypoint.isEditable()) {
         XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
         class_437 addScreen = new GuiAddWaypoint(this.modMain, minimapSession.getWaypointsManager(), parent, parent, (xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal(), this.waypointWorld.getContainer().getRootContainer().getKey(), this.waypointWorld, waypoint.getSetName());
         class_310.method_1551().method_1507(addScreen);
      }
   }

   public void createWaypoint(GuiMap parent, int x, int y, int z, double coordDimensionScale, boolean rightClick) {
      if (this.waypointWorld != null) {
         XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
         WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
         WaypointWorld coordSourceWaypointWorld = rightClick ? this.rightClickWaypointWorld : this.mouseBlockWaypointWorld;
         class_437 addScreen = new GuiAddWaypoint(this.modMain, minimapSession.getWaypointsManager(), parent, parent, (xaero.common.minimap.waypoints.Waypoint)null, this.waypointWorld.getContainer().getRootContainer().getKey(), this.waypointWorld, this.waypointWorld.getCurrent(), true, x, y, z, coordDimensionScale, coordSourceWaypointWorld);
         class_310.method_1551().method_1507(addScreen);
      }
   }

   public void createTempWaypoint(int x, int y, int z, double mapDimensionScale, boolean rightClick) {
      if (this.waypointWorld != null) {
         XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
         WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
         WaypointWorld coordSourceWaypointWorld = rightClick ? this.rightClickWaypointWorld : this.mouseBlockWaypointWorld;
         waypointsManager.createTemporaryWaypoints(this.waypointWorld, x, y, z, y != 32767 && coordSourceWaypointWorld == this.waypointWorld, mapDimensionScale);
         this.requestWaypointsRefresh();
      }
   }

   public boolean canTeleport(WaypointWorld world) {
      XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
      WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
      return world != null && waypointsManager.canTeleport(waypointsManager.isWorldTeleportable(world), world);
   }

   public void teleportToWaypoint(class_437 screen, Waypoint w) {
      this.teleportToWaypoint(screen, w, this.waypointWorld);
   }

   public void teleportToWaypoint(class_437 screen, Waypoint w, WaypointWorld world) {
      if (world != null) {
         XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
         WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
         waypointsManager.teleportToWaypoint((xaero.common.minimap.waypoints.Waypoint)w.getOriginal(), world, screen);
      }
   }

   public void disableWaypoint(Waypoint waypoint) {
      ((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal()).setDisabled(!((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal()).isDisabled());

      try {
         this.modMain.getSettings().saveWaypoints(this.waypointWorld);
      } catch (IOException var3) {
         WorldMap.LOGGER.error("suppressed exception", var3);
      }

      waypoint.setDisabled(((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal()).isDisabled());
      waypoint.setTemporary(((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal()).isTemporary());
   }

   public void deleteWaypoint(Waypoint waypoint) {
      if (!this.allSets) {
         this.waypointSet.getList().remove((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal());
      } else {
         HashMap<String, WaypointSet> sets = this.waypointWorld.getSets();
         Iterator var3 = sets.values().iterator();

         while(var3.hasNext()) {
            WaypointSet set = (WaypointSet)var3.next();
            if (set.getList().remove((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal())) {
               break;
            }
         }
      }

      try {
         this.modMain.getSettings().saveWaypoints(this.waypointWorld);
      } catch (IOException var5) {
         WorldMap.LOGGER.error("suppressed exception", var5);
      }

      this.waypoints.remove(waypoint);
      this.waypointsSorted.remove(waypoint);
      this.waypointMenuRenderer.updateFilteredList();
   }

   public void checkWaypoints(boolean multiplayer, class_5321<class_1937> dimId, String multiworldId, int width, int height, GuiMap screen, MapWorld mapWorld, class_2378<class_2874> dimensionTypes) {
      XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
      WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
      String var10000 = waypointsManager.getAutoRootContainerID();
      String containerId = var10000 + "/" + waypointsManager.getDimensionDirectoryName(dimId);
      String mapBasedMW = !multiplayer ? "waypoints" : multiworldId;
      this.mapWaypointWorld = waypointsManager.getWorld(containerId, mapBasedMW);
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
      WaypointWorld checkingWaypointWorld;
      if ((Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.ONLY_CURRENT_MAP_WAYPOINTS)) {
         checkingWaypointWorld = this.mapWaypointWorld;
      } else {
         checkingWaypointWorld = waypointsManager.getCurrentWorld();
      }

      class_310 mc = class_310.method_1551();
      if (Misc.hasEffect(mc.field_1724, Effects.NO_WAYPOINTS) || Misc.hasEffect(mc.field_1724, Effects.NO_WAYPOINTS_HARMFUL)) {
         checkingWaypointWorld = null;
      }

      boolean shouldRefresh = this.refreshWaypoints;
      if (dimId != this.mapDimId) {
         shouldRefresh = true;
         this.mapDimId = dimId;
      }

      if (checkingWaypointWorld != this.waypointWorld) {
         this.waypointWorld = checkingWaypointWorld;
         screen.closeRightClick();
         if (screen.waypointMenu) {
            screen.method_25423(class_310.method_1551(), width, height);
         }

         shouldRefresh = true;
      }

      WaypointSet checkingSet = checkingWaypointWorld == null ? null : checkingWaypointWorld.getCurrentSet();
      if (checkingSet != this.waypointSet) {
         this.waypointSet = checkingSet;
         shouldRefresh = true;
      }

      ClientConfigManager minimapConfigManager = HudMod.INSTANCE.getHudConfigs().getClientConfigManager();
      boolean renderAllSetsConfig = (Boolean)minimapConfigManager.getEffective(MinimapProfiledConfigOptions.WAYPOINTS_ALL_SETS);
      if (this.allSets != renderAllSetsConfig) {
         this.allSets = renderAllSetsConfig;
         shouldRefresh = true;
      }

      if (shouldRefresh) {
         this.dimDiv = this.waypointWorld == null ? 1.0D : this.getDimensionDivision(mapWorld, dimensionTypes, waypointsManager, this.waypointWorld.getContainer().getKey(), dimId);
         this.waypoints = this.convertWaypoints(this.dimDiv);
         if (this.waypoints == null) {
            this.waypointsSorted = null;
         } else {
            Collections.sort(this.waypoints);
            this.waypointsSorted = new ArrayList();
            ArrayList<KeySortableByOther<Waypoint>> sortingList = new ArrayList();
            Iterator var22 = this.waypoints.iterator();

            while(var22.hasNext()) {
               Waypoint w = (Waypoint)var22.next();
               sortingList.add(new KeySortableByOther(w, new Comparable[]{w.getComparisonName(), w.getName()}));
            }

            Collections.sort(sortingList);
            var22 = sortingList.iterator();

            while(var22.hasNext()) {
               KeySortableByOther<Waypoint> e = (KeySortableByOther)var22.next();
               this.waypointsSorted.add((Waypoint)e.getKey());
            }
         }

         this.waypointMenuRenderer.updateFilteredList();
      }

      this.refreshWaypoints = false;
   }

   private double getDimensionDivision(MapWorld mapWorld, class_2378<class_2874> dimensionTypes, WaypointsManager waypointsManager, String worldContainerID, class_5321<class_1937> mapDimId) {
      if (worldContainerID != null && class_310.method_1551().field_1687 != null) {
         String dimPart = worldContainerID.substring(worldContainerID.lastIndexOf(47) + 1);
         class_5321<class_1937> waypointDimId = waypointsManager.getDimensionKeyForDirectoryName(dimPart);
         MapDimension waypointMapDimension = mapWorld.getDimension(waypointDimId);
         MapDimension mapDimension = mapWorld.getDimension(mapDimId);
         class_2874 waypointDimType = MapDimension.getDimensionType(waypointMapDimension, waypointDimId, dimensionTypes);
         class_2874 mapDimType = MapDimension.getDimensionType(mapDimension, mapDimId, dimensionTypes);
         double waypointDimScale = waypointDimType == null ? 1.0D : waypointDimType.comp_646();
         double mapDimScale = mapDimType == null ? 1.0D : mapDimType.comp_646();
         return mapDimScale / waypointDimScale;
      } else {
         return 1.0D;
      }
   }

   public HoveredMapElementHolder<?, ?> renderWaypointsMenu(class_332 guiGraphics, GuiMap gui, double scale, int width, int height, int mouseX, int mouseY, boolean leftMousePressed, boolean leftMouseClicked, HoveredMapElementHolder<?, ?> hovered, class_310 mc) {
      return this.waypointMenuRenderer.renderMenu(guiGraphics, gui, scale, width, height, mouseX, mouseY, leftMousePressed, leftMouseClicked, hovered, mc);
   }

   public void requestWaypointsRefresh() {
      this.refreshWaypoints = true;
   }

   public class_304 getWaypointKeyBinding() {
      return ModSettings.newWaypoint;
   }

   public class_304 getTempWaypointKeyBinding() {
      return ModSettings.keyInstantWaypoint;
   }

   public class_304 getTempWaypointsMenuKeyBinding() {
      return ModSettings.keyWaypoints;
   }

   public void onMapKeyPressed(class_307 type, int code, GuiMap screen) {
      class_304 kb = null;
      if (KeyMappingUtils.inputMatches(type, code, this.getToggleRadarKey(), 0)) {
         screen.onRadarButton(screen.getRadarButton());
      }

      if (KeyMappingUtils.inputMatches(type, code, ModSettings.keyToggleMapWaypoints, 0)) {
         this.getWaypointMenuRenderer().onRenderWaypointsButton(screen, screen.field_22789, screen.field_22790);
      }

      if (KeyMappingUtils.inputMatches(type, code, ModSettings.keyReverseEntityRadar, 0)) {
         ModSettings.keyReverseEntityRadar.method_23481(true);
      }

      if (KeyMappingUtils.inputMatches(type, code, ModSettings.keySwitchSet, 0)) {
         kb = ModSettings.keySwitchSet;
      }

      if (KeyMappingUtils.inputMatches(type, code, ModSettings.keyAllSets, 0)) {
         kb = ModSettings.keyAllSets;
      }

      if (KeyMappingUtils.inputMatches(type, code, ModSettings.keyWaypoints, 0)) {
         kb = ModSettings.keyWaypoints;
      }

      class_304 minimapSettingsKB = (class_304)this.modMain.getSettingsKey();
      if (KeyMappingUtils.inputMatches(type, code, minimapSettingsKB, 0)) {
         kb = minimapSettingsKB;
      }

      class_304 listPlayerAlternative = this.getMinimapListPlayersAlternative();
      if (listPlayerAlternative != null && KeyMappingUtils.inputMatches(type, code, listPlayerAlternative, 0)) {
         listPlayerAlternative.method_23481(true);
      }

      class_310 mc = class_310.method_1551();
      if (kb != null) {
         if (kb == ModSettings.keyWaypoints) {
            this.openWaypointsMenu(mc, screen);
            return;
         }

         if (minimapSettingsKB != null && kb == minimapSettingsKB) {
            mc.method_1507(this.getSettingsScreen(screen));
            return;
         }

         this.handleMinimapKeyBinding(kb, screen);
      }

   }

   public boolean onMapKeyReleased(class_307 type, int code, GuiMap screen) {
      boolean result = false;
      if (KeyMappingUtils.inputMatches(type, code, ModSettings.keyReverseEntityRadar, 0)) {
         ModSettings.keyReverseEntityRadar.method_23481(false);
         result = true;
      }

      class_304 listPlayerAlternative = this.getMinimapListPlayersAlternative();
      if (listPlayerAlternative != null && KeyMappingUtils.inputMatches(type, code, listPlayerAlternative, 0)) {
         listPlayerAlternative.method_23481(false);
         result = true;
      }

      return result;
   }

   public void handleMinimapKeyBinding(class_304 kb, GuiMap screen) {
      XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
      minimapSession.getControls().keyDown(kb, false, false);
      if ((kb == ModSettings.keySwitchSet || kb == ModSettings.keyAllSets) && screen.waypointMenu) {
         screen.method_25423(class_310.method_1551(), screen.field_22789, screen.field_22790);
      }

   }

   public void drawSetChange(class_332 guiGraphics) {
      XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
      this.modMain.getInterfaces().getMinimapInterface().getWaypointsGuiRenderer().drawSetChange(minimapSession.getWaypointsManager(), guiGraphics, class_310.method_1551().method_22683());
   }

   public class_437 getSettingsScreen(class_437 current) {
      return this.modMain.getGuiHelper().getMinimapSettingsFromScreen(current);
   }

   public String getControlsTooltip() {
      return class_1074.method_4662("gui.xaero_box_controls_minimap", new Object[]{KeyMappingUtils.getKeyName(ModSettings.newWaypoint), KeyMappingUtils.getKeyName(ModSettings.keyInstantWaypoint), KeyMappingUtils.getKeyName(ModSettings.keySwitchSet), KeyMappingUtils.getKeyName(ModSettings.keyAllSets), KeyMappingUtils.getKeyName(ModSettings.keyWaypoints)});
   }

   public void onMapMouseRelease(double par1, double par2, int par3) {
      this.waypointMenuRenderer.onMapMouseRelease(par1, par2, par3);
   }

   public void onMapConstruct() {
      this.waypointMenuRenderer = new WaypointMenuRenderer(new WaypointMenuRenderContext(), new WaypointMenuRenderProvider(this), this.waypointRenderer);
   }

   public void onMapInit(GuiMap mapScreen, class_310 mc, int width, int height) {
      this.waypointMenuRenderer.onMapInit(mapScreen, mc, width, height, this.waypointWorld, this.modMain, XaeroMinimapSession.getCurrentSession());
   }

   public ArrayList<Waypoint> getWaypointsSorted() {
      return this.waypointsSorted;
   }

   public boolean waypointExists(Waypoint w) {
      return this.waypoints != null && this.waypoints.contains(w);
   }

   public void toggleTemporaryWaypoint(Waypoint waypoint) {
      ((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal()).setTemporary(!((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal()).isTemporary());

      try {
         this.modMain.getSettings().saveWaypoints(this.waypointWorld);
      } catch (IOException var3) {
         WorldMap.LOGGER.error("suppressed exception", var3);
      }

      waypoint.setDisabled(((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal()).isDisabled());
      waypoint.setTemporary(((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal()).isTemporary());
   }

   public void openWaypointsMenu(class_310 mc, GuiMap screen) {
      XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
      mc.method_1507(new GuiWaypoints(this.modMain, minimapSession, screen, screen));
   }

   public boolean hidingWaypointCoordinates() {
      ClientConfigManager minimapConfigManager = HudMod.INSTANCE.getHudConfigs().getClientConfigManager();
      return (Boolean)minimapConfigManager.getEffective(MinimapProfiledConfigOptions.HIDE_WAYPOINT_COORDINATES);
   }

   public void shareWaypoint(Waypoint waypoint, GuiMap screen, WaypointWorld world) {
      XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
      minimapSession.getWaypointSharing().shareWaypoint(screen, (xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal(), world);
   }

   public void shareLocation(GuiMap guiMap, int rightClickX, int rightClickY, int rightClickZ) {
      int wpColor = (int)((double)MinimapConfigConstants.COLORS.length * Math.random());
      xaero.common.minimap.waypoints.Waypoint minimapLocationWaypoint = new xaero.common.minimap.waypoints.Waypoint(rightClickX, rightClickY == 32767 ? 0 : rightClickY, rightClickZ, "Shared Location", "S", wpColor, 0, false, rightClickY != 32767);
      Waypoint locationWaypoint = this.convertWaypoint(minimapLocationWaypoint, false, "", 1.0D);
      this.shareWaypoint(locationWaypoint, guiMap, this.rightClickWaypointWorld);
   }

   public WaypointWorld getMapWaypointWorld() {
      return this.mapWaypointWorld;
   }

   public WaypointWorld getWaypointWorld() {
      return this.waypointWorld;
   }

   public double getDimDiv() {
      return this.dimDiv;
   }

   public int getArrowColorIndex() {
      ClientConfigManager minimapConfigManager = HudMod.INSTANCE.getHudConfigs().getClientConfigManager();
      return (Integer)minimapConfigManager.getEffective(MinimapProfiledConfigOptions.ARROW_COLOR);
   }

   public float[] getArrowColor() {
      int arrowColour = this.getArrowColorIndex();
      return arrowColour >= 0 && arrowColour < MinimapConfigConstants.ARROW_COLORS.length ? MinimapConfigConstants.ARROW_COLORS[arrowColour] : null;
   }

   public String getSubWorldNameToRender() {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
      boolean onlyCurrentMapWaypoints = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.ONLY_CURRENT_MAP_WAYPOINTS);
      if (!onlyCurrentMapWaypoints && this.waypointWorld != null) {
         return this.waypointWorld != this.mapWaypointWorld ? class_1074.method_4662("gui.xaero_wm_using_custom_subworld", new Object[]{this.waypointWorld.getContainer().getSubName()}) : null;
      } else {
         return null;
      }
   }

   public void registerMinimapHighlighters(Object highlighterRegistry) {
   }

   public ArrayList<Waypoint> getWaypoints() {
      return this.waypoints;
   }

   public boolean getDeathpoints() {
      return this.deathpoints;
   }

   public WaypointRenderer getWaypointRenderer() {
      return this.waypointRenderer;
   }

   public WaypointMenuRenderer getWaypointMenuRenderer() {
      return this.waypointMenuRenderer;
   }

   public void onClearHighlightHash(int regionX, int regionZ) {
      XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
      if (minimapSession != null) {
         DimensionHighlighterHandler highlightHandler = minimapSession.getMinimapProcessor().getMinimapWriter().getDimensionHighlightHandler();
         if (highlightHandler != null) {
            highlightHandler.requestRefresh(regionX, regionZ);
         }
      }

   }

   public void createRadarRendererWrapper(Object radarRenderer) {
      (new RadarRendererWrapperHelper()).createWrapper(this.modMain, (RadarRenderer)radarRenderer);
   }

   public class_304 getToggleRadarKey() {
      return ModSettings.keyToggleRadar;
   }

   public void onClearHighlightHashes() {
      XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
      if (minimapSession != null) {
         DimensionHighlighterHandler highlightHandler = minimapSession.getMinimapProcessor().getMinimapWriter().getDimensionHighlightHandler();
         if (highlightHandler != null) {
            highlightHandler.requestRefresh();
         }
      }

   }

   public class_304 getToggleAllyPlayersKey() {
      return ModSettings.keyToggleTrackedPlayers;
   }

   public class_304 getToggleClaimsKey() {
      return ModSettings.keyTogglePacChunkClaims;
   }

   public void onSessionFinalized() {
      this.waypointWorld = null;
      this.mapWaypointWorld = null;
   }

   public void openWaypointWorldTeleportCommandScreen(class_437 parent, class_437 escape) {
      XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
      if (minimapSession != null) {
         WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
         String containerId = waypointsManager.getAutoRootContainerID();
         WaypointWorldRootContainer container = waypointsManager.getWorldContainerNullable(containerId).getRootContainer();
         if (container != null) {
            class_310.method_1551().method_1507(new GuiWorldTpCommand(this.modMain, parent, escape, container));
         }

      }
   }

   public class_304 getMinimapListPlayersAlternative() {
      return ModSettings.keyAlternativeListPlayers;
   }

   public int getCaveStart(int defaultWorldMapStart, boolean isMapScreen) {
      if (!this.modMain.getSettings().getMinimap()) {
         return defaultWorldMapStart;
      } else if (!MinimapConfigClientUtils.getEffectiveCaveModeAllowed()) {
         return isMapScreen ? defaultWorldMapStart : Integer.MAX_VALUE;
      } else {
         int usedCaving = this.getUsedCaving();
         if (usedCaving == Integer.MAX_VALUE) {
            ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
            SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
            return (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.CAVE_MODE_START);
         } else {
            return usedCaving;
         }
      }
   }

   public int getUsedCaving() {
      XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
      return minimapSession != null ? minimapSession.getMinimapProcessor().getMinimapWriter().getLoadedCaving() : Integer.MAX_VALUE;
   }

   public boolean isFairPlay() {
      return this.modMain.isFairPlay();
   }

   public IPlayerTrackerSystem<?> getMinimapSyncedPlayerTrackerSystem() {
      if (this.minimapSyncedPlayerTrackerSystem == null) {
         this.minimapSyncedPlayerTrackerSystem = new MinimapSyncedPlayerTrackerSystem(this);
      }

      return this.minimapSyncedPlayerTrackerSystem;
   }

   public void onBlockHover() {
      this.mouseBlockWaypointWorld = this.mapWaypointWorld;
   }

   public void onRightClick() {
      this.rightClickWaypointWorld = this.mouseBlockWaypointWorld;
   }
}
