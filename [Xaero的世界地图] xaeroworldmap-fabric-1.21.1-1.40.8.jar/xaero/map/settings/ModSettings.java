package xaero.map.settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import net.minecraft.class_310;
import net.minecraft.class_3288;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import xaero.lib.XaeroLib;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.primary.option.LibPrimaryCommonConfigOptions;
import xaero.lib.common.config.profile.ConfigProfile;
import xaero.lib.common.util.IOUtils;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;

public class ModSettings {
   public static final String format = "§";
   private int regionCacheHashCode;

   private void loadDefaultSettings() throws IOException {
      File mainConfigFile = WorldMap.optionsFile;
      File defaultConfigFile = mainConfigFile.toPath().getParent().resolveSibling("defaultconfigs").resolve(mainConfigFile.getName()).toFile();
      if (defaultConfigFile.exists()) {
         this.loadSettingsFile(defaultConfigFile);
      }

   }

   public void loadSettings() throws IOException {
      this.loadDefaultSettings();
      File mainConfigFile = WorldMap.optionsFile;
      Path configFolderPath = mainConfigFile.toPath().getParent();
      if (!Files.exists(configFolderPath, new LinkOption[0])) {
         Files.createDirectories(configFolderPath);
      }

      if (mainConfigFile.exists()) {
         this.loadSettingsFile(mainConfigFile);
         IOUtils.quickFileBackupMove(mainConfigFile.toPath());
      }

   }

