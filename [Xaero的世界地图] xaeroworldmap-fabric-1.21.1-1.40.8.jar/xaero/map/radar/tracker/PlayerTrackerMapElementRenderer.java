package xaero.map.radar.tracker;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_1060;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_640;
import net.minecraft.class_4597.class_4598;
import xaero.lib.client.config.ClientConfigManager;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.animation.SlowingAnimation;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.element.MapElementRenderer;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.icon.XaeroIcon;
import xaero.map.icon.XaeroIconAtlas;

public final class PlayerTrackerMapElementRenderer extends MapElementRenderer<PlayerTrackerMapElement<?>, PlayerTrackerMapElementRenderContext, PlayerTrackerMapElementRenderer> {
   private final PlayerTrackerMapElementCollector elementCollector;
   private TrackedPlayerIconManager trackedPlayerIconManager;

   private PlayerTrackerMapElementRenderer(PlayerTrackerMapElementCollector elementCollector, PlayerTrackerMapElementRenderContext context, PlayerTrackerMapElementRenderProvider<PlayerTrackerMapElementRenderContext> provider, PlayerTrackerMapElementReader reader) {
      super(context, provider, reader);
      this.elementCollector = elementCollector;
   }

   public TrackedPlayerIconManager getTrackedPlayerIconManager() {
      return this.trackedPlayerIconManager;
   }

