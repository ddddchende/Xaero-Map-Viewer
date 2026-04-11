package xaero.map.pool;

import xaero.map.graphics.TextureUpload;
import xaero.map.region.texture.BranchTextureRenderer;

public abstract class TextureUploadPool<T extends TextureUpload> extends MapPool<T> {
   public TextureUploadPool(int maxSize) {
      super(maxSize);
   }

   public static class BranchDownload extends TextureUploadPool<TextureUpload.BranchDownload> {
      public BranchDownload(int maxSize) {
         super(maxSize);
      }

      protected TextureUpload.BranchDownload construct(Object... args) {
         return new TextureUpload.BranchDownload(args);
      }

      public TextureUpload.BranchDownload get(int glTexture, int target, int glPackPbo, int pboOffset) {
         return (TextureUpload.BranchDownload)super.get(new Object[]{glTexture, 0, target, 0, 0, 0, 0, 0, 0L, glPackPbo, pboOffset});
      }
   }

   public static class BranchUpdate extends TextureUploadPool<TextureUpload.BranchUpdate> {
      protected boolean allocate;

      public BranchUpdate(int maxSize, boolean allocate) {
         super(maxSize);
         this.allocate = allocate;
      }

      protected TextureUpload.BranchUpdate construct(Object... args) {
         return new TextureUpload.BranchUpdate(args);
      }

      public TextureUpload.BranchUpdate get(int glTexture, int glPbo, int target, int level, int internalFormat, int width, int height, int border, long pixels_buffer_offset, int format, int type, Integer srcTextureTopLeft, Integer srcTextureTopRight, Integer srcTextureBottomLeft, Integer srcTextureBottomRight, BranchTextureRenderer renderer, int glPackPbo, int pboOffset) {
         return (TextureUpload.BranchUpdate)super.get(new Object[]{glTexture, glPbo, target, level, internalFormat, width, height, border, pixels_buffer_offset, format, type, this.allocate, srcTextureTopLeft, srcTextureTopRight, srcTextureBottomLeft, srcTextureBottomRight, renderer, glPackPbo, pboOffset});
      }
   }

   public static class Compressed extends TextureUploadPool<TextureUpload.Compressed> {
      public Compressed(int maxSize) {
         super(maxSize);
      }

      protected TextureUpload.Compressed construct(Object... args) {
         return new TextureUpload.Compressed(args);
      }

      public TextureUpload.Compressed get(int glTexture, int glPbo, int target, int level, int internalFormat, int width, int height, int border, long pixels_buffer_offset, int dataSize) {
         return (TextureUpload.Compressed)super.get(new Object[]{glTexture, glPbo, target, level, internalFormat, width, height, border, pixels_buffer_offset, dataSize});
      }
   }

   public static class SubsequentNormal extends TextureUploadPool<TextureUpload.SubsequentNormal> {
      public SubsequentNormal(int maxSize) {
         super(maxSize);
      }

      protected TextureUpload.SubsequentNormal construct(Object... args) {
         return new TextureUpload.SubsequentNormal(args);
      }

      public TextureUpload.SubsequentNormal get(int glTexture, int glPbo, int target, int level, int width, int height, int border, long pixels_buffer_offset, int format, int type, int xOffset, int yOffset) {
         return (TextureUpload.SubsequentNormal)super.get(new Object[]{glTexture, glPbo, target, level, -1, width, height, border, pixels_buffer_offset, format, type, xOffset, yOffset});
      }
   }

   public static class Normal extends TextureUploadPool<TextureUpload.Normal> {
      public Normal(int maxSize) {
         super(maxSize);
      }

      protected TextureUpload.Normal construct(Object... args) {
         return new TextureUpload.Normal(args);
      }

      public TextureUpload.Normal get(int glTexture, int glPbo, int target, int level, int internalFormat, int width, int height, int border, long pixels_buffer_offset, int format, int type) {
         return (TextureUpload.Normal)super.get(new Object[]{glTexture, glPbo, target, level, internalFormat, width, height, border, pixels_buffer_offset, format, type});
      }
   }
}
