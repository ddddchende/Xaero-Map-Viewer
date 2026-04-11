package xaero.map.gui;

import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_339;
import net.minecraft.class_437;
import net.minecraft.class_3675.class_307;
import xaero.lib.client.controls.util.KeyMappingUtils;
import xaero.lib.client.gui.CustomSettingEntry;
import xaero.lib.client.gui.GuiConstants;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.client.gui.config.EditConfigScreen;
import xaero.lib.client.gui.config.context.IEditConfigScreenContext;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.lib.common.config.option.BuiltInProfiledConfigOptions;
import xaero.lib.common.config.util.ConfigConstants;
import xaero.lib.common.gui.widget.TooltipInfo;
import xaero.map.MapFullReloader;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.common.config.WorldMapConfigConstants;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.controls.ControlsRegister;
import xaero.map.mods.SupportMods;
import xaero.map.settings.ModSettings;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

public class GuiWorldMapSettings extends EditConfigScreen {
   public static final class_2561 PLAYER_TELEPORT_COMMAND_TOOLTIP = class_2561.method_43471("gui.xaero_wm_box_player_teleport_command");
   public static final class_2561 MAP_TELEPORT_COMMAND_TOOLTIP = class_2561.method_43471("gui.xaero_wm_box_map_teleport_command");

   public GuiWorldMapSettings(IEditConfigScreenContext context) {
      this((class_437)null, context);
   }

   public GuiWorldMapSettings(class_437 parent, IEditConfigScreenContext context) {
      this(parent, (class_437)null, context);
   }

