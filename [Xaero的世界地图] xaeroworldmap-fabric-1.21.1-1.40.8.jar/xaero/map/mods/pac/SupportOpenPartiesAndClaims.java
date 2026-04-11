package xaero.map.mods.pac;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.class_1074;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_640;
import net.minecraft.class_3675.class_307;
import xaero.lib.client.controls.util.KeyMappingUtils;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.controls.ControlsRegister;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.highlight.HighlighterRegistry;
import xaero.map.mods.SupportMods;
import xaero.map.mods.pac.gui.claim.ClaimResultElementManager;
import xaero.map.mods.pac.gui.claim.element.ClaimResultElementRenderer;
import xaero.map.mods.pac.gui.claim.result.ClientClaimResultListener;
import xaero.map.mods.pac.highlight.ClaimsHighlighter;
import xaero.map.mods.pac.party.OPACPlayerTrackerSystem;
import xaero.map.radar.tracker.PlayerTrackerMapElement;
import xaero.pac.client.api.OpenPACClientAPI;
import xaero.pac.client.claims.api.IClientClaimsManagerAPI;
import xaero.pac.client.claims.api.IClientDimensionClaimsManagerAPI;
import xaero.pac.client.claims.api.IClientRegionClaimsAPI;
import xaero.pac.client.claims.player.api.IClientPlayerClaimInfoAPI;
import xaero.pac.client.parties.party.api.IClientPartyAPI;
import xaero.pac.client.parties.party.api.IClientPartyMemberDynamicInfoSyncableStorageAPI;
import xaero.pac.client.parties.party.api.IClientPartyStorageAPI;
import xaero.pac.client.player.config.api.IPlayerConfigClientStorageAPI;
import xaero.pac.client.player.config.api.IPlayerConfigClientStorageManagerAPI;
import xaero.pac.client.player.config.api.IPlayerConfigStringableOptionClientStorageAPI;
import xaero.pac.client.world.api.IClientWorldDataAPI;
import xaero.pac.client.world.capability.api.ClientWorldCapabilityTypes;
import xaero.pac.client.world.capability.api.ClientWorldMainCapabilityAPI;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.claims.player.api.IPlayerDimensionClaimsAPI;
import xaero.pac.common.parties.party.ally.api.IPartyAllyAPI;
import xaero.pac.common.parties.party.api.IPartyMemberDynamicInfoSyncableAPI;
import xaero.pac.common.parties.party.api.IPartyPlayerInfoAPI;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;

public class SupportOpenPartiesAndClaims {
   private final OpenPACClientAPI api = OpenPACClientAPI.get();
   private final IClientClaimsManagerAPI<IClientPlayerClaimInfoAPI<IPlayerDimensionClaimsAPI<IPlayerClaimPosListAPI>>, IClientDimensionClaimsManagerAPI<IClientRegionClaimsAPI>> claimsManager;
   private final IClientPartyStorageAPI<IClientPartyAPI<IPartyMemberAPI, IPartyPlayerInfoAPI, IPartyAllyAPI>, IClientPartyMemberDynamicInfoSyncableStorageAPI<IPartyMemberDynamicInfoSyncableAPI>> partyStorage;
   private final IPlayerConfigClientStorageManagerAPI<IPlayerConfigClientStorageAPI<IPlayerConfigStringableOptionClientStorageAPI<?>>> playerConfigs;
   private final ClaimResultElementManager claimResultElementManager;
   private final ClaimResultElementRenderer claimResultElementRenderer;
   private boolean serverModDetected;

   public SupportOpenPartiesAndClaims() {
      this.claimsManager = this.api.getClaimsManager();
      this.partyStorage = this.api.getClientPartyStorage();
      this.playerConfigs = this.api.getPlayerConfigClientStorageManager();
      this.claimResultElementManager = ClaimResultElementManager.Builder.begin().setPac(this).build();
      this.claimResultElementRenderer = ClaimResultElementRenderer.Builder.begin().setManager(this.claimResultElementManager).build();
   }

   public void register() {
      this.claimsManager.getTracker().register(new ClientClaimChangeListener());
      this.claimsManager.getClaimResultTracker().register(new ClientClaimResultListener());
      WorldMap.playerTrackerSystemManager.register("openpartiesandclaims", new OPACPlayerTrackerSystem(this));
   }

