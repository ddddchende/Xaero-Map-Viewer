package xaero.map.radar.tracker;

import com.mojang.blaze3d.platform.GlStateManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1068;
import net.minecraft.class_1657;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_640;
import net.minecraft.class_742;
import xaero.map.icon.XaeroIcon;
import xaero.map.icon.XaeroIconAtlasManager;

public final class TrackedPlayerIconManager {
   private static final int ICON_WIDTH = 32;
   private static final int PREFERRED_ATLAS_WIDTH = 1024;
   private final TrackedPlayerIconPrerenderer prerenderer;
   private final XaeroIconAtlasManager iconAtlasManager;
   private final Map<class_2960, XaeroIcon> icons;
   private final int iconWidth;

   private TrackedPlayerIconManager(TrackedPlayerIconPrerenderer prerenderer, XaeroIconAtlasManager iconAtlasManager, Map<class_2960, XaeroIcon> icons, int iconWidth) {
      this.prerenderer = prerenderer;
      this.iconAtlasManager = iconAtlasManager;
      this.icons = icons;
      this.iconWidth = iconWidth;
   }

   public class_2960 getPlayerSkin(class_1657 player, class_640 info) {
      class_2960 skinTextureLocation = player instanceof class_742 ? ((class_742)player).method_52814().comp_1626() : info.method_52810().comp_1626();
      if (skinTextureLocation == null) {
         skinTextureLocation = class_1068.method_4648(player.method_5667()).comp_1626();
      }

      return skinTextureLocation;
   }

   public XaeroIcon getIcon(class_332 guiGraphics, class_1657 player, class_640 info, PlayerTrackerMapElement<?> element) {
      class_2960 skinTextureLocation = this.getPlayerSkin(player, info);
      XaeroIcon result = (XaeroIcon)this.icons.get(skinTextureLocation);
      if (result == null) {
         this.icons.put(skinTextureLocation, result = this.iconAtlasManager.getCurrentAtlas().createIcon());
         this.prerenderer.prerender(guiGraphics, result, player, this.iconWidth, skinTextureLocation, element);
      }

      return result;
   }

   public static final class Builder {
      public TrackedPlayerIconManager build() {
         int maxTextureSize = GlStateManager._getInteger(3379);
         int atlasTextureSize = Math.min(maxTextureSize, 1024) / 32 * 32;
         return new TrackedPlayerIconManager(new TrackedPlayerIconPrerenderer(), new XaeroIconAtlasManager(32, atlasTextureSize, new ArrayList()), new HashMap(), 32);
      }

      public static TrackedPlayerIconManager.Builder begin() {
         return new TrackedPlayerIconManager.Builder();
      }
   }
}