   public void preRender(ElementRenderInfo renderInfo, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
      class_310 mc = class_310.method_1551();
      WorldMapSession mapSession = WorldMapSession.getCurrentSession();
      MapProcessor mapProcessor = mapSession.getMapProcessor();
      class_4598 renderTypeBuffers = WorldMap.worldMapClientOnly.customVertexConsumers.getRenderTypeBuffers();
      ((PlayerTrackerMapElementRenderContext)this.context).textBGConsumer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_ELEMENT_TEXT_BG);
      ((PlayerTrackerMapElementRenderContext)this.context).uniqueTextureUIObjectRenderer = rendererProvider.getRenderer((t) -> {
         RenderSystem.setShaderTexture(0, t);
      }, MultiTextureRenderTypeRendererProvider::defaultTextureBind, CustomRenderTypes.GUI_BILINEAR_PREMULTIPLIED);
      ((PlayerTrackerMapElementRenderContext)this.context).mapDimId = mapProcessor.getMapWorld().getCurrentDimensionId();
      ((PlayerTrackerMapElementRenderContext)this.context).mapDimDiv = mapProcessor.getMapWorld().getCurrentDimension().calculateDimDiv(mapProcessor.getWorldDimensionTypeRegistry(), mc.field_1687.method_8597());
   }

   public void postRender(ElementRenderInfo renderInfo, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
      class_4598 renderTypeBuffers = WorldMap.worldMapClientOnly.customVertexConsumers.getRenderTypeBuffers();
      rendererProvider.draw(((PlayerTrackerMapElementRenderContext)this.context).uniqueTextureUIObjectRenderer);
      renderTypeBuffers.method_22993();
      if (!shadow) {
         this.elementCollector.resetRenderedOnRadarFlags();
      }

   }

   public void renderElementShadow(PlayerTrackerMapElement<?> element, boolean hovered, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, class_332 guiGraphics, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider) {
   }

   public boolean renderElement(PlayerTrackerMapElement<?> e, boolean hovered, double optionalDepth, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, class_332 guiGraphics, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider) {
      class_4587 matrixStack = guiGraphics.method_51448();
      class_640 info = class_310.method_1551().method_1562().method_2871(e.getPlayerId());
      if (info != null) {
         class_310 mc = class_310.method_1551();
         class_327 fontRenderer = mc.field_1772;
         class_1657 clientPlayer = mc.field_1687.method_18470(e.getPlayerId());
         matrixStack.method_22903();
         double fadeDest = hovered ? 1.0D : 0.0D;
         boolean firstTime = e.getFadeAnim() == null;
         if (firstTime || e.getFadeAnim().getDestination() != fadeDest) {
            e.setFadeAnim(new SlowingAnimation(e.getFadeAnim() == null ? 0.0D : e.getFadeAnim().getCurrent(), fadeDest, 0.8D, 0.001D));
         }

         float alpha = (float)e.getFadeAnim().getCurrent();
         if (!e.wasRenderedOnRadar() || alpha > 0.0F) {
            if (alpha > 0.0F) {
               matrixStack.method_22903();
               matrixStack.method_22905(2.0F, 2.0F, 1.0F);
               String name = info.method_2966().getName();
               int nameWidth = fontRenderer.method_1727(name);
               MapRenderHelper.fillIntoExistingBuffer(matrixStack.method_23760().method_23761(), ((PlayerTrackerMapElementRenderContext)this.context).textBGConsumer, -8 - nameWidth - 2, -6, -7, 6, 0.0F, 0.0F, 0.0F, alpha * 119.0F / 255.0F);
               int textAlphaComponent = (int)(alpha * 255.0F);
               if (textAlphaComponent > 3) {
                  int tc = 16777215 | textAlphaComponent << 24;
                  guiGraphics.method_25303(fontRenderer, name, -8 - nameWidth, -4, tc);
               }

               matrixStack.method_22909();
            }

            matrixStack.method_22904(partialX, partialY, 0.0D);
            matrixStack.method_22905((2.0F + alpha) / 3.0F, (2.0F + alpha) / 3.0F, 1.0F);
            XaeroIcon icon = this.getTrackedPlayerIconManager().getIcon(guiGraphics, clientPlayer, info, e);
            XaeroIconAtlas atlas = icon.getTextureAtlas();
            MapRenderHelper.blitIntoMultiTextureRenderer(matrixStack.method_23760().method_23761(), ((PlayerTrackerMapElementRenderContext)this.context).uniqueTextureUIObjectRenderer, -15.0F, -15.0F, icon.getOffsetX() + 1, icon.getOffsetY() + 31, 30, 30, 30, -30, 1.0F, 1.0F, 1.0F, 1.0F, atlas.getWidth(), atlas.getWidth(), atlas.getTextureId());
         }

         matrixStack.method_22909();
      }

      return false;
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
   }

   /** @deprecated */
   @Deprecated
   public void renderElementPre(int location, PlayerTrackerMapElement<?> element, boolean hovered, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, float optionalScale, double partialX, double partialY, boolean cave, float partialTicks) {
      this.renderElementShadow((PlayerTrackerMapElement)element, hovered, optionalScale, partialX, partialY, (ElementRenderInfo)null, guiGraphics, vanillaBufferSource, rendererProvider);
   }

   /** @deprecated */
   @Deprecated
   public boolean renderElement(int location, PlayerTrackerMapElement<?> element, boolean hovered, class_310 mc, class_332 guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, class_1060 textureManager, class_327 fontRenderer, class_4598 vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, int elementIndex, double optionalDepth, float optionalScale, double partialX, double partialY, boolean cave, float partialTicks) {
      return this.renderElement((PlayerTrackerMapElement)element, hovered, optionalDepth, optionalScale, partialX, partialY, (ElementRenderInfo)null, guiGraphics, vanillaBufferSource, rendererProvider);
   }

   public boolean shouldRender(int location, boolean shadow) {
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      return (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.DISPLAY_TRACKED_PLAYERS);
   }

   public int getOrder() {
      return 200;
   }

   public PlayerTrackerMapElementCollector getCollector() {
      return this.elementCollector;
   }

   public void update(class_310 mc) {
      if (this.trackedPlayerIconManager == null) {
         this.trackedPlayerIconManager = TrackedPlayerIconManager.Builder.begin().build();
      }

      this.elementCollector.update(mc);
   }

   public static final class Builder {
      private Builder() {
      }

      private PlayerTrackerMapElementRenderer.Builder setDefault() {
         return this;
      }

      public PlayerTrackerMapElementRenderer build() {
         PlayerTrackerMapElementCollector collector = new PlayerTrackerMapElementCollector(WorldMap.playerTrackerSystemManager, () -> {
            WorldMap.trackedPlayerMenuRenderer.updateFilteredList();
         });
         return new PlayerTrackerMapElementRenderer(collector, new PlayerTrackerMapElementRenderContext(), new PlayerTrackerMapElementRenderProvider(collector), new PlayerTrackerMapElementReader());
      }

      public static PlayerTrackerMapElementRenderer.Builder begin() {
         return (new PlayerTrackerMapElementRenderer.Builder()).setDefault();
      }
   }
}
