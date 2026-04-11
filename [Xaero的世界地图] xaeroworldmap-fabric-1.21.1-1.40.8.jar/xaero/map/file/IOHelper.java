package xaero.map.file;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

public class IOHelper {
   public static void readToBuffer(byte[] buffer, int count, DataInputStream input) throws IOException {
      int readCount;
      for(int currentTotal = 0; currentTotal < count; currentTotal += readCount) {
         readCount = input.read(buffer, currentTotal, count - currentTotal);
         if (readCount == -1) {
            throw new EOFException();
         }
      }

   }
}
