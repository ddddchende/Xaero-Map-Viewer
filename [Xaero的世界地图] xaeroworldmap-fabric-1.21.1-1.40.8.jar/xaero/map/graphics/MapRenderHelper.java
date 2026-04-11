package xaero.map.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_2561;
import net.minecraft.class_277;
import net.minecraft.class_285;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_9801;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;
import xaero.lib.client.graphics.shader.LibShaders;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;

public class MapRenderHelper {
   private static class_277 defaultShaderDisabledBlendState = new class_277();
   private static class_277 defaultShaderBlendState = new class_277(770, 771, 32774);

   public static void renderBranchUpdate(float x, float y, float width, float height, int textureX, int textureY, float textureW, float textureH, float fullTextureWidth, float fullTextureHeight, boolean first) {
      RenderSystem.setShader(() -> {
         return LibShaders.WORLD_MAP_BRANCH;
      });
      class_289 tessellator = class_289.method_1348();
      class_287 vertexBuffer = tessellator.method_60827(class_5596.field_27382, class_290.field_1585);
      float normalizedTextureX = (float)textureX / fullTextureWidth;
      float normalizedTextureY = (float)textureY / fullTextureHeight;
      float normalizedTextureX2 = ((float)textureX + textureW) / fullTextureWidth;
      float normalizedTextureY2 = ((float)textureY + textureH) / fullTextureHeight;
      vertexBuffer.method_22912(x + 0.0F, y + height, 0.0F).method_22913(normalizedTextureX, normalizedTextureY2);
      vertexBuffer.method_22912(x + width, y + height, 0.0F).method_22913(normalizedTextureX2, normalizedTextureY2);
      vertexBuffer.method_22912(x + width, y + 0.0F, 0.0F).method_22913(normalizedTextureX2, normalizedTextureY);
      vertexBuffer.method_22912(x + 0.0F, y + 0.0F, 0.0F).method_22913(normalizedTextureX, normalizedTextureY);
      class_9801 buffer = vertexBuffer.method_60794();
      if (first) {
         class_286.method_43433(buffer);
         class_285.method_22094(LibShaders.WORLD_MAP_BRANCH.method_1270());
      } else {
         class_286.method_43437(buffer);
      }

   }

   public static void fillIntoExistingBuffer(Matrix4f matrix, class_4588 bufferBuilder, int x1, int y1, int x2, int y2, float r, float g, float b, float a) {
      bufferBuilder.method_22918(matrix, (float)x1, (float)y2, 0.0F).method_22915(r, g, b, a);
      bufferBuilder.method_22918(matrix, (float)x2, (float)y2, 0.0F).method_22915(r, g, b, a);
      bufferBuilder.method_22918(matrix, (float)x2, (float)y1, 0.0F).method_22915(r, g, b, a);
      bufferBuilder.method_22918(matrix, (float)x1, (float)y1, 0.0F).method_22915(r, g, b, a);
   }

   public static void blitIntoExistingBuffer(Matrix4f matrix, class_4588 bufferBuilder, float x, float y, int u, int v, int width, int height, int tW, int tH, float r, float g, float b, float a, int textureWidth, int textureHeight) {
      float factorX = 1.0F / (float)textureWidth;
      float factorY = 1.0F / (float)textureHeight;
      float textureX1 = (float)u * factorX;
      float textureX2 = (float)(u + tW) * factorX;
      float textureY1 = (float)v * factorY;
      float textureY2 = (float)(v + tH) * factorY;
      bufferBuilder.method_22918(matrix, x, y + (float)height, 0.0F).method_22915(r, g, b, a).method_22913(textureX1, textureY2);
      bufferBuilder.method_22918(matrix, x + (float)width, y + (float)height, 0.0F).method_22915(r, g, b, a).method_22913(textureX2, textureY2);
      bufferBuilder.method_22918(matrix, x + (float)width, y, 0.0F).method_22915(r, g, b, a).method_22913(textureX2, textureY1);
      bufferBuilder.method_22918(matrix, x, y, 0.0F).method_22915(r, g, b, a).method_22913(textureX1, textureY1);
   }

