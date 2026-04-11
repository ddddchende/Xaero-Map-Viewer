package xaero.map.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_410;
import net.minecraft.class_437;
import xaero.lib.client.gui.IScreenBase;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

public class ConfirmScreenBase extends class_410 implements IScreenBase {
   public class_437 parent;
   public class_437 escape;
   private boolean renderEscapeInBackground;
   protected boolean canSkipWorldRender;

   public ConfirmScreenBase(class_437 parent, class_437 escape, boolean renderEscapeInBackground, BooleanConsumer _callbackFunction, class_2561 _title, class_2561 _messageLine2) {
      super(_callbackFunction, _title, _messageLine2);
      this.parent = parent;
      this.escape = escape;
      this.renderEscapeInBackground = renderEscapeInBackground;
      this.canSkipWorldRender = true;
   }

   public ConfirmScreenBase(class_437 parent, class_437 escape, boolean renderEscapeInBackground, BooleanConsumer p_i232270_1_, class_2561 p_i232270_2_, class_2561 p_i232270_3_, class_2561 p_i232270_4_, class_2561 p_i232270_5_) {
      super(p_i232270_1_, p_i232270_2_, p_i232270_3_, p_i232270_4_, p_i232270_5_);
      this.parent = parent;
      this.escape = escape;
      this.renderEscapeInBackground = renderEscapeInBackground;
      this.canSkipWorldRender = true;
   }

   protected void onExit(class_437 screen) {
      this.field_22787.method_1507(screen);
   }

   protected void goBack() {
      this.onExit(this.parent);
   }

   public void method_25419() {
      this.onExit(this.escape);
   }

   public void renderEscapeScreen(class_332 guiGraphics, int p_230430_2_, int p_230430_3_, float p_230430_4_) {
      if (this.escape != null) {
         this.escape.method_25394(guiGraphics, p_230430_2_, p_230430_3_, p_230430_4_);
         guiGraphics.method_51452();
      }

      GlStateManager._clear(256, class_310.field_1703);
   }

   public void method_25394(class_332 guiGraphics, int p_230430_2_, int p_230430_3_, float p_230430_4_) {
      if (this.renderEscapeInBackground) {
         this.renderEscapeScreen(guiGraphics, p_230430_2_, p_230430_3_, p_230430_4_);
      }

      super.method_25394(guiGraphics, p_230430_2_, p_230430_3_, p_230430_4_);
   }

   public void method_25426() {
      super.method_25426();
      if (this.escape != null) {
         this.escape.method_25423(this.field_22787, this.field_22789, this.field_22790);
      }

   }

   public boolean shouldSkipWorldRender() {
      return this.canSkipWorldRender && this.renderEscapeInBackground && this.escape instanceof IScreenBase && ((IScreenBase)this.escape).shouldSkipWorldRender();
   }

   public void onDropdownOpen(DropDownWidget menu) {
   }

   public void onDropdownClosed(DropDownWidget menu) {
   }

   public class_437 getEscape() {
      return this.escape;
   }
}
