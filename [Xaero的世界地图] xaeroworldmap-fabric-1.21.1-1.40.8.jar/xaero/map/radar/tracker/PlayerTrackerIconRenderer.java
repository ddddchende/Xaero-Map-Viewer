package xaero.map.radar.tracker;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_1657;
import net.minecraft.class_1664;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_922;
import net.minecraft.class_293.class_5596;
import xaero.lib.client.graphics.XaeroRenderType;
import xaero.lib.client.graphics.shader.LibShaders;
import xaero.map.graphics.MapRenderHelper;

public class PlayerTrackerIconRenderer {
   public void renderIcon(class_332 guiGraphics, class_1657 player, class_2960 skinTextureLocation) {
      class_4587 matrixStack = guiGraphics.method_51448();
      boolean upsideDown = player != null && class_922.method_38563(player);
      int textureY = 8 + (upsideDown ? 8 : 0);
      int textureH = 8 * (upsideDown ? -1 : 1);
      RenderSystem.setShaderTexture(0, skinTextureLocation);
      RenderSystem.setShader(() -> {
         return LibShaders.POSITION_COLOR_TEX;
      });
      class_287 bufferbuilder = class_289.method_1348().method_60827(class_5596.field_27382, XaeroRenderType.POSITION_COLOR_TEX);
      MapRenderHelper.blitIntoExistingBuffer(matrixStack.method_23760().method_23761(), bufferbuilder, -4.0F, -4.0F, 8, textureY, 8, 8, 8, textureH, 1.0F, 1.0F, 1.0F, 1.0F, 64, 64);
      if (player != null && player.method_7348(class_1664.field_7563)) {
         textureY = 8 + (upsideDown ? 8 : 0);
         textureH = 8 * (upsideDown ? -1 : 1);
         MapRenderHelper.blitIntoExistingBuffer(matrixStack.method_23760().method_23761(), bufferbuilder, -4.0F, -4.0F, 40, textureY, 8, 8, 8, textureH, 1.0F, 1.0F, 1.0F, 1.0F, 64, 64);
      }

      class_286.method_43433(bufferbuilder.method_60794());
   }
}
