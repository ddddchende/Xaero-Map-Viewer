package xaero.map.mods.pac.gui.claim.result;

import net.minecraft.class_2561;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.mods.SupportMods;
import xaero.map.mods.pac.gui.claim.ClaimResultElement;
import xaero.pac.client.claims.tracker.result.api.IClaimsManagerClaimResultListenerAPI;
import xaero.pac.common.claims.result.api.AreaClaimResult;

public class ClientClaimResultListener implements IClaimsManagerClaimResultListenerAPI {
   public void onClaimResult(AreaClaimResult result) {
      SupportMods.xaeroPac.getClaimResultElementManager().clear();
      ClaimResultElement resultElement = SupportMods.xaeroPac.getClaimResultElementManager().add(result);
      WorldMapSession session = WorldMapSession.getCurrentSession();
      if (session != null) {
         MapProcessor mapProcessor = session.getMapProcessor();
         resultElement.getFilteredResultTypeIterator().forEachRemaining((type) -> {
            mapProcessor.getMessageBox().addMessageWithSource(class_2561.method_43470("Claims"), type.message);
         });
      }

   }
}
