package xaero.map.gui;

import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import xaero.lib.client.gui.config.EditStringConfigOptionScreen;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.option.ConfigOption;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.mods.SupportMods;

public class EditDefaultTpCommandScreen extends EditStringConfigOptionScreen<String> {
   private class_2561 waypointCommandHint = class_2561.method_43471("gui.xaero_wm_teleport_command_waypoints_hint");
   private boolean playerFormat;
   private boolean dimensionFormat;

   public EditDefaultTpCommandScreen(class_437 parent, class_437 escape, Config config, Config enforcedConfig, ConfigOption<String> option, boolean allowEmpty, boolean emptyMeansNull, Runnable postConfirmAction) {
      super(parent, escape, config, enforcedConfig, option, allowEmpty, emptyMeansNull, postConfirmAction);
      this.playerFormat = option == WorldMapProfiledConfigOptions.DEFAULT_PLAYER_TELEPORT_FORMAT;
      this.dimensionFormat = option == WorldMapProfiledConfigOptions.DEFAULT_MAP_TELEPORT_DIMENSION_FORMAT;
   }

   public void method_25420(class_332 guiGraphics, int mouseX, int mouseY, float partial) {
      super.method_25420(guiGraphics, mouseX, mouseY, partial);
      if (SupportMods.minimap()) {
         guiGraphics.method_27534(this.field_22793, this.waypointCommandHint, this.field_22789 / 2, this.field_22790 / 7 + 61, -5592406);
      }

      String hint = "{x} {y} {z}";
      if (this.playerFormat) {
         hint = hint + " {name}";
      }

      if (this.dimensionFormat) {
         hint = hint + " {d}";
      }

      guiGraphics.method_25303(this.field_22793, hint, this.field_22789 / 2 + 105, this.field_22790 / 7 + 33, -5592406);
   }
}