   public GuiWorldMapSettings(class_437 parent, class_437 escapeScreen, IEditConfigScreenContext context) {
      super(class_2561.method_43471("gui.xaero_world_map_settings"), parent, escapeScreen, context, WorldMap.INSTANCE.getConfigs());
      ScreenSwitchSettingEntry minimapEntry = new ScreenSwitchSettingEntry("gui.xaero_wm_minimap_settings", (current, escape) -> {
         return SupportMods.xaeroMinimap.getSettingsScreen(current);
      }, !SupportMods.minimap() ? new Tooltip(WorldMapConfigConstants.MINIMAP_TOOLTIP) : null, SupportMods.minimap());
      ScreenSwitchSettingEntry resetEntry = new ScreenSwitchSettingEntry("gui.xaero_wm_reset_config_profile_default", (current, escape) -> {
         return new ConfirmScreenBase(current, escape, true, (r) -> {
            this.resetConfirmResult(r, this, escapeScreen);
         }, class_2561.method_43471("gui.xaero_wm_reset_config_profile_default_message"), class_2561.method_43471("gui.xaero_wm_reset_config_profile_default_message2"));
      }, (Tooltip)null, true, false);
      ScreenSwitchSettingEntry mapTeleportCommandEntry = new ScreenSwitchSettingEntry("gui.xaero_wm_teleport_command", (current, escape) -> {
         return new GuiMapTpCommand(current, escape);
      }, new Tooltip(!context.isClientSide() ? GuiConstants.SETTING_ENTRY_WRONG_CONTEXT_COMPONENT : (!ModSettings.canEditIngameSettings() ? WorldMapConfigConstants.INGAME_TOOLTIP : MAP_TELEPORT_COMMAND_TOOLTIP)), context.isClientSide() && ModSettings.canEditIngameSettings());
      ScreenSwitchSettingEntry playerTeleportCommandEntry = new ScreenSwitchSettingEntry("gui.xaero_wm_player_teleport_command", (current, escape) -> {
         return new GuiPlayerTpCommand(current, escape);
      }, new Tooltip(!context.isClientSide() ? GuiConstants.SETTING_ENTRY_WRONG_CONTEXT_COMPONENT : (!ModSettings.canEditIngameSettings() ? WorldMapConfigConstants.INGAME_TOOLTIP : PLAYER_TELEPORT_COMMAND_TOOLTIP)), context.isClientSide() && ModSettings.canEditIngameSettings());
      ISettingEntry ignoreHeightmapsEntry = new CustomSettingEntry(() -> {
         return false;
      }, class_2561.method_43471("gui.xaero_wm_ignore_heightmaps"), context.isClientSide() ? new TooltipInfo("gui.xaero_wm_box_ignore_heightmaps") : new TooltipInfo(GuiConstants.SETTING_ENTRY_WRONG_CONTEXT_COMPONENT, false, true), false, () -> {
         WorldMapSession session = WorldMapSession.getCurrentSession();
         return session == null ? null : session.getMapProcessor().getMapWorld().isIgnoreHeightmaps();
      }, 0, 1, (i) -> {
         return i == 1;
      }, (v) -> {
         return v ? ConfigConstants.ON : ConfigConstants.OFF;
      }, (oldValue, newValue) -> {
         WorldMapSession session = WorldMapSession.getCurrentSession();
         if (session != null) {
            MapWorld mapWorld = session.getMapProcessor().getMapWorld();
            mapWorld.setIgnoreHeightmaps(newValue);
            mapWorld.saveConfig();
            WorldMap.settings.updateRegionCacheHashCode();
         }
      }, () -> {
         return context.isClientSide() && WorldMapSession.getCurrentSession() != null;
      });
      ISettingEntry fullReloadEntry = this.getEntryForFullReloadOption(false);
      ISettingEntry fullResaveEntry = this.getEntryForFullReloadOption(true);
      new CustomSettingEntry(() -> {
         return false;
      }, class_2561.method_43470("Pause Requests"), (TooltipInfo)null, false, () -> {
         return WorldMap.pauseRequests;
      }, 0, 1, (i) -> {
         return i == 1;
      }, (v) -> {
         return v ? ConfigConstants.ON : ConfigConstants.OFF;
      }, (o, n) -> {
         WorldMap.pauseRequests = n;
      }, () -> {
         return true;
      });
      new CustomSettingEntry(() -> {
         return false;
      }, class_2561.method_43470("Extra Debug"), (TooltipInfo)null, false, () -> {
         return WorldMap.extraDebug;
      }, 0, 1, (i) -> {
         return i == 1;
      }, (v) -> {
         return v ? ConfigConstants.ON : ConfigConstants.OFF;
      }, (o, n) -> {
         WorldMap.extraDebug = n;
      }, () -> {
         return true;
      });
      this.entries = new ISettingEntry[]{this.createProfileIDEntry(), this.optionEntry(BuiltInProfiledConfigOptions.PROFILE_NAME), this.optionEntry(WorldMapProfiledConfigOptions.LIGHTING), this.optionEntry(WorldMapProfiledConfigOptions.BLOCK_COLORS), this.optionEntry(WorldMapProfiledConfigOptions.LOAD_NEW_CHUNKS), this.optionEntry(WorldMapProfiledConfigOptions.UPDATE_CHUNKS), this.optionEntry(WorldMapProfiledConfigOptions.TERRAIN_DEPTH), this.optionEntry(WorldMapProfiledConfigOptions.TERRAIN_SLOPES), this.optionEntry(WorldMapProfiledConfigOptions.FOOTSTEPS), this.optionEntry(WorldMapProfiledConfigOptions.COORDINATES), minimapEntry, this.optionEntry(WorldMapProfiledConfigOptions.WAYPOINTS), this.optionEntry(WorldMapProfiledConfigOptions.MINIMAP_RADAR), this.optionEntry(WorldMapProfiledConfigOptions.RENDER_WAYPOINTS), this.optionEntry(WorldMapProfiledConfigOptions.WAYPOINT_BACKGROUNDS), this.optionEntry(WorldMapProfiledConfigOptions.WAYPOINT_SCALE), this.optionEntry(WorldMapProfiledConfigOptions.BIOME_BLENDING), this.optionEntry(WorldMapProfiledConfigOptions.BIOME_COLORS_IN_VANILLA), this.optionEntry(WorldMapProfiledConfigOptions.MIN_ZOOM_LOCAL_WAYPOINTS), this.optionEntry(WorldMapProfiledConfigOptions.ADJUST_HEIGHT_FOR_SHORT_BLOCKS), this.optionEntry(WorldMapProfiledConfigOptions.FLOWERS), this.optionEntry(WorldMapProfiledConfigOptions.STAINED_GLASS), ignoreHeightmapsEntry, this.optionEntry(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED), this.optionEntry(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED_DIMENSIONS), this.optionEntry(WorldMapProfiledConfigOptions.CAVE_MODE_DEPTH), this.optionEntry(WorldMapProfiledConfigOptions.AUTO_CAVE_MODE), this.optionEntry(WorldMapProfiledConfigOptions.LEGIBLE_CAVE_MAPS), this.optionEntry(WorldMapProfiledConfigOptions.CAVE_MODE_TOGGLE_TIMER), this.optionEntry(WorldMapProfiledConfigOptions.DEFAULT_CAVE_MODE_TYPE), this.optionEntry(WorldMapProfiledConfigOptions.DISPLAY_CAVE_MODE_START), this.optionEntry(WorldMapProfiledConfigOptions.WRITING_DISTANCE), this.optionEntry(WorldMapProfiledConfigOptions.ARROW), this.optionEntry(WorldMapProfiledConfigOptions.OPENING_ANIMATION), this.optionEntry(WorldMapProfiledConfigOptions.ARROW_COLOR), this.optionEntry(WorldMapProfiledConfigOptions.DISPLAY_ZOOM), this.optionEntry(WorldMapProfiledConfigOptions.DISPLAY_HOVERED_BIOME), this.optionEntry(WorldMapProfiledConfigOptions.ZOOM_BUTTONS), this.optionEntry(WorldMapProfiledConfigOptions.DEFAULT_MAP_TELEPORT_FORMAT), this.optionEntry(WorldMapProfiledConfigOptions.DEFAULT_MAP_TELEPORT_DIMENSION_FORMAT), mapTeleportCommandEntry, this.optionEntry(WorldMapProfiledConfigOptions.DEFAULT_PLAYER_TELEPORT_FORMAT), this.optionEntry(WorldMapProfiledConfigOptions.DETECT_AMBIGUOUS_Y), playerTeleportCommandEntry, this.optionEntry(WorldMapProfiledConfigOptions.MAP_TELEPORT_ALLOWED), this.optionEntry(WorldMapProfiledConfigOptions.PARTIAL_Y_TELEPORT), this.optionEntry(WorldMapProfiledConfigOptions.MAP_ITEM), this.optionEntry(WorldMapProfiledConfigOptions.DISPLAY_TRACKED_PLAYERS), this.optionEntry(WorldMapProfiledConfigOptions.OPAC_CLAIMS), this.optionEntry(WorldMapProfiledConfigOptions.OPAC_CLAIMS_BORDER_OPACITY), this.primaryOptionEntry(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED), this.optionEntry(WorldMapProfiledConfigOptions.OPAC_CLAIMS_FILL_OPACITY), fullReloadEntry, fullResaveEntry, this.primaryOptionEntry(WorldMapPrimaryClientConfigOptions.UPDATE_NOTIFICATIONS), this.primaryOptionEntry(WorldMapPrimaryClientConfigOptions.DEBUG), resetEntry, this.optionEntry(BuiltInProfiledConfigOptions.IGNORE_ENFORCEMENT_IF_EDITOR)};
   }

