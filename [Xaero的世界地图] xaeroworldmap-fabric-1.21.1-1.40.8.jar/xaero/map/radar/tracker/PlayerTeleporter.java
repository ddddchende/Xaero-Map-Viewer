package xaero.map.radar.tracker;

import net.minecraft.class_310;
import net.minecraft.class_437;
import xaero.map.WorldMap;
import xaero.map.world.MapWorld;

public class PlayerTeleporter {
   public void teleport(class_437 screen, MapWorld mapWorld, String name, int x, int y, int z) {
      class_310.method_1551().method_1507((class_437)null);
      String tpCommand = mapWorld.getEffectivePlayerTeleportCommandFormat();
      tpCommand = tpCommand.replace("{name}", name).replace("{x}", x.makeConcatWithConstants<invokedynamic>(x)).replace("{y}", y.makeConcatWithConstants<invokedynamic>(y)).replace("{z}", z.makeConcatWithConstants<invokedynamic>(z));
      class_310 mc = class_310.method_1551();
      if (tpCommand.startsWith("/")) {
         tpCommand = tpCommand.substring(1);
         if (!mc.field_1724.field_3944.method_45731(tpCommand)) {
            mc.field_1724.field_3944.method_45730(tpCommand);
         }
      } else {
         mc.field_1724.field_3944.method_45729(tpCommand);
      }

   }

   public void teleportToPlayer(class_437 screen, MapWorld mapWorld, PlayerTrackerMapElement<?> target) {
      this.teleport(screen, mapWorld, WorldMap.trackedPlayerRenderer.getReader().getMenuName(target), (int)Math.floor(target.getX()), (int)Math.floor(target.getY()), (int)Math.floor(target.getZ()));
   }
}
