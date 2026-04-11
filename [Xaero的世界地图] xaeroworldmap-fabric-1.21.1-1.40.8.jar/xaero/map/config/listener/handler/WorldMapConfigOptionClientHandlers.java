package xaero.map.config.listener.handler;

import net.minecraft.class_310;
import xaero.lib.client.config.listener.ClientConfigChangeListener;
import xaero.lib.common.config.Config;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;

public class WorldMapConfigOptionClientHandlers {
   private static void handleBlockColors(Config config) {
      WorldMap.settings.updateRegionCacheHashCode();
   }

   private static void handleTerrainDepth(Config config) {
      WorldMap.settings.updateRegionCacheHashCode();
   }

   private static void handleTerrainSlopes(Config config) {
      WorldMap.settings.updateRegionCacheHashCode();
   }

   private static void handleBiomeBlending(Config config) {
      WorldMap.settings.updateRegionCacheHashCode();
   }

   private static void handleBiomesInVanilla(Config config) {
      WorldMap.settings.updateRegionCacheHashCode();
   }

   private static void handleAdjustShortBlockHeight(Config config) {
      WorldMap.settings.updateRegionCacheHashCode();
   }

   private static void handleStainedGlass(Config config) {
      WorldMap.settings.updateRegionCacheHashCode();
   }

   private static void handleLegibleCaveMaps(Config config) {
      WorldMap.settings.updateRegionCacheHashCode();
   }

   private static void handleFlowers(Config config) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         WorldMapSession session = WorldMapSession.getCurrentSession();
         if (session != null) {
            session.getMapProcessor().getMapWriter().setDirtyInWriteDistance(mc.field_1724, mc.field_1687);
         }

      }
   }

   private static void clearAllCachedHighlightHashes() {
      WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
      if (worldmapSession != null) {
         synchronized(worldmapSession.getMapProcessor().uiSync) {
            worldmapSession.getMapProcessor().getMapWorld().clearAllCachedHighlightHashes();
         }
      }
   }

   private static void handleOpacClaims(Config config) {
      clearAllCachedHighlightHashes();
   }

   private static void handleOpacClaimFillOpacity(Config config) {
      clearAllCachedHighlightHashes();
   }

   private static void handleOpacClaimBorderOpacity(Config config) {
      clearAllCachedHighlightHashes();
   }

   private static void handleReloadViewed(Config config) {
      if ((Boolean)config.get(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED)) {
         config.set(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED_VERSION, (Integer)config.get(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED_VERSION) + 1);
      }

   }

   private static void handleMapItem(Config config) {
      WorldMapSession session = WorldMapSession.getCurrentSession();
      if (session != null) {
         session.getMapProcessor().updateMapItem();
      }

   }

   public static void registerAll(ClientConfigChangeListener registry) {
      registry.register(WorldMapProfiledConfigOptions.BLOCK_COLORS, WorldMapConfigOptionClientHandlers::handleBlockColors);
      registry.register(WorldMapProfiledConfigOptions.TERRAIN_DEPTH, WorldMapConfigOptionClientHandlers::handleTerrainDepth);
      registry.register(WorldMapProfiledConfigOptions.TERRAIN_SLOPES, WorldMapConfigOptionClientHandlers::handleTerrainSlopes);
      registry.register(WorldMapProfiledConfigOptions.BIOME_BLENDING, WorldMapConfigOptionClientHandlers::handleBiomeBlending);
      registry.register(WorldMapProfiledConfigOptions.BIOME_COLORS_IN_VANILLA, WorldMapConfigOptionClientHandlers::handleBiomesInVanilla);
      registry.register(WorldMapProfiledConfigOptions.ADJUST_HEIGHT_FOR_SHORT_BLOCKS, WorldMapConfigOptionClientHandlers::handleAdjustShortBlockHeight);
      registry.register(WorldMapProfiledConfigOptions.STAINED_GLASS, WorldMapConfigOptionClientHandlers::handleStainedGlass);
      registry.register(WorldMapProfiledConfigOptions.LEGIBLE_CAVE_MAPS, WorldMapConfigOptionClientHandlers::handleLegibleCaveMaps);
      registry.register(WorldMapProfiledConfigOptions.FLOWERS, WorldMapConfigOptionClientHandlers::handleFlowers);
      registry.register(WorldMapProfiledConfigOptions.OPAC_CLAIMS, WorldMapConfigOptionClientHandlers::handleOpacClaims);
      registry.register(WorldMapProfiledConfigOptions.OPAC_CLAIMS_FILL_OPACITY, WorldMapConfigOptionClientHandlers::handleOpacClaimFillOpacity);
      registry.register(WorldMapProfiledConfigOptions.OPAC_CLAIMS_BORDER_OPACITY, WorldMapConfigOptionClientHandlers::handleOpacClaimBorderOpacity);
      registry.register(WorldMapPrimaryClientConfigOptions.RELOAD_VIEWED, WorldMapConfigOptionClientHandlers::handleReloadViewed);
      registry.register(WorldMapProfiledConfigOptions.MAP_ITEM, WorldMapConfigOptionClientHandlers::handleMapItem);
   }
}
