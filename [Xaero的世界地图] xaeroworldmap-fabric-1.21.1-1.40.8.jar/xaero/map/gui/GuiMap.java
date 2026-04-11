package xaero.map.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1074;
import net.minecraft.class_124;
import net.minecraft.class_1297;
import net.minecraft.class_1792;
import net.minecraft.class_1921;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2378;
import net.minecraft.class_2561;
import net.minecraft.class_287;
import net.minecraft.class_2874;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_342;
import net.minecraft.class_364;
import net.minecraft.class_4068;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_5321;
import net.minecraft.class_6379;
import net.minecraft.class_6599;
import net.minecraft.class_3675.class_307;
import net.minecraft.class_4597.class_4598;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.controls.util.KeyMappingUtils;
import xaero.lib.client.graphics.shader.LibShaders;
import xaero.lib.client.gui.config.context.BuiltInEditConfigScreenContexts;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.lib.common.util.MathUtils;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.animation.Animation;
import xaero.map.animation.SinAnimation;
import xaero.map.animation.SlowingAnimation;
import xaero.map.common.config.WorldMapConfigConstants;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.controls.ControlsRegister;
import xaero.map.core.IWorldMapMinecraftClient;
import xaero.map.effects.Effects;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.dropdown.rightclick.GuiRightClickMenu;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.misc.Misc;
import xaero.map.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaero.map.mods.gui.Waypoint;
import xaero.map.radar.tracker.PlayerTeleporter;
import xaero.map.radar.tracker.PlayerTrackerMapElement;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LayeredRegionManager;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapBlock;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaero.map.region.Overlay;
import xaero.map.region.texture.RegionTexture;
import xaero.map.teleport.MapTeleporter;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

public class GuiMap extends ScreenBase implements IRightClickableElement {
   private static final class_2561 FULL_RELOAD_IN_PROGRESS = class_2561.method_43471("gui.xaero_full_reload_in_progress");
   private static final class_2561 UNKNOWN_DIMENSION_TYPE1 = class_2561.method_43471("gui.xaero_unknown_dimension_type1");
   private static final class_2561 UNKNOWN_DIMENSION_TYPE2 = class_2561.method_43471("gui.xaero_unknown_dimension_type2");
   private static final double ZOOM_STEP = 1.2D;
   private static final int white = -1;
   private static final int black = -16777216;
   private static int lastAmountOfRegionsViewed = 1;
   private long loadingAnimationStart;
   private class_1297 player;
   private double screenScale = 0.0D;
   private int mouseDownPosX = -1;
   private int mouseDownPosY = -1;
   private double mouseDownCameraX = -1.0D;
   private double mouseDownCameraZ = -1.0D;
   private int mouseCheckPosX = -1;
   private int mouseCheckPosY = -1;
   private long mouseCheckTimeNano = -1L;
   private int prevMouseCheckPosX = -1;
   private int prevMouseCheckPosY = -1;
   private long prevMouseCheckTimeNano = -1L;
   private double cameraX = 0.0D;
   private double cameraZ = 0.0D;
   private boolean shouldResetCameraPos;
   private int[] cameraDestination = null;
   private SlowingAnimation cameraDestinationAnimX = null;
   private SlowingAnimation cameraDestinationAnimZ = null;
   private double scale;
   private double userScale;
   private static double destScale = 3.0D;
   private boolean pauseZoomKeys;
   private int lastZoomMethod;
   private double prevPlayerDimDiv;
   private HoveredMapElementHolder<?, ?> viewed = null;
   private boolean viewedInList;
   private HoveredMapElementHolder<?, ?> viewedOnMousePress = null;
   private boolean overWaypointsMenu;
   private Animation zoomAnim;
   public boolean waypointMenu = false;
   private boolean overPlayersMenu;
   public boolean playersMenu = false;
   private static ImprovedFramebuffer primaryScaleFBO = null;
   private float[] colourBuffer = new float[4];
   private ArrayList<MapRegion> regionBuffer = new ArrayList();
   private ArrayList<BranchLeveledRegion> branchRegionBuffer = new ArrayList();
   private boolean prevWaitingForBranchCache = true;
   private boolean prevLoadingLeaves = true;
   private class_5321<class_1937> lastNonNullViewedDimensionId;
   private class_5321<class_1937> lastViewedDimensionId;
   private String lastViewedMultiworldId;
   private int mouseBlockPosX;
   private int mouseBlockPosY;
   private int mouseBlockPosZ;
   private class_5321<class_1937> mouseBlockDim;
   private double mouseBlockCoordinateScale = 1.0D;
   private long lastStartTime;
   private final GuiMapSwitching mapSwitchingGui;
   private MapMouseButtonPress leftMouseButton;
   private MapMouseButtonPress rightMouseButton;
   private MapProcessor mapProcessor;
   private MapDimension futureDimension;
   public boolean noUploadingLimits;
   private boolean[] waitingForBranchCache = new boolean[1];
   private class_4185 settingsButton;
   private class_4185 exportButton;
   private class_4185 waypointsButton;
   private class_4185 playersButton;
   private class_4185 radarButton;
   private class_4185 claimsButton;
   private class_4185 zoomInButton;
   private class_4185 zoomOutButton;
   private class_4185 keybindingsButton;
   private class_4185 caveModeButton;
   private class_4185 dimensionToggleButton;
   private class_4185 buttonPressed;
   private GuiRightClickMenu rightClickMenu;
   private int rightClickX;
   private int rightClickY;
   private int rightClickZ;
   private class_5321<class_1937> rightClickDim;
   private double rightClickCoordinateScale;
   private boolean lastFrameRenderedRootTextures;
   private MapTileSelection mapTileSelection;
   private boolean tabPressed;
   private GuiCaveModeOptions caveModeOptions;
   private static final Matrix4f identityMatrix = new Matrix4f();

