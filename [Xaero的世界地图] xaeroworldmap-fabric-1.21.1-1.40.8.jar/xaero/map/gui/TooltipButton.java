package xaero.map.gui;

import java.util.function.Supplier;
import net.minecraft.class_2561;
import net.minecraft.class_4185;
import net.minecraft.class_4185.class_4241;
import xaero.lib.client.gui.widget.ITooltipHaver;
import xaero.lib.client.gui.widget.Tooltip;

public class TooltipButton extends class_4185 implements ITooltipHaver {
   protected Supplier<Tooltip> tooltipSupplier;

   public TooltipButton(int x, int y, int w, int h, class_2561 message, class_4241 action, Supplier<Tooltip> tooltipSupplier) {
      super(x, y, w, h, message, action, field_40754);
      this.tooltipSupplier = tooltipSupplier;
   }

   public Supplier<Tooltip> getXaero_tooltip() {
      return this.tooltipSupplier;
   }
}
