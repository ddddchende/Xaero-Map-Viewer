package xaero.map.config.channel.register.handler;

import xaero.lib.client.config.channel.register.handler.IConfigChannelClientRegistryHandler;
import xaero.lib.client.config.listener.ClientConfigChangeListener;
import xaero.lib.client.config.option.ClientConfigOptionManager;
import xaero.lib.client.config.option.ui.ConfigOptionUITypeManager;
import xaero.lib.client.config.option.value.redirect.ClientOptionValueRedirectorManager;
import xaero.map.config.listener.handler.WorldMapConfigOptionClientHandlers;
import xaero.map.config.option.ui.WorldMapConfigOptionUIRegister;
import xaero.map.config.option.value.redirect.WorldMapConfigOptionClientRedirectors;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;

public class WorldMapChannelClientRegistryHandler implements IConfigChannelClientRegistryHandler {
   public void registerPrimaryClientOptions(ClientConfigOptionManager manager) {
      WorldMapPrimaryClientConfigOptions.registerAll(manager);
   }

   public void registerConfigOptionUITypes(ConfigOptionUITypeManager configOptionUITypeManager) {
      WorldMapConfigOptionUIRegister.registerAll(configOptionUITypeManager);
   }

   public void registerClientOptionChangeHandlers(ClientConfigChangeListener registry) {
      WorldMapConfigOptionClientHandlers.registerAll(registry);
   }

   public void registerOptionClientRedirectors(ClientOptionValueRedirectorManager manager) {
      WorldMapConfigOptionClientRedirectors.registerAll(manager);
   }
}
