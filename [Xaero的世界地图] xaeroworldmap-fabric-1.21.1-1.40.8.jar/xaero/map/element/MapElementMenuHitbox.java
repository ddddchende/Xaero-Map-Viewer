package xaero.map.element;

public class MapElementMenuHitbox {
   private int x;
   private int y;
   private int w;
   private int h;

   public MapElementMenuHitbox(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getW() {
      return this.w;
   }

   public int getH() {
      return this.h;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setH(int h) {
      this.h = h;
   }
}
