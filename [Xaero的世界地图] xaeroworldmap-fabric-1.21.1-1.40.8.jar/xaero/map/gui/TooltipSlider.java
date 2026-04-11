package xaero.map.gui;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.class_2561;
import net.minecraft.class_357;
import xaero.lib.client.gui.widget.ITooltipHaver;
import xaero.lib.client.gui.widget.Tooltip;

public class TooltipSlider extends class_357 implements ITooltipHaver {
   protected final Supplier<Tooltip> tooltipSupplier;
   protected final Consumer<Double> onValue;
   protected final Function<TooltipSlider, class_2561> messageUpdater;

   public TooltipSlider(int x, int y, int w, int h, class_2561 message, double value, Consumer<Double> onValue, Function<TooltipSlider, class_2561> messageUpdater, Supplier<Tooltip> tooltipSupplier) {
      super(x, y, w, h, message, value);
      this.tooltipSupplier = tooltipSupplier;
      this.onValue = onValue;
      this.messageUpdater = messageUpdater;
   }

   public Supplier<Tooltip> getXaero_tooltip() {
      return this.tooltipSupplier;
   }

   protected void method_25346() {
      this.method_25355((class_2561)this.messageUpdater.apply(this));
   }

   protected void method_25344() {
      this.onValue.accept(this.field_22753);
   }
}
