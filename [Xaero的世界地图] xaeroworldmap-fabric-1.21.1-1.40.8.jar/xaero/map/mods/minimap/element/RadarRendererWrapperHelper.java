package xaero.map.mods.minimap.element;

import xaero.common.IXaeroMinimap;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaero.lib.client.config.ClientConfigManager;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;

public class RadarRendererWrapperHelper {
   public void createWrapper(IXaeroMinimap modMain, RadarRenderer radarRenderer) {
      WorldMap.mapElementRenderHandler.add(MinimapElementRendererWrapper.Builder.begin(radarRenderer).setModMain(modMain).setShouldRenderSupplier(() -> {
         ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
         return (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.MINIMAP_RADAR);
      }).setOrder(100).build());
   }
}
