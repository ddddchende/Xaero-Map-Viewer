package xaero.map.config.option.value.redirect;

import xaero.lib.client.config.option.value.redirect.ClientOptionValueRedirectorManager;
import xaero.lib.common.config.util.ConfigConstants;
import xaero.map.common.config.WorldMapConfigConstants;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.effects.Effects;
import xaero.map.misc.Misc;
import xaero.map.mods.SupportMods;

public class WorldMapConfigOptionClientRedirectors {
   public static void registerAll(ClientOptionValueRedirectorManager manager) {
      manager.register(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED, () -> {
         return false;
      }, (channel) -> {
         return Misc.hasEffect(Effects.NO_CAVE_MAPS) || Misc.hasEffect(Effects.NO_CAVE_MAPS_HARMFUL) || WorldMapClientConfigUtils.isFairPlay() || WorldMapClientConfigUtils.isCaveModeDisabledLegacy();
      }, (current) -> {
         return null;
      }, ConfigConstants.OFF, () -> {
         return !Misc.hasEffect(Effects.NO_CAVE_MAPS) && !Misc.hasEffect(Effects.NO_CAVE_MAPS_HARMFUL) ? (WorldMapClientConfigUtils.isFairPlay() ? WorldMapConfigConstants.FAIRPLAY_TOOLTIP : WorldMapConfigConstants.LEGACY_PLUGIN_TOOLTIP) : WorldMapConfigConstants.EFFECT_TOOLTIP;
      });
      manager.register(WorldMapProfiledConfigOptions.WAYPOINTS, () -> {
         return false;
      }, (channel) -> {
         return !SupportMods.minimap();
      }, (current) -> {
         return null;
      }, ConfigConstants.OFF, () -> {
         return WorldMapConfigConstants.MINIMAP_TOOLTIP;
      });
      manager.register(WorldMapProfiledConfigOptions.WAYPOINT_BACKGROUNDS, () -> {
         return false;
      }, (channel) -> {
         return !SupportMods.minimap();
      }, (current) -> {
         return null;
      }, ConfigConstants.OFF, () -> {
         return WorldMapConfigConstants.MINIMAP_TOOLTIP;
      });
      manager.register(WorldMapProfiledConfigOptions.WAYPOINT_SCALE, () -> {
         return 1.0D;
      }, (channel) -> {
         return !SupportMods.minimap();
      }, (current) -> {
         return null;
      }, ConfigConstants.OFF, () -> {
         return WorldMapConfigConstants.MINIMAP_TOOLTIP;
      });
      manager.register(WorldMapProfiledConfigOptions.MIN_ZOOM_LOCAL_WAYPOINTS, () -> {
         return 0.0D;
      }, (channel) -> {
         return !SupportMods.minimap();
      }, (current) -> {
         return null;
      }, ConfigConstants.OFF, () -> {
         return WorldMapConfigConstants.MINIMAP_TOOLTIP;
      });
      manager.register(WorldMapProfiledConfigOptions.MINIMAP_RADAR, () -> {
         return false;
      }, (channel) -> {
         return !SupportMods.minimap();
      }, (current) -> {
         return null;
      }, ConfigConstants.OFF, () -> {
         return WorldMapConfigConstants.MINIMAP_TOOLTIP;
      });
      manager.register(WorldMapProfiledConfigOptions.RENDER_WAYPOINTS, () -> {
         return false;
      }, (channel) -> {
         return !SupportMods.minimap();
      }, (current) -> {
         return null;
      }, ConfigConstants.OFF, () -> {
         return WorldMapConfigConstants.MINIMAP_TOOLTIP;
      });
   }
}