   public static void blitIntoExistingBuffer(Matrix4f matrix, class_4588 bufferBuilder, int x, int y, int u, int v, int width, int height, float r, float g, float b, float a) {
      blitIntoExistingBuffer(matrix, bufferBuilder, (float)x, (float)y, u, v, width, height, width, height, r, g, b, a, 256, 256);
   }

   public static void blitIntoMultiTextureRenderer(Matrix4f matrix, MultiTextureRenderTypeRenderer renderer, float x, float y, int u, int v, int width, int height, int tW, int tH, float r, float g, float b, float a, int textureWidth, int textureHeight, int texture) {
      class_4588 bufferBuilder = renderer.begin(texture);
      blitIntoExistingBuffer(matrix, bufferBuilder, x, y, u, v, width, height, tW, tH, r, g, b, a, textureWidth, textureHeight);
   }

   public static void blitIntoMultiTextureRenderer(Matrix4f matrix, MultiTextureRenderTypeRenderer renderer, float x, float y, int u, int v, int width, int height, float r, float g, float b, float a, int textureWidth, int textureHeight, int texture) {
      blitIntoMultiTextureRenderer(matrix, renderer, x, y, u, v, width, height, width, height, r, g, b, a, textureWidth, textureHeight, texture);
   }

   public static void blitIntoMultiTextureRenderer(Matrix4f matrix, MultiTextureRenderTypeRenderer renderer, float x, float y, int u, int v, int width, int height, float r, float g, float b, float a, int texture) {
      blitIntoMultiTextureRenderer(matrix, renderer, x, y, u, v, width, height, r, g, b, a, 256, 256, texture);
   }

