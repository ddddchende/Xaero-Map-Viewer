package xaero.map.pool;

import java.util.ArrayList;
import java.util.List;

public abstract class MapPool<T extends PoolUnit> {
   private int maxSize;
   private List<T> units;

   public MapPool(int maxSize) {
      this.maxSize = maxSize;
      this.units = new ArrayList();
   }

   protected T get(Object... args) {
      T unit = null;
      synchronized(this.units) {
         if (!this.units.isEmpty()) {
            unit = this.takeFromPool();
         }
      }

      if (unit == null) {
         return this.construct(args);
      } else {
         unit.create(args);
         return unit;
      }
   }

   public boolean addToPool(T unit) {
      synchronized(this.units) {
         if (this.units.size() < this.maxSize) {
            this.units.add(unit);
            return true;
         } else {
            return false;
         }
      }
   }

   private T takeFromPool() {
      return (PoolUnit)this.units.remove(this.units.size() - 1);
   }

   public int size() {
      return this.units.size();
   }

   protected abstract T construct(Object... var1);
}
