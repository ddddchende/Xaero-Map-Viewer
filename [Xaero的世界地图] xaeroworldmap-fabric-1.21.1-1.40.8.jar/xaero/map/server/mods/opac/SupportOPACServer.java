package xaero.map.server.mods.opac;

import net.minecraft.class_3222;
import xaero.map.server.player.ServerPlayerData;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigManagerAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

public class SupportOPACServer {
   public boolean isPositionSyncAllowed(int relationship, ServerPlayerData fromPlayerData, boolean receive) {
      if (!receive) {
         return false;
      } else if (relationship <= 0) {
         return false;
      } else {
         ServerPlayerOpacData fromPlayerOpacData = this.getPlayerOpacData(fromPlayerData);
         if (relationship == 1 && !fromPlayerOpacData.shareLocationWithMutualAllies) {
            return false;
         } else {
            return relationship <= 1 || fromPlayerOpacData.shareLocationWithParty;
         }
      }
   }

   public boolean getReceiveLocationsFromMutualAlliesConfigValue(class_3222 player) {
      IPlayerConfigManagerAPI configManager = OpenPACServerAPI.get(player.method_5682()).getPlayerConfigs();
      IPlayerConfigAPI config = configManager.getLoadedConfig(player.method_5667());
      return (Boolean)config.getEffective(PlayerConfigOptions.RECEIVE_LOCATIONS_FROM_PARTY_MUTUAL_ALLIES);
   }

   public boolean getReceiveLocationsFromPartyConfigValue(class_3222 player) {
      IPlayerConfigManagerAPI configManager = OpenPACServerAPI.get(player.method_5682()).getPlayerConfigs();
      IPlayerConfigAPI config = configManager.getLoadedConfig(player.method_5667());
      return (Boolean)config.getEffective(PlayerConfigOptions.RECEIVE_LOCATIONS_FROM_PARTY);
   }

   public void updateShareLocationConfigValues(class_3222 player, ServerPlayerData playerData) {
      ServerPlayerOpacData opacData = this.getPlayerOpacData(playerData);
      IPlayerConfigManagerAPI configManager = OpenPACServerAPI.get(player.method_5682()).getPlayerConfigs();
      IPlayerConfigAPI config = configManager.getLoadedConfig(player.method_5667());
      opacData.shareLocationWithParty = (Boolean)config.getEffective(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY);
      opacData.shareLocationWithMutualAllies = (Boolean)config.getEffective(PlayerConfigOptions.SHARE_LOCATION_WITH_PARTY_MUTUAL_ALLIES);
   }

   private ServerPlayerOpacData getPlayerOpacData(ServerPlayerData playerData) {
      ServerPlayerOpacData opacData = (ServerPlayerOpacData)playerData.getOpacData();
      if (opacData == null) {
         playerData.setOpacData(opacData = new ServerPlayerOpacData());
      }

      return opacData;
   }
}
