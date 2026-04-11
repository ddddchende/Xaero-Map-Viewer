package xaero.map.palette;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;

public final class FastIntPalette {
   private final Int2IntMap indexHelper;
   private final List<FastIntPalette.Element> elements;
   private final int maxCountPerElement;

   private FastIntPalette(Int2IntMap indexHelper, List<FastIntPalette.Element> elements, int maxCountPerElement) {
      this.indexHelper = indexHelper;
      this.elements = elements;
      this.maxCountPerElement = maxCountPerElement;
   }

   public synchronized int get(int index, int defaultValue) {
      if (index >= 0 && index < this.elements.size()) {
         FastIntPalette.Element element = (FastIntPalette.Element)this.elements.get(index);
         return element == null ? defaultValue : element.getValue();
      } else {
         return defaultValue;
      }
   }

   public synchronized int add(int elementValue) {
      int existing = this.indexHelper.getOrDefault(elementValue, -1);
      if (existing != -1) {
         return existing;
      } else {
         int newIndex = this.elements.size();
         boolean add = true;

         for(int i = 0; i < this.elements.size(); ++i) {
            if (this.elements.get(i) == null) {
               newIndex = i;
               add = false;
               break;
            }
         }

         this.indexHelper.put(elementValue, newIndex);
         FastIntPalette.Element element = new FastIntPalette.Element(elementValue);
         if (add) {
            this.elements.add(element);
         } else {
            this.elements.set(newIndex, element);
         }

         return newIndex;
      }
   }

   public synchronized int add(int elementValue, int count) {
      if (count >= 0 && count <= this.maxCountPerElement) {
         int index = this.add(elementValue);
         ((FastIntPalette.Element)this.elements.get(index)).count = (short)count;
         return index;
      } else {
         throw new IllegalArgumentException("illegal count!");
      }
   }

   public synchronized int append(int elementValue, int count) {
      if (count >= 0 && count <= this.maxCountPerElement) {
         int existing = this.indexHelper.getOrDefault(elementValue, -1);
         if (existing != -1) {
            throw new IllegalArgumentException("duplicate palette element!");
         } else {
            int newIndex = this.elements.size();
            this.indexHelper.put(elementValue, newIndex);
            FastIntPalette.Element element = new FastIntPalette.Element(elementValue);
            element.count = (short)count;
            this.elements.add(element);
            return newIndex;
         }
      } else {
         throw new IllegalArgumentException("illegal count!");
      }
   }

   public synchronized int getIndex(int elementValue) {
      return this.indexHelper.getOrDefault(elementValue, -1);
   }

   public synchronized int count(int index, boolean up) {
      FastIntPalette.Element element = (FastIntPalette.Element)this.elements.get(index);
      element.count(up, this.maxCountPerElement);
      return element.getCount();
   }

   public synchronized int getCount(int index) {
      FastIntPalette.Element element = (FastIntPalette.Element)this.elements.get(index);
      return element.getCount();
   }

   public synchronized void remove(int index) {
      FastIntPalette.Element previous = (FastIntPalette.Element)this.elements.set(index, (Object)null);
      if (previous != null) {
         this.indexHelper.remove(previous.getValue());
      }

      if (index == this.elements.size() - 1) {
         while(!this.elements.isEmpty() && this.elements.get(this.elements.size() - 1) == null) {
            this.elements.remove(this.elements.size() - 1);
         }
      }

   }

   public synchronized boolean replace(int elementValue, int newValue) {
      int index = this.indexHelper.getOrDefault(elementValue, -1);
      return index == -1 ? false : this.replaceAtIndex(index, newValue);
   }

   public synchronized boolean replaceAtIndex(int index, int newValue) {
      FastIntPalette.Element element = (FastIntPalette.Element)this.elements.get(index);
      int elementValue = element.getValue();
      element.setValue(newValue);
      this.indexHelper.remove(elementValue);
      this.indexHelper.put(newValue, index);
      return true;
   }

   public synchronized void addNull() {
      this.elements.add((Object)null);
   }

   public int getSize() {
      return this.elements.size();
   }

   public int getNonNullCount() {
      return this.indexHelper.size();
   }

   private static class Element {
      private int value;
      private short count;

      private Element(int elementValue) {
         this.value = elementValue;
      }

      private void setValue(int elementValue) {
         this.value = elementValue;
      }

      private int getValue() {
         return this.value;
      }

      private int getCount() {
         return this.count & '\uffff';
      }

      private void count(boolean up, int maxCount) {
         if ((!up || this.count != maxCount) && (up || this.count != 0)) {
            this.count = (short)(this.count + (up ? 1 : -1));
         } else {
            throw new IllegalStateException();
         }
      }
   }

   public static final class Builder {
      private int maxCountPerElement;

      private Builder() {
      }

      public FastIntPalette.Builder setDefault() {
         this.setMaxCountPerElement(0);
         return this;
      }

      public FastIntPalette.Builder setMaxCountPerElement(int maxCountPerElement) {
         this.maxCountPerElement = maxCountPerElement;
         return this;
      }

      public FastIntPalette build() {
         if (this.maxCountPerElement == 0) {
            throw new IllegalStateException();
         } else if (this.maxCountPerElement > 65535) {
            throw new IllegalStateException("the max count must be within 0 - 65535");
         } else {
            Int2IntMap indexHelper = new Int2IntOpenHashMap();
            List<FastIntPalette.Element> elements = new ArrayList();
            return new FastIntPalette(indexHelper, elements, this.maxCountPerElement);
         }
      }

      public static FastIntPalette.Builder begin() {
         return (new FastIntPalette.Builder()).setDefault();
      }
   }
}
