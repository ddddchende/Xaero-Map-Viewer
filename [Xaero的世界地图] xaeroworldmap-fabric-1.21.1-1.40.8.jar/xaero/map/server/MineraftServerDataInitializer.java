package xaero.map.server;

import net.minecraft.server.MinecraftServer;
import xaero.map.server.mods.SupportServerMods;
import xaero.map.server.radar.tracker.SyncedPlayerTracker;
import xaero.map.server.radar.tracker.SyncedPlayerTrackerSystemManager;

public class MineraftServerDataInitializer {
   public void init(MinecraftServer server) {
      SyncedPlayerTrackerSystemManager syncedPlayerTrackerSystemManager = new SyncedPlayerTrackerSystemManager();
      if (SupportServerMods.hasFtbTeams()) {
         syncedPlayerTrackerSystemManager.register("ftb_teams", SupportServerMods.getFtbTeams().getSyncedPlayerTrackerSystem());
      }

      if (SupportServerMods.hasArgonauts()) {
         syncedPlayerTrackerSystemManager.register("argonauts", SupportServerMods.getArgonauts().getSyncedPlayerTrackerSystem());
      }

      SyncedPlayerTracker syncedPlayerTracker = new SyncedPlayerTracker();
      MinecraftServerData data = new MinecraftServerData(syncedPlayerTrackerSystemManager, syncedPlayerTracker);
      ((IMinecraftServer)server).setXaeroWorldMapServerData(data);
   }
}
