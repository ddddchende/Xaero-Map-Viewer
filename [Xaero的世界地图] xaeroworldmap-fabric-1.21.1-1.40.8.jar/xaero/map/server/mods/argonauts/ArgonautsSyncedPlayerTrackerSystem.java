package xaero.map.server.mods.argonauts;

import earth.terrarium.argonauts.api.guild.Guild;
import earth.terrarium.argonauts.api.guild.GuildApi;
import earth.terrarium.argonauts.api.party.Party;
import earth.terrarium.argonauts.api.party.PartyApi;
import net.minecraft.class_1657;
import xaero.map.server.radar.tracker.ISyncedPlayerTrackerSystem;

public class ArgonautsSyncedPlayerTrackerSystem implements ISyncedPlayerTrackerSystem {
   public int getTrackingLevel(class_1657 tracker, class_1657 tracked) {
      int partyTrackingLevel = this.getPartyTrackingLevel(tracker, tracked);
      int guildTrackingLevel = this.getGuildTrackingLevel(tracker, tracked);
      return Math.max(partyTrackingLevel, guildTrackingLevel);
   }

   public boolean isPartySystem() {
      return true;
   }

   private int getPartyTrackingLevel(class_1657 tracker, class_1657 tracked) {
      Party trackerParty = PartyApi.API.get(tracker);
      if (trackerParty == null) {
         return 0;
      } else {
         Party trackedParty = PartyApi.API.get(tracked);
         return trackerParty == trackedParty ? 2 : 0;
      }
   }

   private int getGuildTrackingLevel(class_1657 tracker, class_1657 tracked) {
      Guild trackerGuild = GuildApi.API.getPlayerGuild(tracker.method_5682(), tracker.method_5667());
      if (trackerGuild == null) {
         return 0;
      } else {
         Guild trackedGuild = GuildApi.API.getPlayerGuild(tracked.method_5682(), tracked.method_5667());
         return trackerGuild == trackedGuild ? 2 : 0;
      }
   }
}
