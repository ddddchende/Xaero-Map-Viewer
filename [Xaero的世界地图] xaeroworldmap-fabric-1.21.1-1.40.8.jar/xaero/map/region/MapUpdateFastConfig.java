package xaero.map.region;

import xaero.lib.client.config.ClientConfigManager;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;

public class MapUpdateFastConfig {
   public final int blockColors;
   public final boolean biomeBlending;
   public final boolean biomeColorsInVanilla;
   public final int terrainSlopes;
   public final boolean terrainDepth;
   public final boolean stainedGlass;
   public final boolean legibleCaveMaps;
   public final boolean adjustHeightForShortBlocks;

   public MapUpdateFastConfig() {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      this.blockColors = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.BLOCK_COLORS);
      this.biomeBlending = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.BIOME_BLENDING);
      this.biomeColorsInVanilla = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.BIOME_COLORS_IN_VANILLA);
      this.terrainSlopes = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.TERRAIN_SLOPES);
      this.terrainDepth = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.TERRAIN_DEPTH);
      this.stainedGlass = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.STAINED_GLASS);
      this.legibleCaveMaps = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.LEGIBLE_CAVE_MAPS);
      this.adjustHeightForShortBlocks = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.ADJUST_HEIGHT_FOR_SHORT_BLOCKS);
   }
}
