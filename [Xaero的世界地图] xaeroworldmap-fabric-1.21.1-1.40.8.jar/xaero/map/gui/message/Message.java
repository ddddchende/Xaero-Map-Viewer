package xaero.map.gui.message;

import net.minecraft.class_2561;

public class Message {
   private final class_2561 text;
   private final long additionTime;

   public Message(class_2561 text, long additionTime) {
      this.text = text;
      this.additionTime = additionTime;
   }

   public class_2561 getText() {
      return this.text;
   }

   public long getAdditionTime() {
      return this.additionTime;
   }
}
