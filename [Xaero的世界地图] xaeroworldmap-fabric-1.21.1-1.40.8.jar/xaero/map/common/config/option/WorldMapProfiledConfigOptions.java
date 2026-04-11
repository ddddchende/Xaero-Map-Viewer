package xaero.map.common.config.option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import xaero.lib.common.config.option.BooleanConfigOption;
import xaero.lib.common.config.option.ConfigOption;
import xaero.lib.common.config.option.ConfigOptionManager;
import xaero.lib.common.config.option.RangeConfigOption;
import xaero.lib.common.config.option.SteppedConfigOption;
import xaero.lib.common.config.option.BooleanConfigOption.Builder;
import xaero.lib.common.config.option.ConfigOption.FinalBuilder;
import xaero.lib.common.config.option.value.type.BuiltInConfigValueTypes;
import xaero.lib.common.config.util.ConfigUtils;
import xaero.map.common.config.WorldMapConfigConstants;

public class WorldMapProfiledConfigOptions {
   private static final List<ConfigOption<?>> ALL = new ArrayList();
   public static final BooleanConfigOption LIGHTING;
   public static final RangeConfigOption BLOCK_COLORS;
   public static final BooleanConfigOption LOAD_NEW_CHUNKS;
   public static final BooleanConfigOption UPDATE_CHUNKS;
   public static final BooleanConfigOption TERRAIN_DEPTH;
   public static final RangeConfigOption TERRAIN_SLOPES;
   public static final BooleanConfigOption FOOTSTEPS;
   public static final BooleanConfigOption COORDINATES;
   public static final BooleanConfigOption WAYPOINTS;
   public static final BooleanConfigOption RENDER_WAYPOINTS;
   public static final BooleanConfigOption WAYPOINT_BACKGROUNDS;
   public static final SteppedConfigOption WAYPOINT_SCALE;
   public static final BooleanConfigOption BIOME_BLENDING;
   public static final BooleanConfigOption BIOME_COLORS_IN_VANILLA;
   public static final SteppedConfigOption MIN_ZOOM_LOCAL_WAYPOINTS;
   public static final BooleanConfigOption ADJUST_HEIGHT_FOR_SHORT_BLOCKS;
   public static final BooleanConfigOption FLOWERS;
   public static final BooleanConfigOption STAINED_GLASS;
   public static final BooleanConfigOption CAVE_MODE_ALLOWED;
   public static final ConfigOption<Set<class_2960>> CAVE_MODE_ALLOWED_DIMENSIONS;
   public static final RangeConfigOption CAVE_MODE_DEPTH;
   public static final BooleanConfigOption LEGIBLE_CAVE_MAPS;
   public static final RangeConfigOption AUTO_CAVE_MODE;
   public static final SteppedConfigOption CAVE_MODE_TOGGLE_TIMER;
   public static final RangeConfigOption DEFAULT_CAVE_MODE_TYPE;
   public static final BooleanConfigOption DISPLAY_CAVE_MODE_START;
   public static final RangeConfigOption WRITING_DISTANCE;
   public static final BooleanConfigOption ARROW;
   public static final BooleanConfigOption OPENING_ANIMATION;
   public static final RangeConfigOption ARROW_COLOR;
   public static final BooleanConfigOption DISPLAY_ZOOM;
   public static final BooleanConfigOption DISPLAY_HOVERED_BIOME;
   public static final BooleanConfigOption ZOOM_BUTTONS;
   public static final BooleanConfigOption DETECT_AMBIGUOUS_Y;
   public static final ConfigOption<String> DEFAULT_MAP_TELEPORT_FORMAT;
   public static final ConfigOption<String> DEFAULT_MAP_TELEPORT_DIMENSION_FORMAT;
   public static final ConfigOption<String> DEFAULT_PLAYER_TELEPORT_FORMAT;
   public static final BooleanConfigOption MAP_TELEPORT_ALLOWED;
   public static final BooleanConfigOption PARTIAL_Y_TELEPORT;
   public static final BooleanConfigOption OPAC_CLAIMS;
   public static final RangeConfigOption OPAC_CLAIMS_BORDER_OPACITY;
   public static final RangeConfigOption OPAC_CLAIMS_FILL_OPACITY;
   public static final ConfigOption<String> MAP_ITEM;
   public static final BooleanConfigOption MINIMAP_RADAR;
   public static final BooleanConfigOption DISPLAY_TRACKED_PLAYERS;

