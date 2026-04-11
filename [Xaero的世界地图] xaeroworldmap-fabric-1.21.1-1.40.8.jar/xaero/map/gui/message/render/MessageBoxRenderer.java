package xaero.map.gui.message.render;

import java.util.Iterator;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import xaero.map.gui.message.Message;
import xaero.map.gui.message.MessageBox;

public class MessageBoxRenderer {
   private final int OPAQUE_FOR = 5000;
   private final int FADE_FOR = 3000;

   public void render(class_332 guiGraphics, MessageBox messageBox, class_327 font, int x, int y, boolean rightAlign) {
      class_4587 matrixStack = guiGraphics.method_51448();
      long time = System.currentTimeMillis();
      matrixStack.method_22903();
      matrixStack.method_46416((float)x, (float)y, 0.0F);
      int index = 0;

      for(Iterator iterator = messageBox.getIterator(); iterator.hasNext(); ++index) {
         Message message = (Message)iterator.next();
         int passed = (int)(time - message.getAdditionTime());
         float opacity = passed < 5000 ? 1.0F : (float)(3000 - (passed - 5000)) / 3000.0F;
         int alphaInt = (int)(opacity * 255.0F);
         if (alphaInt <= 3) {
            break;
         }

         int textColor = 16777215 | alphaInt << 24;
         int bgColor = (int)(0.5F * (float)alphaInt) << 24;
         int textWidth = font.method_27525(message.getText());
         int textX = rightAlign ? -textWidth - 1 : 2;
         int textY = -index * 10 - 4;
         int bgWidth = textWidth + 3;
         guiGraphics.method_25294(textX - 2, textY - 1, textX - 2 + bgWidth, textY + 9, bgColor);
         guiGraphics.method_27535(font, message.getText(), textX, textY, textColor);
      }

      matrixStack.method_22909();
   }
}
