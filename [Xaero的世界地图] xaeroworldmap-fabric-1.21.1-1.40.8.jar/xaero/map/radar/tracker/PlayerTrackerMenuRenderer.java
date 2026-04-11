package xaero.map.radar.tracker;

import net.minecraft.class_124;
import net.minecraft.class_1657;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_640;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.controls.util.KeyMappingUtils;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.element.MapElementMenuRenderer;
import xaero.map.element.render.ElementRenderer;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiTexturedButton;

public final class PlayerTrackerMenuRenderer extends MapElementMenuRenderer<PlayerTrackerMapElement<?>, PlayerTrackerMenuRenderContext> {
   private final PlayerTrackerIconRenderer iconRenderer;
   private final PlayerTrackerMapElementRenderer renderer;
   private class_4185 showPlayersButton;

   private PlayerTrackerMenuRenderer(PlayerTrackerMapElementRenderer renderer, PlayerTrackerIconRenderer iconRenderer, PlayerTrackerMenuRenderContext context, PlayerTrackerMapElementRenderProvider<PlayerTrackerMenuRenderContext> provider) {
      super(context, provider);
      this.iconRenderer = iconRenderer;
      this.renderer = renderer;
   }

   public void onMapInit(GuiMap screen, class_310 mc, int width, int height) {
      super.onMapInit(screen, mc, width, height);
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      boolean trackedPlayers = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.DISPLAY_TRACKED_PLAYERS);
      Tooltip showPlayersTooltip = new Tooltip(class_2561.method_43469(trackedPlayers ? "gui.xaero_box_showing_tracked_players" : "gui.xaero_box_hiding_tracked_players", new Object[]{class_2561.method_43470(KeyMappingUtils.getKeyName(screen.getTrackedPlayerKeyBinding())).method_27694((s) -> {
         return s.method_10977(class_124.field_1077);
      })}), true);
      screen.addButton(this.showPlayersButton = new GuiTexturedButton(width - 173, height - 33, 20, 20, trackedPlayers ? 197 : 213, 48, 16, 16, WorldMap.guiTextures, (b) -> {
         this.onShowPlayersButton(screen, width, height);
      }, () -> {
         return showPlayersTooltip;
      }));
      this.showPlayersButton.field_22763 = !WorldMapClientConfigUtils.isOptionServerEnforced(WorldMapProfiledConfigOptions.DISPLAY_TRACKED_PLAYERS);
   }

   public void onShowPlayersButton(GuiMap screen, int width, int height) {
      WorldMapClientConfigUtils.tryTogglingCurrentProfileOption(WorldMapProfiledConfigOptions.DISPLAY_TRACKED_PLAYERS);
      screen.method_25423(this.mc, width, height);
      screen.method_25395(this.showPlayersButton);
   }

   protected void beforeMenuRender() {
   }

   protected void afterMenuRender() {
   }

   public void renderInMenu(PlayerTrackerMapElement<?> element, class_332 guiGraphics, class_437 gui, int mouseX, int mouseY, double scale, boolean enabled, boolean hovered, class_310 mc, boolean pressed, int textX) {
      class_640 info = mc.method_1562().method_2871(element.getPlayerId());
      if (info != null) {
         class_1657 clientPlayer = mc.field_1687.method_18470(element.getPlayerId());
         this.iconRenderer.renderIcon(guiGraphics, clientPlayer, this.renderer.getTrackedPlayerIconManager().getPlayerSkin(clientPlayer, info));
      }

   }

   protected void beforeFiltering() {
   }

   public int menuStartPos(int height) {
      return height - 59;
   }

   public int menuSearchPadding() {
      return 1;
   }

   protected String getFilterPlaceholder() {
      return "gui.xaero_filter_players_by_name";
   }

   protected ElementRenderer<? super PlayerTrackerMapElement<?>, ?, ?> getRenderer(PlayerTrackerMapElement<?> element) {
      return this.renderer;
   }

   public boolean canJumpTo(PlayerTrackerMapElement<?> element) {
      return !this.renderer.getReader().isHidden(element, (PlayerTrackerMapElementRenderContext)this.renderer.getContext());
   }

   public static final class Builder {
      private PlayerTrackerMapElementRenderer renderer;

      private Builder() {
      }

      private PlayerTrackerMenuRenderer.Builder setDefault() {
         this.setRenderer((PlayerTrackerMapElementRenderer)null);
         return this;
      }

      public PlayerTrackerMenuRenderer.Builder setRenderer(PlayerTrackerMapElementRenderer renderer) {
         this.renderer = renderer;
         return this;
      }

      public PlayerTrackerMenuRenderer build() {
         if (this.renderer == null) {
            throw new IllegalStateException();
         } else {
            return new PlayerTrackerMenuRenderer(this.renderer, new PlayerTrackerIconRenderer(), new PlayerTrackerMenuRenderContext(), new PlayerTrackerMapElementRenderProvider(this.renderer.getCollector()));
         }
      }

      public static PlayerTrackerMenuRenderer.Builder begin() {
         return (new PlayerTrackerMenuRenderer.Builder()).setDefault();
      }
   }
}