   public static void registerAll(ConfigOptionManager manager) {
      Iterator var1 = ALL.iterator();

      while(var1.hasNext()) {
         ConfigOption<?> option = (ConfigOption)var1.next();
         manager.register(option);
      }

   }

   static {
      LIGHTING = ((Builder)((Builder)((Builder)Builder.begin().setId("lighting")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_lighting"))).build(ALL);
      BLOCK_COLORS = ((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)xaero.lib.common.config.option.RangeConfigOption.Builder.begin().setId("block_colors")).setDefaultValue(0)).setMinIndex(0)).setMaxIndex(WorldMapConfigConstants.BLOCK_COLORS_NAMES.length - 1)).setDisplayGetter((o, v) -> {
         return WorldMapConfigConstants.BLOCK_COLORS_NAMES[v];
      })).setDisplayName(class_2561.method_43471("gui.xaero_block_colours"))).build(ALL);
      LOAD_NEW_CHUNKS = ((Builder)((Builder)((Builder)Builder.begin().setId("load_new_chunks")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_load_chunks"))).build(ALL);
      UPDATE_CHUNKS = ((Builder)((Builder)((Builder)Builder.begin().setId("update_chunks")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_update_chunks"))).build(ALL);
      TERRAIN_DEPTH = ((Builder)((Builder)((Builder)Builder.begin().setId("terrain_depth")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_terrain_depth"))).build(ALL);
      TERRAIN_SLOPES = ((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)xaero.lib.common.config.option.RangeConfigOption.Builder.begin().setId("terrain_slopes")).setDefaultValue(2)).setMinIndex(0)).setMaxIndex(WorldMapConfigConstants.TERRAIN_SLOPES_NAMES.length - 1)).setDisplayGetter((o, v) -> {
         return WorldMapConfigConstants.TERRAIN_SLOPES_NAMES[v];
      })).setDisplayName(class_2561.method_43471("gui.xaero_terrain_slopes"))).build(ALL);
      FOOTSTEPS = ((Builder)((Builder)((Builder)Builder.begin().setId("footsteps")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_footsteps"))).build(ALL);
      COORDINATES = ((Builder)((Builder)((Builder)Builder.begin().setId("display_coordinates")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_coordinates"))).build(ALL);
      WAYPOINTS = ((Builder)((Builder)((Builder)Builder.begin().setId("waypoints")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_worldmap_waypoints"))).build(ALL);
      RENDER_WAYPOINTS = ((Builder)((Builder)((Builder)Builder.begin().setId("render_waypoints")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_render_worldmap_waypoints"))).build(ALL);
      WAYPOINT_BACKGROUNDS = ((Builder)((Builder)((Builder)Builder.begin().setId("waypoint_backgrounds")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_waypoint_backgrounds"))).build(ALL);
      WAYPOINT_SCALE = ((xaero.lib.common.config.option.SteppedConfigOption.Builder)((xaero.lib.common.config.option.SteppedConfigOption.Builder)((xaero.lib.common.config.option.SteppedConfigOption.Builder)xaero.lib.common.config.option.SteppedConfigOption.Builder.begin().setId("waypoint_scale")).setDefaultValue(1.0D)).setMinValue(0.5D).setMaxValue(5.0D).setStep(0.5D).setRangeValidator(true).setDisplayName(class_2561.method_43471("gui.xaero_wm_waypoint_scale"))).build(ALL);
      BIOME_BLENDING = ((Builder)((Builder)((Builder)((Builder)Builder.begin().setId("biome_blending")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_biome_blending"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_biome_blending"))).build(ALL);
      BIOME_COLORS_IN_VANILLA = ((Builder)((Builder)((Builder)Builder.begin().setId("biome_colors_in_vanilla")).setDefaultValue(false)).setDisplayName(class_2561.method_43471("gui.xaero_biome_colors"))).build(ALL);
      MIN_ZOOM_LOCAL_WAYPOINTS = ((xaero.lib.common.config.option.SteppedConfigOption.Builder)((xaero.lib.common.config.option.SteppedConfigOption.Builder)((xaero.lib.common.config.option.SteppedConfigOption.Builder)xaero.lib.common.config.option.SteppedConfigOption.Builder.begin().setId("minimum_zoom_for_local_waypoints")).setDefaultValue(0.0D)).setMinValue(0.0D).setMaxValue(3.0D).setStep(0.01D).setRangeValidator(true).setDisplayName(class_2561.method_43471("gui.xaero_wm_min_zoom_local_waypoints"))).build(ALL);
      ADJUST_HEIGHT_FOR_SHORT_BLOCKS = ((Builder)((Builder)((Builder)((Builder)Builder.begin().setId("adjust_height_for_short_blocks")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_adjust_height_for_carpetlike_blocks"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_adjust_height_for_carpetlike_blocks"))).build(ALL);
      FLOWERS = ((Builder)((Builder)((Builder)Builder.begin().setId("display_flowers")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_flowers"))).build(ALL);
      STAINED_GLASS = ((Builder)((Builder)((Builder)Builder.begin().setId("display_stained_glass")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_display_stained_glass"))).build(ALL);
      CAVE_MODE_ALLOWED = ((Builder)((Builder)((Builder)((Builder)Builder.begin().setId("cave_mode_allowed")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_cave_mode_allowed"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_cave_mode_allowed"))).build(ALL);
      CAVE_MODE_ALLOWED_DIMENSIONS = ((FinalBuilder)((FinalBuilder)((FinalBuilder)((FinalBuilder)((FinalBuilder)((FinalBuilder)FinalBuilder.begin().setId("cave_mode_allowed_dimensions")).setDefaultValue(Collections.unmodifiableSet(new LinkedHashSet()))).setValueType(xaero.lib.common.config.option.value.type.CollectionConfigValueType.Builder.begin().setElementValueType(BuiltInConfigValueTypes.RESOURCE_LOCATION).setIoCodecSeparator(',').build())).setDisplayName(class_2561.method_43471("gui.xaero_wm_cave_mode_allowed_dimensions"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_cave_mode_allowed_dimensions"))).setOverridable(false)).build(ALL);
      CAVE_MODE_DEPTH = ((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)xaero.lib.common.config.option.RangeConfigOption.Builder.begin().setId("cave_mode_depth")).setDefaultValue(30)).setMinIndex(1)).setMaxIndex(64)).setDisplayName(class_2561.method_43471("gui.xaero_wm_cave_mode_depth"))).build(ALL);
      LEGIBLE_CAVE_MAPS = ((Builder)((Builder)((Builder)((Builder)Builder.begin().setId("legible_cave_maps")).setDefaultValue(false)).setDisplayName(class_2561.method_43471("gui.xaero_wm_legible_cave_maps"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_legible_cave_maps"))).build(ALL);
      AUTO_CAVE_MODE = ((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)xaero.lib.common.config.option.RangeConfigOption.Builder.begin().setId("auto_cave_mode")).setDefaultValue(-1)).setMinIndex(-1)).setMaxIndex(3)).setDisplayGetter((o, v) -> {
         if (v == 0) {
            return class_2561.method_43471("gui.xaero_off");
         } else if (v < 0) {
            return class_2561.method_43471("gui.xaero_auto_cave_mode_minimap");
         } else {
            int roofSideSize = v * 2 - 1;
            return class_2561.method_43469("gui.xaero_wm_ceiling", new Object[]{roofSideSize + "x" + roofSideSize});
         }
      })).setDisplayName(class_2561.method_43471("gui.xaero_auto_cave_mode"))).setTooltip(class_2561.method_43471("gui.xaero_box_auto_cave_mode"))).build(ALL);
      CAVE_MODE_TOGGLE_TIMER = ((xaero.lib.common.config.option.SteppedConfigOption.Builder)((xaero.lib.common.config.option.SteppedConfigOption.Builder)((xaero.lib.common.config.option.SteppedConfigOption.Builder)((xaero.lib.common.config.option.SteppedConfigOption.Builder)((xaero.lib.common.config.option.SteppedConfigOption.Builder)xaero.lib.common.config.option.SteppedConfigOption.Builder.begin().setId("cave_mode_toggle_timer")).setDefaultValue(1.0D)).setMinValue(0.0D).setMaxValue(10.0D).setStep(0.1D).setDisplayGetter((o, v) -> {
         return ConfigUtils.getDisplayForSimpleNumber(o, v, WorldMapConfigConstants.SEC);
      })).setDisplayName(class_2561.method_43471("gui.xaero_wm_cave_mode_toggle_timer"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_cave_mode_toggle_timer"))).build(ALL);
      DEFAULT_CAVE_MODE_TYPE = ((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)xaero.lib.common.config.option.RangeConfigOption.Builder.begin().setId("default_cave_mode_type")).setDefaultValue(1)).setMinIndex(0)).setMaxIndex(WorldMapConfigConstants.DEFAULT_CAVE_MODE_TYPE_NAMES.length - 1)).setDisplayGetter((o, v) -> {
         return WorldMapConfigConstants.DEFAULT_CAVE_MODE_TYPE_NAMES[v];
      })).setDisplayName(class_2561.method_43471("gui.xaero_wm_default_cave_mode_type"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_default_cave_mode_type"))).build(ALL);
      DISPLAY_CAVE_MODE_START = ((Builder)((Builder)((Builder)Builder.begin().setId("display_cave_mode_start")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_display_cave_mode_start"))).build(ALL);
      WRITING_DISTANCE = ((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)xaero.lib.common.config.option.RangeConfigOption.Builder.begin().setId("map_writing_distance")).setDefaultValue(-1)).setMinIndex(-1)).setMaxIndex(32)).setDisplayGetter((o, v) -> {
         return v < 0 ? class_2561.method_43471("gui.xaero_map_writing_distance_unlimited") : class_2561.method_43470(v.makeConcatWithConstants<invokedynamic>(v));
      })).setDisplayName(class_2561.method_43471("gui.xaero_map_writing_distance"))).setTooltip(class_2561.method_43471("gui.xaero_box_map_writing_distance"))).build(ALL);
      ARROW = ((Builder)((Builder)((Builder)Builder.begin().setId("display_player_as_arrow")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_render_arrow"))).build(ALL);
      OPENING_ANIMATION = ((Builder)((Builder)((Builder)Builder.begin().setId("opening_animation")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_open_map_animation"))).build(ALL);
      ARROW_COLOR = ((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)xaero.lib.common.config.option.RangeConfigOption.Builder.begin().setId("arrow_color")).setDefaultValue(-2)).setMinIndex(-2)).setMaxIndex(WorldMapConfigConstants.ARROW_COLORS.length - 1)).setDisplayGetter((o, v) -> {
         return WorldMapConfigConstants.ARROW_COLOR_NAMES[v + 2];
      })).setDisplayName(class_2561.method_43471("gui.xaero_wm_arrow_colour"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_arrow_color"))).build(ALL);
      DISPLAY_ZOOM = ((Builder)((Builder)((Builder)Builder.begin().setId("display_zoom")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_display_zoom"))).build(ALL);
      DISPLAY_HOVERED_BIOME = ((Builder)((Builder)((Builder)Builder.begin().setId("display_hovered_biome")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_hovered_biome"))).build(ALL);
      ZOOM_BUTTONS = ((Builder)((Builder)((Builder)Builder.begin().setId("zoom_buttons")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_zoom_buttons"))).build(ALL);
      DETECT_AMBIGUOUS_Y = ((Builder)((Builder)((Builder)((Builder)Builder.begin().setId("detect_ambiguous_y")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_detect_ambiguous_y"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_detect_ambiguous_y"))).build(ALL);
      DEFAULT_MAP_TELEPORT_FORMAT = ((FinalBuilder)((FinalBuilder)((FinalBuilder)((FinalBuilder)FinalBuilder.begin().setId("default_map_teleport_command_format")).setDefaultValue("/tp @s {x} {y} {z}")).setValueType(BuiltInConfigValueTypes.getString(500))).setDisplayName(class_2561.method_43471("gui.xaero_wm_default_teleport_command"))).build(ALL);
      DEFAULT_MAP_TELEPORT_DIMENSION_FORMAT = ((FinalBuilder)((FinalBuilder)((FinalBuilder)((FinalBuilder)FinalBuilder.begin().setId("default_map_teleport_command_dimension_format")).setDefaultValue("/execute as @s in {d} run tp {x} {y} {z}")).setValueType(BuiltInConfigValueTypes.getString(500))).setDisplayName(class_2561.method_43471("gui.xaero_wm_default_teleport_command_dimension"))).build(ALL);
      DEFAULT_PLAYER_TELEPORT_FORMAT = ((FinalBuilder)((FinalBuilder)((FinalBuilder)((FinalBuilder)FinalBuilder.begin().setId("default_player_teleport_command_format")).setDefaultValue("/tp @s {name}")).setValueType(BuiltInConfigValueTypes.getString(500))).setDisplayName(class_2561.method_43471("gui.xaero_wm_default_player_teleport_command"))).build(ALL);
      MAP_TELEPORT_ALLOWED = ((Builder)((Builder)((Builder)((Builder)Builder.begin().setId("map_teleport_allowed")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_teleport_allowed"))).setTooltip(class_2561.method_43471("gui.xaero_wm_teleport_allowed_tooltip"))).build(ALL);
      PARTIAL_Y_TELEPORT = ((Builder)((Builder)((Builder)((Builder)Builder.begin().setId("partial_y_teleport")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_partial_y_teleportation"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_partial_y_teleportation"))).build(ALL);
      OPAC_CLAIMS = ((Builder)((Builder)((Builder)((Builder)Builder.begin().setId("display_opac_claims")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_pac_claims"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_pac_claims"))).build(ALL);
      OPAC_CLAIMS_BORDER_OPACITY = ((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)xaero.lib.common.config.option.RangeConfigOption.Builder.begin().setId("opac_claims_border_opacity")).setDefaultValue(80)).setMinIndex(1)).setMaxIndex(100)).setDisplayName(class_2561.method_43471("gui.xaero_wm_pac_claims_border_opacity"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_pac_claims_border_opacity"))).build(ALL);
      OPAC_CLAIMS_FILL_OPACITY = ((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)((xaero.lib.common.config.option.RangeConfigOption.Builder)xaero.lib.common.config.option.RangeConfigOption.Builder.begin().setId("opac_claims_fill_opacity")).setDefaultValue(46)).setMinIndex(1)).setMaxIndex(100)).setDisplayName(class_2561.method_43471("gui.xaero_wm_pac_claims_fill_opacity"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_pac_claims_fill_opacity"))).build(ALL);
      MAP_ITEM = ((FinalBuilder)((FinalBuilder)((FinalBuilder)((FinalBuilder)((FinalBuilder)((FinalBuilder)FinalBuilder.begin().setId("map_item")).setDefaultValue("-")).setValueType(BuiltInConfigValueTypes.getString(BuiltInConfigValueTypes.RESOURCE_LOCATION.getIoCodec().getMaxStringLength()))).setDisplayGetter(ConfigUtils::getDisplayForString)).setDisplayName(class_2561.method_43471("gui.xaero_map_item"))).setTooltip(class_2561.method_43471("gui.xaero_box_map_item"))).build(ALL);
      MINIMAP_RADAR = ((Builder)((Builder)((Builder)((Builder)Builder.begin().setId("display_minimap_radar")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_display_minimap_radar"))).setTooltip(class_2561.method_43471("gui.xaero_wm_box_display_minimap_radar"))).build(ALL);
      DISPLAY_TRACKED_PLAYERS = ((Builder)((Builder)((Builder)Builder.begin().setId("display_tracked_players")).setDefaultValue(true)).setDisplayName(class_2561.method_43471("gui.xaero_wm_display_tracked_players"))).build(ALL);
   }
}
