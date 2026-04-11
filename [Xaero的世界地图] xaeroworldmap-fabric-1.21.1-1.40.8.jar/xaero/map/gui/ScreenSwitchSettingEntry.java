package xaero.map.gui;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.class_1074;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_339;
import net.minecraft.class_437;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.client.gui.widget.Tooltip;

public class ScreenSwitchSettingEntry implements ISettingEntry {
   private String name;
   private BiFunction<class_437, class_437, class_437> screenFactory;
   private Supplier<Tooltip> tooltipSupplier;
   private boolean active;
   private final boolean consideredAnExit;

   public ScreenSwitchSettingEntry(String name, BiFunction<class_437, class_437, class_437> screenFactoryFromCurrentAndEscape, Tooltip tooltip, boolean active) {
      this(name, screenFactoryFromCurrentAndEscape, tooltip, active, true);
   }

   public ScreenSwitchSettingEntry(String name, BiFunction<class_437, class_437, class_437> screenFactoryFromCurrentAndEscape, Tooltip tooltip, boolean active, boolean consideredAnExit) {
      this.name = name;
      this.screenFactory = screenFactoryFromCurrentAndEscape;
      this.tooltipSupplier = () -> {
         return tooltip;
      };
      this.active = active;
      this.consideredAnExit = consideredAnExit;
   }

   public String getStringForSearch() {
      Tooltip entryTooltip = this.tooltipSupplier == null ? null : (Tooltip)this.tooltipSupplier.get();
      String tooltipFullCode = entryTooltip == null ? null : entryTooltip.getFullCode();
      String var10000 = class_1074.method_4662(this.name, new Object[0]);
      return var10000 + " " + this.name.replace("gui.xaero", "") + (tooltipFullCode != null ? " " + tooltipFullCode.replace("gui.xaero", "") : "") + (entryTooltip != null ? " " + entryTooltip.getPlainText() : "");
   }

   public class_339 createWidget(int x, int y, int w) {
      TooltipButton button = new TooltipButton(x, y, w, 20, class_2561.method_43471(this.name), (b) -> {
         class_310 mc = class_310.method_1551();
         class_437 current = mc.field_1755;
         class_437 currentEscScreen = current instanceof xaero.lib.client.gui.ScreenBase ? ((xaero.lib.client.gui.ScreenBase)current).escape : null;
         class_437 targetScreen = (class_437)this.screenFactory.apply(current, currentEscScreen);
         if (this.consideredAnExit && current instanceof xaero.lib.client.gui.ScreenBase) {
            ((xaero.lib.client.gui.ScreenBase)current).onExit(targetScreen);
         } else {
            mc.method_1507(targetScreen);
         }

      }, this.tooltipSupplier);
      button.field_22763 = this.active;
      return button;
   }

   public BiFunction<class_437, class_437, class_437> getScreenFactory() {
      return this.screenFactory;
   }
}
