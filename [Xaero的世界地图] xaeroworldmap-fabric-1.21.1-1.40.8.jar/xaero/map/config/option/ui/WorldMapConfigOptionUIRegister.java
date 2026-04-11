package xaero.map.config.option.ui;

import xaero.lib.client.config.option.ui.ConfigOptionUITypeManager;
import xaero.lib.client.config.option.ui.type.BuiltInConfigOptionUITypes;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.option.ui.type.WorldMapConfigOptionUITypes;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;

public class WorldMapConfigOptionUIRegister {
   public static void registerAll(ConfigOptionUITypeManager manager) {
      manager.registerUIType(WorldMapPrimaryClientConfigOptions.UPDATE_NOTIFICATIONS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.LIGHTING, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.BLOCK_COLORS, BuiltInConfigOptionUITypes.INT_INDEXED_BUTTON);
      manager.registerUIType(WorldMapProfiledConfigOptions.LOAD_NEW_CHUNKS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.UPDATE_CHUNKS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.TERRAIN_DEPTH, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.TERRAIN_SLOPES, BuiltInConfigOptionUITypes.INT_INDEXED_BUTTON);
      manager.registerUIType(WorldMapProfiledConfigOptions.FOOTSTEPS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.COORDINATES, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.WAYPOINTS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.WAYPOINT_BACKGROUNDS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.WAYPOINT_SCALE, BuiltInConfigOptionUITypes.DOUBLE_INDEXED_SLIDER);
      manager.registerUIType(WorldMapProfiledConfigOptions.BIOME_BLENDING, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.BIOME_COLORS_IN_VANILLA, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.MIN_ZOOM_LOCAL_WAYPOINTS, BuiltInConfigOptionUITypes.DOUBLE_INDEXED_SLIDER);
      manager.registerUIType(WorldMapProfiledConfigOptions.ADJUST_HEIGHT_FOR_SHORT_BLOCKS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.FLOWERS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.STAINED_GLASS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED_DIMENSIONS, BuiltInConfigOptionUITypes.getStringEdit());
      manager.registerUIType(WorldMapProfiledConfigOptions.CAVE_MODE_DEPTH, BuiltInConfigOptionUITypes.INT_INDEXED_SLIDER);
      manager.registerUIType(WorldMapProfiledConfigOptions.AUTO_CAVE_MODE, BuiltInConfigOptionUITypes.INT_INDEXED_BUTTON);
      manager.registerUIType(WorldMapProfiledConfigOptions.LEGIBLE_CAVE_MAPS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.CAVE_MODE_TOGGLE_TIMER, BuiltInConfigOptionUITypes.DOUBLE_INDEXED_SLIDER);
      manager.registerUIType(WorldMapProfiledConfigOptions.DEFAULT_CAVE_MODE_TYPE, BuiltInConfigOptionUITypes.INT_INDEXED_BUTTON);
      manager.registerUIType(WorldMapProfiledConfigOptions.DISPLAY_CAVE_MODE_START, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.WRITING_DISTANCE, BuiltInConfigOptionUITypes.INT_INDEXED_SLIDER);
      manager.registerUIType(WorldMapProfiledConfigOptions.ARROW, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.OPENING_ANIMATION, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.ARROW_COLOR, BuiltInConfigOptionUITypes.INT_INDEXED_BUTTON);
      manager.registerUIType(WorldMapProfiledConfigOptions.DISPLAY_ZOOM, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.DISPLAY_HOVERED_BIOME, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.ZOOM_BUTTONS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.DEFAULT_MAP_TELEPORT_FORMAT, WorldMapConfigOptionUITypes.DEFAULT_MAP_TP_COMMAND);
      manager.registerUIType(WorldMapProfiledConfigOptions.DEFAULT_MAP_TELEPORT_DIMENSION_FORMAT, WorldMapConfigOptionUITypes.DEFAULT_MAP_TP_COMMAND);
      manager.registerUIType(WorldMapProfiledConfigOptions.DEFAULT_PLAYER_TELEPORT_FORMAT, WorldMapConfigOptionUITypes.DEFAULT_MAP_TP_COMMAND);
      manager.registerUIType(WorldMapProfiledConfigOptions.DETECT_AMBIGUOUS_Y, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.MAP_TELEPORT_ALLOWED, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.PARTIAL_Y_TELEPORT, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.OPAC_CLAIMS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.OPAC_CLAIMS_BORDER_OPACITY, BuiltInConfigOptionUITypes.INT_INDEXED_SLIDER);
      manager.registerUIType(WorldMapProfiledConfigOptions.OPAC_CLAIMS_FILL_OPACITY, BuiltInConfigOptionUITypes.INT_INDEXED_SLIDER);
      manager.registerUIType(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapPrimaryClientConfigOptions.DEBUG, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.MAP_ITEM, BuiltInConfigOptionUITypes.STRING_STRING_EDIT);
      manager.registerUIType(WorldMapPrimaryClientConfigOptions.EXPORT_MULTIPLE_IMAGES, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapPrimaryClientConfigOptions.EXPORT_HIGHLIGHTS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapPrimaryClientConfigOptions.NIGHT_EXPORT, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapPrimaryClientConfigOptions.EXPORT_SCALE_DOWN_SQUARE, BuiltInConfigOptionUITypes.INT_INDEXED_SLIDER);
      manager.registerUIType(WorldMapProfiledConfigOptions.MINIMAP_RADAR, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.RENDER_WAYPOINTS, BuiltInConfigOptionUITypes.TOGGLE);
      manager.registerUIType(WorldMapProfiledConfigOptions.DISPLAY_TRACKED_PLAYERS, BuiltInConfigOptionUITypes.TOGGLE);
   }
}
