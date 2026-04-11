package xaero.map.teleport;

import net.minecraft.class_1074;
import net.minecraft.class_124;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_5321;
import xaero.lib.client.config.ClientConfigManager;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.world.MapConnectionNode;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

public class MapTeleporter {
   public void teleport(class_437 screen, MapWorld mapWorld, int x, int y, int z, class_5321<class_1937> d) {
      class_310.method_1551().method_1507((class_437)null);
      if (class_310.method_1551().field_1761.method_2908()) {
         MapDimension destinationDim = mapWorld.getDimension(d != null ? d : class_310.method_1551().field_1687.method_27983());
         MapConnectionNode playerMapKey = mapWorld.getPlayerMapKey();
         if (playerMapKey == null) {
            class_5250 messageComponent = class_2561.method_43470(class_1074.method_4662("gui.xaero_wm_teleport_never_confirmed", new Object[0]));
            messageComponent.method_10862(messageComponent.method_10866().method_10977(class_124.field_1061));
            class_310.method_1551().field_1705.method_1743().method_1812(messageComponent);
            return;
         }

         MapConnectionNode destinationMapKey = destinationDim == null ? null : destinationDim.getSelectedMapKeyUnsynced();
         if (!mapWorld.getMapConnections().isConnected(playerMapKey, destinationMapKey)) {
            class_5250 messageComponent = class_2561.method_43470(class_1074.method_4662("gui.xaero_wm_teleport_not_connected", new Object[0]));
            messageComponent.method_10862(messageComponent.method_10866().method_10977(class_124.field_1061));
            class_310.method_1551().field_1705.method_1743().method_1812(messageComponent);
            return;
         }
      }

      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      boolean partialYTeleportConfig = (Boolean)configManager.getEffective(WorldMapProfiledConfigOptions.PARTIAL_Y_TELEPORT);
      String tpCommand = d == null ? mapWorld.getEffectiveTeleportCommandFormat() : mapWorld.getEffectiveDimensionTeleportCommandFormat();
      String yString = y == 32767 ? "~" : (partialYTeleportConfig ? ((double)y + 0.5D).makeConcatWithConstants<invokedynamic>((double)y + 0.5D) : y.makeConcatWithConstants<invokedynamic>(y));
      tpCommand = tpCommand.replace("{x}", x.makeConcatWithConstants<invokedynamic>(x)).replace("{y}", yString).replace("{z}", z.makeConcatWithConstants<invokedynamic>(z));
      if (d != null) {
         tpCommand = tpCommand.replace("{d}", d.method_29177().toString());
      }

      class_310 mc = class_310.method_1551();
      if (tpCommand.startsWith("/")) {
         tpCommand = tpCommand.substring(1);
         if (!mc.field_1724.field_3944.method_45731(tpCommand)) {
            mc.field_1724.field_3944.method_45730(tpCommand);
         }
      } else {
         mc.field_1724.field_3944.method_45729(tpCommand);
      }

      mapWorld.setCustomDimensionId((class_5321)null);
      mapWorld.getMapProcessor().checkForWorldUpdate();
   }
}