   public IPlayerChunkClaimAPI claimAt(class_2960 dimension, int chunkX, int chunkZ) {
      return this.claimsManager.get(dimension, chunkX, chunkZ);
   }

   public void onMapRender(class_310 mc, class_4587 matrixStack, int scaledMouseX, int scaledMouseY, float partialTicks, class_2960 dimension, int highlightChunkX, int highlightChunkZ) {
   }

   public boolean isFromParty(UUID playerId) {
      IClientPartyAPI<IPartyMemberAPI, IPartyPlayerInfoAPI, IPartyAllyAPI> party = this.partyStorage.getParty();
      if (party == null) {
         return false;
      } else {
         return this.partyStorage.getParty().getMemberInfo(playerId) != null;
      }
   }

   public void openPlayerConfigScreen(class_437 escape, class_437 parent, PlayerTrackerMapElement<?> player) {
      if (player.getPlayerId().equals(class_310.method_1551().field_1724.method_5667())) {
         this.playerConfigs.openMyPlayerConfigScreen(escape, parent);
      } else {
         class_640 info = class_310.method_1551().method_1562().method_2871(player.getPlayerId());
         if (info != null) {
            this.playerConfigs.openOtherPlayerConfigScreen(escape, parent, info.method_2966().getName());
         }
      }

   }

   public boolean onMapKeyPressed(class_307 type, int code, GuiMap screen) {
      if (KeyMappingUtils.inputMatches(type, code, this.getPacClaimsKeyBinding(), 0)) {
         screen.onClaimsButton((class_4185)null);
         return true;
      } else if (KeyMappingUtils.inputMatches(type, code, this.api.getKeyBindings().getOpenModMenuKeyBinding(), 0)) {
         this.api.openMainMenuScreen(screen, screen);
         return true;
      } else {
         return false;
      }
   }

   public String getControlsTooltip() {
      return class_1074.method_4662("gui.xaero_box_controls_pac", new Object[]{KeyMappingUtils.getKeyName(this.api.getKeyBindings().getOpenModMenuKeyBinding())});
   }

   public void registerHighlighters(HighlighterRegistry highlightRegistry) {
      highlightRegistry.register(new ClaimsHighlighter(this.claimsManager));
   }

   public int getClaimDistance() {
      return this.claimsManager.getMaxClaimDistance();
   }

   public ClaimResultElementManager getClaimResultElementManager() {
      return this.claimResultElementManager;
   }

   public ClaimResultElementRenderer getCaimResultElementRenderer() {
      return this.claimResultElementRenderer;
   }

   public void onMapChange(boolean changedDimension) {
      if (changedDimension) {
         this.claimResultElementManager.clear();
      }

   }

