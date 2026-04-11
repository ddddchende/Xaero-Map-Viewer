package xaero.map.graphics;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL15;
import xaero.map.WorldMap;

public class PixelBuffers {
   private static int buffersType = 0;

   private static int innerGenBuffers() {
      switch(buffersType) {
      case 0:
         return GL15.glGenBuffers();
      default:
         return 0;
      }
   }

   public static int glGenBuffers() {
      int attempts = 5;

      int result;
      do {
         result = innerGenBuffers();
         --attempts;
      } while(attempts > 0 && result == 0);

      if (result == 0) {
         WorldMap.LOGGER.error("Failed to generate a PBO after multiple attempts. Likely caused by previous errors from other mods.");
      }

      return result;
   }

   public static void glBindBuffer(int target, int buffer) {
      switch(buffersType) {
      case 0:
         GL15.glBindBuffer(target, buffer);
      default:
      }
   }

   public static void glBufferData(int target, long size, int usage) {
      switch(buffersType) {
      case 0:
         GL15.glBufferData(target, size, usage);
      default:
      }
   }

   public static ByteBuffer glMapBuffer(int target, int access, long length, ByteBuffer old_buffer) {
      switch(buffersType) {
      case 0:
         return GL15.glMapBuffer(target, access, length, old_buffer);
      default:
         return null;
      }
   }

   public static boolean glUnmapBuffer(int target) {
      switch(buffersType) {
      case 0:
         return GL15.glUnmapBuffer(target);
      default:
         return false;
      }
   }

   public static void glDeleteBuffers(int buffer) {
      switch(buffersType) {
      case 0:
         GL15.glDeleteBuffers(buffer);
      default:
      }
   }

   public static void glDeleteBuffers(IntBuffer buffers) {
      switch(buffersType) {
      case 0:
         GL15.glDeleteBuffers(buffers);
      default:
      }
   }

   public static ByteBuffer glMapBuffer(int target, int access) {
      switch(buffersType) {
      case 0:
         return GL15.glMapBuffer(target, access);
      default:
         return null;
      }
   }
}
