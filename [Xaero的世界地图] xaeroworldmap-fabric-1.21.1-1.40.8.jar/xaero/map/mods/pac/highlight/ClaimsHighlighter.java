package xaero.map.mods.pac.highlight;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.class_1074;
import net.minecraft.class_124;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_5321;
import xaero.lib.client.config.ClientConfigManager;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.highlight.ChunkHighlighter;
import xaero.pac.client.claims.api.IClientClaimsManagerAPI;
import xaero.pac.client.claims.api.IClientDimensionClaimsManagerAPI;
import xaero.pac.client.claims.api.IClientRegionClaimsAPI;
import xaero.pac.client.claims.player.api.IClientPlayerClaimInfoAPI;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.claims.player.api.IPlayerDimensionClaimsAPI;
import xaero.pac.common.server.player.config.PlayerConfig;

public class ClaimsHighlighter extends ChunkHighlighter {
   private final IClientClaimsManagerAPI<IClientPlayerClaimInfoAPI<IPlayerDimensionClaimsAPI<IPlayerClaimPosListAPI>>, IClientDimensionClaimsManagerAPI<IClientRegionClaimsAPI>> claimsManager;
   private class_2561 cachedTooltip;
   private IPlayerChunkClaimAPI cachedTooltipFor;
   private String cachedForCustomName;
   private int cachedForClaimsColor;

   public ClaimsHighlighter(IClientClaimsManagerAPI<IClientPlayerClaimInfoAPI<IPlayerDimensionClaimsAPI<IPlayerClaimPosListAPI>>, IClientDimensionClaimsManagerAPI<IClientRegionClaimsAPI>> claimsManager) {
      super(true);
      this.claimsManager = claimsManager;
   }

   public boolean regionHasHighlights(class_5321<class_1937> dimension, int regionX, int regionZ) {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      if (!(Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.OPAC_CLAIMS)) {
         return false;
      } else {
         IClientDimensionClaimsManagerAPI<IClientRegionClaimsAPI> claimsDimension = this.claimsManager.getDimension(dimension.method_29177());
         if (claimsDimension == null) {
            return false;
         } else {
            return claimsDimension.getRegion(regionX, regionZ) != null;
         }
      }
   }

