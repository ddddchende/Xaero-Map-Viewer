package xaero.map.mods.pac.gui.claim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import xaero.map.misc.Area;
import xaero.map.mods.pac.SupportOpenPartiesAndClaims;
import xaero.pac.common.claims.result.api.AreaClaimResult;

public class ClaimResultElementManager {
   private Map<Area, ClaimResultElement> claimResults;

   private ClaimResultElementManager(Map<Area, ClaimResultElement> claimResults) {
      this.claimResults = claimResults;
   }

   public static long getLongCoordinatesFor(int x, int z) {
      return (long)x << 32 | (long)z & 4294967295L;
   }

   public static int getXFromLongCoordinates(long key) {
      return (int)(key >> 32);
   }

   public static int getZFromLongCoordinates(long key) {
      return (int)(key & -1L);
   }

   public ClaimResultElement add(AreaClaimResult result) {
      Area key = new Area(result.getLeft(), result.getTop(), result.getRight(), result.getBottom());
      ClaimResultElement toReturn;
      this.claimResults.put(key, toReturn = ClaimResultElement.Builder.begin().setKey(key).setResult(result).build());
      return toReturn;
   }

   public void remove(ClaimResultElement element) {
      this.claimResults.remove(element.getKey());
   }

   public Iterator<ClaimResultElement> getIterator() {
      return this.claimResults.values().iterator();
   }

   public void clear() {
      this.claimResults.clear();
   }

   public static final class Builder {
      private SupportOpenPartiesAndClaims pac;

      private Builder() {
      }

      private ClaimResultElementManager.Builder setDefault() {
         return this;
      }

      public ClaimResultElementManager.Builder setPac(SupportOpenPartiesAndClaims pac) {
         this.pac = pac;
         return this;
      }

      public ClaimResultElementManager build() {
         if (this.pac == null) {
            throw new IllegalStateException();
         } else {
            ClaimResultElementManager result = new ClaimResultElementManager(new HashMap());
            return result;
         }
      }

      public static ClaimResultElementManager.Builder begin() {
         return (new ClaimResultElementManager.Builder()).setDefault();
      }
   }
}
