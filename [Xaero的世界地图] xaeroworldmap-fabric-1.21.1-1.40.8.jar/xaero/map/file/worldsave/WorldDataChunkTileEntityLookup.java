package xaero.map.file.worldsave;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.class_2487;
import net.minecraft.class_2499;

public class WorldDataChunkTileEntityLookup {
   private class_2499 tileEntitiesNbt;
   private Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<class_2487>>> tileEntities;

   public WorldDataChunkTileEntityLookup(class_2499 tileEntitiesNbt) {
      this.tileEntitiesNbt = tileEntitiesNbt;
   }

   private void loadIfNeeded() {
      if (this.tileEntities == null) {
         this.tileEntities = new Int2ObjectOpenHashMap();
         this.tileEntitiesNbt.forEach((tag) -> {
            if (tag instanceof class_2487) {
               class_2487 compoundNbt = (class_2487)tag;
               if (!compoundNbt.method_10573("x", 99)) {
                  return;
               }

               int x = compoundNbt.method_10550("x") & 15;
               if (!compoundNbt.method_10573("y", 99)) {
                  return;
               }

               int y = compoundNbt.method_10550("y");
               if (!compoundNbt.method_10573("z", 99)) {
                  return;
               }

               int z = compoundNbt.method_10550("z") & 15;
               Int2ObjectMap<Int2ObjectMap<class_2487>> byX = (Int2ObjectMap)this.tileEntities.get(x);
               if (byX == null) {
                  this.tileEntities.put(x, byX = new Int2ObjectOpenHashMap());
               }

               Int2ObjectMap<class_2487> byY = (Int2ObjectMap)((Int2ObjectMap)byX).get(y);
               if (byY == null) {
                  ((Int2ObjectMap)byX).put(y, byY = new Int2ObjectOpenHashMap());
               }

               ((Int2ObjectMap)byY).put(z, compoundNbt);
            }

         });
         this.tileEntitiesNbt = null;
      }

   }

   public class_2487 getTileEntityNbt(int x, int y, int z) {
      this.loadIfNeeded();
      Int2ObjectMap<Int2ObjectMap<class_2487>> byX = (Int2ObjectMap)this.tileEntities.get(x);
      if (byX == null) {
         return null;
      } else {
         Int2ObjectMap<class_2487> byY = (Int2ObjectMap)byX.get(y);
         return byY == null ? null : (class_2487)byY.get(z);
      }
   }
}