   protected int[] getColors(class_5321<class_1937> dimension, int chunkX, int chunkZ) {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      if (!(Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.OPAC_CLAIMS)) {
         return null;
      } else {
         IPlayerChunkClaimAPI currentClaim = this.claimsManager.get(dimension.method_29177(), chunkX, chunkZ);
         if (currentClaim == null) {
            return null;
         } else {
            IPlayerChunkClaimAPI topClaim = this.claimsManager.get(dimension.method_29177(), chunkX, chunkZ - 1);
            IPlayerChunkClaimAPI rightClaim = this.claimsManager.get(dimension.method_29177(), chunkX + 1, chunkZ);
            IPlayerChunkClaimAPI bottomClaim = this.claimsManager.get(dimension.method_29177(), chunkX, chunkZ + 1);
            IPlayerChunkClaimAPI leftClaim = this.claimsManager.get(dimension.method_29177(), chunkX - 1, chunkZ);
            IClientPlayerClaimInfoAPI<IPlayerDimensionClaimsAPI<IPlayerClaimPosListAPI>> claimInfo = this.claimsManager.getPlayerInfo(currentClaim.getPlayerId());
            int claimColor = this.getClaimsColor(currentClaim, claimInfo);
            int claimColorFormatted = (claimColor & 255) << 24 | (claimColor >> 8 & 255) << 16 | (claimColor >> 16 & 255) << 8;
            int fillOpacity = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.OPAC_CLAIMS_FILL_OPACITY);
            int borderOpacity = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.OPAC_CLAIMS_BORDER_OPACITY);
            int centerColor = claimColorFormatted | 255 * fillOpacity / 100;
            int sideColor = claimColorFormatted | 255 * borderOpacity / 100;
            this.resultStore[0] = centerColor;
            this.resultStore[1] = topClaim != currentClaim ? sideColor : centerColor;
            this.resultStore[2] = rightClaim != currentClaim ? sideColor : centerColor;
            this.resultStore[3] = bottomClaim != currentClaim ? sideColor : centerColor;
            this.resultStore[4] = leftClaim != currentClaim ? sideColor : centerColor;
            return this.resultStore;
         }
      }
   }

   public int calculateRegionHash(class_5321<class_1937> dimension, int regionX, int regionZ) {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      if (!(Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.OPAC_CLAIMS)) {
         return 0;
      } else {
         IClientDimensionClaimsManagerAPI<IClientRegionClaimsAPI> claimsDimension = this.claimsManager.getDimension(dimension.method_29177());
         if (claimsDimension == null) {
            return 0;
         } else {
            IClientRegionClaimsAPI claimsRegion = claimsDimension.getRegion(regionX, regionZ);
            if (claimsRegion == null) {
               return 0;
            } else {
               IClientRegionClaimsAPI topRegion = claimsDimension.getRegion(regionX, regionZ - 1);
               IClientRegionClaimsAPI rightRegion = claimsDimension.getRegion(regionX + 1, regionZ);
               IClientRegionClaimsAPI bottomRegion = claimsDimension.getRegion(regionX, regionZ + 1);
               IClientRegionClaimsAPI leftRegion = claimsDimension.getRegion(regionX - 1, regionZ);
               long accumulator = 0L;
               accumulator = accumulator * 37L + (long)(Integer)configManager.getEffective(WorldMapProfiledConfigOptions.OPAC_CLAIMS_BORDER_OPACITY);
               accumulator = accumulator * 37L + (long)(Integer)configManager.getEffective(WorldMapProfiledConfigOptions.OPAC_CLAIMS_FILL_OPACITY);

               for(int i = 0; i < 32; ++i) {
                  accumulator = this.accountClaim(accumulator, topRegion != null ? topRegion.get(i, 31) : null);
                  accumulator = this.accountClaim(accumulator, rightRegion != null ? rightRegion.get(0, i) : null);
                  accumulator = this.accountClaim(accumulator, bottomRegion != null ? bottomRegion.get(i, 0) : null);
                  accumulator = this.accountClaim(accumulator, leftRegion != null ? leftRegion.get(31, i) : null);

                  for(int j = 0; j < 32; ++j) {
                     IPlayerChunkClaimAPI claim = claimsRegion.get(i, j);
                     accumulator = this.accountClaim(accumulator, claim);
                  }
               }

               return (int)(accumulator >> 32) * 37 + (int)(accumulator & -1L);
            }
         }
      }
   }

   private long accountClaim(long accumulator, IPlayerChunkClaimAPI claim) {
      if (claim != null) {
         UUID playerId = claim.getPlayerId();
         accumulator += playerId.getLeastSignificantBits();
         accumulator *= 37L;
         accumulator += claim.getPlayerId().getMostSignificantBits();
         accumulator *= 37L;
         IClientPlayerClaimInfoAPI<IPlayerDimensionClaimsAPI<IPlayerClaimPosListAPI>> claimInfo = this.claimsManager.getPlayerInfo(playerId);
         accumulator += (long)this.getClaimsColor(claim, claimInfo);
         accumulator *= 37L;
         accumulator += claim.isForceloadable() ? 1L : 0L;
         accumulator *= 37L;
         accumulator += (long)claim.getSubConfigIndex();
      }

      accumulator *= 37L;
      return accumulator;
   }

   public boolean chunkIsHighlit(class_5321<class_1937> dimension, int chunkX, int chunkZ) {
      return this.claimsManager.get(dimension.method_29177(), chunkX, chunkZ) != null;
   }

   public class_2561 getChunkHighlightSubtleTooltip(class_5321<class_1937> dimension, int chunkX, int chunkZ) {
      IPlayerChunkClaimAPI currentClaim = this.claimsManager.get(dimension.method_29177(), chunkX, chunkZ);
      if (currentClaim == null) {
         return null;
      } else {
         UUID currentClaimId = currentClaim.getPlayerId();
         IClientPlayerClaimInfoAPI<IPlayerDimensionClaimsAPI<IPlayerClaimPosListAPI>> claimInfo = this.claimsManager.getPlayerInfo(currentClaimId);
         String customName = this.getClaimsName(currentClaim, claimInfo);
         int actualClaimsColor = this.getClaimsColor(currentClaim, claimInfo);
         int claimsColor = actualClaimsColor | -16777216;
         if (!Objects.equals(currentClaim, this.cachedTooltipFor) || this.cachedForClaimsColor != claimsColor || !Objects.equals(customName, this.cachedForCustomName)) {
            this.cachedTooltip = class_2561.method_43470("□ ").method_27694((s) -> {
               return s.method_36139(claimsColor);
            });
            if (Objects.equals(currentClaimId, PlayerConfig.SERVER_CLAIM_UUID)) {
               this.cachedTooltip.method_10855().add(class_2561.method_43469("gui.xaero_wm_pac_server_claim_tooltip", new Object[]{currentClaim.isForceloadable() ? class_2561.method_43471("gui.xaero_wm_pac_marked_for_forceload") : ""}).method_27692(class_124.field_1068));
            } else if (Objects.equals(currentClaimId, PlayerConfig.EXPIRED_CLAIM_UUID)) {
               this.cachedTooltip.method_10855().add(class_2561.method_43469("gui.xaero_wm_pac_expired_claim_tooltip", new Object[]{currentClaim.isForceloadable() ? class_2561.method_43471("gui.xaero_wm_pac_marked_for_forceload") : ""}).method_27692(class_124.field_1068));
            } else {
               this.cachedTooltip.method_10855().add(class_2561.method_43469("gui.xaero_wm_pac_claim_tooltip", new Object[]{claimInfo.getPlayerUsername(), currentClaim.isForceloadable() ? class_2561.method_43471("gui.xaero_wm_pac_marked_for_forceload") : ""}).method_27692(class_124.field_1068));
            }

            if (!customName.isEmpty()) {
               this.cachedTooltip.method_10855().add(0, class_2561.method_43470(class_1074.method_4662(customName, new Object[0]) + " - ").method_27692(class_124.field_1068));
            }

            this.cachedTooltipFor = currentClaim;
            this.cachedForCustomName = customName;
            this.cachedForClaimsColor = claimsColor;
         }

         return this.cachedTooltip;
      }
   }

   public class_2561 getChunkHighlightBluntTooltip(class_5321<class_1937> dimension, int chunkX, int chunkZ) {
      return null;
   }

   public void addMinimapBlockHighlightTooltips(List<class_2561> list, class_5321<class_1937> dimension, int blockX, int blockZ, int width) {
   }

   private String getClaimsName(IPlayerChunkClaimAPI currentClaim, IClientPlayerClaimInfoAPI<IPlayerDimensionClaimsAPI<IPlayerClaimPosListAPI>> claimInfo) {
      int subConfigIndex = currentClaim.getSubConfigIndex();
      String customName = claimInfo.getClaimsName(subConfigIndex);
      if (subConfigIndex != -1 && customName == null) {
         customName = claimInfo.getClaimsName();
      }

      return customName;
   }

   private int getClaimsColor(IPlayerChunkClaimAPI currentClaim, IClientPlayerClaimInfoAPI<IPlayerDimensionClaimsAPI<IPlayerClaimPosListAPI>> claimInfo) {
      int subConfigIndex = currentClaim.getSubConfigIndex();
      Integer actualClaimsColor = claimInfo.getClaimsColor(subConfigIndex);
      if (subConfigIndex != -1 && actualClaimsColor == null) {
         actualClaimsColor = claimInfo.getClaimsColor();
      }

      return actualClaimsColor;
   }
}
