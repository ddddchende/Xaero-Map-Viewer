package xaero.map.pool.buffer;

import xaero.map.pool.MapPool;

public class TextureDirectBufferPool extends MapPool<PoolTextureDirectBufferUnit> {
   public TextureDirectBufferPool() {
      super(4096);
   }

   protected PoolTextureDirectBufferUnit construct(Object... args) {
      return new PoolTextureDirectBufferUnit(args);
   }

   public PoolTextureDirectBufferUnit get(boolean zeroFillIfReused) {
      return (PoolTextureDirectBufferUnit)super.get(zeroFillIfReused);
   }
}
