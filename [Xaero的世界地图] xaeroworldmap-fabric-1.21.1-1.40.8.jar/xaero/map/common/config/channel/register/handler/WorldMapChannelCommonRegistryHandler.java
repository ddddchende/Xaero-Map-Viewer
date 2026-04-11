package xaero.map.common.config.channel.register.handler;

import xaero.lib.common.config.channel.register.handler.IConfigChannelCommonRegistryHandler;
import xaero.lib.common.config.option.ConfigOptionManager;
import xaero.lib.common.config.option.value.redirect.OptionValueRedirectorManager;
import xaero.lib.common.config.server.listener.ServerConfigChangeListener;
import xaero.map.common.config.listener.handler.WorldMapConfigOptionServerHandlers;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.common.config.option.value.redirect.WorldMapConfigOptionServerRedirectors;
import xaero.map.common.config.primary.option.WorldMapPrimaryCommonConfigOptions;

public class WorldMapChannelCommonRegistryHandler implements IConfigChannelCommonRegistryHandler {
   public void registerPrimaryCommonOptions(ConfigOptionManager manager) {
      WorldMapPrimaryCommonConfigOptions.registerAll(manager);
   }

   public void registerProfiledOptions(ConfigOptionManager manager) {
      WorldMapProfiledConfigOptions.registerAll(manager);
   }

   public void registerServerOptionChangeHandlers(ServerConfigChangeListener registry) {
      WorldMapConfigOptionServerHandlers.registerAll(registry);
   }

   public void registerOptionServerRedirectors(OptionValueRedirectorManager manager) {
      WorldMapConfigOptionServerRedirectors.registerAll(manager);
   }
}