   private void loadSettingsFile(File file) throws IOException {
      BufferedReader reader = null;
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      ConfigProfile currentProfile = configManager.getCurrentProfile();
      Config primaryConfig = configManager.getPrimaryConfigManager().getConfig();

      try {
         reader = new BufferedReader(new FileReader(file));

         String s;
         while((s = reader.readLine()) != null) {
            String[] args = s.split(":");

            try {
               if (args[0].equalsIgnoreCase("ignoreUpdate")) {
                  primaryConfig.set(WorldMapPrimaryClientConfigOptions.IGNORED_UPDATE, Integer.parseInt(args[1]));
               } else if (args[0].equalsIgnoreCase("updateNotification")) {
                  primaryConfig.set(WorldMapPrimaryClientConfigOptions.UPDATE_NOTIFICATIONS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("allowInternetAccess")) {
                  boolean savedAllowInternetAccess = args[1].equals("true");
                  if (!savedAllowInternetAccess) {
                     XaeroLib.INSTANCE.getLibConfigChannel().getPrimaryCommonConfigManager().getConfig().set(LibPrimaryCommonConfigOptions.ALLOW_INTERNET, false);
                     XaeroLib.INSTANCE.getLibConfigChannel().getPrimaryCommonConfigManagerIO().save();
                  }
               } else if (args[0].equalsIgnoreCase("differentiateByServerAddress")) {
                  primaryConfig.set(WorldMapPrimaryClientConfigOptions.DIFFERENTIATE_BY_SERVER_ADDRESS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("caveMapsAllowed")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("debug")) {
                  primaryConfig.set(WorldMapPrimaryClientConfigOptions.DEBUG, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("lighting")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.LIGHTING, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("colours")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.BLOCK_COLORS, Integer.parseInt(args[1]));
               } else if (args[0].equalsIgnoreCase("loadChunks")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.LOAD_NEW_CHUNKS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("updateChunks")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.UPDATE_CHUNKS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("terrainSlopes")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.TERRAIN_SLOPES, args[1].equals("true") ? 2 : (args[1].equals("false") ? 0 : Integer.parseInt(args[1])));
               } else if (args[0].equalsIgnoreCase("terrainDepth")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.TERRAIN_DEPTH, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("footsteps")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.FOOTSTEPS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("flowers")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.FLOWERS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("coordinates")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.COORDINATES, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("hoveredBiome")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.DISPLAY_HOVERED_BIOME, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("biomeColorsVanillaMode")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.BIOME_COLORS_IN_VANILLA, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("waypoints")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.WAYPOINTS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("renderArrow")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.ARROW, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("displayZoom")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.DISPLAY_ZOOM, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("worldmapWaypointsScale")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.WAYPOINT_SCALE, (double)Float.parseFloat(args[1]));
               } else if (args[0].equalsIgnoreCase("openMapAnimation")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.OPENING_ANIMATION, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("reloadVersion")) {
                  primaryConfig.set(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED_VERSION, Integer.parseInt(args[1]));
               } else if (args[0].equalsIgnoreCase("reloadEverything")) {
                  primaryConfig.set(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("zoomButtons")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.ZOOM_BUTTONS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("waypointBackgrounds")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.WAYPOINT_BACKGROUNDS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("mapItemId")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.MAP_ITEM, args[1] + ":" + args[2]);
               } else if (args[0].equalsIgnoreCase("detectAmbiguousY")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.DETECT_AMBIGUOUS_Y, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("showDisabledWaypoints")) {
                  primaryConfig.set(WorldMapPrimaryClientConfigOptions.DISPLAY_DISABLED_WAYPOINTS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("closeWaypointsWhenHopping")) {
                  primaryConfig.set(WorldMapPrimaryClientConfigOptions.CLOSE_WAYPOINTS_AFTER_HOP, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("adjustHeightForCarpetLikeBlocks")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.ADJUST_HEIGHT_FOR_SHORT_BLOCKS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("onlyCurrentMapWaypoints")) {
                  primaryConfig.set(WorldMapPrimaryClientConfigOptions.ONLY_CURRENT_MAP_WAYPOINTS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("minZoomForLocalWaypoints")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.MIN_ZOOM_LOCAL_WAYPOINTS, Double.parseDouble(args[1]));
               } else if (args[0].equalsIgnoreCase("arrowColour")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.ARROW_COLOR, Integer.parseInt(args[1]));
               } else if (args[0].equalsIgnoreCase("minimapRadar")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.MINIMAP_RADAR, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("renderWaypoints")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.RENDER_WAYPOINTS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("partialYTeleportation")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.PARTIAL_Y_TELEPORT, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("displayStainedGlass")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.STAINED_GLASS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("caveModeDepth")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.CAVE_MODE_DEPTH, Integer.parseInt(args[1]));
               } else if (args[0].equalsIgnoreCase("caveModeStart")) {
                  primaryConfig.set(WorldMapPrimaryClientConfigOptions.CAVE_MODE_START, Integer.parseInt(args[1]));
               } else if (args[0].equalsIgnoreCase("autoCaveMode")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.AUTO_CAVE_MODE, Integer.parseInt(args[1]));
               } else if (args[0].equalsIgnoreCase("legibleCaveMaps")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.LEGIBLE_CAVE_MAPS, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("displayCaveModeStart")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.DISPLAY_CAVE_MODE_START, args[1].equals("true"));
               } else if (args[0].equalsIgnoreCase("caveModeToggleTimer")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.CAVE_MODE_TOGGLE_TIMER, (double)Integer.parseInt(args[1]) / 1000.0D);
               } else if (args[0].equalsIgnoreCase("defaultCaveModeType")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.DEFAULT_CAVE_MODE_TYPE, Integer.parseInt(args[1]));
               } else if (args[0].equalsIgnoreCase("biomeBlending")) {
                  currentProfile.set(WorldMapProfiledConfigOptions.BIOME_BLENDING, args[1].equals("true"));
               } else if (!args[0].equalsIgnoreCase("trackedPlayers") && !args[0].equalsIgnoreCase("pacPlayers")) {
                  if (args[0].equalsIgnoreCase("multipleImagesExport")) {
                     primaryConfig.set(WorldMapPrimaryClientConfigOptions.EXPORT_MULTIPLE_IMAGES, args[1].equals("true"));
                  } else if (args[0].equalsIgnoreCase("nightExport")) {
                     primaryConfig.set(WorldMapPrimaryClientConfigOptions.NIGHT_EXPORT, args[1].equals("true"));
                  } else if (args[0].equalsIgnoreCase("highlightsExport")) {
                     primaryConfig.set(WorldMapPrimaryClientConfigOptions.EXPORT_HIGHLIGHTS, args[1].equals("true"));
                  } else if (args[0].equalsIgnoreCase("exportScaleDownSquare")) {
                     primaryConfig.set(WorldMapPrimaryClientConfigOptions.EXPORT_SCALE_DOWN_SQUARE, Integer.parseInt(args[1]));
                  } else if (args[0].equalsIgnoreCase("mapWritingDistance")) {
                     currentProfile.set(WorldMapProfiledConfigOptions.WRITING_DISTANCE, Integer.parseInt(args[1]));
                  } else if (args[0].equalsIgnoreCase("displayClaims")) {
                     currentProfile.set(WorldMapProfiledConfigOptions.OPAC_CLAIMS, args[1].equals("true"));
                  } else if (args[0].equalsIgnoreCase("claimsOpacity")) {
                     int claimsBorderOpacity = Math.min(100, Math.max(0, Integer.parseInt(args[1])));
                     int claimsFillOpacity = claimsBorderOpacity * 58 / 100;
                     currentProfile.set(WorldMapProfiledConfigOptions.OPAC_CLAIMS_BORDER_OPACITY, claimsBorderOpacity);
                     currentProfile.set(WorldMapProfiledConfigOptions.OPAC_CLAIMS_FILL_OPACITY, claimsFillOpacity);
                  } else if (args[0].equalsIgnoreCase("claimsBorderOpacity")) {
                     currentProfile.set(WorldMapProfiledConfigOptions.OPAC_CLAIMS_BORDER_OPACITY, Integer.parseInt(args[1]));
                  } else if (args[0].equalsIgnoreCase("claimsFillOpacity")) {
                     currentProfile.set(WorldMapProfiledConfigOptions.OPAC_CLAIMS_FILL_OPACITY, Integer.parseInt(args[1]));
                  } else if (args[0].equalsIgnoreCase("globalVersion")) {
                     primaryConfig.set(WorldMapPrimaryClientConfigOptions.GLOBAL_VERSION, Integer.parseInt(args[1]));
                  }
               } else {
                  currentProfile.set(WorldMapProfiledConfigOptions.DISPLAY_TRACKED_PLAYERS, args[1].equals("true"));
               }
            } catch (Exception var13) {
               WorldMap.LOGGER.info("Skipping setting:" + args[0]);
            }
         }
      } finally {
         if (reader != null) {
            reader.close();
         }

      }

   }

   public int getRegionCacheHashCode() {
      return this.regionCacheHashCode;
   }

   public void updateRegionCacheHashCode() {
      int currentRegionCacheHashCode = this.regionCacheHashCode;
      if (!class_310.method_1551().method_18854()) {
         throw new RuntimeException("Wrong thread!");
      } else {
         ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
         int colours = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.BLOCK_COLORS);
         boolean terrainDepth = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.TERRAIN_DEPTH);
         int terrainSlopes = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.TERRAIN_SLOPES);
         boolean biomeBlending = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.BIOME_BLENDING);
         boolean biomeColorsVanillaMode = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.BIOME_COLORS_IN_VANILLA);
         boolean adjustHeightForCarpetLikeBlocks = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.ADJUST_HEIGHT_FOR_SHORT_BLOCKS);
         boolean displayStainedGlass = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.STAINED_GLASS);
         boolean legibleCaveMaps = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.LEGIBLE_CAVE_MAPS);
         boolean ignoreHeightMaps = false;
         WorldMapSession session = WorldMapSession.getCurrentSession();
         if (session != null) {
            ignoreHeightMaps = session.getMapProcessor().getMapWorld().isIgnoreHeightmaps();
         }

         HashCodeBuilder hcb = new HashCodeBuilder();
         hcb.append(colours).append(terrainDepth).append(terrainSlopes).append(false).append(colours == 1 && biomeColorsVanillaMode).append(ignoreHeightMaps).append(adjustHeightForCarpetLikeBlocks).append(displayStainedGlass).append(legibleCaveMaps).append(biomeBlending);
         Collection<class_3288> enabledResourcePacks = class_310.method_1551().method_1520().method_14444();
         Iterator var15 = enabledResourcePacks.iterator();

         while(var15.hasNext()) {
            class_3288 resourcePack = (class_3288)var15.next();
            hcb.append(resourcePack.method_14463());
         }

         this.regionCacheHashCode = hcb.toHashCode();
         if (currentRegionCacheHashCode != this.regionCacheHashCode) {
            WorldMap.LOGGER.info("New world map region cache hash code: " + this.regionCacheHashCode);
         }

      }
   }

   public static boolean canEditIngameSettings() {
      WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
      return worldmapSession != null && worldmapSession.getMapProcessor().getMapWorld() != null;
   }
}
