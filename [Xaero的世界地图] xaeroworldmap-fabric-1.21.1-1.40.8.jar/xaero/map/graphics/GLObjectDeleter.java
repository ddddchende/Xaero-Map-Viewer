package xaero.map.graphics;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class GLObjectDeleter {
   private static final int DELETE_PER_FRAME = 5;
   private static ByteBuffer buffer = BufferUtils.createByteBuffer(20);
   private static IntBuffer bufferIntView;
   private ArrayList<Integer> texturesToDelete = new ArrayList();
   private ArrayList<Integer> buffersToDelete = new ArrayList();

   public void work() {
      int i;
      if (!this.texturesToDelete.isEmpty()) {
         do {
            synchronized(this.texturesToDelete) {
               bufferIntView.clear();

               for(i = 0; i < 5 && !this.texturesToDelete.isEmpty(); ++i) {
                  bufferIntView.put((Integer)this.texturesToDelete.remove(this.texturesToDelete.size() - 1));
               }

               bufferIntView.flip();
               GL11.glDeleteTextures(bufferIntView);
            }
         } while(this.texturesToDelete.size() > 640);
      }

      if (!this.buffersToDelete.isEmpty()) {
         do {
            synchronized(this.buffersToDelete) {
               bufferIntView.clear();

               for(i = 0; i < 5 && !this.buffersToDelete.isEmpty(); ++i) {
                  bufferIntView.put((Integer)this.buffersToDelete.remove(this.buffersToDelete.size() - 1));
               }

               bufferIntView.flip();
               PixelBuffers.glDeleteBuffers(bufferIntView);
            }
         } while(this.buffersToDelete.size() > 640);
      }

   }

   public void requestTextureDeletion(int texture) {
      synchronized(this.texturesToDelete) {
         this.texturesToDelete.add(texture);
      }
   }

   public void requestBufferToDelete(int bufferId) {
      synchronized(this.buffersToDelete) {
         this.buffersToDelete.add(bufferId);
      }
   }

   static {
      bufferIntView = buffer.asIntBuffer();
   }
}
