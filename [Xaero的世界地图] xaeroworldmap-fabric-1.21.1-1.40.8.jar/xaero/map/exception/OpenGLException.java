package xaero.map.exception;

import org.lwjgl.opengl.GL11;
import xaero.map.WorldMap;

public class OpenGLException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   public OpenGLException(int error) {
      super("OpenGL error: " + error);
   }

   public static void checkGLError() throws OpenGLException {
      checkGLError(true, (String)null);
   }

   public static void checkGLError(boolean crash, String where) throws OpenGLException {
      int error = GL11.glGetError();
      if (error != 0) {
         if (crash) {
            throw new OpenGLException(error);
         }

         WorldMap.LOGGER.warn("Ignoring OpenGL error " + error + " when " + where + ". Most likely caused by another mod.");
      }

   }
}
