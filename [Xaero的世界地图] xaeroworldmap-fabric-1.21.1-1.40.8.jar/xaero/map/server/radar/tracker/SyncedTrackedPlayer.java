package xaero.map.server.radar.tracker;

import java.util.UUID;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_5321;

public class SyncedTrackedPlayer {
   private final UUID id;
   private double x;
   private double y;
   private double z;
   private class_5321<class_1937> dimension;

   public SyncedTrackedPlayer(UUID id, double x, double y, double z, class_5321<class_1937> dimension) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.z = z;
      this.dimension = dimension;
   }

   public SyncedTrackedPlayer setPos(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
      return this;
   }

   public SyncedTrackedPlayer setDimension(class_5321<class_1937> dimension) {
      this.dimension = dimension;
      return this;
   }

   public UUID getId() {
      return this.id;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public class_5321<class_1937> getDimension() {
      return this.dimension;
   }

   public boolean matchesEnough(class_1657 player, double maxAxisDistance) {
      return Math.abs(player.method_23317() - this.x) <= maxAxisDistance && Math.abs(player.method_23318() - this.y) <= maxAxisDistance && Math.abs(player.method_23321() - this.z) <= maxAxisDistance && player.method_37908().method_27983().method_29177().equals(this.dimension);
   }

   public void update(class_1657 player) {
      this.setPos(player.method_23317(), player.method_23318(), player.method_23321()).setDimension(player.method_37908().method_27983());
   }

   public void copyFrom(SyncedTrackedPlayer trackedPlayer) {
      this.setPos(trackedPlayer.getX(), trackedPlayer.getY(), trackedPlayer.getZ()).setDimension(trackedPlayer.getDimension());
   }
}
