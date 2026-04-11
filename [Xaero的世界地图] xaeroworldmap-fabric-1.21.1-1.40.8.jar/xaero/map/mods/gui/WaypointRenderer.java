package xaero.map.mods.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_1060;
import net.minecraft.class_243;
import net.minecraft.class_276;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_4597.class_4598;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.animation.SlowingAnimation;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.element.MapElementRenderer;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.GuiMap;
import xaero.map.icon.XaeroIcon;
import xaero.map.misc.Misc;
import xaero.map.mods.SupportXaeroMinimap;
import xaero.map.world.MapDimension;

public final class WaypointRenderer extends MapElementRenderer<Waypoint, WaypointRenderContext, WaypointRenderer> {
   private final SupportXaeroMinimap minimap;
   private final WaypointSymbolCreator symbolCreator;
   private ElementRenderInfo compatibleRenderInfo;

   private WaypointRenderer(WaypointRenderContext context, WaypointRenderProvider provider, WaypointReader reader, SupportXaeroMinimap minimap, WaypointSymbolCreator symbolCreator) {
      super(context, provider, reader);
      this.minimap = minimap;
      this.symbolCreator = symbolCreator;
   }

   public WaypointSymbolCreator getSymbolCreator() {
      return this.symbolCreator;
   }

   public void renderElementShadow(Waypoint w, boolean hovered, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, class_332 guiGraphics, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider) {
      class_4587 matrixStack = guiGraphics.method_51448();
      matrixStack.method_22904(partialX, partialY, 0.0D);
      matrixStack.method_22905(optionalScale * ((WaypointRenderContext)this.context).worldmapWaypointsScale, optionalScale * ((WaypointRenderContext)this.context).worldmapWaypointsScale, 1.0F);
      float visibilityAlpha = w.isDisabled() ? 0.3F : 1.0F;
      matrixStack.method_46416(-14.0F, -41.0F, 0.0F);
      MapRenderHelper.blitIntoExistingBuffer(matrixStack.method_23760().method_23761(), ((WaypointRenderContext)this.context).regularUIObjectConsumer, 0, 19, 0, 117, 41, 22, 0.0F, 0.0F, 0.0F, renderInfo.brightness * visibilityAlpha / ((WaypointRenderContext)this.context).worldmapWaypointsScale);
   }

