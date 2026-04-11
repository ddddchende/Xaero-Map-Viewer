package xaero.map.gui;

import java.util.function.Supplier;
import net.minecraft.class_1074;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_339;
import net.minecraft.class_342;
import net.minecraft.class_3532;
import net.minecraft.class_4185;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.lib.client.gui.widget.XaeroSliderWidget;
import xaero.lib.common.config.Config;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.world.MapDimension;

public class GuiCaveModeOptions {
   private MapDimension dimension;
   private boolean enabled;
   private class_339 caveModeStartSlider;
   private class_342 caveModeStartField;
   private boolean shouldUpdateSlider;

   public void onInit(GuiMap screen, MapProcessor mapProcessor) {
      this.caveModeStartSlider = null;
      this.caveModeStartField = null;
      this.dimension = mapProcessor.getMapWorld().getFutureDimension();
      this.enabled = this.enabled && this.dimension != null && WorldMapClientConfigUtils.getEffectiveCaveModeAllowed();
      if (this.enabled && this.dimension != null) {
         this.updateSlider(screen);
         this.updateField(screen);
         Tooltip caveModeTypeButtonTooltip = new Tooltip("gui.xaero_wm_box_cave_mode_type");
         screen.addButton(new TooltipButton(20, screen.field_22790 - 62, 150, 20, this.getCaveModeTypeButtonMessage(), (b) -> {
            this.onCaveModeTypeButton(b, screen);
         }, () -> {
            return caveModeTypeButtonTooltip;
         }));
      }

   }

   private void onCaveModeTypeButton(class_4185 b, GuiMap screen) {
      this.dimension.toggleCaveModeType(true);
      synchronized(screen.getMapProcessor().uiSync) {
         this.dimension.saveConfigUnsynced();
      }

      b.method_25355(this.getCaveModeTypeButtonMessage());
   }

   private class_342 createField(GuiMap screen) {
      class_342 field = new class_342(class_310.method_1551().field_1772, 172, screen.field_22790 - 40, 50, 20, class_2561.method_43471("gui.xaero_wm_cave_mode_start"));
      field.method_1880(7);
      int initialCaveModeStart = this.getCaveStart();
      field.method_1852(initialCaveModeStart == Integer.MAX_VALUE ? "" : initialCaveModeStart.makeConcatWithConstants<invokedynamic>(initialCaveModeStart));
      field.method_1863((text) -> {
         try {
            this.setCaveStart(!text.isEmpty() && !text.equalsIgnoreCase("auto") ? Integer.parseInt(text) : Integer.MAX_VALUE);
            this.shouldUpdateSlider = true;
         } catch (NumberFormatException var3) {
         }

      });
      return field;
   }

   private int getCaveStart() {
      Config primaryConfig = WorldMap.INSTANCE.getConfigs().getPrimaryClientConfigManager().getConfig();
      return (Integer)primaryConfig.get(WorldMapPrimaryClientConfigOptions.CAVE_MODE_START);
   }

   private void setCaveStart(int y) {
      Config primaryConfig = WorldMap.INSTANCE.getConfigs().getPrimaryClientConfigManager().getConfig();
      primaryConfig.set(WorldMapPrimaryClientConfigOptions.CAVE_MODE_START, y);
      WorldMap.INSTANCE.getConfigs().getPrimaryClientConfigManagerIO().save();
   }

   private class_339 createSlider(GuiMap screen) {
      class_2561 displayName = class_2561.method_43471("gui.xaero_wm_cave_mode_start");
      Supplier<class_2561> labelGetter = () -> {
         return displayName;
      };
      int initialCaveStart = this.getCaveStart();
      int minY = -64;
      int maxY = 319;
      int minOption = minY - 1;
      int range = maxY - minOption;
      double initialSliderValue = class_3532.method_15350((double)(initialCaveStart - minOption) / (double)range, 0.0D, 1.0D);
      return new XaeroSliderWidget(20, screen.field_22790 - 40, 150, 20, (class_2561)labelGetter.get(), initialSliderValue, (newSliderValue) -> {
         int selectedY = (int)Math.round(newSliderValue * (double)range) + minOption;
         if (selectedY == minOption) {
            selectedY = Integer.MAX_VALUE;
         }

         this.setCaveStart(selectedY);
         screen.onCaveModeStartSet();
      }, labelGetter);
   }

   private void updateField(GuiMap screen) {
      if (this.caveModeStartField == null) {
         screen.addButton(this.caveModeStartField = this.createField(screen));
      } else {
         screen.replaceRenderableWidget(this.caveModeStartField, this.caveModeStartField = this.createField(screen));
      }

   }

   private void updateSlider(GuiMap screen) {
      if (this.caveModeStartSlider == null) {
         screen.addButton(this.caveModeStartSlider = this.createSlider(screen));
      } else {
         screen.replaceRenderableWidget(this.caveModeStartSlider, this.caveModeStartSlider = this.createSlider(screen));
      }

   }

   public void toggle(GuiMap screen) {
      this.enabled = WorldMapClientConfigUtils.getEffectiveCaveModeAllowed() && !this.enabled;
      screen.method_25423(class_310.method_1551(), screen.field_22789, screen.field_22790);
   }

   public void onCaveModeStartSet(GuiMap screen) {
      if (this.enabled) {
         this.updateField(screen);
      }

   }

   public void tick(GuiMap screen) {
      if (this.shouldUpdateSlider) {
         this.updateSlider(screen);
         this.shouldUpdateSlider = false;
      }

      if (this.enabled) {
         this.caveModeStartField.method_1887(this.caveModeStartField.method_1882().isEmpty() ? class_1074.method_4662("gui.xaero_wm_cave_mode_start_auto", new Object[0]) : "");
      }

   }

   public void unfocusAll() {
      if (this.caveModeStartField != null) {
         this.caveModeStartField.method_25365(false);
      }

      if (this.caveModeStartSlider != null) {
         this.caveModeStartSlider.method_25365(false);
      }

   }

   private class_2561 getCaveModeTypeButtonMessage() {
      String var10000 = class_1074.method_4662("gui.xaero_wm_cave_mode_type", new Object[0]);
      return class_2561.method_43470(var10000 + ": " + class_1074.method_4662(this.dimension == null ? "N/A" : (this.dimension.getCaveModeType() == 0 ? "gui.xaero_off" : (this.dimension.getCaveModeType() == 1 ? "gui.xaero_wm_cave_mode_type_layered" : "gui.xaero_wm_cave_mode_type_full")), new Object[0]));
   }

   public boolean isEnabled() {
      return this.enabled;
   }
}
