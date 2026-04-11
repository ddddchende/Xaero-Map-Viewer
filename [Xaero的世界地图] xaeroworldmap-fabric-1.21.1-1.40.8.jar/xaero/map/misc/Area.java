package xaero.map.misc;

public class Area {
   private final int left;
   private final int top;
   private final int right;
   private final int bottom;

   public Area(int left, int top, int right, int bottom) {
      this.left = left;
      this.top = top;
      this.right = right;
      this.bottom = bottom;
   }

   public int getLeft() {
      return this.left;
   }

   public int getTop() {
      return this.top;
   }

   public int getRight() {
      return this.right;
   }

   public int getBottom() {
      return this.bottom;
   }

   public int hashCode() {
      int hash = this.left;
      hash = hash * 37 + this.top;
      hash = hash * 37 + this.right;
      hash = hash * 37 + this.bottom;
      hash = hash * 37 + this.right;
      hash = hash * 37 + this.top;
      hash = hash * 37 + this.left;
      return hash;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof Area)) {
         return false;
      } else {
         Area other = (Area)obj;
         return this.left == other.left && this.top == other.top && this.right == other.right && this.bottom == other.bottom;
      }
   }
}
