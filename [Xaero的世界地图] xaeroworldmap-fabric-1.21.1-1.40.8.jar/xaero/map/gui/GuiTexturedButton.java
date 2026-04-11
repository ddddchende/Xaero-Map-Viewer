package xaero.map.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Supplier;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_4185.class_4241;
import xaero.lib.client.gui.widget.Tooltip;

public class GuiTexturedButton extends TooltipButton {
   protected int textureX;
   protected int textureY;
   protected int textureW;
   protected int textureH;
   protected class_2960 texture;

   public GuiTexturedButton(int x, int y, int w, int h, int textureX, int textureY, int textureW, int textureH, class_2960 texture, class_4241 onPress, Supplier<Tooltip> tooltip) {
      super(x, y, w, h, class_2561.method_43470(""), onPress, tooltip);
      this.textureX = textureX;
      this.textureY = textureY;
      this.textureW = textureW;
      this.textureH = textureH;
      this.texture = texture;
   }

   public class_2561 method_25369() {
      return (class_2561)(this.tooltipSupplier != null ? class_2561.method_43470(((Tooltip)this.tooltipSupplier.get()).getPlainText()) : super.method_25369());
   }

   public void method_48579(class_332 guiGraphics, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
      int iconX = this.method_46426() + this.field_22758 / 2 - this.textureW / 2;
      int iconY = this.method_46427() + this.field_22759 / 2 - this.textureH / 2;
      if (this.field_22763) {
         if (this.field_22762) {
            --iconY;
            RenderSystem.setShaderColor(0.9F, 0.9F, 0.9F, 1.0F);
         } else {
            RenderSystem.setShaderColor(0.9882F, 0.9882F, 0.9882F, 1.0F);
         }
      } else {
         RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
      }

      if (this.method_25370()) {
         guiGraphics.method_25294(iconX, iconY, iconX + this.textureW, iconY + this.textureH, 1442840575);
      }

      guiGraphics.method_25302(this.texture, iconX, iconY, this.textureX, this.textureY, this.textureW, this.textureH);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
