package xaero.map.palette;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;

public final class FastPalette<T> {
   private final Object2IntMap<T> indexHelper;
   private final List<FastPalette.Element<T>> elements;
   private final int maxCountPerElement;

   private FastPalette(Object2IntMap<T> indexHelper, List<FastPalette.Element<T>> elements, int maxCountPerElement) {
      this.indexHelper = indexHelper;
      this.elements = elements;
      this.maxCountPerElement = maxCountPerElement;
   }

   public synchronized T get(int index) {
      if (index >= 0 && index < this.elements.size()) {
         FastPalette.Element<T> element = (FastPalette.Element)this.elements.get(index);
         return element == null ? null : element.getObject();
      } else {
         return null;
      }
   }

   public synchronized int add(T elementObject) {
      int existing = this.indexHelper.getOrDefault(elementObject, -1);
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

         this.indexHelper.put(elementObject, newIndex);
         FastPalette.Element<T> element = new FastPalette.Element(elementObject);
         if (add) {
            this.elements.add(element);
         } else {
            this.elements.set(newIndex, element);
         }

         return newIndex;
      }
   }

   public synchronized int add(T elementObject, int count) {
      if (count >= 0 && count <= this.maxCountPerElement) {
         int index = this.add(elementObject);
         ((FastPalette.Element)this.elements.get(index)).count = (short)count;
         return index;
      } else {
         throw new IllegalArgumentException("illegal count!");
      }
   }

   public synchronized int append(T elementObject, int count) {
      if (count >= 0 && count <= this.maxCountPerElement) {
         int existing = this.indexHelper.getOrDefault(elementObject, -1);
         if (existing != -1) {
            throw new IllegalArgumentException("duplicate palette element!");
         } else {
            int newIndex = this.elements.size();
            this.indexHelper.put(elementObject, newIndex);
            FastPalette.Element<T> element = new FastPalette.Element(elementObject);
            element.count = (short)count;
            this.elements.add(element);
            return newIndex;
         }
      } else {
         throw new IllegalArgumentException("illegal count!");
      }
   }

   public synchronized int getIndex(T elementObject) {
      return this.indexHelper.getOrDefault(elementObject, -1);
   }

   public synchronized int count(int index, boolean up) {
      FastPalette.Element<T> element = (FastPalette.Element)this.elements.get(index);
      element.count(up, this.maxCountPerElement);
      return element.getCount();
   }

   public synchronized int getCount(int index) {
      FastPalette.Element<T> element = (FastPalette.Element)this.elements.get(index);
      return element.getCount();
   }

   public synchronized void remove(int index) {
      FastPalette.Element<T> previous = (FastPalette.Element)this.elements.set(index, (Object)null);
      if (previous != null) {
         this.indexHelper.removeInt(previous.getObject());
      }

      if (index == this.elements.size() - 1) {
         while(!this.elements.isEmpty() && this.elements.get(this.elements.size() - 1) == null) {
            this.elements.remove(this.elements.size() - 1);
         }
      }

   }

   public synchronized boolean replace(T elementObject, T newObject) {
      int index = this.indexHelper.getOrDefault(elementObject, -1);
      return index == -1 ? false : this.replace(index, newObject);
   }

   public synchronized boolean replace(int index, T newObject) {
      FastPalette.Element<T> element = (FastPalette.Element)this.elements.get(index);
      T elementObject = element.getObject();
      element.setObject(newObject);
      this.indexHelper.removeInt(elementObject);
      this.indexHelper.put(newObject, index);
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

   private static class Element<T> {
      private T object;
      private short count;

      private Element(T elementObject) {
         this.object = elementObject;
      }

      private void setObject(T elementObject) {
         this.object = elementObject;
      }

      private T getObject() {
         return this.object;
      }

      private int getCount() {
         return this.count & '\uffff';
      }

      private void count(boolean up, int maxCount) {
         this.count = (short)(this.count + (up ? 1 : -1));
      }
   }

   public static final class Builder<T> {
      private int maxCountPerElement;

      private Builder() {
      }

      public FastPalette.Builder<T> setDefault() {
         this.setMaxCountPerElement(0);
         return this;
      }

      public FastPalette.Builder<T> setMaxCountPerElement(int maxCountPerElement) {
         this.maxCountPerElement = maxCountPerElement;
         return this;
      }

      public FastPalette<T> build() {
         if (this.maxCountPerElement == 0) {
            throw new IllegalStateException();
         } else if (this.maxCountPerElement > 65535) {
            throw new IllegalStateException("the max count must be within 0 - 65535");
         } else {
            Object2IntMap<T> indexHelper = new Object2IntOpenHashMap();
            List<FastPalette.Element<T>> elements = new ArrayList();
            return new FastPalette(indexHelper, elements, this.maxCountPerElement);
         }
      }

      public static <T> FastPalette.Builder<T> begin() {
         return (new FastPalette.Builder()).setDefault();
      }
   }
}
