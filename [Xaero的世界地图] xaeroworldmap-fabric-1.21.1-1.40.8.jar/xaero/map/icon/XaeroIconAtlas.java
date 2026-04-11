package xaero.map.icon;

import com.mojang.blaze3d.platform.GlStateManager;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;
import xaero.map.WorldMap;
import xaero.map.exception.OpenGLException;

public final class XaeroIconAtlas {
   private final int textureId;
   private final int width;
   private int currentIndex;
   private final int iconWidth;
   private final int sideIconCount;
   private final int maxIconCount;

   private XaeroIconAtlas(int textureId, int width, int iconWidth) {
      this.textureId = textureId;
      this.width = width;
      this.iconWidth = iconWidth;
      this.sideIconCount = width / iconWidth;
      this.maxIconCount = this.sideIconCount * this.sideIconCount;
   }

   public int getTextureId() {
      return this.textureId;
   }

   public int getWidth() {
      return this.width;
   }

   public int getCurrentIndex() {
      return this.currentIndex;
   }

   public boolean isFull() {
      return this.currentIndex >= this.maxIconCount;
   }

   public XaeroIcon createIcon() {
      if (!this.isFull()) {
         int offsetX = this.currentIndex % this.sideIconCount * this.iconWidth;
         int offsetY = this.currentIndex / this.sideIconCount * this.iconWidth;
         ++this.currentIndex;
         return new XaeroIcon(this, offsetX, offsetY);
      } else {
         return null;
      }
   }

   public static class Builder {
      private int width;
      private int preparedTexture;
      private int iconWidth;

      private Builder() {
      }

      public XaeroIconAtlas.Builder setDefault() {
         this.setIconWidth(64);
         return this;
      }

      public XaeroIconAtlas.Builder setPreparedTexture(int preparedTexture) {
         this.preparedTexture = preparedTexture;
         return this;
      }

      public XaeroIconAtlas.Builder setWidth(int width) {
         this.width = width;
         return this;
      }

      public XaeroIconAtlas.Builder setIconWidth(int iconWidth) {
         this.iconWidth = iconWidth;
         return this;
      }

      private int createGlTexture(int actualWidth) {
         int texture = GlStateManager._genTexture();
         OpenGLException.checkGLError();
         if (texture == 0) {
            return 0;
         } else {
            GlStateManager._bindTexture(texture);
            GL11.glTexParameteri(3553, 33085, 0);
            GL11.glTexParameterf(3553, 33082, 0.0F);
            GL11.glTexParameterf(3553, 33083, 0.0F);
            GL11.glTexParameterf(3553, 34049, 0.0F);
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            GlStateManager._texImage2D(3553, 0, 32856, actualWidth, actualWidth, 0, 32993, 32821, (IntBuffer)null);
            GlStateManager._bindTexture(0);
            OpenGLException.checkGLError();
            return texture;
         }
      }

      public XaeroIconAtlas build() {
         if (this.width != 0 && this.iconWidth > 0) {
            if (this.width / this.iconWidth * this.iconWidth != this.width) {
               throw new IllegalArgumentException();
            } else {
               int texture = this.preparedTexture == 0 ? this.createGlTexture(this.width) : this.preparedTexture;
               if (texture == 0) {
                  WorldMap.LOGGER.error("Failed to create a GL texture for a new xaero icon atlas!");
                  return null;
               } else {
                  return new XaeroIconAtlas(texture, this.width, this.iconWidth);
               }
            }
         } else {
            throw new IllegalStateException();
         }
      }

      public static XaeroIconAtlas.Builder begin() {
         return (new XaeroIconAtlas.Builder()).setDefault();
      }
   }
}
