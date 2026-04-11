package xaero.map.events;

import java.nio.file.Path;
import java.util.Set;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_5218;
import net.minecraft.server.MinecraftServer;
import xaero.lib.common.config.profile.ConfigProfile;
import xaero.lib.common.config.server.ServerConfigManager;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.message.basic.ClientboundRulesPacket;
import xaero.map.message.basic.HandshakePacket;
import xaero.map.message.tracker.ClientboundPlayerTrackerResetPacket;
import xaero.map.server.MinecraftServerData;
import xaero.map.server.MineraftServerDataInitializer;
import xaero.map.server.level.LevelMapProperties;
import xaero.map.server.player.IServerPlayer;
import xaero.map.server.player.ServerPlayerData;

public class CommonEvents {
   protected void onPlayerClone(class_1657 oldPlayer, class_1657 newPlayer, boolean alive) {
      if (oldPlayer instanceof class_3222) {
         class_3222 oldServerPlayer = (class_3222)oldPlayer;
         ((IServerPlayer)newPlayer).setXaeroWorldMapPlayerData(ServerPlayerData.get(oldServerPlayer));
      }

   }

   public void onServerStarting(MinecraftServer server) {
      (new MineraftServerDataInitializer()).init(server);
   }

   public void onServerStopped(MinecraftServer server) {
   }

   public void onPlayerLogIn(class_1657 player) {
      if (player instanceof class_3222) {
         class_3222 serverPlayer = (class_3222)player;
         WorldMap.messageHandler.sendToPlayer(serverPlayer, new ClientboundPlayerTrackerResetPacket());
      }

   }

   public void onPlayerWorldJoin(class_3222 player) {
      WorldMap.messageHandler.sendToPlayer(player, new HandshakePacket());
      ServerConfigManager configManager = WorldMap.INSTANCE.getConfigs().getServerConfigManager();
      ConfigProfile defaultEnforcedProfile = configManager.getDefaultEnforcedProfile();
      boolean caveModeConfig = !Boolean.FALSE.equals(defaultEnforcedProfile.get(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED));
      Set<class_2960> caveModeDimensionsConfig = (Set)defaultEnforcedProfile.get(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED_DIMENSIONS);
      boolean allowOverworldCaveModeOnServer = caveModeConfig && (caveModeDimensionsConfig == null || caveModeDimensionsConfig.isEmpty() || caveModeDimensionsConfig.contains(class_1937.field_25179.method_29177()));
      boolean allowNetherCaveModeOnServer = caveModeConfig && (caveModeDimensionsConfig == null || caveModeDimensionsConfig.isEmpty() || caveModeDimensionsConfig.contains(class_1937.field_25180.method_29177()));
      WorldMap.messageHandler.sendToPlayer(player, new ClientboundRulesPacket(allowOverworldCaveModeOnServer, allowNetherCaveModeOnServer));
      Path propertiesPath = player.method_37908().method_8503().method_27050(class_5218.field_24184).getParent().resolve("xaeromap.txt");

      try {
         MinecraftServerData serverData = MinecraftServerData.get(player.method_5682());
         LevelMapProperties properties = serverData.getLevelProperties(propertiesPath);
         if (properties.isUsable()) {
            WorldMap.messageHandler.sendToPlayer(player, properties);
         }
      } catch (Throwable var11) {
         WorldMap.LOGGER.error("suppressed exception", var11);
         player.field_13987.method_52396(class_2561.method_43471("gui.xaero_wm_error_loading_properties"));
      }

   }

   public void handlePlayerTickStart(class_1657 player) {
      if (player instanceof class_3222) {
         WorldMap.serverPlayerTickHandler.tick((class_3222)player);
      } else {
         if (WorldMap.events != null) {
            WorldMap.events.handlePlayerTickStart(player);
         }

      }
   }
}