   public GuiMap(class_437 parent, class_437 escape, MapProcessor mapProcessor, class_1297 player) {
      super(parent, escape, class_2561.method_43471("gui.xaero_world_map_screen"));
      this.player = player;
      this.shouldResetCameraPos = true;
      this.leftMouseButton = new MapMouseButtonPress();
      this.rightMouseButton = new MapMouseButtonPress();
      this.mapSwitchingGui = new GuiMapSwitching(mapProcessor);
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      boolean openingAnimationConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.OPENING_ANIMATION);
      this.userScale = destScale * (double)(openingAnimationConfig ? 1.5F : 1.0F);
      this.zoomAnim = new SlowingAnimation(this.userScale, destScale, 0.88D, destScale * 0.001D);
      this.mapProcessor = mapProcessor;
      this.caveModeOptions = new GuiCaveModeOptions();
      if (SupportMods.minimap()) {
         SupportMods.xaeroMinimap.onMapConstruct();
      }

   }

   private double getScaleMultiplier(int screenShortSide) {
      return screenShortSide <= 1080 ? 1.0D : (double)screenShortSide / 1080.0D;
   }

   public <T extends class_364 & class_4068 & class_6379> T method_37063(T guiEventListener) {
      return super.method_37063(guiEventListener);
   }

   public <T extends class_364 & class_4068 & class_6379> T addButton(T guiEventListener) {
      return this.method_37063(guiEventListener);
   }

   public <T extends class_364 & class_6379> T method_25429(T guiEventListener) {
      return super.method_25429(guiEventListener);
   }

   public void method_25426() {
      super.method_25426();
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      MapWorld mapWorld = this.mapProcessor.getMapWorld();
      this.futureDimension = mapWorld != null && mapWorld.getFutureDimensionId() != null ? mapWorld.getFutureDimension() : null;
      this.tabPressed = false;
      boolean waypointsEnabled = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.WAYPOINTS);
      this.waypointMenu = this.waypointMenu && waypointsEnabled;
      this.mapSwitchingGui.init(this, this.field_22787, this.field_22789, this.field_22790);
      boolean effectiveCaveModeAllowed = WorldMapClientConfigUtils.getEffectiveCaveModeAllowed();
      Tooltip caveModeButtonTooltip = new Tooltip(class_2561.method_43471(effectiveCaveModeAllowed ? "gui.xaero_box_cave_mode" : "gui.xaero_box_cave_mode_not_allowed"));
      this.caveModeButton = new GuiTexturedButton(0, this.field_22790 - 40, 20, 20, 229, 64, 16, 16, WorldMap.guiTextures, this::onCaveModeButton, () -> {
         return caveModeButtonTooltip;
      });
      this.caveModeButton.field_22763 = effectiveCaveModeAllowed;
      this.addButton(this.caveModeButton);
      this.caveModeOptions.onInit(this, this.mapProcessor);
      Tooltip dimensionToggleButtonTooltip = new Tooltip(class_2561.method_43469("gui.xaero_dimension_toggle_button", new Object[]{KeyMappingUtils.getKeyName(ControlsRegister.keyToggleDimension)}));
      this.dimensionToggleButton = new GuiTexturedButton(0, this.field_22790 - 60, 20, 20, 197, 80, 16, 16, WorldMap.guiTextures, this::onDimensionToggleButton, () -> {
         return dimensionToggleButtonTooltip;
      });
      this.addButton(this.dimensionToggleButton);
      this.loadingAnimationStart = System.currentTimeMillis();
      if (SupportMods.minimap()) {
         SupportMods.xaeroMinimap.requestWaypointsRefresh();
      }

      this.screenScale = class_310.method_1551().method_22683().method_4495();
      this.pauseZoomKeys = false;
      Tooltip openSettingsTooltip = new Tooltip(class_2561.method_43469("gui.xaero_box_open_settings", new Object[]{KeyMappingUtils.getKeyName(ControlsRegister.keyOpenSettings)}));
      this.addButton(this.settingsButton = new GuiTexturedButton(0, 0, 30, 30, 113, 0, 20, 20, WorldMap.guiTextures, this::onSettingsButton, () -> {
         return openSettingsTooltip;
      }));
      Tooltip waypointsTooltip;
      if (waypointsEnabled) {
         waypointsTooltip = new Tooltip(this.waypointMenu ? "gui.xaero_box_close_waypoints" : "gui.xaero_box_open_waypoints");
      } else {
         waypointsTooltip = new Tooltip(!SupportMods.minimap() ? "gui.xaero_box_waypoints_minimap_required" : "gui.xaero_box_waypoints_disabled");
      }

      Tooltip playersTooltip = new Tooltip(this.playersMenu ? "gui.xaero_box_close_players" : "gui.xaero_box_open_players");
      boolean displayClaimsConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.OPAC_CLAIMS);
      Tooltip claimsTooltip;
      if (SupportMods.pac()) {
         claimsTooltip = new Tooltip(class_2561.method_43469(displayClaimsConfig ? "gui.xaero_box_pac_displaying_claims" : "gui.xaero_box_pac_not_displaying_claims", new Object[]{class_2561.method_43470(KeyMappingUtils.getKeyName(SupportMods.xaeroPac.getPacClaimsKeyBinding())).method_27692(class_124.field_1077)}));
      } else {
         claimsTooltip = new Tooltip(class_2561.method_43471("gui.xaero_box_claims_pac_required"));
      }

      this.addButton(this.waypointsButton = new GuiTexturedButton(this.field_22789 - 20, this.field_22790 - 20, 20, 20, 213, 0, 16, 16, WorldMap.guiTextures, this::onWaypointsButton, () -> {
         return waypointsTooltip;
      }));
      this.waypointsButton.field_22763 = waypointsEnabled;
      this.addButton(this.playersButton = new GuiTexturedButton(this.field_22789 - 20, this.field_22790 - 40, 20, 20, 197, 32, 16, 16, WorldMap.guiTextures, this::onPlayersButton, () -> {
         return playersTooltip;
      }));
      boolean minimapRadarConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.MINIMAP_RADAR);
      Tooltip radarButtonTooltip = new Tooltip(class_2561.method_43469(minimapRadarConfig ? "gui.xaero_box_minimap_radar" : "gui.xaero_box_no_minimap_radar", new Object[]{class_2561.method_43470(KeyMappingUtils.getKeyName(SupportMods.minimap() ? SupportMods.xaeroMinimap.getToggleRadarKey() : null)).method_27692(class_124.field_1077)}));
      this.addButton(this.radarButton = new GuiTexturedButton(this.field_22789 - 20, this.field_22790 - 60, 20, 20, minimapRadarConfig ? 213 : 229, 32, 16, 16, WorldMap.guiTextures, this::onRadarButton, () -> {
         return radarButtonTooltip;
      }));
      this.getRadarButton().field_22763 = SupportMods.minimap();
      this.addButton(this.claimsButton = new GuiTexturedButton(this.field_22789 - 20, this.field_22790 - 80, 20, 20, displayClaimsConfig ? 197 : 213, 64, 16, 16, WorldMap.guiTextures, this::onClaimsButton, () -> {
         return claimsTooltip;
      }));
      this.claimsButton.field_22763 = SupportMods.pac() && !WorldMapClientConfigUtils.isOptionServerEnforced(WorldMapProfiledConfigOptions.OPAC_CLAIMS);
      Tooltip exportButtonTooltip = new Tooltip("gui.xaero_box_export");
      this.addButton(this.exportButton = new GuiTexturedButton(this.field_22789 - 20, this.field_22790 - 100, 20, 20, 133, 0, 16, 16, WorldMap.guiTextures, this::onExportButton, () -> {
         return exportButtonTooltip;
      }));
      Object[] var10003 = new Object[1];
      String var10006 = SupportMods.minimap() ? SupportMods.xaeroMinimap.getControlsTooltip() : "";
      var10003[0] = var10006 + (SupportMods.pac() ? SupportMods.xaeroPac.getControlsTooltip() : "");
      Tooltip controlsButtonTooltip = new Tooltip(class_1074.method_4662("gui.xaero_box_controls", var10003));
      controlsButtonTooltip.setStartWidth(400);
      this.addButton(this.keybindingsButton = new GuiTexturedButton(this.field_22789 - 20, this.field_22790 - 120, 20, 20, 197, 0, 16, 16, WorldMap.guiTextures, this::onKeybindingsButton, () -> {
         return controlsButtonTooltip;
      }));
      Tooltip zoomInButtonTooltip = new Tooltip(class_2561.method_43469("gui.xaero_box_zoom_in", new Object[]{class_2561.method_43470(KeyMappingUtils.getKeyName(ControlsRegister.keyZoomIn)).method_27692(class_124.field_1077)}));
      this.zoomInButton = new GuiTexturedButton(this.field_22789 - 20, this.field_22790 - 160, 20, 20, 165, 0, 16, 16, WorldMap.guiTextures, this::onZoomInButton, () -> {
         return zoomInButtonTooltip;
      });
      Tooltip zoomOutButtonTooltip = new Tooltip(class_2561.method_43469("gui.xaero_box_zoom_out", new Object[]{class_2561.method_43470(KeyMappingUtils.getKeyName(ControlsRegister.keyZoomOut)).method_27692(class_124.field_1077)}));
      this.zoomOutButton = new GuiTexturedButton(this.field_22789 - 20, this.field_22790 - 140, 20, 20, 181, 0, 16, 16, WorldMap.guiTextures, this::onZoomOutButton, () -> {
         return zoomOutButtonTooltip;
      });
      if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.ZOOM_BUTTONS)) {
         this.addButton(this.zoomOutButton);
         this.addButton(this.zoomInButton);
      }

      if (this.rightClickMenu != null) {
         this.rightClickMenu.setClosed(true);
         this.rightClickMenu = null;
      }

      if (SupportMods.minimap() && this.waypointMenu) {
         SupportMods.xaeroMinimap.onMapInit(this, this.field_22787, this.field_22789, this.field_22790);
      }

      if (this.playersMenu) {
         WorldMap.trackedPlayerMenuRenderer.onMapInit(this, this.field_22787, this.field_22789, this.field_22790);
      }

   }

   protected void method_56131() {
   }

   private void onCaveModeButton(class_4185 b) {
      this.caveModeOptions.toggle(this);
      this.method_25395(this.caveModeButton);
   }

   private void onDimensionToggleButton(class_4185 b) {
      this.mapProcessor.getMapWorld().toggleDimension(!method_25442());
      String messageType = this.mapProcessor.getMapWorld().getCustomDimensionId() == null ? "gui.xaero_switched_to_current_dimension" : "gui.xaero_switched_to_dimension";
      class_2960 messageDimLoc = this.mapProcessor.getMapWorld().getFutureDimensionId() == null ? null : this.mapProcessor.getMapWorld().getFutureDimensionId().method_29177();
      this.mapProcessor.getMessageBox().addMessage(class_2561.method_43469(messageType, new Object[]{messageDimLoc.toString()}));
      this.method_25423(this.field_22787, this.field_22789, this.field_22790);
      this.method_25395(this.dimensionToggleButton);
   }

   private void onSettingsButton(class_4185 b) {
      this.field_22787.method_1507(new GuiWorldMapSettings(this, this, BuiltInEditConfigScreenContexts.CLIENT));
   }

   private void onKeybindingsButton(class_4185 b) {
      this.field_22787.method_1507(new class_6599(this, this.field_22787.field_1690));
   }

   private void onExportButton(class_4185 b) {
      this.field_22787.method_1507(new ExportScreen(this, this, this.mapProcessor, this.mapTileSelection));
   }

   private void toggleWaypointMenu() {
      if (this.playersMenu) {
         this.togglePlayerMenu();
      }

      this.waypointMenu = !this.waypointMenu;
      if (!this.waypointMenu) {
         SupportMods.xaeroMinimap.getWaypointMenuRenderer().onMenuClosed();
         this.unfocusAll();
      }

   }

   private void togglePlayerMenu() {
      if (this.waypointMenu) {
         this.toggleWaypointMenu();
      }

      this.playersMenu = !this.playersMenu;
      if (!this.playersMenu) {
         WorldMap.trackedPlayerMenuRenderer.onMenuClosed();
         this.unfocusAll();
      }

   }

   private void onPlayersButton(class_4185 b) {
      this.togglePlayerMenu();
      this.method_25423(this.field_22787, this.field_22789, this.field_22790);
      this.method_25395(this.playersButton);
   }

   public void onClaimsButton(class_4185 unused) {
      WorldMapClientConfigUtils.tryTogglingCurrentProfileOption(WorldMapProfiledConfigOptions.OPAC_CLAIMS);
      this.method_25423(this.field_22787, this.field_22789, this.field_22790);
      this.method_25395(this.claimsButton);
   }

   private void onWaypointsButton(class_4185 b) {
      this.toggleWaypointMenu();
      this.method_25423(this.field_22787, this.field_22789, this.field_22790);
      this.method_25395(this.waypointsButton);
   }

   public void onRadarButton(class_4185 b) {
      WorldMapClientConfigUtils.tryTogglingCurrentProfileOption(WorldMapProfiledConfigOptions.MINIMAP_RADAR);
      this.method_25423(this.field_22787, this.field_22789, this.field_22790);
      this.method_25395(this.radarButton);
   }

   private void onZoomInButton(class_4185 b) {
      this.buttonPressed = this.buttonPressed == null ? b : null;
   }

   private void onZoomOutButton(class_4185 b) {
      this.buttonPressed = this.buttonPressed == null ? b : null;
   }

   public boolean method_25402(double par1, double par2, int par3) {
      boolean toReturn = super.method_25402(par1, par2, par3);
      if (!toReturn) {
         if (par3 == 0) {
            this.leftMouseButton.isDown = this.leftMouseButton.clicked = true;
            this.leftMouseButton.pressedAtX = (int)Misc.getMouseX(this.field_22787, SupportMods.vivecraft);
            this.leftMouseButton.pressedAtY = (int)Misc.getMouseY(this.field_22787, SupportMods.vivecraft);
         } else if (par3 == 1) {
            this.rightMouseButton.isDown = this.rightMouseButton.clicked = true;
            this.rightMouseButton.pressedAtX = (int)Misc.getMouseX(this.field_22787, SupportMods.vivecraft);
            this.rightMouseButton.pressedAtY = (int)Misc.getMouseY(this.field_22787, SupportMods.vivecraft);
            this.viewedOnMousePress = this.viewed;
            this.rightClickX = this.mouseBlockPosX;
            this.rightClickY = this.mouseBlockPosY;
            this.rightClickZ = this.mouseBlockPosZ;
            this.rightClickDim = this.mouseBlockDim;
            this.rightClickCoordinateScale = this.mouseBlockCoordinateScale;
            if (SupportMods.minimap()) {
               SupportMods.xaeroMinimap.onRightClick();
            }

            if (this.viewedOnMousePress == null || !this.viewedOnMousePress.isRightClickValid()) {
               this.mapTileSelection = new MapTileSelection(this.rightClickX >> 4, this.rightClickZ >> 4);
            }
         } else {
            toReturn = this.onInputPress(class_307.field_1672, par3);
         }

         if (!toReturn && this.caveModeOptions.isEnabled()) {
            this.caveModeOptions.toggle(this);
            toReturn = true;
         }
      }

      return toReturn;
   }

   public boolean method_25406(double par1, double par2, int par3) {
      this.buttonPressed = null;
      int mouseX = (int)Misc.getMouseX(this.field_22787, SupportMods.vivecraft);
      int mouseY = (int)Misc.getMouseY(this.field_22787, SupportMods.vivecraft);
      if (this.leftMouseButton.isDown && par3 == 0) {
         this.leftMouseButton.isDown = false;
         if (Math.abs(this.leftMouseButton.pressedAtX - mouseX) < 5 && Math.abs(this.leftMouseButton.pressedAtY - mouseY) < 5) {
            this.mapClicked(0, this.leftMouseButton.pressedAtX, this.leftMouseButton.pressedAtY);
         }

         this.leftMouseButton.pressedAtX = -1;
         this.leftMouseButton.pressedAtY = -1;
      }

      if (this.rightMouseButton.isDown && par3 == 1) {
         this.rightMouseButton.isDown = false;
         this.mapClicked(1, mouseX, mouseY);
         this.rightMouseButton.pressedAtX = -1;
         this.rightMouseButton.pressedAtY = -1;
      }

      if (this.waypointMenu) {
         SupportMods.xaeroMinimap.onMapMouseRelease(par1, par2, par3);
      }

      if (this.playersMenu) {
         WorldMap.trackedPlayerMenuRenderer.onMapMouseRelease(par1, par2, par3);
      }

      boolean toReturn = super.method_25406(par1, par2, par3);
      if (!toReturn) {
         toReturn = this.onInputRelease(class_307.field_1672, par3);
      }

      return toReturn;
   }

   public boolean method_25401(double par1, double par2, double g, double wheel) {
      int direction = wheel > 0.0D ? 1 : -1;
      if (this.waypointMenu && this.overWaypointsMenu) {
         SupportMods.xaeroMinimap.getWaypointMenuRenderer().mouseScrolled(direction);
      } else if (this.playersMenu && this.overPlayersMenu) {
         WorldMap.trackedPlayerMenuRenderer.mouseScrolled(direction);
      } else {
         this.changeZoom(wheel, 0);
      }

      return super.method_25401(par1, par2, g, wheel);
   }

   private void changeZoom(double factor, int zoomMethod) {
      this.closeDropdowns();
      this.lastZoomMethod = zoomMethod;
      this.cameraDestinationAnimX = null;
      this.cameraDestinationAnimZ = null;
      if (method_25441()) {
         double destScaleBefore = destScale;
         if (destScale >= 1.0D) {
            if (factor > 0.0D) {
               destScale = Math.ceil(destScale);
            } else {
               destScale = Math.floor(destScale);
            }

            if (destScaleBefore == destScale) {
               destScale += factor > 0.0D ? 1.0D : -1.0D;
            }

            if (destScale == 0.0D) {
               destScale = 0.5D;
            }
         } else {
            double reversedScale = 1.0D / destScale;
            double log2 = Math.log(reversedScale) / Math.log(2.0D);
            if (factor > 0.0D) {
               log2 = Math.floor(log2);
            } else {
               log2 = Math.ceil(log2);
            }

            destScale = 1.0D / Math.pow(2.0D, log2);
            if (destScaleBefore == destScale) {
               destScale = 1.0D / Math.pow(2.0D, log2 + (double)(factor > 0.0D ? -1 : 1));
            }
         }
      } else {
         destScale *= Math.pow(1.2D, factor);
      }

      if (destScale < 0.0625D) {
         destScale = 0.0625D;
      } else if (destScale > 50.0D) {
         destScale = 50.0D;
      }

   }

   public void method_25432() {
      super.method_25432();
      this.leftMouseButton.isDown = false;
      this.rightMouseButton.isDown = false;
   }

   public void method_25394(class_332 guiGraphics, int scaledMouseX, int scaledMouseY, float partialTicks) {
      guiGraphics.method_51452();

      while(GL11.glGetError() != 0) {
      }

      GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      LibShaders.ensureShaders();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      class_310 mc = class_310.method_1551();
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      SingleConfigManager<Config> primaryConfigManager = WorldMap.INSTANCE.getConfigs().getPrimaryClientConfigManager();
      long startTime = System.currentTimeMillis();
      MapDimension currentFutureDim = !this.mapProcessor.isMapWorldUsable() ? null : this.mapProcessor.getMapWorld().getFutureDimension();
      if (currentFutureDim != this.futureDimension) {
         this.method_25423(this.field_22787, this.field_22789, this.field_22790);
      }

      class_4587 matrixStack = guiGraphics.method_51448();
      double playerDimDiv = this.prevPlayerDimDiv;
      synchronized(this.mapProcessor.renderThreadPauseSync) {
         if (!this.mapProcessor.isRenderingPaused()) {
            class_2378<class_2874> dimTypes = this.mapProcessor.getWorldDimensionTypeRegistry();
            if (dimTypes != null) {
               playerDimDiv = this.mapProcessor.getMapWorld().getCurrentDimension().calculateDimDiv(dimTypes, this.player.method_37908().method_8597());
            }
         }
      }

      double scaledPlayerX = this.player.method_23317() / playerDimDiv;
      double scaledPlayerZ = this.player.method_23321() / playerDimDiv;
      double cameraXBefore;
      double cameraZBefore;
      if (this.shouldResetCameraPos) {
         this.cameraX = (double)((float)scaledPlayerX);
         this.cameraZ = (double)((float)scaledPlayerZ);
         this.shouldResetCameraPos = false;
      } else if (this.prevPlayerDimDiv != 0.0D && playerDimDiv != this.prevPlayerDimDiv) {
         cameraXBefore = this.player.method_23317() / this.prevPlayerDimDiv;
         cameraZBefore = this.player.method_23321() / this.prevPlayerDimDiv;
         this.cameraX = this.cameraX - cameraXBefore + scaledPlayerX;
         this.cameraZ = this.cameraZ - cameraZBefore + scaledPlayerZ;
         this.cameraDestinationAnimX = null;
         this.cameraDestinationAnimZ = null;
         this.cameraDestination = null;
      }

      this.prevPlayerDimDiv = playerDimDiv;
      cameraXBefore = this.cameraX;
      cameraZBefore = this.cameraZ;
      double scaleBefore = this.scale;
      this.mapSwitchingGui.preMapRender(this, this.field_22787, this.field_22789, this.field_22790);
      long passed = this.lastStartTime == 0L ? 16L : startTime - this.lastStartTime;
      double passedScrolls = (double)((float)passed / 64.0F);
      int direction = this.buttonPressed != this.zoomInButton && !KeyMappingUtils.isPhysicallyDown(ControlsRegister.keyZoomIn) ? (this.buttonPressed != this.zoomOutButton && !KeyMappingUtils.isPhysicallyDown(ControlsRegister.keyZoomOut) ? 0 : -1) : 1;
      boolean discoveredForHighlights;
      if (direction != 0) {
         discoveredForHighlights = method_25441();
         if (!discoveredForHighlights || !this.pauseZoomKeys) {
            this.changeZoom((double)direction * passedScrolls, this.buttonPressed != this.zoomInButton && this.buttonPressed != this.zoomOutButton ? 1 : 2);
            if (discoveredForHighlights) {
               this.pauseZoomKeys = true;
            }
         }
      } else {
         this.pauseZoomKeys = false;
      }

      this.lastStartTime = startTime;
      if (this.cameraDestination != null) {
         this.cameraDestinationAnimX = new SlowingAnimation(this.cameraX, (double)this.cameraDestination[0], 0.9D, 0.01D);
         this.cameraDestinationAnimZ = new SlowingAnimation(this.cameraZ, (double)this.cameraDestination[1], 0.9D, 0.01D);
         this.cameraDestination = null;
      }

      if (this.cameraDestinationAnimX != null) {
         this.cameraX = this.cameraDestinationAnimX.getCurrent();
         if (this.cameraX == this.cameraDestinationAnimX.getDestination()) {
            this.cameraDestinationAnimX = null;
         }
      }

      if (this.cameraDestinationAnimZ != null) {
         this.cameraZ = this.cameraDestinationAnimZ.getCurrent();
         if (this.cameraZ == this.cameraDestinationAnimZ.getDestination()) {
            this.cameraDestinationAnimZ = null;
         }
      }

      this.lastViewedDimensionId = null;
      this.lastViewedMultiworldId = null;
      this.mouseBlockPosY = 32767;
      discoveredForHighlights = false;
      synchronized(this.mapProcessor.renderThreadPauseSync) {
         if (this.mapProcessor.isRenderingPaused()) {
            this.renderLoadingScreen(guiGraphics);
         } else {
            boolean mapLoaded = this.mapProcessor.getCurrentWorldId() != null && !this.mapProcessor.isWaitingForWorldUpdate() && this.mapProcessor.getMapSaveLoad().isRegionDetectionComplete();
            boolean noWorldMapEffect = mc.field_1724 == null || Misc.hasEffect(mc.field_1724, Effects.NO_WORLD_MAP) || Misc.hasEffect(mc.field_1724, Effects.NO_WORLD_MAP_HARMFUL);
            class_1792 mapItem = this.mapProcessor.getMapItem();
            boolean allowedBasedOnItem = mapItem == null || mc.field_1724 != null && Misc.hasItem(mc.field_1724, mapItem);
            boolean isLocked = this.mapProcessor.isCurrentMapLocked();
            if (mapLoaded && !noWorldMapEffect && allowedBasedOnItem && !isLocked) {
               if (SupportMods.vivecraft) {
                  GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
                  GlStateManager._clear(16384, class_310.field_1703);
               }

               this.mapProcessor.updateCaveStart();
               this.lastNonNullViewedDimensionId = this.lastViewedDimensionId = this.mapProcessor.getMapWorld().getCurrentDimension().getDimId();
               this.lastViewedMultiworldId = this.mapProcessor.getMapWorld().getCurrentDimension().getCurrentMultiworld();
               if (SupportMods.minimap()) {
                  SupportMods.xaeroMinimap.checkWaypoints(this.mapProcessor.getMapWorld().isMultiplayer(), this.lastViewedDimensionId, this.lastViewedMultiworldId, this.field_22789, this.field_22790, this, this.mapProcessor.getMapWorld(), this.mapProcessor.getWorldDimensionTypeRegistry());
               }

               int mouseXPos = (int)Misc.getMouseX(mc, false);
               int mouseYPos = (int)Misc.getMouseY(mc, false);
               double scaleMultiplier = this.getScaleMultiplier(Math.min(mc.method_22683().method_4489(), mc.method_22683().method_4506()));
               this.scale = this.userScale * scaleMultiplier;
               if (this.mouseCheckPosX == -1 || System.nanoTime() - this.mouseCheckTimeNano > 30000000L) {
                  this.prevMouseCheckPosX = this.mouseCheckPosX;
                  this.prevMouseCheckPosY = this.mouseCheckPosY;
                  this.prevMouseCheckTimeNano = this.mouseCheckTimeNano;
                  this.mouseCheckPosX = mouseXPos;
                  this.mouseCheckPosY = mouseYPos;
                  this.mouseCheckTimeNano = System.nanoTime();
               }

               double oldMousePosZ;
               double preScale;
               double fboScale;
               double secondaryScale;
               double mousePosX;
               double mousePosZ;
               if (!this.leftMouseButton.isDown) {
                  if (this.mouseDownPosX != -1) {
                     this.mouseDownPosX = -1;
                     this.mouseDownPosY = -1;
                     if (this.prevMouseCheckTimeNano != -1L) {
                        double downTime = 0.0D;
                        int draggedX = false;
                        int draggedY = false;
                        downTime = (double)(System.nanoTime() - this.prevMouseCheckTimeNano);
                        int draggedX = mouseXPos - this.prevMouseCheckPosX;
                        int draggedY = mouseYPos - this.prevMouseCheckPosY;
                        oldMousePosZ = 1.6666666666666666E7D;
                        preScale = downTime / oldMousePosZ;
                        fboScale = (double)(-draggedX) / this.scale / preScale;
                        double speed_z = (double)(-draggedY) / this.scale / preScale;
                        secondaryScale = Math.sqrt(fboScale * fboScale + speed_z * speed_z);
                        if (secondaryScale > 0.0D) {
                           mousePosX = fboScale / secondaryScale;
                           mousePosZ = speed_z / secondaryScale;
                           double maxSpeed = 500.0D / this.userScale;
                           secondaryScale = Math.abs(secondaryScale) > maxSpeed ? Math.copySign(maxSpeed, secondaryScale) : secondaryScale;
                           double speed_factor = 0.9D;
                           double ln = Math.log(speed_factor);
                           double move_distance = -secondaryScale / ln;
                           double moveX = mousePosX * move_distance;
                           double moveZ = mousePosZ * move_distance;
                           this.cameraDestinationAnimX = new SlowingAnimation(this.cameraX, this.cameraX + moveX, 0.9D, 0.01D);
                           this.cameraDestinationAnimZ = new SlowingAnimation(this.cameraZ, this.cameraZ + moveZ, 0.9D, 0.01D);
                        }
                     }
                  }
               } else if (this.viewed == null || !this.viewedInList || this.mouseDownPosX != -1) {
                  if (this.mouseDownPosX != -1) {
                     this.cameraX = (double)(this.mouseDownPosX - mouseXPos) / this.scale + this.mouseDownCameraX;
                     this.cameraZ = (double)(this.mouseDownPosY - mouseYPos) / this.scale + this.mouseDownCameraZ;
                  } else {
                     this.mouseDownPosX = mouseXPos;
                     this.mouseDownPosY = mouseYPos;
                     this.mouseDownCameraX = this.cameraX;
                     this.mouseDownCameraZ = this.cameraZ;
                     this.cameraDestinationAnimX = null;
                     this.cameraDestinationAnimZ = null;
                  }
               }

               int mouseFromCentreX = mouseXPos - mc.method_22683().method_4489() / 2;
               int mouseFromCentreY = mouseYPos - mc.method_22683().method_4506() / 2;
               double oldMousePosX = (double)mouseFromCentreX / this.scale + this.cameraX;
               oldMousePosZ = (double)mouseFromCentreY / this.scale + this.cameraZ;
               preScale = this.scale;
               if (destScale != this.userScale) {
                  if (this.zoomAnim != null) {
                     this.userScale = this.zoomAnim.getCurrent();
                     this.scale = this.userScale * scaleMultiplier;
                  }

                  if (this.zoomAnim == null || MathUtils.round(this.zoomAnim.getDestination(), 4) != MathUtils.round(destScale, 4)) {
                     this.zoomAnim = new SinAnimation(this.userScale, destScale, 100L);
                  }
               }

               if (this.scale > preScale && this.lastZoomMethod != 2) {
                  this.cameraX = oldMousePosX - (double)mouseFromCentreX / this.scale;
                  this.cameraZ = oldMousePosZ - (double)mouseFromCentreY / this.scale;
               }

               int textureLevel = 0;
               if (this.scale >= 1.0D) {
                  fboScale = Math.max(1.0D, Math.floor(this.scale));
               } else {
                  fboScale = this.scale;
               }

               if (this.userScale < 1.0D) {
                  double reversedScale = 1.0D / this.userScale;
                  double log2 = Math.floor(Math.log(reversedScale) / Math.log(2.0D));
                  textureLevel = Math.min((int)log2, 3);
               }

               this.mapProcessor.getMapSaveLoad().mainTextureLevel = textureLevel;
               int leveledRegionShift = 9 + textureLevel;
               secondaryScale = this.scale / fboScale;
               matrixStack.method_22903();
               mousePosX = (double)mouseFromCentreX / this.scale + this.cameraX;
               mousePosZ = (double)mouseFromCentreY / this.scale + this.cameraZ;
               matrixStack.method_22903();
               matrixStack.method_46416(0.0F, 0.0F, 971.0F);
               this.mouseBlockPosX = (int)Math.floor(mousePosX);
               this.mouseBlockPosZ = (int)Math.floor(mousePosZ);
               this.mouseBlockDim = this.mapProcessor.getMapWorld().getCurrentDimension().getDimId();
               this.mouseBlockCoordinateScale = this.getCurrentMapCoordinateScale();
               if (SupportMods.minimap()) {
                  SupportMods.xaeroMinimap.onBlockHover();
               }

               int mouseRegX = this.mouseBlockPosX >> leveledRegionShift;
               int mouseRegZ = this.mouseBlockPosZ >> leveledRegionShift;
               int renderedCaveLayer = this.mapProcessor.getCurrentCaveLayer();
               LeveledRegion<?> reg = this.mapProcessor.getLeveledRegion(renderedCaveLayer, mouseRegX, mouseRegZ, textureLevel);
               int maxRegBlockCoord = (1 << leveledRegionShift) - 1;
               int mouseRegPixelX = (this.mouseBlockPosX & maxRegBlockCoord) >> textureLevel;
               int mouseRegPixelZ = (this.mouseBlockPosZ & maxRegBlockCoord) >> textureLevel;
               this.mouseBlockPosX = (mouseRegX << leveledRegionShift) + (mouseRegPixelX << textureLevel);
               this.mouseBlockPosZ = (mouseRegZ << leveledRegionShift) + (mouseRegPixelZ << textureLevel);
               if (this.mapTileSelection != null && this.rightClickMenu == null) {
                  this.mapTileSelection.setEnd(this.mouseBlockPosX >> 4, this.mouseBlockPosZ >> 4);
               }

               MapRegion leafRegion = this.mapProcessor.getLeafMapRegion(renderedCaveLayer, this.mouseBlockPosX >> 9, this.mouseBlockPosZ >> 9, false);
               MapTileChunk chunk = leafRegion == null ? null : leafRegion.getChunk(this.mouseBlockPosX >> 6 & 7, this.mouseBlockPosZ >> 6 & 7);
               int debugTextureX = this.mouseBlockPosX >> leveledRegionShift - 3 & 7;
               int debugTextureY = this.mouseBlockPosZ >> leveledRegionShift - 3 & 7;
               RegionTexture tex = reg != null && reg.hasTextures() ? reg.getTexture(debugTextureX, debugTextureY) : null;
               boolean debugConfig = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DEBUG);
               int mouseBlockBottomY;
               int i;
               if (debugConfig) {
                  if (reg != null) {
                     List<String> debugLines = new ArrayList();
                     if (tex != null) {
                        tex.addDebugLines(debugLines);
                        MapTile mouseTile = chunk == null ? null : chunk.getTile(this.mouseBlockPosX >> 4 & 3, this.mouseBlockPosZ >> 4 & 3);
                        if (mouseTile != null) {
                           MapBlock block = mouseTile.getBlock(this.mouseBlockPosX & 15, this.mouseBlockPosZ & 15);
                           if (block != null) {
                              guiGraphics.method_25300(mc.field_1772, block.toRenderString(leafRegion.getBiomeRegistry()), this.field_22789 / 2, 22, -1);
                              if (block.getNumberOfOverlays() != 0) {
                                 for(mouseBlockBottomY = 0; mouseBlockBottomY < block.getOverlays().size(); ++mouseBlockBottomY) {
                                    guiGraphics.method_25300(mc.field_1772, ((Overlay)block.getOverlays().get(mouseBlockBottomY)).toRenderString(), this.field_22789 / 2, 32 + mouseBlockBottomY * 10, -1);
                                 }
                              }
                           }
                        }
                     }

                     debugLines.add("");
                     debugLines.add(reg.toString());
                     reg.addDebugLines(debugLines, this.mapProcessor, debugTextureX, debugTextureY);

                     for(i = 0; i < debugLines.size(); ++i) {
                        guiGraphics.method_25303(mc.field_1772, (String)debugLines.get(i), 5, 15 + 10 * i, -1);
                     }
                  }

                  class_2874 dimType = this.mapProcessor.getMapWorld().getCurrentDimension().getDimensionType(this.mapProcessor.getWorldDimensionTypeRegistry());
                  class_2960 dimTypeId = this.mapProcessor.getMapWorld().getCurrentDimension().getDimensionTypeId();
                  guiGraphics.method_25303(mc.field_1772, "MultiWorld ID: " + this.mapProcessor.getMapWorld().getCurrentMultiworld() + " Dim Type: " + String.valueOf(dimType == null ? "unknown" : dimTypeId), 5, 265, -1);
                  LayeredRegionManager regions = this.mapProcessor.getMapWorld().getCurrentDimension().getLayeredMapRegions();
                  guiGraphics.method_25303(mc.field_1772, String.format("regions: %d loaded: %d processed: %d viewed: %d benchmarks %s", regions.size(), regions.loadedCount(), this.mapProcessor.getProcessedCount(), lastAmountOfRegionsViewed, WorldMap.textureUploadBenchmark.getTotalsString()), 5, 275, -1);
                  guiGraphics.method_25303(mc.field_1772, String.format("toLoad: %d toSave: %d tile pool: %d overlays: %d toLoadBranchCache: %d buffers: %d", this.mapProcessor.getMapSaveLoad().getSizeOfToLoad(), this.mapProcessor.getMapSaveLoad().getToSave().size(), this.mapProcessor.getTilePool().size(), this.mapProcessor.getOverlayManager().getNumberOfUniqueOverlays(), this.mapProcessor.getMapSaveLoad().getSizeOfToLoadBranchCache(), WorldMap.textureDirectBufferPool.size()), 5, 285, -1);
                  long i = Runtime.getRuntime().maxMemory();
                  long j = Runtime.getRuntime().totalMemory();
                  long k = Runtime.getRuntime().freeMemory();
                  long l = j - k;
                  int debugFPS = ((IWorldMapMinecraftClient)mc).getXaeroWorldMap_fps();
                  guiGraphics.method_25303(mc.field_1772, String.format("FPS: %d", debugFPS), 5, 295, -1);
                  guiGraphics.method_25303(mc.field_1772, String.format("Mem: % 2d%% %03d/%03dMB", l * 100L / i, bytesToMb(l), bytesToMb(i)), 5, 315, -1);
                  guiGraphics.method_25303(mc.field_1772, String.format("Allocated: % 2d%% %03dMB", j * 100L / i, bytesToMb(j)), 5, 325, -1);
                  guiGraphics.method_25303(mc.field_1772, String.format("Available VRAM: %dMB", this.mapProcessor.getMapLimiter().getAvailableVRAM() / 1024), 5, 335, -1);
               }

               int pixelInsideTexX = mouseRegPixelX & 63;
               i = mouseRegPixelZ & 63;
               boolean hasAmbiguousHeight = false;
               mouseBlockBottomY = 32767;
               int mouseBlockTopY = 32767;
               class_5321<class_1959> pointedAtBiome = null;
               if (tex != null) {
                  mouseBlockBottomY = this.mouseBlockPosY = tex.getHeight(pixelInsideTexX, i);
                  mouseBlockTopY = tex.getTopHeight(pixelInsideTexX, i);
                  hasAmbiguousHeight = this.mouseBlockPosY != mouseBlockTopY;
                  pointedAtBiome = tex.getBiome(pixelInsideTexX, i);
               }

               if (hasAmbiguousHeight) {
                  if (mouseBlockTopY != 32767) {
                     this.mouseBlockPosY = mouseBlockTopY;
                  } else if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.DETECT_AMBIGUOUS_Y)) {
                     this.mouseBlockPosY = 32767;
                  }
               }

               matrixStack.method_22909();
               if (primaryScaleFBO == null || primaryScaleFBO.field_1480 != mc.method_22683().method_4489() || primaryScaleFBO.field_1477 != mc.method_22683().method_4506()) {
                  primaryScaleFBO = new ImprovedFramebuffer(mc.method_22683().method_4489(), mc.method_22683().method_4506(), false);
               }

               if (primaryScaleFBO.field_1476 == -1) {
                  matrixStack.method_22909();
                  return;
               }

               primaryScaleFBO.bindAsMainTarget(false);
               GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
               GlStateManager._clear(16384, class_310.field_1703);
               matrixStack.method_22905((float)(1.0D / this.screenScale), (float)(1.0D / this.screenScale), 1.0F);
               matrixStack.method_46416((float)(mc.method_22683().method_4489() / 2), (float)(mc.method_22683().method_4506() / 2), 0.0F);
               matrixStack.method_22903();
               int flooredCameraX = (int)Math.floor(this.cameraX);
               int flooredCameraZ = (int)Math.floor(this.cameraZ);
               double primaryOffsetX = 0.0D;
               double primaryOffsetY = 0.0D;
               double secondaryOffsetX;
               double secondaryOffsetY;
               double leftBorder;
               double topBorder;
               double bottomBorder;
               if (fboScale < 1.0D) {
                  leftBorder = 1.0D / fboScale;
                  int xInFullPixels = (int)Math.floor(this.cameraX / leftBorder);
                  int zInFullPixels = (int)Math.floor(this.cameraZ / leftBorder);
                  topBorder = (double)xInFullPixels * leftBorder;
                  bottomBorder = (double)zInFullPixels * leftBorder;
                  flooredCameraX = (int)Math.floor(topBorder);
                  flooredCameraZ = (int)Math.floor(bottomBorder);
                  primaryOffsetX = topBorder - (double)flooredCameraX;
                  primaryOffsetY = bottomBorder - (double)flooredCameraZ;
                  secondaryOffsetX = (this.cameraX - topBorder) * fboScale;
                  secondaryOffsetY = (this.cameraZ - bottomBorder) * fboScale;
               } else {
                  secondaryOffsetX = (this.cameraX - (double)flooredCameraX) * fboScale;
                  secondaryOffsetY = (this.cameraZ - (double)flooredCameraZ) * fboScale;
                  int offset;
                  if (secondaryOffsetX >= 1.0D) {
                     offset = (int)secondaryOffsetX;
                     matrixStack.method_46416((float)(-offset), 0.0F, 0.0F);
                     secondaryOffsetX -= (double)offset;
                  }

                  if (secondaryOffsetY >= 1.0D) {
                     offset = (int)secondaryOffsetY;
                     matrixStack.method_46416(0.0F, (float)offset, 0.0F);
                     secondaryOffsetY -= (double)offset;
                  }
               }

               matrixStack.method_22905((float)fboScale, (float)(-fboScale), 1.0F);
               matrixStack.method_22904(-primaryOffsetX, -primaryOffsetY, 0.0D);
               leftBorder = this.cameraX - (double)(mc.method_22683().method_4489() / 2) / this.scale;
               double rightBorder = leftBorder + (double)mc.method_22683().method_4489() / this.scale;
               topBorder = this.cameraZ - (double)(mc.method_22683().method_4506() / 2) / this.scale;
               bottomBorder = topBorder + (double)mc.method_22683().method_4506() / this.scale;
               int minRegX = (int)Math.floor(leftBorder) >> leveledRegionShift;
               int maxRegX = (int)Math.floor(rightBorder) >> leveledRegionShift;
               int minRegZ = (int)Math.floor(topBorder) >> leveledRegionShift;
               int maxRegZ = (int)Math.floor(bottomBorder) >> leveledRegionShift;
               int blockToTextureConversion = 6 + textureLevel;
               int minTextureX = (int)Math.floor(leftBorder) >> blockToTextureConversion;
               int maxTextureX = (int)Math.floor(rightBorder) >> blockToTextureConversion;
               int minTextureZ = (int)Math.floor(topBorder) >> blockToTextureConversion;
               int maxTextureZ = (int)Math.floor(bottomBorder) >> blockToTextureConversion;
               int minLeafRegX = minTextureX << blockToTextureConversion >> 9;
               int maxLeafRegX = (maxTextureX + 1 << blockToTextureConversion) - 1 >> 9;
               int minLeafRegZ = minTextureZ << blockToTextureConversion >> 9;
               int maxLeafRegZ = (maxTextureZ + 1 << blockToTextureConversion) - 1 >> 9;
               lastAmountOfRegionsViewed = (maxRegX - minRegX + 1) * (maxRegZ - minRegZ + 1);
               if (this.mapProcessor.getMapLimiter().getMostRegionsAtATime() < lastAmountOfRegionsViewed) {
                  this.mapProcessor.getMapLimiter().setMostRegionsAtATime(lastAmountOfRegionsViewed);
               }

               this.regionBuffer.clear();
               this.branchRegionBuffer.clear();
               float brightness = this.mapProcessor.getBrightness();
               int globalRegionCacheHashCode = WorldMap.settings.getRegionCacheHashCode();
               int globalCaveStart = this.mapProcessor.getMapWorld().getCurrentDimension().getLayeredMapRegions().getLayer(renderedCaveLayer).getCaveStart();
               int globalCaveDepth = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.CAVE_MODE_DEPTH);
               boolean reloadEverything = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED);
               int globalReloadVersion = (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED_VERSION);
               int globalVersion = (Integer)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.GLOBAL_VERSION);
               boolean prevWaitingForBranchCache = this.prevWaitingForBranchCache;
               this.waitingForBranchCache[0] = false;
               Matrix4f matrix = matrixStack.method_23760().method_23761();
               class_4598 renderTypeBuffers = this.mapProcessor.getCvc().getRenderTypeBuffers();
               MultiTextureRenderTypeRendererProvider rendererProvider = this.mapProcessor.getMultiTextureRenderTypeRenderers();
               MultiTextureRenderTypeRenderer withLightRenderer = rendererProvider.getRenderer((t) -> {
                  RenderSystem.setShaderTexture(0, t);
               }, MultiTextureRenderTypeRendererProvider::defaultTextureBind, CustomRenderTypes.MAP);
               MultiTextureRenderTypeRenderer noLightRenderer = rendererProvider.getRenderer((t) -> {
                  RenderSystem.setShaderTexture(0, t);
               }, MultiTextureRenderTypeRendererProvider::defaultTextureBind, CustomRenderTypes.MAP);
               class_4588 overlayBuffer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_COLOR_OVERLAY);
               LeveledRegion.setComparison(this.mouseBlockPosX >> leveledRegionShift, this.mouseBlockPosZ >> leveledRegionShift, textureLevel, this.mouseBlockPosX >> 9, this.mouseBlockPosZ >> 9);
               LeveledRegion<?> lastUpdatedRootLeveledRegion = null;
               boolean cacheOnlyMode = this.mapProcessor.getMapWorld().isCacheOnlyMode();
               boolean frameRenderedRootTextures = false;
               boolean loadingLeaves = false;
               int leveledRegX = minRegX;

               while(true) {
                  int highlightChunkX;
                  int highlightChunkZ;
                  int leafRegionMinX;
                  int leafRegionMinZ;
                  int leafX;
                  int minZBlocks;
                  int textureSize;
                  int firstTextureZ;
                  int levelDiff;
                  int rootSize;
                  int maxInsideCoord;
                  int firstRootTextureX;
                  int firstRootTextureZ;
                  int firstInsideTextureX;
                  int firstInsideTextureZ;
                  boolean toTheLeft;
                  boolean toTheRight;
                  int subtleTooltipOffset;
                  int insideX;
                  int insideZ;
                  int firstTextureX;
                  float configuredG;
                  float configuredB;
                  if (leveledRegX > maxRegX) {
                     this.lastFrameRenderedRootTextures = frameRenderedRootTextures;
                     LibShaders.WORLD_MAP.setBrightness(brightness);
                     LibShaders.WORLD_MAP.setWithLight(true);
                     rendererProvider.draw(withLightRenderer);
                     LibShaders.WORLD_MAP.setWithLight(false);
                     rendererProvider.draw(noLightRenderer);
                     LeveledRegion<?> nextToLoad = this.mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
                     boolean shouldRequest = false;
                     if (nextToLoad != null) {
                        shouldRequest = nextToLoad.shouldAllowAnotherRegionToLoad();
                     } else {
                        shouldRequest = true;
                     }

                     shouldRequest = shouldRequest && this.mapProcessor.getAffectingLoadingFrequencyCount() < 16;
                     if (shouldRequest && !WorldMap.pauseRequests) {
                        int toRequest = 2;
                        highlightChunkZ = 0;

                        for(leafRegionMinX = 0; leafRegionMinX < this.branchRegionBuffer.size() && highlightChunkZ < toRequest; ++leafRegionMinX) {
                           BranchLeveledRegion region = (BranchLeveledRegion)this.branchRegionBuffer.get(leafRegionMinX);
                           if (!region.reloadHasBeenRequested() && !region.recacheHasBeenRequested() && !region.isLoaded()) {
                              region.setReloadHasBeenRequested(true, "Gui");
                              this.mapProcessor.getMapSaveLoad().requestBranchCache(region, "Gui");
                              if (highlightChunkZ == 0) {
                                 this.mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                              }

                              ++highlightChunkZ;
                           }
                        }

                        toRequest = 1;
                        highlightChunkZ = 0;
                        if (!prevWaitingForBranchCache) {
                           for(leafRegionMinX = 0; leafRegionMinX < this.regionBuffer.size() && highlightChunkZ < toRequest; ++leafRegionMinX) {
                              MapRegion region = (MapRegion)this.regionBuffer.get(leafRegionMinX);
                              if (region != nextToLoad || this.regionBuffer.size() <= 1) {
                                 synchronized(region) {
                                    if (region.canRequestReload_unsynced()) {
                                       if (region.getLoadState() == 2) {
                                          region.requestRefresh(this.mapProcessor);
                                       } else {
                                          this.mapProcessor.getMapSaveLoad().requestLoad(region, "Gui");
                                       }

                                       if (highlightChunkZ == 0) {
                                          this.mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                                       }

                                       ++highlightChunkZ;
                                       if (region.getLoadState() == 4) {
                                          break;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     this.prevWaitingForBranchCache = this.waitingForBranchCache[0];
                     this.prevLoadingLeaves = loadingLeaves;
                     highlightChunkX = this.mouseBlockPosX >> 4;
                     highlightChunkZ = this.mouseBlockPosZ >> 4;
                     leafRegionMinX = highlightChunkX << 4;
                     leafRegionMinZ = highlightChunkX + 1 << 4;
                     int chunkHighlightTopZ = highlightChunkZ << 4;
                     leafX = highlightChunkZ + 1 << 4;
                     MapRenderHelper.renderDynamicHighlight(matrixStack, overlayBuffer, flooredCameraX, flooredCameraZ, leafRegionMinX, leafRegionMinZ, chunkHighlightTopZ, leafX, 0.0F, 0.0F, 0.0F, 0.2F, 1.0F, 1.0F, 1.0F, 0.1569F);
                     MapTileSelection mapTileSelectionToRender = this.mapTileSelection;
                     if (mapTileSelectionToRender == null && this.field_22787.field_1755 instanceof ExportScreen) {
                        mapTileSelectionToRender = ((ExportScreen)this.field_22787.field_1755).getSelection();
                     }

                     int cursorDisplayOffset;
                     if (mapTileSelectionToRender != null) {
                        MapRenderHelper.renderDynamicHighlight(matrixStack, overlayBuffer, flooredCameraX, flooredCameraZ, mapTileSelectionToRender.getLeft() << 4, mapTileSelectionToRender.getRight() + 1 << 4, mapTileSelectionToRender.getTop() << 4, mapTileSelectionToRender.getBottom() + 1 << 4, 0.0F, 0.0F, 0.0F, 0.2F, 1.0F, 0.5F, 0.5F, 0.4F);
                        if (SupportMods.pac() && !this.mapProcessor.getMapWorld().isUsingCustomDimension()) {
                           minZBlocks = (int)Math.floor(this.player.method_23317());
                           textureSize = (int)Math.floor(this.player.method_23321());
                           firstTextureX = minZBlocks >> 4;
                           firstTextureZ = textureSize >> 4;
                           levelDiff = SupportMods.xaeroPac.getClaimDistance();
                           rootSize = firstTextureX - levelDiff;
                           maxInsideCoord = firstTextureZ - levelDiff;
                           firstRootTextureX = firstTextureX + levelDiff;
                           firstRootTextureZ = firstTextureZ + levelDiff;
                           firstInsideTextureX = rootSize << 4;
                           firstInsideTextureZ = firstRootTextureX + 1 << 4;
                           int claimableAreaHighlightTopZ = maxInsideCoord << 4;
                           cursorDisplayOffset = firstRootTextureZ + 1 << 4;
                           MapRenderHelper.renderDynamicHighlight(matrixStack, overlayBuffer, flooredCameraX, flooredCameraZ, firstInsideTextureX, firstInsideTextureZ, claimableAreaHighlightTopZ, cursorDisplayOffset, 0.0F, 0.0F, 1.0F, 0.3F, 0.0F, 0.0F, 1.0F, 0.15F);
                        }
                     }

                     RenderSystem.disableCull();
                     renderTypeBuffers.method_22993();
                     RenderSystem.enableCull();
                     primaryScaleFBO.method_1240();
                     primaryScaleFBO.bindDefaultFramebuffer(mc);
                     matrixStack.method_22909();
                     matrixStack.method_22903();
                     matrixStack.method_22905((float)secondaryScale, (float)secondaryScale, 1.0F);
                     primaryScaleFBO.method_35610();
                     GL11.glTexParameteri(3553, 10240, 9729);
                     GL11.glTexParameteri(3553, 10241, 9729);
                     RenderSystem.depthMask(false);
                     class_4588 colorBackgroundConsumer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_COLOR_FILLER);
                     textureSize = -mc.method_22683().method_4489() / 2;
                     firstTextureX = mc.method_22683().method_4506() / 2 - 5;
                     firstTextureZ = mc.method_22683().method_4489();
                     int lineH = 6;
                     MapRenderHelper.fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), colorBackgroundConsumer, textureSize, firstTextureX, textureSize + firstTextureZ, firstTextureX + lineH, 0.0F, 0.0F, 0.0F, 1.0F);
                     textureSize = mc.method_22683().method_4489() / 2 - 5;
                     firstTextureX = -mc.method_22683().method_4506() / 2;
                     int lineW = 6;
                     levelDiff = mc.method_22683().method_4506();
                     MapRenderHelper.fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), colorBackgroundConsumer, textureSize, firstTextureX, textureSize + lineW, firstTextureX + levelDiff, 0.0F, 0.0F, 0.0F, 1.0F);
                     renderTypeBuffers.method_22993();
                     class_1921 mainFrameRenderType = CustomRenderTypes.GUI_BILINEAR;
                     if (SupportMods.vivecraft) {
                        mainFrameRenderType = CustomRenderTypes.MAP_FRAME_TEXTURE_OVER_TRANSPARENT;
                     }

                     MultiTextureRenderTypeRenderer mainFrameRenderer = rendererProvider.getRenderer((t) -> {
                        RenderSystem.setShaderTexture(0, t);
                     }, MultiTextureRenderTypeRendererProvider::defaultTextureBind, mainFrameRenderType);
                     class_4588 mainFrameVertexConsumer = mainFrameRenderer.begin(primaryScaleFBO.getFramebufferTexture());
                     renderTexturedModalRect(matrixStack.method_23760().method_23761(), mainFrameVertexConsumer, (float)(-mc.method_22683().method_4489() / 2) - (float)secondaryOffsetX, (float)(-mc.method_22683().method_4506() / 2) - (float)secondaryOffsetY, 0, 0, (float)primaryScaleFBO.field_1480, (float)primaryScaleFBO.field_1477, (float)primaryScaleFBO.field_1480, (float)primaryScaleFBO.field_1477, 1.0F, 1.0F, 1.0F, 1.0F);
                     rendererProvider.draw(mainFrameRenderer);
                     RenderSystem.depthMask(true);
                     matrixStack.method_22909();
                     matrixStack.method_22905((float)this.scale, (float)this.scale, 1.0F);
                     double screenSizeBasedScale = scaleMultiplier;
                     WorldMap.trackedPlayerRenderer.update(mc);

                     try {
                        this.viewed = WorldMap.mapElementRenderHandler.render(this, guiGraphics, renderTypeBuffers, rendererProvider, this.cameraX, this.cameraZ, mc.method_22683().method_4489(), mc.method_22683().method_4506(), screenSizeBasedScale, this.scale, playerDimDiv, mousePosX, mousePosZ, brightness, renderedCaveLayer != Integer.MAX_VALUE, this.viewed, mc, partialTicks);
                     } catch (Throwable var170) {
                        WorldMap.LOGGER.error("error rendering map elements", var170);
                        throw var170;
                     }

                     this.viewedInList = false;
                     matrixStack.method_22903();
                     matrixStack.method_46416(0.0F, 0.0F, 970.0F);
                     class_4588 regularUIObjectConsumer = renderTypeBuffers.getBuffer(CustomRenderTypes.GUI_BILINEAR);
                     if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.FOOTSTEPS)) {
                        ArrayList<Double[]> footprints = this.mapProcessor.getFootprints();
                        synchronized(footprints) {
                           for(subtleTooltipOffset = 0; subtleTooltipOffset < footprints.size(); ++subtleTooltipOffset) {
                              Double[] coords = (Double[])footprints.get(subtleTooltipOffset);
                              this.setColourBuffer(1.0F, 0.1F, 0.1F, 1.0F);
                              this.drawDotOnMap(matrixStack, regularUIObjectConsumer, coords[0] / playerDimDiv - this.cameraX, coords[1] / playerDimDiv - this.cameraZ, 0.0F, 1.0D / this.scale);
                           }
                        }
                     }

                     if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.ARROW)) {
                        toTheLeft = scaledPlayerX < leftBorder;
                        toTheRight = scaledPlayerX > rightBorder;
                        boolean down = scaledPlayerZ > bottomBorder;
                        boolean up = scaledPlayerZ < topBorder;
                        float configuredR = 1.0F;
                        configuredG = 1.0F;
                        configuredB = 1.0F;
                        insideX = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.ARROW_COLOR);
                        if (insideX == -2 && !SupportMods.minimap()) {
                           insideX = 0;
                        }

                        if (insideX == -2 && SupportMods.xaeroMinimap.getArrowColorIndex() == -1) {
                           insideX = -1;
                        }

                        float[] c;
                        if (insideX == -1) {
                           insideZ = Misc.getTeamColour((class_1297)(mc.field_1724 == null ? mc.method_1560() : mc.field_1724));
                           if (insideZ == -1) {
                              insideX = 0;
                           } else {
                              configuredR = (float)(insideZ >> 16 & 255) / 255.0F;
                              configuredG = (float)(insideZ >> 8 & 255) / 255.0F;
                              configuredB = (float)(insideZ & 255) / 255.0F;
                           }
                        } else if (insideX == -2) {
                           c = SupportMods.xaeroMinimap.getArrowColor();
                           if (c == null) {
                              insideX = 0;
                           } else {
                              configuredR = c[0];
                              configuredG = c[1];
                              configuredB = c[2];
                           }
                        }

                        if (insideX >= 0) {
                           c = WorldMapConfigConstants.ARROW_COLORS[insideX];
                           configuredR = c[0];
                           configuredG = c[1];
                           configuredB = c[2];
                        }

                        if (!toTheLeft && !toTheRight && !up && !down) {
                           this.setColourBuffer(0.0F, 0.0F, 0.0F, 0.9F);
                           this.drawArrowOnMap(matrixStack, regularUIObjectConsumer, scaledPlayerX - this.cameraX, scaledPlayerZ + 2.0D * scaleMultiplier / this.scale - this.cameraZ, this.player.method_36454(), scaleMultiplier / this.scale);
                           this.setColourBuffer(configuredR, configuredG, configuredB, 1.0F);
                           this.drawArrowOnMap(matrixStack, regularUIObjectConsumer, scaledPlayerX - this.cameraX, scaledPlayerZ - this.cameraZ, this.player.method_36454(), scaleMultiplier / this.scale);
                        } else {
                           double arrowX = scaledPlayerX;
                           double arrowZ = scaledPlayerZ;
                           float a = 0.0F;
                           if (toTheLeft) {
                              a = up ? 1.5F : (down ? 0.5F : 1.0F);
                              arrowX = leftBorder;
                           } else if (toTheRight) {
                              a = up ? 2.5F : (down ? 3.5F : 3.0F);
                              arrowX = rightBorder;
                           }

                           if (down) {
                              arrowZ = bottomBorder;
                           } else if (up) {
                              if (a == 0.0F) {
                                 a = 2.0F;
                              }

                              arrowZ = topBorder;
                           }

                           this.setColourBuffer(0.0F, 0.0F, 0.0F, 0.9F);
                           this.drawFarArrowOnMap(matrixStack, regularUIObjectConsumer, arrowX - this.cameraX, arrowZ + 2.0D * scaleMultiplier / this.scale - this.cameraZ, a, scaleMultiplier / this.scale);
                           this.setColourBuffer(configuredR, configuredG, configuredB, 1.0F);
                           this.drawFarArrowOnMap(matrixStack, regularUIObjectConsumer, arrowX - this.cameraX, arrowZ - this.cameraZ, a, scaleMultiplier / this.scale);
                        }
                     }

                     this.field_22787.method_1531().method_22813(WorldMap.guiTextures);
                     GL11.glTexParameteri(3553, 10240, 9729);
                     GL11.glTexParameteri(3553, 10241, 9729);
                     renderTypeBuffers.method_22993();
                     this.field_22787.method_1531().method_22813(WorldMap.guiTextures);
                     GL11.glTexParameteri(3553, 10240, 9728);
                     GL11.glTexParameteri(3553, 10241, 9728);
                     matrixStack.method_22909();
                     matrixStack.method_22909();
                     class_4588 backgroundVertexBuffer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_COLOR_OVERLAY);
                     cursorDisplayOffset = 0;
                     if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.COORDINATES)) {
                        String coordsString = "X: " + this.mouseBlockPosX;
                        if (mouseBlockBottomY != 32767) {
                           coordsString = coordsString + " Y: " + mouseBlockBottomY;
                        }

                        if (hasAmbiguousHeight && mouseBlockTopY != 32767) {
                           coordsString = coordsString + " (" + mouseBlockTopY + ")";
                        }

                        coordsString = coordsString + " Z: " + this.mouseBlockPosZ;
                        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, this.field_22793, (String)coordsString, this.field_22789 / 2, 2 + cursorDisplayOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer);
                        cursorDisplayOffset += 10;
                     }

                     String subWorldNameToRender;
                     if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.DISPLAY_HOVERED_BIOME) && pointedAtBiome != null) {
                        class_2960 biomeRL = pointedAtBiome.method_29177();
                        subWorldNameToRender = biomeRL == null ? class_1074.method_4662("gui.xaero_wm_unknown_biome", new Object[0]) : class_1074.method_4662("biome." + biomeRL.method_12836() + "." + biomeRL.method_12832(), new Object[0]);
                        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, this.field_22793, (String)subWorldNameToRender, this.field_22789 / 2, 2 + cursorDisplayOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer);
                     }

                     subtleTooltipOffset = 12;
                     if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.DISPLAY_ZOOM)) {
                        double var10000 = (double)Math.round(destScale * 1000.0D);
                        subWorldNameToRender = var10000 / 1000.0D + "x";
                        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, mc.field_1772, (String)subWorldNameToRender, this.field_22789 / 2, this.field_22790 - subtleTooltipOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer);
                     }

                     if (this.mapProcessor.getMapWorld().getCurrentDimension().getFullReloader() != null) {
                        subtleTooltipOffset += 12;
                        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, mc.field_1772, (class_2561)FULL_RELOAD_IN_PROGRESS, this.field_22789 / 2, this.field_22790 - subtleTooltipOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer);
                     }

                     if (this.mapProcessor.getMapWorld().isUsingUnknownDimensionType()) {
                        subtleTooltipOffset += 24;
                        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, mc.field_1772, (class_2561)UNKNOWN_DIMENSION_TYPE2, this.field_22789 / 2, this.field_22790 - subtleTooltipOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer);
                        subtleTooltipOffset += 12;
                        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, mc.field_1772, (class_2561)UNKNOWN_DIMENSION_TYPE1, this.field_22789 / 2, this.field_22790 - subtleTooltipOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer);
                     }

                     if ((Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.DISPLAY_CAVE_MODE_START)) {
                        subtleTooltipOffset += 12;
                        if (globalCaveStart != Integer.MAX_VALUE && globalCaveStart != Integer.MIN_VALUE) {
                           subWorldNameToRender = class_1074.method_4662("gui.xaero_wm_cave_mode_start_display", new Object[]{globalCaveStart});
                           MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, mc.field_1772, (String)subWorldNameToRender, this.field_22789 / 2, this.field_22790 - subtleTooltipOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer);
                        }
                     }

                     if (SupportMods.minimap()) {
                        subWorldNameToRender = SupportMods.xaeroMinimap.getSubWorldNameToRender();
                        if (subWorldNameToRender != null) {
                           subtleTooltipOffset += 24;
                           MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, mc.field_1772, (String)subWorldNameToRender, this.field_22789 / 2, this.field_22790 - subtleTooltipOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer);
                        }
                     }

                     discoveredForHighlights = mouseBlockBottomY != 32767;
                     class_2561 subtleHighlightTooltip = this.mapProcessor.getMapWorld().getCurrentDimension().getHighlightHandler().getBlockHighlightSubtleTooltip(this.mouseBlockPosX, this.mouseBlockPosZ, discoveredForHighlights);
                     if (subtleHighlightTooltip != null) {
                        subtleTooltipOffset += 12;
                        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, mc.field_1772, (class_2561)subtleHighlightTooltip, this.field_22789 / 2, this.field_22790 - subtleTooltipOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer);
                     }

                     renderTypeBuffers.method_22993();
                     this.overWaypointsMenu = false;
                     this.overPlayersMenu = false;
                     boolean renderingMenus = this.waypointMenu || this.playersMenu;
                     if (renderingMenus) {
                        matrixStack.method_22903();
                        matrixStack.method_46416(0.0F, 0.0F, 972.0F);
                     }

                     HoveredMapElementHolder hovered;
                     if (this.waypointMenu) {
                        if (SupportMods.xaeroMinimap.getWaypointsSorted() != null) {
                           hovered = SupportMods.xaeroMinimap.renderWaypointsMenu(guiGraphics, this, this.scale, this.field_22789, this.field_22790, scaledMouseX, scaledMouseY, this.leftMouseButton.isDown, this.leftMouseButton.clicked, this.viewed, mc);
                           if (hovered != null) {
                              this.overWaypointsMenu = true;
                              if (hovered.getElement() instanceof Waypoint) {
                                 this.viewed = hovered;
                                 this.viewedInList = true;
                                 if (this.leftMouseButton.clicked) {
                                    this.cameraDestination = new int[]{(int)((Waypoint)this.viewed.getElement()).getRenderX(), (int)((Waypoint)this.viewed.getElement()).getRenderZ()};
                                    this.leftMouseButton.isDown = false;
                                    boolean closeWaypointsWhenHopping = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.CLOSE_WAYPOINTS_AFTER_HOP);
                                    if (closeWaypointsWhenHopping) {
                                       this.onWaypointsButton(this.waypointsButton);
                                    }
                                 }
                              }
                           }
                        }
                     } else if (this.playersMenu) {
                        hovered = WorldMap.trackedPlayerMenuRenderer.renderMenu(guiGraphics, this, this.scale, this.field_22789, this.field_22790, scaledMouseX, scaledMouseY, this.leftMouseButton.isDown, this.leftMouseButton.clicked, this.viewed, mc);
                        if (hovered != null) {
                           this.overPlayersMenu = true;
                           if (hovered.getElement() instanceof PlayerTrackerMapElement && WorldMap.trackedPlayerMenuRenderer.canJumpTo((PlayerTrackerMapElement)hovered.getElement())) {
                              this.viewed = hovered;
                              this.viewedInList = true;
                              if (this.leftMouseButton.clicked) {
                                 PlayerTrackerMapElement<?> clickedPlayer = (PlayerTrackerMapElement)this.viewed.getElement();
                                 MapDimension clickedPlayerDim = this.mapProcessor.getMapWorld().getDimension(clickedPlayer.getDimension());
                                 class_2874 clickedPlayerDimType = MapDimension.getDimensionType(clickedPlayerDim, clickedPlayer.getDimension(), this.mapProcessor.getWorldDimensionTypeRegistry());
                                 double clickedPlayerDimDiv = this.mapProcessor.getMapWorld().getCurrentDimension().calculateDimDiv(this.mapProcessor.getWorldDimensionTypeRegistry(), clickedPlayerDimType);
                                 double jumpX = clickedPlayer.getX() / clickedPlayerDimDiv;
                                 double jumpZ = clickedPlayer.getZ() / clickedPlayerDimDiv;
                                 this.cameraDestination = new int[]{(int)jumpX, (int)jumpZ};
                                 this.leftMouseButton.isDown = false;
                              }
                           }
                        }
                     }

                     if (renderingMenus) {
                        matrixStack.method_22909();
                     }

                     if (SupportMods.minimap()) {
                        SupportMods.xaeroMinimap.drawSetChange(guiGraphics);
                     }

                     if (SupportMods.pac()) {
                        SupportMods.xaeroPac.onMapRender(this.field_22787, matrixStack, scaledMouseX, scaledMouseY, partialTicks, this.mapProcessor.getWorld().method_27983().method_29177(), highlightChunkX, highlightChunkZ);
                     }
                     break;
                  }

                  for(int leveledRegZ = minRegZ; leveledRegZ <= maxRegZ; ++leveledRegZ) {
                     highlightChunkX = 1 << textureLevel;
                     highlightChunkZ = highlightChunkX * 512;
                     leafRegionMinX = leveledRegX * highlightChunkX;
                     leafRegionMinZ = leveledRegZ * highlightChunkX;
                     LeveledRegion<?> leveledRegion = null;

                     int minXBlocks;
                     for(leafX = 0; leafX < highlightChunkX; ++leafX) {
                        for(minXBlocks = 0; minXBlocks < highlightChunkX; ++minXBlocks) {
                           minZBlocks = leafRegionMinX + leafX;
                           if (minZBlocks >= minLeafRegX && minZBlocks <= maxLeafRegX) {
                              textureSize = leafRegionMinZ + minXBlocks;
                              if (textureSize >= minLeafRegZ && textureSize <= maxLeafRegZ) {
                                 MapRegion region = this.mapProcessor.getLeafMapRegion(renderedCaveLayer, minZBlocks, textureSize, false);
                                 if (region == null) {
                                    region = this.mapProcessor.getLeafMapRegion(renderedCaveLayer, minZBlocks, textureSize, this.mapProcessor.regionExists(renderedCaveLayer, minZBlocks, textureSize));
                                 }

                                 if (region != null) {
                                    if (leveledRegion == null) {
                                       leveledRegion = this.mapProcessor.getLeveledRegion(renderedCaveLayer, leveledRegX, leveledRegZ, textureLevel);
                                    }

                                    if (!prevWaitingForBranchCache) {
                                       synchronized(region) {
                                          if (textureLevel != 0 && region.getLoadState() == 0 && region.loadingNeededForBranchLevel != 0 && region.loadingNeededForBranchLevel != textureLevel) {
                                             region.loadingNeededForBranchLevel = 0;
                                             region.getParent().setShouldCheckForUpdatesRecursive(true);
                                          }

                                          if (region.canRequestReload_unsynced() && (!cacheOnlyMode && (reloadEverything && region.getReloadVersion() != globalReloadVersion || region.getCacheHashCode() != globalRegionCacheHashCode || region.caveStartOutdated(globalCaveStart, globalCaveDepth) || region.getVersion() != globalVersion || region.getLoadState() != 2 && region.shouldCache()) || region.getLoadState() == 0 && (!region.isMetaLoaded() || textureLevel == 0 || region.loadingNeededForBranchLevel == textureLevel) || (region.isMetaLoaded() || region.getLoadState() != 0 || !region.hasHadTerrain()) && region.getHighlightsHash() != region.getDim().getHighlightHandler().getRegionHash(region.getRegionX(), region.getRegionZ()))) {
                                             loadingLeaves = true;
                                             region.calculateSortingDistance();
                                             Misc.addToListOfSmallest(10, this.regionBuffer, region);
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     if (leveledRegion != null) {
                        LeveledRegion<?> rootLeveledRegion = leveledRegion.getRootRegion();
                        if (rootLeveledRegion == leveledRegion) {
                           rootLeveledRegion = null;
                        }

                        if (rootLeveledRegion != null && !rootLeveledRegion.isLoaded()) {
                           if (!rootLeveledRegion.recacheHasBeenRequested() && !rootLeveledRegion.reloadHasBeenRequested()) {
                              rootLeveledRegion.calculateSortingDistance();
                              Misc.addToListOfSmallest(10, this.branchRegionBuffer, (BranchLeveledRegion)rootLeveledRegion);
                           }

                           this.waitingForBranchCache[0] = true;
                           rootLeveledRegion = null;
                        }

                        if (!this.mapProcessor.isUploadingPaused() && !WorldMap.pauseRequests) {
                           BranchLeveledRegion branchRegion;
                           if (leveledRegion instanceof BranchLeveledRegion) {
                              branchRegion = (BranchLeveledRegion)leveledRegion;
                              branchRegion.checkForUpdates(this.mapProcessor, prevWaitingForBranchCache, this.waitingForBranchCache, this.branchRegionBuffer, textureLevel, minLeafRegX, minLeafRegZ, maxLeafRegX, maxLeafRegZ);
                           }

                           if ((textureLevel != 0 && !prevWaitingForBranchCache || textureLevel == 0 && !this.prevLoadingLeaves) && this.lastFrameRenderedRootTextures && rootLeveledRegion != null && rootLeveledRegion != lastUpdatedRootLeveledRegion) {
                              branchRegion = (BranchLeveledRegion)rootLeveledRegion;
                              branchRegion.checkForUpdates(this.mapProcessor, prevWaitingForBranchCache, this.waitingForBranchCache, this.branchRegionBuffer, textureLevel, minLeafRegX, minLeafRegZ, maxLeafRegX, maxLeafRegZ);
                              lastUpdatedRootLeveledRegion = rootLeveledRegion;
                           }

                           this.mapProcessor.getMapWorld().getCurrentDimension().getLayeredMapRegions().bumpLoadedRegion(leveledRegion);
                           if (rootLeveledRegion != null) {
                              this.mapProcessor.getMapWorld().getCurrentDimension().getLayeredMapRegions().bumpLoadedRegion(rootLeveledRegion);
                           }
                        } else {
                           this.waitingForBranchCache[0] = prevWaitingForBranchCache;
                        }

                        minXBlocks = leveledRegX * highlightChunkZ;
                        minZBlocks = leveledRegZ * highlightChunkZ;
                        textureSize = 64 * highlightChunkX;
                        firstTextureX = leveledRegX << 3;
                        firstTextureZ = leveledRegZ << 3;
                        levelDiff = 3 - textureLevel;
                        rootSize = 1 << levelDiff;
                        maxInsideCoord = rootSize - 1;
                        firstRootTextureX = firstTextureX >> levelDiff & 7;
                        firstRootTextureZ = firstTextureZ >> levelDiff & 7;
                        firstInsideTextureX = firstTextureX & maxInsideCoord;
                        firstInsideTextureZ = firstTextureZ & maxInsideCoord;
                        toTheLeft = leveledRegion.hasTextures();
                        toTheRight = rootLeveledRegion != null && rootLeveledRegion.hasTextures();
                        int textureX;
                        int p;
                        int textureZ;
                        if (toTheLeft || toTheRight) {
                           for(subtleTooltipOffset = 0; subtleTooltipOffset < 8; ++subtleTooltipOffset) {
                              textureX = minXBlocks + subtleTooltipOffset * textureSize;
                              if (!((double)textureX > rightBorder) && !((double)(textureX + textureSize) < leftBorder)) {
                                 for(p = 0; p < 8; ++p) {
                                    textureZ = minZBlocks + p * textureSize;
                                    if (!((double)textureZ > bottomBorder) && !((double)(textureZ + textureSize) < topBorder)) {
                                       RegionTexture<?> regionTexture = toTheLeft ? leveledRegion.getTexture(subtleTooltipOffset, p) : null;
                                       if (regionTexture != null && regionTexture.getGlColorTexture() != -1) {
                                          insideX = regionTexture.getGlColorTexture();
                                          if (insideX != -1) {
                                             boolean hasLight = regionTexture.getTextureHasLight();
                                             renderTexturedModalRectWithLighting3(matrix, (float)(textureX - flooredCameraX), (float)(textureZ - flooredCameraZ), (float)textureSize, (float)textureSize, insideX, hasLight, hasLight ? withLightRenderer : noLightRenderer);
                                          }
                                       } else if (toTheRight) {
                                          insideX = firstInsideTextureX + subtleTooltipOffset;
                                          insideZ = firstInsideTextureZ + p;
                                          int rootTextureX = firstRootTextureX + (insideX >> levelDiff);
                                          int rootTextureZ = firstRootTextureZ + (insideZ >> levelDiff);
                                          regionTexture = rootLeveledRegion.getTexture(rootTextureX, rootTextureZ);
                                          if (regionTexture != null) {
                                             int texture = regionTexture.getGlColorTexture();
                                             if (texture != -1) {
                                                frameRenderedRootTextures = true;
                                                int insideTextureX = insideX & maxInsideCoord;
                                                int insideTextureZ = insideZ & maxInsideCoord;
                                                float textureX1 = (float)insideTextureX / (float)rootSize;
                                                float textureX2 = (float)(insideTextureX + 1) / (float)rootSize;
                                                float textureY1 = (float)insideTextureZ / (float)rootSize;
                                                float textureY2 = (float)(insideTextureZ + 1) / (float)rootSize;
                                                boolean hasLight = regionTexture.getTextureHasLight();
                                                renderTexturedModalSubRectWithLighting(matrix, (float)(textureX - flooredCameraX), (float)(textureZ - flooredCameraZ), textureX1, textureY1, textureX2, textureY2, (float)textureSize, (float)textureSize, texture, hasLight, hasLight ? withLightRenderer : noLightRenderer);
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }

                        if (leveledRegion.loadingAnimation()) {
                           matrixStack.method_22903();
                           matrixStack.method_22904((double)highlightChunkZ * ((double)leveledRegX + 0.5D) - (double)flooredCameraX, (double)highlightChunkZ * ((double)leveledRegZ + 0.5D) - (double)flooredCameraZ, 0.0D);
                           float loadingAnimationPassed = (float)(System.currentTimeMillis() - this.loadingAnimationStart);
                           if (loadingAnimationPassed > 0.0F) {
                              int period = 2000;
                              int numbersOfActors = 3;
                              configuredG = loadingAnimationPassed % (float)period / (float)period * 360.0F;
                              configuredB = 360.0F / (float)numbersOfActors;
                              OptimizedMath.rotatePose(matrixStack, configuredG, OptimizedMath.ZP);
                              insideX = 1 + (int)loadingAnimationPassed % (3 * period) / period;
                              matrixStack.method_22905((float)highlightChunkX, (float)highlightChunkX, 1.0F);

                              for(insideZ = 0; insideZ < insideX; ++insideZ) {
                                 OptimizedMath.rotatePose(matrixStack, configuredB, OptimizedMath.ZP);
                                 MapRenderHelper.fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), overlayBuffer, 16, -8, 32, 8, 1.0F, 1.0F, 1.0F, 1.0F);
                              }
                           }

                           matrixStack.method_22909();
                        }

                        if (debugConfig && leveledRegion instanceof MapRegion) {
                           MapRegion region = (MapRegion)leveledRegion;
                           matrixStack.method_22903();
                           matrixStack.method_46416((float)(512 * region.getRegionX() + 32 - flooredCameraX), (float)(512 * region.getRegionZ() + 32 - flooredCameraZ), 0.0F);
                           matrixStack.method_22905(10.0F, 10.0F, 1.0F);
                           Misc.drawNormalText(matrixStack, region.getLoadState().makeConcatWithConstants<invokedynamic>(region.getLoadState()), 0.0F, 0.0F, -1, true, renderTypeBuffers);
                           matrixStack.method_22909();
                        }

                        if (debugConfig && textureLevel > 0) {
                           for(subtleTooltipOffset = 0; subtleTooltipOffset < highlightChunkX; ++subtleTooltipOffset) {
                              for(textureX = 0; textureX < highlightChunkX; ++textureX) {
                                 p = leafRegionMinX + subtleTooltipOffset;
                                 textureZ = leafRegionMinZ + textureX;
                                 MapRegion region = this.mapProcessor.getLeafMapRegion(renderedCaveLayer, p, textureZ, false);
                                 if (region != null) {
                                    boolean currentlyLoading = this.mapProcessor.getMapSaveLoad().getNextToLoadByViewing() == region;
                                    if (currentlyLoading || region.isLoaded() || region.isMetaLoaded()) {
                                       matrixStack.method_22903();
                                       matrixStack.method_46416((float)(512 * region.getRegionX() - flooredCameraX), (float)(512 * region.getRegionZ() - flooredCameraZ), 0.0F);
                                       float r = 0.0F;
                                       float g = 0.0F;
                                       float b = 0.0F;
                                       float a = 0.1569F;
                                       if (currentlyLoading) {
                                          b = 1.0F;
                                          r = 1.0F;
                                       } else if (region.isLoaded()) {
                                          g = 1.0F;
                                       } else {
                                          g = 1.0F;
                                          r = 1.0F;
                                       }

                                       MapRenderHelper.fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), overlayBuffer, 0, 0, 512, 512, r, g, b, a);
                                       matrixStack.method_22909();
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }

                  ++leveledRegX;
               }
            } else if (!mapLoaded) {
               this.renderLoadingScreen(guiGraphics);
            } else if (isLocked) {
               this.renderMessageScreen(guiGraphics, class_1074.method_4662("gui.xaero_current_map_locked1", new Object[0]), class_1074.method_4662("gui.xaero_current_map_locked2", new Object[0]));
            } else if (noWorldMapEffect) {
               this.renderMessageScreen(guiGraphics, class_1074.method_4662("gui.xaero_no_world_map_message", new Object[0]));
            } else if (!allowedBasedOnItem) {
               String configuredMapItemString = (String)configManager.getEffective(WorldMapProfiledConfigOptions.MAP_ITEM);
               String var10002 = class_1074.method_4662("gui.xaero_no_world_map_item_message", new Object[0]);
               String var10003 = mapItem.method_7848().getString();
               this.renderMessageScreen(guiGraphics, var10002, var10003 + " (" + configuredMapItemString + ")");
            }
         }

         this.mapSwitchingGui.renderText(guiGraphics, this.field_22787, scaledMouseX, scaledMouseY, this.field_22789, this.field_22790);
         guiGraphics.method_25302(WorldMap.guiTextures, this.field_22789 - 34, 2, 0, 37, 32, 32);
      }

      matrixStack.method_22903();
      matrixStack.method_46416(0.0F, 0.0F, 973.0F);
      super.method_25394(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
      if (this.rightClickMenu != null) {
         this.rightClickMenu.method_25394(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
      }

      matrixStack.method_46416(0.0F, 0.0F, 10.0F);
      if (mc.field_1755 == this) {
         if (!this.renderTooltips(guiGraphics, scaledMouseX, scaledMouseY, partialTicks) && !this.leftMouseButton.isDown && !this.rightMouseButton.isDown) {
            if (this.viewed != null) {
               Tooltip hoveredTooltip = this.hoveredElementTooltipHelper(this.viewed, this.viewedInList);
               if (hoveredTooltip != null) {
                  hoveredTooltip.drawBox(guiGraphics, scaledMouseX, scaledMouseY, this.field_22789, this.field_22790);
               }
            } else {
               synchronized(this.mapProcessor.renderThreadPauseSync) {
                  if (!this.mapProcessor.isRenderingPaused() && this.mapProcessor.getCurrentWorldId() != null && this.mapProcessor.getMapSaveLoad().isRegionDetectionComplete()) {
                     class_2561 bluntHighlightTooltip = this.mapProcessor.getMapWorld().getCurrentDimension().getHighlightHandler().getBlockHighlightBluntTooltip(this.mouseBlockPosX, this.mouseBlockPosZ, discoveredForHighlights);
                     if (bluntHighlightTooltip != null) {
                        (new Tooltip(bluntHighlightTooltip)).drawBox(guiGraphics, scaledMouseX, scaledMouseY, this.field_22789, this.field_22790);
                     }
                  }
               }
            }
         }

         matrixStack.method_46416(0.0F, 0.0F, 1.0F);
         this.mapProcessor.getMessageBoxRenderer().render(guiGraphics, this.mapProcessor.getMessageBox(), this.field_22793, 1, this.field_22790 / 2, false);
      }

      matrixStack.method_22909();
      this.leftMouseButton.clicked = this.rightMouseButton.clicked = false;
      this.noUploadingLimits = this.cameraX == cameraXBefore && this.cameraZ == cameraZBefore && scaleBefore == this.scale;
      MapRenderHelper.restoreDefaultShaderBlendState();
   }

   public void method_25420(class_332 guiGraphics, int i, int j, float f) {
   }

   protected void renderPreDropdown(class_332 guiGraphics, int scaledMouseX, int scaledMouseY, float partialTicks) {
      super.renderPreDropdown(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
      if (this.waypointMenu) {
         SupportMods.xaeroMinimap.getWaypointMenuRenderer().postMapRender(guiGraphics, this, scaledMouseX, scaledMouseY, this.field_22789, this.field_22790, partialTicks);
      }

      if (this.playersMenu) {
         WorldMap.trackedPlayerMenuRenderer.postMapRender(guiGraphics, this, scaledMouseX, scaledMouseY, this.field_22789, this.field_22790, partialTicks);
      }

      this.mapSwitchingGui.postMapRender(guiGraphics, this.field_22787, scaledMouseX, scaledMouseY, this.field_22789, this.field_22790);
   }

   private <E, C> Tooltip hoveredElementTooltipHelper(HoveredMapElementHolder<E, C> hovered, boolean viewedInList) {
      return hovered.getRenderer().getReader().getTooltip(hovered.getElement(), hovered.getRenderer().getContext(), viewedInList);
   }

   private void renderLoadingScreen(class_332 guiGraphics) {
      this.renderMessageScreen(guiGraphics, "Preparing World Map...");
   }

   private void renderMessageScreen(class_332 guiGraphics, String message) {
      this.renderMessageScreen(guiGraphics, message, (String)null);
   }

   private void renderMessageScreen(class_332 guiGraphics, String message, String message2) {
      class_4587 matrixStack = guiGraphics.method_51448();
      guiGraphics.method_25294(0, 0, this.field_22787.method_22683().method_4489(), this.field_22787.method_22683().method_4506(), -16777216);
      matrixStack.method_22903();
      matrixStack.method_46416(0.0F, 0.0F, 500.0F);
      guiGraphics.method_25300(this.field_22787.field_1772, message, this.field_22787.method_22683().method_4486() / 2, this.field_22787.method_22683().method_4502() / 2, -1);
      if (message2 != null) {
         guiGraphics.method_25300(this.field_22787.field_1772, message2, this.field_22787.method_22683().method_4486() / 2, this.field_22787.method_22683().method_4502() / 2 + 10, -1);
      }

      matrixStack.method_22909();
   }

   public void drawDotOnMap(class_4587 matrixStack, class_4588 guiLinearBuffer, double x, double z, float angle, double sc) {
      this.drawObjectOnMap(matrixStack, guiLinearBuffer, x, z, angle, sc, 2.5F, 2.5F, 0, 69, 5, 5, 9729);
   }

   public void drawArrowOnMap(class_4587 matrixStack, class_4588 guiLinearBuffer, double x, double z, float angle, double sc) {
      this.drawObjectOnMap(matrixStack, guiLinearBuffer, x, z, angle, sc, 13.0F, 5.0F, 0, 0, 26, 28, 9729);
   }

   public void drawFarArrowOnMap(class_4587 matrixStack, class_4588 guiLinearBuffer, double x, double z, float angle, double sc) {
      this.drawObjectOnMap(matrixStack, guiLinearBuffer, x, z, angle * 90.0F, sc, 27.0F, 13.0F, 26, 0, 54, 13, 9729);
   }

   public void drawObjectOnMap(class_4587 matrixStack, class_4588 guiLinearBuffer, double x, double z, float angle, double sc, float offX, float offY, int textureX, int textureY, int w, int h, int filter) {
      matrixStack.method_22903();
      matrixStack.method_22904(x, z, 0.0D);
      matrixStack.method_22905((float)sc, (float)sc, 1.0F);
      if (angle != 0.0F) {
         OptimizedMath.rotatePose(matrixStack, angle, OptimizedMath.ZP);
      }

      Matrix4f matrix = matrixStack.method_23760().method_23761();
      renderTexturedModalRect(matrix, guiLinearBuffer, -offX, -offY, textureX, textureY, (float)w, (float)h, 256.0F, 256.0F, this.colourBuffer[0], this.colourBuffer[1], this.colourBuffer[2], this.colourBuffer[3]);
      matrixStack.method_22909();
   }

   public static void renderTexturedModalRectWithLighting3(Matrix4f matrix, float x, float y, float width, float height, int texture, boolean hasLight, MultiTextureRenderTypeRenderer renderer) {
      buildTexturedModalRectWithLighting(matrix, renderer.begin(texture), x, y, width, height);
   }

   public static void renderTexturedModalSubRectWithLighting(Matrix4f matrix, float x, float y, float textureX1, float textureY1, float textureX2, float textureY2, float width, float height, int texture, boolean hasLight, MultiTextureRenderTypeRenderer renderer) {
      buildTexturedModalSubRectWithLighting(matrix, renderer.begin(texture), x, y, textureX1, textureY1, textureX2, textureY2, width, height);
   }

   public static void buildTexturedModalRectWithLighting(Matrix4f matrix, class_287 vertexBuffer, float x, float y, float width, float height) {
      vertexBuffer.method_22918(matrix, x + 0.0F, y + height, 0.0F).method_22913(0.0F, 1.0F);
      vertexBuffer.method_22918(matrix, x + width, y + height, 0.0F).method_22913(1.0F, 1.0F);
      vertexBuffer.method_22918(matrix, x + width, y + 0.0F, 0.0F).method_22913(1.0F, 0.0F);
      vertexBuffer.method_22918(matrix, x + 0.0F, y + 0.0F, 0.0F).method_22913(0.0F, 0.0F);
   }

   public static void buildTexturedModalSubRectWithLighting(Matrix4f matrix, class_287 vertexBuffer, float x, float y, float textureX1, float textureY1, float textureX2, float textureY2, float width, float height) {
      vertexBuffer.method_22918(matrix, x + 0.0F, y + height, 0.0F).method_22913(textureX1, textureY2);
      vertexBuffer.method_22918(matrix, x + width, y + height, 0.0F).method_22913(textureX2, textureY2);
      vertexBuffer.method_22918(matrix, x + width, y + 0.0F, 0.0F).method_22913(textureX2, textureY1);
      vertexBuffer.method_22918(matrix, x + 0.0F, y + 0.0F, 0.0F).method_22913(textureX1, textureY1);
   }

   public static void renderTexturedModalRect(Matrix4f matrix, class_4588 vertexBuffer, float x, float y, int textureX, int textureY, float width, float height, float textureWidth, float textureHeight, float r, float g, float b, float a) {
      float normalizedTextureX = (float)textureX / textureWidth;
      float normalizedTextureY = (float)textureY / textureHeight;
      float normalizedTextureX2 = ((float)textureX + width) / textureWidth;
      float normalizedTextureY2 = ((float)textureY + height) / textureHeight;
      vertexBuffer.method_22918(matrix, x + 0.0F, y + height, 0.0F).method_22915(r, g, b, a).method_22913(normalizedTextureX, normalizedTextureY2);
      vertexBuffer.method_22918(matrix, x + width, y + height, 0.0F).method_22915(r, g, b, a).method_22913(normalizedTextureX2, normalizedTextureY2);
      vertexBuffer.method_22918(matrix, x + width, y + 0.0F, 0.0F).method_22915(r, g, b, a).method_22913(normalizedTextureX2, normalizedTextureY);
      vertexBuffer.method_22918(matrix, x + 0.0F, y + 0.0F, 0.0F).method_22915(r, g, b, a).method_22913(normalizedTextureX, normalizedTextureY);
   }

   public void mapClicked(int button, int x, int y) {
      if (button == 1) {
         if (this.viewedOnMousePress == null || !this.viewedOnMousePress.isRightClickValid() || this.viewedOnMousePress.getElement() instanceof Waypoint && !SupportMods.xaeroMinimap.waypointExists((Waypoint)this.viewedOnMousePress.getElement())) {
            this.handleRightClick(this, (int)((double)x / this.screenScale), (int)((double)y / this.screenScale));
         } else {
            this.handleRightClick(this.viewedOnMousePress, (int)((double)x / this.screenScale), (int)((double)y / this.screenScale));
            this.mouseDownPosX = -1;
            this.mouseDownPosY = -1;
            this.mapTileSelection = null;
         }
      }

   }

   private void handleRightClick(IRightClickableElement target, int x, int y) {
      if (this.rightClickMenu != null) {
         this.rightClickMenu.setClosed(true);
      }

      this.rightClickMenu = GuiRightClickMenu.getMenu(target, this, x, y, 150);
   }

   public boolean method_25400(char par1, int par2) {
      boolean result = super.method_25400(par1, par2);
      if (this.waypointMenu && SupportMods.xaeroMinimap.getWaypointMenuRenderer().charTyped()) {
         return true;
      } else {
         return this.playersMenu && WorldMap.trackedPlayerMenuRenderer.charTyped() ? true : result;
      }
   }

   public boolean method_25404(int par1, int par2, int par3) {
      if (par1 == 258) {
         ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
         boolean minimapRadarConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.MINIMAP_RADAR);
         if (this.tabPressed && SupportMods.minimap() && minimapRadarConfig && class_310.method_1551().field_1690.field_1907.method_1417(par1, par2)) {
            return true;
         }

         this.tabPressed = true;
      }

      boolean result = super.method_25404(par1, par2, par3);
      if (this.isUsingTextField()) {
         if (this.waypointMenu && SupportMods.xaeroMinimap.getWaypointMenuRenderer().keyPressed(this, par1)) {
            result = true;
         } else if (this.playersMenu && WorldMap.trackedPlayerMenuRenderer.keyPressed(this, par1)) {
            result = true;
         }
      } else {
         result = this.onInputPress(par1 != -1 ? class_307.field_1668 : class_307.field_1671, par1 != -1 ? par1 : par2) || result;
      }

      return result;
   }

   public boolean method_16803(int par1, int par2, int par3) {
      if (par1 == 258) {
         this.tabPressed = false;
      }

      return this.onInputRelease(par1 != -1 ? class_307.field_1668 : class_307.field_1671, par1 != -1 ? par1 : par2) ? true : super.method_16803(par1, par2, par3);
   }

   private static long bytesToMb(long bytes) {
      return bytes / 1024L / 1024L;
   }

   private void setColourBuffer(float r, float g, float b, float a) {
      this.colourBuffer[0] = r;
      this.colourBuffer[1] = g;
      this.colourBuffer[2] = b;
      this.colourBuffer[3] = a;
   }

   private boolean isUsingTextField() {
      class_339 currentFocused = (class_339)this.method_25399();
      return currentFocused != null && currentFocused.method_25370() && currentFocused instanceof class_342;
   }

   public void method_25393() {
      super.method_25393();
      if (this.waypointMenu) {
         SupportMods.xaeroMinimap.getWaypointMenuRenderer().tick();
      }

      if (this.playersMenu) {
         WorldMap.trackedPlayerMenuRenderer.tick();
      }

      this.caveModeOptions.tick(this);
   }

   public class_304 getTrackedPlayerKeyBinding() {
      return SupportMods.minimap() ? SupportMods.xaeroMinimap.getToggleAllyPlayersKey() : ControlsRegister.keyToggleTrackedPlayers;
   }

   private boolean onInputPress(class_307 type, int code) {
      if (KeyMappingUtils.inputMatches(type, code, ControlsRegister.keyOpenSettings, 0)) {
         this.onSettingsButton(this.settingsButton);
         return true;
      } else {
         boolean result = false;
         if (KeyMappingUtils.inputMatches(type, code, this.field_22787.field_1690.field_1907, 0)) {
            this.field_22787.field_1690.field_1907.method_23481(true);
            result = true;
         }

         if (KeyMappingUtils.inputMatches(type, code, ControlsRegister.keyOpenMap, 0)) {
            this.goBack();
            result = true;
         }

         if (KeyMappingUtils.inputMatches(type, code, this.getTrackedPlayerKeyBinding(), 0)) {
            WorldMap.trackedPlayerMenuRenderer.onShowPlayersButton(this, this.field_22789, this.field_22790);
            return true;
         } else {
            if ((type == class_307.field_1668 && code == 257 || KeyMappingUtils.inputMatches(type, code, ControlsRegister.keyQuickConfirm, 0)) && this.mapSwitchingGui.active) {
               this.mapSwitchingGui.confirm(this, this.field_22787, this.field_22789, this.field_22790);
               result = true;
            }

            if (KeyMappingUtils.inputMatches(type, code, ControlsRegister.keyToggleDimension, 1)) {
               this.onDimensionToggleButton(this.dimensionToggleButton);
               result = true;
            }

            if (SupportMods.minimap()) {
               SupportMods.xaeroMinimap.onMapKeyPressed(type, code, this);
               result = true;
            }

            if (SupportMods.pac()) {
               result = SupportMods.xaeroPac.onMapKeyPressed(type, code, this) || result;
            }

            IRightClickableElement hoverTarget = this.getHoverTarget();
            if (hoverTarget != null && type == class_307.field_1668) {
               boolean isValid = hoverTarget.isRightClickValid();
               if (isValid) {
                  if (hoverTarget instanceof HoveredMapElementHolder && ((HoveredMapElementHolder)hoverTarget).getElement() instanceof Waypoint) {
                     switch(code) {
                     case 72:
                        SupportMods.xaeroMinimap.disableWaypoint((Waypoint)((HoveredMapElementHolder)hoverTarget).getElement());
                        this.closeRightClick();
                        result = true;
                        break;
                     case 261:
                        SupportMods.xaeroMinimap.deleteWaypoint((Waypoint)((HoveredMapElementHolder)hoverTarget).getElement());
                        this.closeRightClick();
                        result = true;
                     }
                  } else if (SupportMods.pac() && hoverTarget instanceof HoveredMapElementHolder && ((HoveredMapElementHolder)hoverTarget).getElement() instanceof PlayerTrackerMapElement) {
                     switch(code) {
                     case 67:
                        SupportMods.xaeroPac.openPlayerConfigScreen(this, this, (PlayerTrackerMapElement)((HoveredMapElementHolder)hoverTarget).getElement());
                        this.closeRightClick();
                        result = true;
                     }
                  }
               } else {
                  this.closeRightClick();
               }
            }

            return result;
         }
      }
   }

   private double getCurrentMapCoordinateScale() {
      return this.mapProcessor.getMapWorld().getCurrentDimension().calculateDimScale(this.mapProcessor.getWorldDimensionTypeRegistry());
   }

   private boolean onInputRelease(class_307 type, int code) {
      boolean result = false;
      if (KeyMappingUtils.inputMatches(type, code, this.field_22787.field_1690.field_1907, 0)) {
         this.field_22787.field_1690.field_1907.method_23481(false);
         result = true;
      }

      if (SupportMods.minimap() && SupportMods.xaeroMinimap.onMapKeyReleased(type, code, this)) {
         result = true;
      }

      if (SupportMods.minimap() && this.lastViewedDimensionId != null && !this.isUsingTextField()) {
         ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
         boolean waypointsConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.WAYPOINTS);
         int waypointDestinationX = this.mouseBlockPosX;
         int waypointDestinationY = this.mouseBlockPosY;
         int waypointDestinationZ = this.mouseBlockPosZ;
         double waypointDestinationCoordinateScale = this.mouseBlockCoordinateScale;
         boolean waypointDestinationRightClick = false;
         if (this.rightClickMenu != null && this.rightClickMenu.getTarget() == this) {
            waypointDestinationX = this.rightClickX;
            waypointDestinationY = this.rightClickY;
            waypointDestinationZ = this.rightClickZ;
            waypointDestinationCoordinateScale = this.rightClickCoordinateScale;
            waypointDestinationRightClick = true;
         }

         if (KeyMappingUtils.inputMatches(type, code, SupportMods.xaeroMinimap.getWaypointKeyBinding(), 0) && waypointsConfig) {
            SupportMods.xaeroMinimap.createWaypoint(this, waypointDestinationX, waypointDestinationY == 32767 ? 32767 : waypointDestinationY + 1, waypointDestinationZ, waypointDestinationCoordinateScale, waypointDestinationRightClick);
            this.closeRightClick();
            result = true;
         }

         if (KeyMappingUtils.inputMatches(type, code, SupportMods.xaeroMinimap.getTempWaypointKeyBinding(), 0) && waypointsConfig) {
            this.closeRightClick();
            SupportMods.xaeroMinimap.createTempWaypoint(waypointDestinationX, waypointDestinationY == 32767 ? 32767 : waypointDestinationY + 1, waypointDestinationZ, waypointDestinationCoordinateScale, waypointDestinationRightClick);
            result = true;
         }

         IRightClickableElement hoverTarget = this.getHoverTarget();
         if (hoverTarget != null && !KeyMappingUtils.inputMatches(type, code, ControlsRegister.keyOpenMap, 0) && type == class_307.field_1668) {
            boolean isValid = hoverTarget.isRightClickValid();
            if (isValid) {
               if (hoverTarget instanceof HoveredMapElementHolder && ((HoveredMapElementHolder)hoverTarget).getElement() instanceof Waypoint) {
                  switch(code) {
                  case 69:
                     SupportMods.xaeroMinimap.openWaypoint(this, (Waypoint)((HoveredMapElementHolder)hoverTarget).getElement());
                     this.closeRightClick();
                     result = true;
                     break;
                  case 84:
                     SupportMods.xaeroMinimap.teleportToWaypoint(this, (Waypoint)((HoveredMapElementHolder)hoverTarget).getElement());
                     this.closeRightClick();
                     result = true;
                  }
               } else if (hoverTarget instanceof HoveredMapElementHolder && ((HoveredMapElementHolder)hoverTarget).getElement() instanceof PlayerTrackerMapElement) {
                  switch(code) {
                  case 84:
                     (new PlayerTeleporter()).teleportToPlayer(this, this.mapProcessor.getMapWorld(), (PlayerTrackerMapElement)((HoveredMapElementHolder)hoverTarget).getElement());
                     this.closeRightClick();
                     result = true;
                  }
               }
            } else {
               this.closeRightClick();
            }
         }
      }

      return result;
   }

   private IRightClickableElement getHoverTarget() {
      return (IRightClickableElement)(this.rightClickMenu != null ? this.rightClickMenu.getTarget() : this.viewed);
   }

   private void unfocusAll() {
      if (SupportMods.minimap()) {
         SupportMods.xaeroMinimap.getWaypointMenuRenderer().unfocusAll();
      }

      WorldMap.trackedPlayerMenuRenderer.unfocusAll();
      this.caveModeOptions.unfocusAll();
      this.method_25395((class_364)null);
   }

   public void closeRightClick() {
      if (this.rightClickMenu != null) {
         this.rightClickMenu.setClosed(true);
      }

   }

   public void onRightClickClosed() {
      this.rightClickMenu = null;
      this.mapTileSelection = null;
   }

   private void closeDropdowns() {
      if (this.openDropdown != null) {
         this.openDropdown.setClosed(true);
      }

   }

   public ArrayList<RightClickOption> getRightClickOptions() {
      ArrayList<RightClickOption> options = new ArrayList();
      options.add(new RightClickOption(this, "gui.xaero_right_click_map_title", options.size(), this) {
         public void onAction(class_437 screen) {
         }
      });
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      boolean coordinatesConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.COORDINATES);
      boolean waypointsConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.WAYPOINTS);
      if (coordinatesConfig && (!SupportMods.minimap() || !SupportMods.xaeroMinimap.hidingWaypointCoordinates())) {
         if (this.mapTileSelection != null) {
            String chunkOption = this.mapTileSelection.getStartX() == this.mapTileSelection.getEndX() && this.mapTileSelection.getStartZ() == this.mapTileSelection.getEndZ() ? String.format("C: (%d;%d)", this.mapTileSelection.getLeft(), this.mapTileSelection.getTop()) : String.format("C: (%d;%d):(%d;%d)", this.mapTileSelection.getLeft(), this.mapTileSelection.getTop(), this.mapTileSelection.getRight(), this.mapTileSelection.getBottom());
            options.add(new RightClickOption(this, chunkOption, options.size(), this) {
               public void onAction(class_437 screen) {
               }
            });
         }

         options.add(new RightClickOption(this, String.format(this.rightClickY != 32767 ? "X: %1$d, Y: %2$d, Z: %3$d" : "X: %1$d, Z: %3$d", this.rightClickX, this.rightClickY, this.rightClickZ), options.size(), this) {
            public void onAction(class_437 screen) {
            }
         });
      }

      if (SupportMods.minimap() && waypointsConfig) {
         options.add((new RightClickOption("gui.xaero_right_click_map_create_waypoint", options.size(), this) {
            public void onAction(class_437 screen) {
               SupportMods.xaeroMinimap.createWaypoint(GuiMap.this, GuiMap.this.rightClickX, GuiMap.this.rightClickY == 32767 ? 32767 : GuiMap.this.rightClickY + 1, GuiMap.this.rightClickZ, GuiMap.this.rightClickCoordinateScale, true);
            }
         }).setNameFormatArgs(new Object[]{KeyMappingUtils.getKeyName(SupportMods.xaeroMinimap.getWaypointKeyBinding())}));
         options.add((new RightClickOption("gui.xaero_right_click_map_create_temporary_waypoint", options.size(), this) {
            public void onAction(class_437 screen) {
               SupportMods.xaeroMinimap.createTempWaypoint(GuiMap.this.rightClickX, GuiMap.this.rightClickY == 32767 ? 32767 : GuiMap.this.rightClickY + 1, GuiMap.this.rightClickZ, GuiMap.this.rightClickCoordinateScale, true);
            }
         }).setNameFormatArgs(new Object[]{KeyMappingUtils.getKeyName(SupportMods.xaeroMinimap.getTempWaypointKeyBinding())}));
      }

      MapDimension currentDimension = this.mapProcessor.getMapWorld().getCurrentDimension();
      if (this.field_22787.field_1761.method_2908() && currentDimension == null) {
         options.add(new RightClickOption(this, "gui.xaero_right_click_map_cant_teleport_world", options.size(), this) {
            public void onAction(class_437 screen) {
            }
         });
      } else {
         boolean teleportAllowed = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.MAP_TELEPORT_ALLOWED);
         if (!teleportAllowed || this.rightClickY == 32767 && this.field_22787.field_1761.method_2908()) {
            if (!teleportAllowed) {
               options.add(new RightClickOption(this, "gui.xaero_wm_right_click_map_teleport_not_allowed", options.size(), this) {
                  public void onAction(class_437 screen) {
                  }
               });
            } else {
               options.add(new RightClickOption(this, "gui.xaero_right_click_map_cant_teleport", options.size(), this) {
                  public void onAction(class_437 screen) {
                  }
               });
            }
         } else {
            options.add(new RightClickOption("gui.xaero_right_click_map_teleport", options.size(), this) {
               public void onAction(class_437 screen) {
                  MapDimension currentDimension = GuiMap.this.mapProcessor.getMapWorld().getCurrentDimension();
                  if ((!GuiMap.this.field_22787.field_1761.method_2908() || currentDimension != null) && (GuiMap.this.rightClickY != 32767 || !GuiMap.this.field_22787.field_1761.method_2908())) {
                     class_5321<class_1937> tpDim = GuiMap.this.rightClickDim != GuiMap.this.field_22787.field_1687.method_27983() ? GuiMap.this.rightClickDim : null;
                     (new MapTeleporter()).teleport(GuiMap.this, GuiMap.this.mapProcessor.getMapWorld(), GuiMap.this.rightClickX, GuiMap.this.rightClickY == 32767 ? 32767 : GuiMap.this.rightClickY + 1, GuiMap.this.rightClickZ, tpDim);
                  }

               }
            });
         }
      }

      if (SupportMods.minimap()) {
         options.add(new RightClickOption("gui.xaero_right_click_map_share_location", options.size(), this) {
            public void onAction(class_437 screen) {
               SupportMods.xaeroMinimap.shareLocation(GuiMap.this, GuiMap.this.rightClickX, GuiMap.this.rightClickY == 32767 ? 32767 : GuiMap.this.rightClickY + 1, GuiMap.this.rightClickZ);
            }
         });
         if (waypointsConfig) {
            options.add((new RightClickOption("gui.xaero_right_click_map_waypoints_menu", options.size(), this) {
               public void onAction(class_437 screen) {
                  SupportMods.xaeroMinimap.openWaypointsMenu(GuiMap.this.field_22787, GuiMap.this);
               }
            }).setNameFormatArgs(new Object[]{KeyMappingUtils.getKeyName(SupportMods.xaeroMinimap.getTempWaypointsMenuKeyBinding())}));
         }
      }

      if (SupportMods.pac()) {
         SupportMods.xaeroPac.addRightClickOptions(this, options, this.mapTileSelection, this.mapProcessor);
      }

      options.add(new RightClickOption("gui.xaero_right_click_box_map_export", options.size(), this) {
         public void onAction(class_437 screen) {
            GuiMap.this.onExportButton(GuiMap.this.exportButton);
         }
      });
      options.add((new RightClickOption("gui.xaero_right_click_box_map_settings", options.size(), this) {
         public void onAction(class_437 screen) {
            GuiMap.this.onSettingsButton(GuiMap.this.settingsButton);
         }
      }).setNameFormatArgs(new Object[]{KeyMappingUtils.getKeyName(ControlsRegister.keyOpenSettings)}));
      return options;
   }

   public boolean isRightClickValid() {
      return true;
   }

   public int getRightClickTitleBackgroundColor() {
      return -10461088;
   }

   public boolean shouldSkipWorldRender() {
      return true;
   }

   public double getUserScale() {
      return this.userScale;
   }

   public class_4185 getRadarButton() {
      return this.radarButton;
   }

   public void onDropdownOpen(DropDownWidget menu) {
      super.onDropdownOpen(menu);
      this.unfocusAll();
   }

   public void onDropdownClosed(DropDownWidget menu) {
      super.onDropdownClosed(menu);
      if (menu == this.rightClickMenu) {
         this.onRightClickClosed();
      }

   }

   public void onCaveModeStartSet() {
      this.caveModeOptions.onCaveModeStartSet(this);
   }

   public MapDimension getFutureDimension() {
      return this.futureDimension;
   }

   public MapProcessor getMapProcessor() {
      return this.mapProcessor;
   }

   public void enableCaveModeOptions() {
      if (!this.caveModeOptions.isEnabled()) {
         this.caveModeOptions.toggle(this);
      }

   }

   public void method_37066(class_364 current) {
      super.method_37066(current);
   }

   static {
      identityMatrix.identity();
   }
}
