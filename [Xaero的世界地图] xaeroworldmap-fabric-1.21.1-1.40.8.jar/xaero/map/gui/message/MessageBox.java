package xaero.map.gui.message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.class_2561;
import xaero.lib.common.util.TextSplitter;

public class MessageBox {
   private final List<Message> messages;
   private final int width;
   private final int capacity;

   private MessageBox(List<Message> messages, int width, int capacity) {
      this.messages = messages;
      this.width = width;
      this.capacity = capacity;
   }

   private void addMessageLine(class_2561 text) {
      Message msg = new Message(text, System.currentTimeMillis());
      this.messages.add(0, msg);
      if (this.messages.size() > this.capacity) {
         this.messages.remove(this.messages.size() - 1);
      }

   }

   public void addMessage(class_2561 text) {
      List<class_2561> splitDest = new ArrayList();
      TextSplitter.splitTextIntoLines(splitDest, this.width, this.width, text, (StringBuilder)null);
      Iterator var3 = splitDest.iterator();

      while(var3.hasNext()) {
         class_2561 line = (class_2561)var3.next();
         this.addMessageLine(line);
      }

   }

   public void addMessageWithSource(class_2561 source, class_2561 text) {
      class_2561 fullText = class_2561.method_43470("<");
      fullText.method_10855().add(source);
      fullText.method_10855().add(class_2561.method_43470("> "));
      fullText.method_10855().add(text);
      this.addMessage(fullText);
   }

   public int getCapacity() {
      return this.capacity;
   }

   public Iterator<Message> getIterator() {
      return this.messages.iterator();
   }

   public static class Builder {
      private int width;
      private int capacity;

      private Builder() {
      }

      public MessageBox.Builder setDefault() {
         this.setWidth(250);
         this.setCapacity(5);
         return this;
      }

      public MessageBox.Builder setWidth(int width) {
         this.width = width;
         return this;
      }

      public MessageBox.Builder setCapacity(int capacity) {
         this.capacity = capacity;
         return this;
      }

      public MessageBox build() {
         return new MessageBox(new ArrayList(this.capacity), this.width, this.capacity);
      }

      public static MessageBox.Builder begin() {
         return (new MessageBox.Builder()).setDefault();
      }
   }
}
