package xaero.map.mods.pac.party;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_1937;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_7924;
import xaero.map.radar.tracker.system.ITrackedPlayerReader;
import xaero.pac.common.parties.party.api.IPartyMemberDynamicInfoSyncableAPI;

public class OPACTrackedPlayerReader implements ITrackedPlayerReader<IPartyMemberDynamicInfoSyncableAPI> {
   private final Map<class_2960, class_5321<class_1937>> dimensionKeyCache = new HashMap();

   public UUID getId(IPartyMemberDynamicInfoSyncableAPI player) {
      return player.getPlayerId();
   }

   public double getX(IPartyMemberDynamicInfoSyncableAPI player) {
      return player.getX();
   }

   public double getY(IPartyMemberDynamicInfoSyncableAPI player) {
      return player.getY();
   }

   public double getZ(IPartyMemberDynamicInfoSyncableAPI player) {
      return player.getZ();
   }

   public class_5321<class_1937> getDimension(IPartyMemberDynamicInfoSyncableAPI player) {
      if (player.getDimension() == null) {
         return null;
      } else {
         class_5321<class_1937> result = (class_5321)this.dimensionKeyCache.get(player.getDimension());
         if (result == null) {
            this.dimensionKeyCache.put(player.getDimension(), result = class_5321.method_29179(class_7924.field_41223, player.getDimension()));
         }

         return result;
      }
   }
}
