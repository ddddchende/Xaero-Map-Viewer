package xaero.map.palette;

import xaero.map.misc.ConsistentBitArray;

public class Paletted2DFastBitArrayStorage<T> {
   private final FastPalette<T> palette;
   private final int width;
   private final int height;
   private ConsistentBitArray data;
   private final T defaultValue;
   private int defaultValueCount;

   private Paletted2DFastBitArrayStorage(FastPalette<T> palette, T defaultValue, int width, int height, int defaultValueCount, ConsistentBitArray data) {
      this.palette = palette;
      this.defaultValue = defaultValue;
      this.width = width;
      this.height = height;
      this.data = data;
      this.defaultValueCount = defaultValueCount;
   }

   private void checkRange(int x, int y) {
      if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
         throw new IllegalArgumentException("out of bounds! (x: " + x + "; y: " + y + ") (w: " + this.width + "; h: " + this.height + ")");
      }
   }

   private int getIndex(int x, int y) {
      return y * this.width + x;
   }

   public synchronized T get(int x, int y) {
      this.checkRange(x, y);
      int index = this.getIndex(x, y);
      int paletteIndex = this.data.get(index);
      return paletteIndex == 0 ? this.defaultValue : this.palette.get(paletteIndex - 1);
   }

   public synchronized void set(int x, int y, T object) {
      this.checkRange(x, y);
      int index = this.getIndex(x, y);
      int currentPaletteIndex = this.data.get(index);
      int newPaletteIndex = 0;
      if (currentPaletteIndex > 0) {
         newPaletteIndex = this.palette.getIndex(object) + 1;
         if (newPaletteIndex == currentPaletteIndex) {
            return;
         }

         int replacedObjectCount = this.palette.count(currentPaletteIndex - 1, false);
         if (replacedObjectCount == 0) {
            this.palette.remove(currentPaletteIndex - 1);
         }
      } else {
         --this.defaultValueCount;
      }

      if (object != this.defaultValue) {
         if (newPaletteIndex == 0) {
            newPaletteIndex = this.palette.add(object) + 1;
         }

         this.palette.count(newPaletteIndex - 1, true);
      } else {
         ++this.defaultValueCount;
      }

      this.data.set(index, newPaletteIndex);
   }

   public boolean contains(T object) {
      return this.palette.getIndex(object) != -1;
   }

   public int getPaletteSize() {
      return this.palette.getSize();
   }

   public int getPaletteNonNullCount() {
      return this.palette.getNonNullCount();
   }

   public T getPaletteElement(int index) {
      return this.palette.get(index);
   }

   public int getPaletteElementCount(int index) {
      return this.palette.getCount(index);
   }

   public int getDefaultValueCount() {
      return this.defaultValueCount;
   }

   public static final class Builder<T> {
      private int width;
      private int height;
      private int maxPaletteElements;
      private T defaultValue;
      private FastPalette<T> palette;
      private ConsistentBitArray data;
      private int defaultValueCount;

      private Builder() {
      }

      public Paletted2DFastBitArrayStorage.Builder<T> setDefault() {
         this.setWidth(0);
         this.setHeight(0);
         this.setDefaultValue((Object)null);
         this.setMaxPaletteElements(0);
         this.setPalette((FastPalette)null);
         this.setData((ConsistentBitArray)null);
         this.setDefaultValueCount(Integer.MIN_VALUE);
         return this;
      }

      public Paletted2DFastBitArrayStorage.Builder<T> setWidth(int width) {
         this.width = width;
         return this;
      }

      public Paletted2DFastBitArrayStorage.Builder<T> setHeight(int height) {
         this.height = height;
         return this;
      }

      public Paletted2DFastBitArrayStorage.Builder<T> setMaxPaletteElements(int maxPaletteElements) {
         this.maxPaletteElements = maxPaletteElements;
         return this;
      }

      public Paletted2DFastBitArrayStorage.Builder<T> setDefaultValue(T defaultValue) {
         this.defaultValue = defaultValue;
         return this;
      }

      public Paletted2DFastBitArrayStorage.Builder<T> setPalette(FastPalette<T> palette) {
         this.palette = palette;
         return this;
      }

      public Paletted2DFastBitArrayStorage.Builder<T> setData(ConsistentBitArray data) {
         this.data = data;
         return this;
      }

      public Paletted2DFastBitArrayStorage.Builder<T> setDefaultValueCount(int defaultValueCount) {
         this.defaultValueCount = defaultValueCount;
         return this;
      }

      public Paletted2DFastBitArrayStorage<T> build() {
         if (this.width != 0 && this.height != 0 && this.maxPaletteElements != 0) {
            if (this.palette == null) {
               this.palette = FastPalette.Builder.begin().setMaxCountPerElement(this.width * this.height).build();
            }

            int bitsPerEntry = (int)Math.ceil(Math.log((double)(this.maxPaletteElements + 1)) / Math.log(2.0D));
            if (this.data == null) {
               this.data = new ConsistentBitArray(bitsPerEntry, this.width * this.height);
            }

            if (this.data.getBitsPerEntry() != bitsPerEntry) {
               throw new IllegalStateException();
            } else {
               if (this.defaultValueCount == Integer.MIN_VALUE) {
                  this.defaultValueCount = this.width * this.height;
               }

               if (this.defaultValueCount < 0) {
                  throw new IllegalStateException();
               } else {
                  return new Paletted2DFastBitArrayStorage(this.palette, this.defaultValue, this.width, this.height, this.defaultValueCount, this.data);
               }
            }
         } else {
            throw new IllegalStateException();
         }
      }

      public static <T> Paletted2DFastBitArrayStorage.Builder<T> begin() {
         return (new Paletted2DFastBitArrayStorage.Builder()).setDefault();
      }
   }
}
