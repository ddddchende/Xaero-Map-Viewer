package xaero.map.icon;

import com.mojang.blaze3d.platform.GlStateManager;
import java.util.Iterator;
import java.util.List;

public class XaeroIconAtlasManager {
   private final int iconWidth;
   private final int atlasTextureSize;
   private final List<XaeroIconAtlas> atlases;
   private int currentAtlasIndex;

   public XaeroIconAtlasManager(int iconWidth, int atlasTextureSize, List<XaeroIconAtlas> atlases) {
      this.iconWidth = iconWidth;
      this.atlasTextureSize = atlasTextureSize;
      this.atlases = atlases;
      this.currentAtlasIndex = -1;
   }

   public void clearAtlases() {
      Iterator var1 = this.atlases.iterator();

      while(var1.hasNext()) {
         XaeroIconAtlas entityIconAtlas = (XaeroIconAtlas)var1.next();
         GlStateManager._deleteTexture(entityIconAtlas.getTextureId());
      }

      this.currentAtlasIndex = -1;
      this.atlases.clear();
   }

   public XaeroIconAtlas getCurrentAtlas() {
      if (this.currentAtlasIndex < 0 || ((XaeroIconAtlas)this.atlases.get(this.currentAtlasIndex)).isFull()) {
         this.atlases.add(XaeroIconAtlas.Builder.begin().setWidth(this.atlasTextureSize).setIconWidth(this.iconWidth).build());
         this.currentAtlasIndex = this.atlases.size() - 1;
      }

      return (XaeroIconAtlas)this.atlases.get(this.currentAtlasIndex);
   }
}
