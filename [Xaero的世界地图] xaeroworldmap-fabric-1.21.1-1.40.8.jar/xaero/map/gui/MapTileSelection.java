package xaero.map.gui;

public class MapTileSelection {
   private final int startX;
   private final int startZ;
   private int endX;
   private int endZ;

   public MapTileSelection(int startX, int startZ) {
      this.startX = startX;
      this.startZ = startZ;
   }

   public void setEnd(int endX, int endZ) {
      this.endX = endX;
      this.endZ = endZ;
   }

   public int getStartX() {
      return this.startX;
   }

   public int getStartZ() {
      return this.startZ;
   }

   public int getEndX() {
      return this.endX;
   }

   public int getEndZ() {
      return this.endZ;
   }

   public int getLeft() {
      return Math.min(this.startX, this.endX);
   }

   public int getRight() {
      return Math.max(this.startX, this.endX);
   }

   public int getTop() {
      return Math.min(this.startZ, this.endZ);
   }

   public int getBottom() {
      return Math.max(this.startZ, this.endZ);
   }
}
