package xaero.map.region;

import net.minecraft.class_1058;
import net.minecraft.class_1959;
import net.minecraft.class_2246;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_5321;
import xaero.map.MapProcessor;

public class OverlayBuilder {
   private static final int MAX_OVERLAYS = 10;
   private Overlay[] overlayBuildingSet;
   private int currentOverlayIndex;
   private OverlayManager overlayManager;
   private class_1058 prevIcon;
   private class_5321<class_1959> overlayBiome;

   public OverlayBuilder(OverlayManager overlayManager) {
      this.overlayManager = overlayManager;
      this.overlayBuildingSet = new Overlay[10];

      for(int i = 0; i < this.overlayBuildingSet.length; ++i) {
         this.overlayBuildingSet[i] = new Overlay(class_2246.field_10124.method_9564(), (byte)0, false);
      }

      this.currentOverlayIndex = -1;
   }

   public void startBuilding() {
      this.currentOverlayIndex = -1;
      this.setOverlayBiome((class_5321)null);
   }

   public void build(class_2680 state, int opacity, byte light, MapProcessor mapProcessor, class_5321<class_1959> biomeId) {
      Overlay currentOverlay = this.getCurrentOverlay();
      Overlay nextOverlay = null;
      if (this.currentOverlayIndex < this.overlayBuildingSet.length - 1) {
         nextOverlay = this.overlayBuildingSet[this.currentOverlayIndex + 1];
      }

      class_1058 icon = null;
      boolean changed = false;
      if (currentOverlay == null || currentOverlay.getState() != state) {
         icon = class_310.method_1551().method_1541().method_3351().method_3339(state);
         changed = icon != this.prevIcon;
      }

      if (nextOverlay != null && (currentOverlay == null || changed)) {
         boolean glowing = false;

         try {
            glowing = mapProcessor.getMapWriter().isGlowing(state);
         } catch (Exception var12) {
         }

         if (this.getOverlayBiome() == null) {
            this.setOverlayBiome(biomeId);
         }

         nextOverlay.write(state, light, glowing);
         currentOverlay = nextOverlay;
         ++this.currentOverlayIndex;
      }

      currentOverlay.increaseOpacity(opacity);
      if (changed) {
         this.prevIcon = icon;
      }

   }

   public boolean isEmpty() {
      return this.currentOverlayIndex < 0;
   }

   public Overlay getCurrentOverlay() {
      Overlay currentOverlay = null;
      if (this.currentOverlayIndex >= 0) {
         currentOverlay = this.overlayBuildingSet[this.currentOverlayIndex];
      }

      return currentOverlay;
   }

   public void finishBuilding(MapBlock block) {
      for(int i = 0; i <= this.currentOverlayIndex; ++i) {
         Overlay o = this.overlayBuildingSet[i];
         Overlay original = this.overlayManager.getOriginal(o);
         if (o == original) {
            this.overlayBuildingSet[i] = new Overlay(class_2246.field_10124.method_9564(), (byte)0, false);
         }

         block.addOverlay(original);
      }

   }

   public class_5321<class_1959> getOverlayBiome() {
      return this.overlayBiome;
   }

   public void setOverlayBiome(class_5321<class_1959> overlayBiome) {
      this.overlayBiome = overlayBiome;
   }
}
