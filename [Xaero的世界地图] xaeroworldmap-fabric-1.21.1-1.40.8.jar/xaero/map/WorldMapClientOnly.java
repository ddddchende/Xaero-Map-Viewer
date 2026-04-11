package xaero.map;

import xaero.map.graphics.CustomVertexConsumers;
import xaero.map.region.texture.BranchTextureRenderer;

public class WorldMapClientOnly {
   public BranchTextureRenderer branchTextureRenderer;
   public CustomVertexConsumers customVertexConsumers;

   public void preInit(String modId) {
   }

   public void postInit() {
      this.branchTextureRenderer = new BranchTextureRenderer();
      this.customVertexConsumers = new CustomVertexConsumers();
   }
}
