package xaero.map.server.mods.ftbteams;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamRank;
import net.minecraft.class_1657;
import xaero.map.server.radar.tracker.ISyncedPlayerTrackerSystem;

public class FTBTeamsSyncedPlayerTrackerSystem implements ISyncedPlayerTrackerSystem {
   public int getTrackingLevel(class_1657 tracker, class_1657 tracked) {
      if (FTBTeamsAPI.api().getManager().arePlayersInSameTeam(tracker.method_5667(), tracked.method_5667())) {
         return 2;
      } else {
         Team trackerTeam = (Team)FTBTeamsAPI.api().getManager().getTeamForPlayerID(tracker.method_5667()).orElse((Object)null);
         if (trackerTeam == null) {
            return 0;
         } else {
            Team trackedTeam = (Team)FTBTeamsAPI.api().getManager().getTeamForPlayerID(tracked.method_5667()).orElse((Object)null);
            if (trackedTeam == null) {
               return 0;
            } else {
               return trackerTeam.getRankForPlayer(tracked.method_5667()) == TeamRank.ALLY && trackedTeam.getRankForPlayer(tracker.method_5667()) == TeamRank.ALLY ? 1 : 0;
            }
         }
      }
   }

   public boolean isPartySystem() {
      return true;
   }
}