   public boolean shouldRender(int location, boolean shadow) {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      boolean waypointBackgroundsConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.WAYPOINT_BACKGROUNDS);
      boolean renderWaypoints = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.RENDER_WAYPOINTS);
      return renderWaypoints && (!shadow || waypointBackgroundsConfig);
   }

   public boolean renderElement(Waypoint w, boolean hovered, double optionalDepth, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, class_332 guiGraphics, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider) {
      class_4587 matrixStack = guiGraphics.method_51448();
      boolean renderBackground = hovered || ((WaypointRenderContext)this.context).waypointBackgrounds;
      matrixStack.method_22904(partialX, partialY, 0.0D);
      matrixStack.method_22905(optionalScale * ((WaypointRenderContext)this.context).worldmapWaypointsScale, optionalScale * ((WaypointRenderContext)this.context).worldmapWaypointsScale, 1.0F);
      matrixStack.method_22903();
      float visibilityAlpha = w.isDisabled() ? 0.3F : 1.0F;
      int color = w.getColor();
      String symbol = w.getSymbol();
      int type = w.getType();
      float red = (float)(color >> 16 & 255) / 255.0F;
      float green = (float)(color >> 8 & 255) / 255.0F;
      float blue = (float)(color & 255) / 255.0F;
      int flagU = 35;
      int flagV = 34;
      int flagW = 30;
      int flagH = 43;
      if (symbol.length() > 1) {
         flagU += 35;
         flagW += 13;
      }

      if (w.isTemporary()) {
         flagU += 83;
      }

      matrixStack.method_46416((float)(-flagW) / 2.0F, (float)(-flagH + 1), 0.0F);
      if (renderBackground) {
         class_1060 textureManager = class_310.method_1551().method_1531();
         MapRenderHelper.blitIntoMultiTextureRenderer(matrixStack.method_23760().method_23761(), ((WaypointRenderContext)this.context).uniqueTextureUIObjectRenderer, 0.0F, 0.0F, flagU, flagV, flagW, flagH, red * visibilityAlpha, green * visibilityAlpha, blue * visibilityAlpha, visibilityAlpha, textureManager.method_4619(WorldMap.guiTextures).method_4624());
      }

      matrixStack.method_22909();
      float oldDestAlpha = w.getDestAlpha();
      if (hovered) {
         w.setDestAlpha(255.0F);
      } else {
         w.setDestAlpha(0.0F);
      }

      if (oldDestAlpha != w.getDestAlpha()) {
         w.setAlphaAnim(new SlowingAnimation((double)w.getAlpha(), (double)w.getDestAlpha(), 0.8D, 1.0D));
      }

      if (w.getAlphaAnim() != null) {
         w.setAlpha((float)w.getAlphaAnim().getCurrent());
      }

      float alpha = w.getAlpha();
      XaeroIcon symbolIcon = null;
      int symbolVerticalOffset = 0;
      int symbolWidth = 0;
      class_327 fontRenderer = class_310.method_1551().field_1772;
      int stringWidth = fontRenderer.method_1727(symbol);
      int symbolFrameWidth = stringWidth / 2 > 4 ? 62 : 32;
      if (type != 1 && alpha < 200.0F) {
         symbolVerticalOffset = 5;
         symbolWidth = (stringWidth - 1) * 3;
         symbolIcon = this.symbolCreator.getSymbolTexture(guiGraphics, symbol);
      } else if (type == 1) {
         symbolVerticalOffset = 3;
         symbolWidth = 27;
         symbolIcon = this.symbolCreator.getDeathSymbolTexture(guiGraphics);
      }

      if (symbolIcon != null) {
         matrixStack.method_22903();
         matrixStack.method_46416(-1.0F - (float)symbolWidth / 2.0F, (float)(62 + (renderBackground ? -43 + symbolVerticalOffset - 1 : -12)), 0.0F);
         matrixStack.method_22905(1.0F, -1.0F, 1.0F);
         MapRenderHelper.blitIntoMultiTextureRenderer(matrixStack.method_23760().method_23761(), ((WaypointRenderContext)this.context).uniqueTextureUIObjectRenderer, 0.0F, 0.0F, symbolIcon.getOffsetX() + 1, symbolIcon.getOffsetY() + 1, symbolFrameWidth, 62, visibilityAlpha, visibilityAlpha, visibilityAlpha, visibilityAlpha, symbolIcon.getTextureAtlas().getWidth(), symbolIcon.getTextureAtlas().getWidth(), symbolIcon.getTextureAtlas().getTextureId());
         matrixStack.method_22909();
      }

      if ((int)alpha > 0) {
         int tc = (int)alpha << 24 | 16777215;
         String name = w.getName();
         int len = fontRenderer.method_1727(name);
         matrixStack.method_46416(0.0F, (float)(renderBackground ? -38 : -11), 0.0F);
         matrixStack.method_22905(3.0F, 3.0F, 1.0F);
         int bgLen = Math.max(len + 2, 10);
         MapRenderHelper.fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), ((WaypointRenderContext)this.context).textBGConsumer, -bgLen / 2, -1, bgLen / 2, 9, red, green, blue, alpha / 255.0F);
         MapRenderHelper.fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), ((WaypointRenderContext)this.context).textBGConsumer, -bgLen / 2, -1, bgLen / 2, 8, 0.0F, 0.0F, 0.0F, alpha / 255.0F * 200.0F / 255.0F);
         if ((int)alpha > 3) {
            matrixStack.method_46416(0.0F, 0.0F, 1.0F);
            Misc.drawNormalText(matrixStack, name, (float)(-(len - 1)) / 2.0F, 0.0F, tc, false, vanillaBufferSource);
         }
      }

      return false;
   }

   public void preRender(ElementRenderInfo renderInfo, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
      class_4598 renderTypeBuffers = WorldMap.worldMapClientOnly.customVertexConsumers.getRenderTypeBuffers();
      ((WaypointRenderContext)this.context).regularUIObjectConsumer = renderTypeBuffers.getBuffer(CustomRenderTypes.GUI_BILINEAR);
      ((WaypointRenderContext)this.context).textBGConsumer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_ELEMENT_TEXT_BG);
      ((WaypointRenderContext)this.context).uniqueTextureUIObjectRenderer = rendererProvider.getRenderer((t) -> {
         RenderSystem.setShaderTexture(0, t);
      }, MultiTextureRenderTypeRendererProvider::defaultTextureBind, CustomRenderTypes.GUI_BILINEAR_PREMULTIPLIED);
      ((WaypointRenderContext)this.context).deathpoints = this.minimap.getDeathpoints();
      class_310 mc = class_310.method_1551();
      ((WaypointRenderContext)this.context).userScale = mc.field_1755 != null && mc.field_1755 instanceof GuiMap ? ((GuiMap)mc.field_1755).getUserScale() : 1.0D;
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      ((WaypointRenderContext)this.context).waypointBackgrounds = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.WAYPOINT_BACKGROUNDS);
      ((WaypointRenderContext)this.context).minZoomForLocalWaypoints = (Double)configManager.getEffective(WorldMapProfiledConfigOptions.MIN_ZOOM_LOCAL_WAYPOINTS);
      SingleConfigManager<Config> primaryConfigManager = configManager.getPrimaryConfigManager();
      ((WaypointRenderContext)this.context).showDisabledWaypoints = (Boolean)primaryConfigManager.getEffective(WorldMapPrimaryClientConfigOptions.DISPLAY_DISABLED_WAYPOINTS);
   }

   public void postRender(ElementRenderInfo renderInfo, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
      class_4598 renderTypeBuffers = WorldMap.worldMapClientOnly.customVertexConsumers.getRenderTypeBuffers();
      rendererProvider.draw(((WaypointRenderContext)this.context).uniqueTextureUIObjectRenderer);
      renderTypeBuffers.method_22993();
   }

   /** @deprecated */
   @Deprecated
   public void beforeRender(int location, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre) {
      this.preRender((ElementRenderInfo)null, vanillaBufferSource, rendererProvider, pre);
   }

   /** @deprecated */
   @Deprecated
   public void afterRender(int location, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre) {
      this.postRender((ElementRenderInfo)null, vanillaBufferSource, rendererProvider, pre);
      this.compatibleRenderInfo = null;
   }

   /** @deprecated */
   @Deprecated
   public void renderElementPre(int location, Waypoint element, boolean hovered, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, float optionalScale, double partialX, double partialY, boolean cave, float partialTicks) {
      if (this.compatibleRenderInfo == null) {
         MapProcessor mapProcessor = WorldMapSession.getCurrentSession().getMapProcessor();
         MapDimension mapDimension = mapProcessor.getMapWorld().getCurrentDimension();
         double mapDimScale = mapDimension.calculateDimScale(mapProcessor.getWorldDimensionTypeRegistry());
         this.compatibleRenderInfo = new ElementRenderInfo(ElementRenderLocation.fromIndex(location), mc.method_1560(), mc.field_1724, new class_243(cameraX, -1.0D, cameraZ), mouseX, mouseZ, scale, cave, partialTicks, brightness, screenSizeBasedScale, (class_276)null, mapDimScale, mapDimension.getDimId());
      }

      this.renderElementShadow(element, hovered, optionalScale, partialX, partialY, this.compatibleRenderInfo, guiGraphics, vanillaBufferSource, rendererProvider);
   }

   /** @deprecated */
   @Deprecated
   public boolean renderElement(int location, Waypoint element, boolean hovered, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, int elementIndex, double optionalDepth, float optionalScale, double partialX, double partialY, boolean cave, float partialTicks) {
      return this.renderElement((Waypoint)element, hovered, optionalDepth, optionalScale, partialX, partialY, (ElementRenderInfo)null, guiGraphics, vanillaBufferSource, rendererProvider);
   }

   public int getOrder() {
      return 200;
   }

   public boolean shouldBeDimScaled() {
      return false;
   }

   public static final class Builder {
      private SupportXaeroMinimap minimap;
      private WaypointSymbolCreator symbolCreator;

      private Builder() {
      }

      private WaypointRenderer.Builder setDefault() {
         this.setMinimap((SupportXaeroMinimap)null);
         this.setSymbolCreator((WaypointSymbolCreator)null);
         return this;
      }

      public WaypointRenderer.Builder setMinimap(SupportXaeroMinimap minimap) {
         this.minimap = minimap;
         return this;
      }

      public WaypointRenderer.Builder setSymbolCreator(WaypointSymbolCreator symbolCreator) {
         this.symbolCreator = symbolCreator;
         return this;
      }

      public WaypointRenderer build() {
         if (this.minimap != null && this.symbolCreator != null) {
            return new WaypointRenderer(new WaypointRenderContext(), new WaypointRenderProvider(this.minimap), new WaypointReader(), this.minimap, this.symbolCreator);
         } else {
            throw new IllegalStateException();
         }
      }

      public static WaypointRenderer.Builder begin() {
         return (new WaypointRenderer.Builder()).setDefault();
      }
   }
}
