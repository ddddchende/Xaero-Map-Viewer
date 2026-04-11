package xaero.map.palette;

import java.io.DataOutputStream;
import java.io.IOException;
import xaero.map.misc.ConsistentBitArray;

public class Paletted2DFastBitArrayIntStorage {
   private final FastIntPalette palette;
   private final int width;
   private final int height;
   private ConsistentBitArray data;
   private final int defaultValue;
   private int defaultValueCount;

   private Paletted2DFastBitArrayIntStorage(FastIntPalette palette, int defaultValue, int width, int height, int defaultValueCount, ConsistentBitArray data) {
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

   public synchronized int get(int x, int y) {
      this.checkRange(x, y);
      int index = this.getIndex(x, y);
      int paletteIndex = this.data.get(index);
      return paletteIndex == 0 ? this.defaultValue : this.palette.get(paletteIndex - 1, this.defaultValue);
   }

   public synchronized int getRaw(int x, int y) {
      this.checkRange(x, y);
      int index = this.getIndex(x, y);
      return this.data.get(index);
   }

   public synchronized void set(int x, int y, int value) {
      this.checkRange(x, y);
      int index = this.getIndex(x, y);
      int currentPaletteIndex = this.data.get(index);
      int newPaletteIndex = 0;
      if (currentPaletteIndex > 0) {
         newPaletteIndex = this.palette.getIndex(value) + 1;
         if (newPaletteIndex == currentPaletteIndex) {
            return;
         }

         int replacedValueCount = this.palette.count(currentPaletteIndex - 1, false);
         if (replacedValueCount == 0) {
            this.palette.remove(currentPaletteIndex - 1);
         }
      } else {
         --this.defaultValueCount;
      }

      if (value != this.defaultValue) {
         if (newPaletteIndex == 0) {
            newPaletteIndex = this.palette.add(value) + 1;
         }

         this.palette.count(newPaletteIndex - 1, true);
      } else {
         ++this.defaultValueCount;
      }

      this.data.set(index, newPaletteIndex);
   }

   public void writeData(DataOutputStream output) throws IOException {
      this.data.write(output);
   }

   public boolean contains(int value) {
      return this.palette.getIndex(value) != -1;
   }

   public int getPaletteSize() {
      return this.palette.getSize();
   }

   public int getPaletteNonNullCount() {
      return this.palette.getNonNullCount();
   }

   public int getPaletteElement(int index) {
      return this.palette.get(index, this.defaultValue);
   }

   public int getPaletteElementCount(int index) {
      return this.palette.getCount(index);
   }

   public int getDefaultValueCount() {
      return this.defaultValueCount;
   }

   public String getBiomePaletteDebug() {
      String biomePaletteLine = this.defaultValueCount + " / ";

      for(int i = 0; i < this.palette.getSize(); ++i) {
         if (i > 0) {
            biomePaletteLine = biomePaletteLine + ", ";
         }

         int paletteElement = this.palette.get(i, -1);
         biomePaletteLine = biomePaletteLine + paletteElement + ":" + (paletteElement == -1 ? 0 : this.palette.getCount(i));
      }

      return biomePaletteLine;
   }

   public static final class Builder {
      private int width;
      private int height;
      private int maxPaletteElements;
      private int defaultValue;
      private FastIntPalette palette;
      private ConsistentBitArray data;
      private int defaultValueCount;

      private Builder() {
      }

      public Paletted2DFastBitArrayIntStorage.Builder setDefault() {
         this.setWidth(0);
         this.setHeight(0);
         this.setDefaultValue(-1);
         this.setMaxPaletteElements(0);
         this.setPalette((FastIntPalette)null);
         this.setData((ConsistentBitArray)null);
         this.setDefaultValueCount(Integer.MIN_VALUE);
         return this;
      }

      public Paletted2DFastBitArrayIntStorage.Builder setWidth(int width) {
         this.width = width;
         return this;
      }

      public Paletted2DFastBitArrayIntStorage.Builder setHeight(int height) {
         this.height = height;
         return this;
      }

      public Paletted2DFastBitArrayIntStorage.Builder setMaxPaletteElements(int maxPaletteElements) {
         this.maxPaletteElements = maxPaletteElements;
         return this;
      }

      public Paletted2DFastBitArrayIntStorage.Builder setDefaultValue(int defaultValue) {
         this.defaultValue = defaultValue;
         return this;
      }

      public Paletted2DFastBitArrayIntStorage.Builder setPalette(FastIntPalette palette) {
         this.palette = palette;
         return this;
      }

      public Paletted2DFastBitArrayIntStorage.Builder setData(ConsistentBitArray data) {
         this.data = data;
         return this;
      }

      public Paletted2DFastBitArrayIntStorage.Builder setDefaultValueCount(int defaultValueCount) {
         this.defaultValueCount = defaultValueCount;
         return this;
      }

      public Paletted2DFastBitArrayIntStorage build() {
         if (this.width != 0 && this.height != 0 && this.maxPaletteElements != 0) {
            if (this.palette == null) {
               this.palette = FastIntPalette.Builder.begin().setMaxCountPerElement(this.width * this.height).build();
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
                  return new Paletted2DFastBitArrayIntStorage(this.palette, this.defaultValue, this.width, this.height, this.defaultValueCount, this.data);
               }
            }
         } else {
            throw new IllegalStateException();
         }
      }

      public static <T> Paletted2DFastBitArrayIntStorage.Builder begin() {
         return (new Paletted2DFastBitArrayIntStorage.Builder()).setDefault();
      }
   }
}
