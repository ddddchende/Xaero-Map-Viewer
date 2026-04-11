package xaero.map.graphics;

import com.mojang.blaze3d.platform.GlStateManager;
import java.nio.ByteBuffer;
import net.minecraft.class_310;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import xaero.map.exception.OpenGLException;
import xaero.map.pool.PoolUnit;
import xaero.map.region.texture.BranchTextureRenderer;

public abstract class TextureUpload implements PoolUnit {
   protected int glTexture;
   private int glUnpackPbo;
   private int target;
   private int level;
   private int internalFormat;
   private int width;
   private int height;
   private int border;
   private long pixels_buffer_offset;
   private int uploadType;

   public void create(Object... args) {
      this.glTexture = (Integer)args[0];
      this.glUnpackPbo = (Integer)args[1];
      this.target = (Integer)args[2];
      this.level = (Integer)args[3];
      this.internalFormat = (Integer)args[4];
      this.width = (Integer)args[5];
      this.height = (Integer)args[6];
      this.border = (Integer)args[7];
      this.pixels_buffer_offset = (Long)args[8];
   }

   public void run() throws OpenGLException {
      GlStateManager._bindTexture(this.glTexture);
      OpenGLException.checkGLError(false, "preparing to upload a map texture");
      PixelBuffers.glBindBuffer(35052, this.glUnpackPbo);
      this.upload();
      OpenGLException.checkGLError();
      PixelBuffers.glBindBuffer(35052, 0);
      GlStateManager._bindTexture(0);
      OpenGLException.checkGLError();
   }

   abstract void upload() throws OpenGLException;

   public int getUploadType() {
      return this.uploadType;
   }

   public static class BranchDownload extends TextureUpload {
      private int glPackPbo;
      private int pboOffset;

      public BranchDownload(int uploadType) {
         super.uploadType = uploadType;
      }

      public BranchDownload(Object... args) {
         this(5);
         this.create(args);
      }

      void upload() throws OpenGLException {
         if (this.glPackPbo != 0) {
            PixelBuffers.glBindBuffer(35051, this.glPackPbo);
            int target = super.target;
            GL11.glGetTexImage(target, 0, 32993, 32821, (long)this.pboOffset);
            PixelBuffers.glBindBuffer(35051, 0);
            OpenGLException.checkGLError();
         }
      }

      public void create(Object... args) {
         super.create(args);
         this.glPackPbo = (Integer)args[9];
         this.pboOffset = (Integer)args[10];
      }
   }

   public static class BranchUpdate extends TextureUpload {
      private int format;
      private int type;
      private boolean allocate;
      private Integer srcTextureTopLeft;
      private Integer srcTextureTopRight;
      private Integer srcTextureBottomLeft;
      private Integer srcTextureBottomRight;
      private BranchTextureRenderer renderer;
      private int glPackPbo;
      private int pboOffset;

      public BranchUpdate(int uploadType) {
         super.uploadType = uploadType;
      }

      public BranchUpdate(Object... args) {
         this((Boolean)args[11] ? 4 : 3);
         this.create(args);
      }

      void upload() throws OpenGLException {
         if (this.allocate) {
            GL11.glTexImage2D(super.target, super.level, super.internalFormat, super.width, super.height, 0, this.format, this.type, (ByteBuffer)null);
            OpenGLException.checkGLError();
         }

         this.renderer.render(this.glTexture, this.srcTextureTopLeft, this.srcTextureTopRight, this.srcTextureBottomLeft, this.srcTextureBottomRight, class_310.method_1551().method_1522(), this.allocate);
         GlStateManager._bindTexture(this.glTexture);
         PixelBuffers.glBindBuffer(35051, this.glPackPbo);
         if (this.glPackPbo != 0) {
            int target = super.target;
            GL11.glGetTexImage(target, 0, 32993, 32821, (long)this.pboOffset);
            PixelBuffers.glBindBuffer(35051, 0);
            OpenGLException.checkGLError();
         }
      }

      public void create(Object... args) {
         super.create(args);
         this.format = (Integer)args[9];
         this.type = (Integer)args[10];
         this.allocate = (Boolean)args[11];
         this.srcTextureTopLeft = (Integer)args[12];
         this.srcTextureTopRight = (Integer)args[13];
         this.srcTextureBottomLeft = (Integer)args[14];
         this.srcTextureBottomRight = (Integer)args[15];
         this.renderer = (BranchTextureRenderer)args[16];
         this.glPackPbo = (Integer)args[17];
         this.pboOffset = (Integer)args[18];
      }
   }

   public static class Compressed extends TextureUpload {
      private int dataSize;

      public Compressed(Object... args) {
         this.create(args);
         super.uploadType = 2;
      }

      void upload() throws OpenGLException {
         GL13.glCompressedTexImage2D(super.target, super.level, super.internalFormat, super.width, super.height, super.border, this.dataSize, super.pixels_buffer_offset);
      }

      public void create(Object... args) {
         super.create(args);
         this.dataSize = (Integer)args[9];
      }
   }

   public static class SubsequentNormal extends TextureUpload {
      private int format;
      private int type;
      private int xOffset;
      private int yOffset;

      public SubsequentNormal(int uploadType) {
         super.uploadType = uploadType;
      }

      public SubsequentNormal(Object... args) {
         this(6);
         this.create(args);
      }

      void upload() throws OpenGLException {
         GL11.glTexSubImage2D(super.target, super.level, this.xOffset, this.yOffset, super.width, super.height, this.format, this.type, super.pixels_buffer_offset);
         OpenGLException.checkGLError();
      }

      public void create(Object... args) {
         super.create(args);
         this.format = (Integer)args[9];
         this.type = (Integer)args[10];
         this.xOffset = (Integer)args[11];
         this.yOffset = (Integer)args[12];
      }
   }

   public static class Normal extends TextureUpload {
      private int format;
      private int type;

      public Normal(int uploadType) {
         super.uploadType = uploadType;
      }

      public Normal(Object... args) {
         this(0);
         this.create(args);
      }

      void upload() throws OpenGLException {
         GL11.glHint(34031, 4354);
         OpenGLException.checkGLError();
         GL11.glTexImage2D(super.target, super.level, super.internalFormat, super.width, super.height, 0, this.format, this.type, super.pixels_buffer_offset);
         OpenGLException.checkGLError(false, "uploading a map texture");
      }

      public void create(Object... args) {
         super.create(args);
         this.format = (Integer)args[9];
         this.type = (Integer)args[10];
      }
   }
}
