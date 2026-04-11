package xaero.map.server.radar.tracker;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;
import xaero.lib.XaeroLib;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.primary.option.LibPrimaryCommonConfigOptions;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.map.WorldMap;
import xaero.map.message.tracker.ClientboundTrackedPlayerPacket;
import xaero.map.server.MinecraftServerData;
import xaero.map.server.mods.SupportServerMods;
import xaero.map.server.player.ServerPlayerData;

public class SyncedPlayerTracker {
   public void onTick(MinecraftServer server, class_3222 player, MinecraftServerData serverData, ServerPlayerData playerData) {
      long currentTime = System.currentTimeMillis();
      if (currentTime - playerData.getLastTrackedPlayerSync() >= 250L) {
         playerData.setLastTrackedPlayerSync(currentTime);
         boolean playerHasMod = playerData.hasMod();
         boolean shouldSyncToPlayer = playerHasMod;
         if (SupportServerMods.hasMinimap() && SupportServerMods.getMinimap().supportsTrackedPlayers() && SupportServerMods.getMinimap().playerSupportsTrackedPlayers(player)) {
            if (playerData.getCurrentlySyncedPlayers() != null && !playerData.getCurrentlySyncedPlayers().isEmpty()) {
               Iterator var9 = playerData.getCurrentlySyncedPlayers().iterator();

               while(var9.hasNext()) {
                  UUID id = (UUID)var9.next();
                  this.sendRemovePacket(player, id);
               }

               playerData.getCurrentlySyncedPlayers().clear();
            }

            shouldSyncToPlayer = false;
         }

         SingleConfigManager<Config> primaryCommonConfig = WorldMap.INSTANCE.getConfigs().getPrimaryCommonConfigManager();
         SingleConfigManager<Config> libPrimaryCommonConfig = XaeroLib.INSTANCE.getLibConfigChannel().getPrimaryCommonConfigManager();
         boolean everyoneIsTracked = (Boolean)libPrimaryCommonConfig.getEffective(LibPrimaryCommonConfigOptions.EVERYONE_TRACKS_EVERYONE);
         Iterable<ISyncedPlayerTrackerSystem> playerTrackerSystems = serverData.getSyncedPlayerTrackerSystemManager().getSystems();
         Set<UUID> syncedPlayers = playerData.ensureCurrentlySyncedPlayers();
         Set<UUID> leftoverPlayers = new HashSet(syncedPlayers);
         SyncedTrackedPlayer toSync = playerData.getLastSyncedData();
         boolean shouldSyncToOthers = toSync == null || !toSync.matchesEnough(player, 0.0D);
         if (shouldSyncToOthers) {
            toSync = playerData.ensureLastSyncedData();
            toSync.update(player);
         }

         boolean opacReceiveParty = shouldSyncToPlayer && SupportServerMods.hasOpac() && SupportServerMods.getOpac().getReceiveLocationsFromPartyConfigValue(player);
         boolean opacReceiveMutualAllies = shouldSyncToPlayer && SupportServerMods.hasOpac() && SupportServerMods.getOpac().getReceiveLocationsFromMutualAlliesConfigValue(player);
         if (SupportServerMods.hasOpac()) {
            SupportServerMods.getOpac().updateShareLocationConfigValues(player, playerData);
         }

         Iterator var19 = server.method_3760().method_14571().iterator();

         while(true) {
            class_3222 otherPlayer;
            ServerPlayerData otherPlayerData;
            do {
               do {
                  if (!var19.hasNext()) {
                     var19 = leftoverPlayers.iterator();

                     while(var19.hasNext()) {
                        UUID offlineId = (UUID)var19.next();
                        syncedPlayers.remove(offlineId);
                        this.sendRemovePacket(player, offlineId);
                     }

                     return;
                  }

                  otherPlayer = (class_3222)var19.next();
               } while(otherPlayer == player);

               leftoverPlayers.remove(otherPlayer.method_5667());
               otherPlayerData = ServerPlayerData.get(otherPlayer);
               if (shouldSyncToOthers) {
                  Set<UUID> otherPlayerSyncedPlayers = otherPlayerData.getCurrentlySyncedPlayers();
                  if (otherPlayerSyncedPlayers != null && otherPlayerSyncedPlayers.contains(player.method_5667())) {
                     this.sendTrackedPlayerPacket(otherPlayer, toSync);
                  }
               }
            } while(!shouldSyncToPlayer);

            boolean tracked = everyoneIsTracked;
            boolean opacConfigsAllowPartySync;
            if (!everyoneIsTracked) {
               label195: {
                  opacConfigsAllowPartySync = !SupportServerMods.hasOpac() || SupportServerMods.getOpac().isPositionSyncAllowed(2, otherPlayerData, opacReceiveParty);
                  boolean opacConfigsAllowAllySync = !SupportServerMods.hasOpac() || SupportServerMods.getOpac().isPositionSyncAllowed(1, otherPlayerData, opacReceiveMutualAllies);
                  Iterator var25 = playerTrackerSystems.iterator();

                  ISyncedPlayerTrackerSystem system;
                  int trackingLevel;
                  do {
                     do {
                        if (!var25.hasNext()) {
                           break label195;
                        }

                        system = (ISyncedPlayerTrackerSystem)var25.next();
                        trackingLevel = system.getTrackingLevel(player, otherPlayer);
                     } while(trackingLevel <= 0);
                  } while(system.isPartySystem() && (trackingLevel != 1 || !opacConfigsAllowAllySync) && (trackingLevel <= 1 || !opacConfigsAllowPartySync));

                  tracked = true;
               }
            }

            opacConfigsAllowPartySync = syncedPlayers.contains(otherPlayer.method_5667());
            if (!tracked) {
               if (opacConfigsAllowPartySync) {
                  syncedPlayers.remove(otherPlayer.method_5667());
                  this.sendRemovePacket(player, otherPlayer.method_5667());
               }
            } else if (!opacConfigsAllowPartySync && otherPlayerData.getLastSyncedData() != null) {
               syncedPlayers.add(otherPlayer.method_5667());
               this.sendTrackedPlayerPacket(player, otherPlayerData.getLastSyncedData());
            }
         }
      }
   }

   private void sendRemovePacket(class_3222 player, UUID toRemove) {
      WorldMap.messageHandler.sendToPlayer(player, new ClientboundTrackedPlayerPacket(true, toRemove, 0.0D, 0.0D, 0.0D, (class_2960)null));
   }

   private void sendTrackedPlayerPacket(class_3222 player, SyncedTrackedPlayer tracked) {
      WorldMap.messageHandler.sendToPlayer(player, new ClientboundTrackedPlayerPacket(false, tracked.getId(), tracked.getX(), tracked.getY(), tracked.getZ(), tracked.getDimension().method_29177()));
   }
}