   public void addRightClickOptions(GuiMap screen, ArrayList<RightClickOption> options, MapTileSelection mapTileSelection, MapProcessor mapProcessor) {
      if (mapTileSelection != null) {
         if (mapProcessor.getMapWorld().isUsingCustomDimension()) {
            options.add(new RightClickOption(this, "gui.xaero_pac_claim_selection_out_of_dimension", options.size(), screen) {
               public void onAction(class_437 screen) {
               }
            });
            return;
         }

         boolean hasUnclaimed = false;
         boolean hasClaimed = false;
         boolean hasForceloaded = false;
         boolean hasUnforceloaded = false;
         class_310 mc = class_310.method_1551();
         int fromX = mc.field_1724.method_31476().field_9181;
         int fromZ = mc.field_1724.method_31476().field_9180;
         final int left = mapTileSelection.getLeft();
         final int top = mapTileSelection.getTop();
         final int right = mapTileSelection.getRight();
         final int bottom = mapTileSelection.getBottom();
         int checkLeft = left;
         int checkTop = top;
         int checkRight = right;
         int checkBottom = bottom;
         if (!this.claimsManager.isAdminMode()) {
            int maxClaimDistance = this.claimsManager.getMaxClaimDistance();
            if (left < fromX - maxClaimDistance) {
               checkLeft = fromX - maxClaimDistance;
            }

            if (top < fromZ - maxClaimDistance) {
               checkTop = fromZ - maxClaimDistance;
            }

            if (right > fromX + maxClaimDistance) {
               checkRight = fromX + maxClaimDistance;
            }

            if (bottom > fromZ + maxClaimDistance) {
               checkBottom = fromZ + maxClaimDistance;
            }
         }

         int maxRequestLength = 32;
         if (checkRight - checkLeft >= maxRequestLength) {
            checkRight = checkLeft + maxRequestLength - 1;
         }

         if (checkBottom - checkTop >= maxRequestLength) {
            checkBottom = checkTop + maxRequestLength - 1;
         }

         IPlayerChunkClaimAPI potentialClaimState = this.claimsManager.getPotentialClaimStateReflection();

         label122:
         for(int x = checkLeft; x <= checkRight; ++x) {
            for(int z = checkTop; z <= checkBottom; ++z) {
               IPlayerChunkClaimAPI claim = this.claimsManager.get(mc.field_1687.method_27983().method_29177(), x, z);
               if (claim == null || (this.claimsManager.isAdminMode() || claim.getPlayerId().equals(potentialClaimState.getPlayerId())) && !potentialClaimState.isSameClaimType(claim)) {
                  hasUnclaimed = true;
               }

               if (claim != null) {
                  hasClaimed = true;
                  if (claim.isForceloadable()) {
                     hasForceloaded = true;
                  } else {
                     hasUnforceloaded = true;
                  }
               }

               if (hasUnclaimed && hasClaimed && hasForceloaded && hasUnforceloaded) {
                  break label122;
               }
            }
         }

         if (left < checkLeft) {
            left = checkLeft - 1;
         }

         if (top < checkTop) {
            top = checkTop - 1;
         }

         if (right > checkRight) {
            right = checkRight + 1;
         }

         if (bottom > checkBottom) {
            bottom = checkBottom + 1;
         }

         if (hasUnclaimed) {
            options.add(new RightClickOption("gui.xaero_pac_claim_chunks", options.size(), screen) {
               public void onAction(class_437 screen) {
                  SupportOpenPartiesAndClaims.this.claimsManager.requestAreaClaim(left, top, right, bottom, false);
               }
            });
         }

         if (hasClaimed) {
            options.add(new RightClickOption("gui.xaero_pac_unclaim_chunks", options.size(), screen) {
               public void onAction(class_437 screen) {
                  SupportOpenPartiesAndClaims.this.claimsManager.requestAreaUnclaim(left, top, right, bottom, false);
               }
            });
         }

         if (hasUnforceloaded) {
            options.add(new RightClickOption("gui.xaero_pac_forceload_chunks", options.size(), screen) {
               public void onAction(class_437 screen) {
                  SupportOpenPartiesAndClaims.this.claimsManager.requestAreaForceload(left, top, right, bottom, true, false);
               }
            });
         }

         if (hasForceloaded) {
            options.add(new RightClickOption("gui.xaero_pac_unforceload_chunks", options.size(), screen) {
               public void onAction(class_437 screen) {
                  SupportOpenPartiesAndClaims.this.claimsManager.requestAreaForceload(left, top, right, bottom, false, false);
               }
            });
         }

         if (!hasUnclaimed && !hasClaimed && !hasUnforceloaded && !hasForceloaded) {
            options.add(new RightClickOption(this, "gui.xaero_pac_claim_selection_out_of_range", options.size(), screen) {
               public void onAction(class_437 screen) {
               }
            });
         }
      }

   }

   public class_304 getPacClaimsKeyBinding() {
      return SupportMods.minimap() ? SupportMods.xaeroMinimap.getToggleClaimsKey() : ControlsRegister.keyTogglePacChunkClaims;
   }

   public void resetDetection() {
      this.serverModDetected = false;
   }

   private void detectStuff() {
      if (!this.serverModDetected) {
         ClientWorldMainCapabilityAPI capability = (ClientWorldMainCapabilityAPI)this.api.getCapabilityHelper().getCapability(class_310.method_1551().field_1687, ClientWorldCapabilityTypes.MAIN_CAP);
         if (capability != null) {
            IClientWorldDataAPI worldData = capability.getClientWorldData();
            this.serverModDetected = worldData.serverHasMod();
         }
      }

   }

   public boolean serverHasMod() {
      this.detectStuff();
      return this.serverModDetected;
   }

   public Iterator<IPartyMemberDynamicInfoSyncableAPI> getAllyIterator() {
      return this.partyStorage.getPartyMemberDynamicInfoSyncableStorage().getAllStream().iterator();
   }
}
