package xaero.map.graphics.renderer.multitexture;

import com.mojang.blaze3d.platform.GlStateManager;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.function.IntConsumer;
import net.minecraft.class_1921;

public class MultiTextureRenderTypeRendererProvider {
   private Deque<MultiTextureRenderTypeRenderer> availableRenderers = new ArrayDeque();
   private HashSet<MultiTextureRenderTypeRenderer> usedRenderers;

   public MultiTextureRenderTypeRendererProvider(int rendererCount) {
      for(int i = 0; i < rendererCount; ++i) {
         this.availableRenderers.add(new MultiTextureRenderTypeRenderer());
      }

      this.usedRenderers = new HashSet();
   }

   public MultiTextureRenderTypeRenderer getRenderer(IntConsumer textureBinderShader, IntConsumer textureBinder, class_1921 renderType) {
      return this.getRenderer(textureBinderShader, textureBinder, (Runnable)null, renderType);
   }

   public MultiTextureRenderTypeRenderer getRenderer(IntConsumer textureBinderShader, IntConsumer textureBinder, Runnable textureFinalizer, class_1921 renderType) {
      if (this.availableRenderers.isEmpty()) {
         throw new RuntimeException("No renderers available!");
      } else {
         MultiTextureRenderTypeRenderer renderer = (MultiTextureRenderTypeRenderer)this.availableRenderers.removeFirst();
         renderer.init(textureBinderShader, textureBinder, textureFinalizer, renderType);
         this.usedRenderers.add(renderer);
         return renderer;
      }
   }

   public void draw(MultiTextureRenderTypeRenderer renderer) {
      if (this.usedRenderers.remove(renderer)) {
         renderer.draw();
         this.availableRenderers.add(renderer);
      } else {
         throw new RuntimeException("The renderer requested for drawing was not provided by this provider!");
      }
   }

   public static void defaultTextureBind(int texture) {
      GlStateManager._activeTexture(33984);
      GlStateManager._bindTexture(texture);
   }
}
