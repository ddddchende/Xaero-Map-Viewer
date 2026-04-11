package xaero.map.misc;

import java.io.DataOutputStream;
import java.io.IOException;

public class ConsistentBitArray {
   private int insideALong;
   private int bitsPerEntry;
   private int entries;
   private long[] data;
   private int entryMask;

   public ConsistentBitArray(int bitsPerEntry, int entries) {
      this(bitsPerEntry, entries, (long[])null);
   }

   public ConsistentBitArray(int bitsPerEntry, int entries, long[] data) {
      if (bitsPerEntry > 32) {
         throw new RuntimeException("Entry size too big for int! " + bitsPerEntry);
      } else {
         this.insideALong = 64 / bitsPerEntry;
         int longs = (entries + this.insideALong - 1) / this.insideALong;
         if (data != null) {
            if (data.length != longs) {
               throw new RuntimeException("Incorrect data length: " + data.length + " VS " + longs);
            }

            this.data = data;
         } else {
            this.data = new long[longs];
         }

         this.bitsPerEntry = bitsPerEntry;
         this.entries = entries;
         this.entryMask = (1 << bitsPerEntry) - 1;
      }
   }

   public int get(int index) {
      if (index >= this.entries) {
         throw new ArrayIndexOutOfBoundsException(index);
      } else {
         int longIndex = index / this.insideALong;
         int insideIndex = index % this.insideALong;
         return (int)(this.data[longIndex] >> insideIndex * this.bitsPerEntry & (long)this.entryMask);
      }
   }

   public void set(int index, int value) {
      if (index >= this.entries) {
         throw new ArrayIndexOutOfBoundsException(index);
      } else {
         int longIndex = index / this.insideALong;
         int insideIndex = index % this.insideALong;
         long currentLong = this.data[longIndex];
         int offset = insideIndex * this.bitsPerEntry;
         long shiftedMask = (long)this.entryMask << offset;
         long shiftedValue = (long)(value & this.entryMask) << offset;
         this.data[longIndex] = currentLong & ~shiftedMask | shiftedValue;
      }
   }

   public void write(DataOutputStream output) throws IOException {
      for(int i = 0; i < this.data.length; ++i) {
         output.writeLong(this.data[i]);
      }

   }

   public long[] getData() {
      return this.data;
   }

   public void setData(long[] data) {
      this.data = data;
   }

   public int getBitsPerEntry() {
      return this.bitsPerEntry;
   }
}