   public static void renderDynamicHighlight(class_4587 matrixStack, class_4588 overlayBuffer, int flooredCameraX, int flooredCameraZ, int leftX, int rightX, int topZ, int bottomZ, float sideR, float sideG, float sideB, float sideA, float centerR, float centerG, float centerB, float centerA) {
      fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), overlayBuffer, leftX - 1 - flooredCameraX, topZ - 1 - flooredCameraZ, leftX - flooredCameraX, bottomZ + 1 - flooredCameraZ, sideR, sideG, sideB, sideA);
      fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), overlayBuffer, leftX - flooredCameraX, topZ - 1 - flooredCameraZ, rightX - flooredCameraX, topZ - flooredCameraZ, sideR, sideG, sideB, sideA);
      fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), overlayBuffer, rightX - flooredCameraX, topZ - 1 - flooredCameraZ, rightX + 1 - flooredCameraX, bottomZ + 1 - flooredCameraZ, sideR, sideG, sideB, sideA);
      fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), overlayBuffer, leftX - flooredCameraX, bottomZ - flooredCameraZ, rightX - flooredCameraX, bottomZ + 1 - flooredCameraZ, sideR, sideG, sideB, sideA);
      fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), overlayBuffer, leftX - flooredCameraX, topZ - flooredCameraZ, rightX - flooredCameraX, bottomZ - flooredCameraZ, centerR, centerG, centerB, centerA);
   }

   public static void drawCenteredStringWithBackground(class_332 guiGraphics, class_327 font, String string, int x, int y, int color, float bgRed, float bgGreen, float bgBlue, float bgAlpha, class_4588 backgroundVertexBuffer) {
      int stringWidth = font.method_1727(string);
      drawStringWithBackground(guiGraphics, font, string, x - stringWidth / 2, y, color, bgRed, bgGreen, bgBlue, bgAlpha, backgroundVertexBuffer);
   }

   public static void drawStringWithBackground(class_332 guiGraphics, class_327 font, String string, int x, int y, int color, float bgRed, float bgGreen, float bgBlue, float bgAlpha, class_4588 backgroundVertexBuffer) {
      class_4587 matrixStack = guiGraphics.method_51448();
      int stringWidth = font.method_1727(string);
      matrixStack.method_46416(0.0F, 0.0F, -1.0F);
      fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), backgroundVertexBuffer, x - 1, y - 1, x + stringWidth + 1, y + 9, bgRed, bgGreen, bgBlue, bgAlpha);
      matrixStack.method_46416(0.0F, 0.0F, 1.0F);
      guiGraphics.method_25303(font, string, x, y, color);
   }

   public static void drawCenteredStringWithBackground(class_332 guiGraphics, class_327 font, class_2561 text, int x, int y, int color, float bgRed, float bgGreen, float bgBlue, float bgAlpha, class_4588 backgroundVertexBuffer) {
      int stringWidth = font.method_27525(text);
      drawStringWithBackground(guiGraphics, font, text, x - stringWidth / 2, y, color, bgRed, bgGreen, bgBlue, bgAlpha, backgroundVertexBuffer);
   }

   public static void drawStringWithBackground(class_332 guiGraphics, class_327 font, class_2561 text, int x, int y, int color, float bgRed, float bgGreen, float bgBlue, float bgAlpha, class_4588 backgroundVertexBuffer) {
      class_4587 matrixStack = guiGraphics.method_51448();
      int stringWidth = font.method_27525(text);
      matrixStack.method_46416(0.0F, 0.0F, -1.0F);
      fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), backgroundVertexBuffer, x - 1, y - 1, x + stringWidth + 1, y + 9, bgRed, bgGreen, bgBlue, bgAlpha);
      matrixStack.method_46416(0.0F, 0.0F, 1.0F);
      guiGraphics.method_27535(font, text, x, y, color);
   }

   public static void blitIntoExistingBuffer(Matrix4f matrix, class_4588 bufferBuilder, float x, float y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
      float factorX = 1.0F / (float)textureWidth;
      float factorY = 1.0F / (float)textureHeight;
      float textureX1 = (float)u * factorX;
      float textureX2 = (float)(u + width) * factorX;
      float textureY1 = (float)v * factorY;
      float textureY2 = (float)(v + height) * factorY;
      bufferBuilder.method_22918(matrix, x, y + (float)height, 0.0F).method_22913(textureX1, textureY2);
      bufferBuilder.method_22918(matrix, x + (float)width, y + (float)height, 0.0F).method_22913(textureX2, textureY2);
      bufferBuilder.method_22918(matrix, x + (float)width, y, 0.0F).method_22913(textureX2, textureY1);
      bufferBuilder.method_22918(matrix, x, y, 0.0F).method_22913(textureX1, textureY1);
   }

   public static void blitIntoExistingBuffer(Matrix4f matrix, class_4588 bufferBuilder, int x, int y, int u, int v, int width, int height) {
      blitIntoExistingBuffer(matrix, bufferBuilder, (float)x, (float)y, u, v, width, height, 256, 256);
   }

   public static void blitIntoMultiTextureRenderer(Matrix4f matrix, MultiTextureRenderTypeRenderer renderer, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight, int texture) {
      class_4588 bufferBuilder = renderer.begin(texture);
      blitIntoExistingBuffer(matrix, bufferBuilder, (float)x, (float)y, u, v, width, height, textureWidth, textureHeight);
   }

   public static void blitIntoMultiTextureRenderer(Matrix4f matrix, MultiTextureRenderTypeRenderer renderer, int x, int y, int u, int v, int width, int height, int texture) {
      blitIntoMultiTextureRenderer(matrix, renderer, x, y, u, v, width, height, 256, 256, texture);
   }

   public static void restoreDefaultShaderBlendState() {
      defaultShaderDisabledBlendState.method_1244();
      defaultShaderBlendState.method_1244();
      RenderSystem.defaultBlendFunc();
   }
}
