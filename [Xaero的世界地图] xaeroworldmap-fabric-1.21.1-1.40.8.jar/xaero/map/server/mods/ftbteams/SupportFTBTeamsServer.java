package xaero.map.server.mods.ftbteams;

import xaero.map.server.radar.tracker.ISyncedPlayerTrackerSystem;

public class SupportFTBTeamsServer {
   private final ISyncedPlayerTrackerSystem syncedPlayerTrackerSystem = new FTBTeamsSyncedPlayerTrackerSystem();

   public ISyncedPlayerTrackerSystem getSyncedPlayerTrackerSystem() {
      return this.syncedPlayerTrackerSystem;
   }
}