   private ISettingEntry getEntryForFullReloadOption(boolean isResave) {
      return new CustomSettingEntry(() -> {
         return false;
      }, class_2561.method_43471(isResave ? "gui.xaero_full_resave" : "gui.xaero_full_reload"), this.context.isClientSide() ? new TooltipInfo(isResave ? "gui.xaero_box_full_resave" : "gui.xaero_box_full_reload") : new TooltipInfo(GuiConstants.SETTING_ENTRY_WRONG_CONTEXT_COMPONENT, false, true), false, () -> {
         WorldMapSession session = WorldMapSession.getCurrentSession();
         if (session == null) {
            return false;
         } else {
            MapDimension mapDimension = session.getMapProcessor().getMapWorld().getCurrentDimension();
            MapFullReloader reloader = mapDimension == null ? null : mapDimension.getFullReloader();
            return reloader != null && (!isResave || reloader.isResave());
         }
      }, 0, 1, (i) -> {
         return i == 1;
      }, (v) -> {
         return v ? ConfigConstants.ON : ConfigConstants.OFF;
      }, (oldValue, newValue) -> {
         WorldMapSession session = WorldMapSession.getCurrentSession();
         MapProcessor mapProcessor = session.getMapProcessor();
         MapDimension mapDimension = mapProcessor.getMapWorld().getCurrentDimension();
         if (mapDimension != null) {
            if (!newValue || mapDimension.getFullReloader() != null && (mapDimension.getFullReloader().isResave() || !isResave)) {
               if (!newValue) {
                  mapDimension.clearFullMapReload();
               }
            } else {
               mapDimension.startFullMapReload(mapProcessor.getCurrentCaveLayer(), isResave, mapProcessor);
            }

            this.refresh();
         }
      }, () -> {
         return this.context.isClientSide() && WorldMapSession.getCurrentSession() != null;
      });
   }

   protected void resetConfirmResult(boolean result, class_437 parent, class_437 escScreen) {
      if (result) {
         this.resetProfileToDefaults();
      }

      class_310.method_1551().method_1507(parent);
   }

   public void method_25426() {
      super.method_25426();
      Tooltip closeSettingsTooltip = new Tooltip(class_2561.method_43469("gui.xaero_box_close_settings", new Object[]{KeyMappingUtils.getKeyName(ControlsRegister.keyOpenSettings)}));
      if (this.parent instanceof GuiMap) {
         this.method_37063(new GuiTexturedButton(0, 0, 30, 30, 113, 0, 20, 20, WorldMap.guiTextures, this::onSettingsButton, () -> {
            return closeSettingsTooltip;
         }));
      }

   }

   private void onSettingsButton(class_339 button) {
      this.goBack();
   }

   public boolean method_25404(int key, int scancode, int mods) {
      if (super.method_25404(key, scancode, mods)) {
         return true;
      } else if ((!this.context.isClientSide() || !KeyMappingUtils.inputMatches(class_307.field_1668, key, ControlsRegister.keyOpenSettings, 0)) && (this.context.isClientSide() || !KeyMappingUtils.inputMatches(class_307.field_1668, key, ControlsRegister.keyOpenServerSettings, 0))) {
         return false;
      } else {
         this.onExit(this.escape);
         return true;
      }
   }
}
