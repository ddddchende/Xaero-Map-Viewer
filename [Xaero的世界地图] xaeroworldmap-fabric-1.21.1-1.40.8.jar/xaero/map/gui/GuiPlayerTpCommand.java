package xaero.map.gui;

import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_437;
import net.minecraft.class_5244;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.gui.widget.MySmallButton;
import xaero.lib.common.config.option.ConfigOption;
import xaero.lib.common.config.util.ConfigUtils;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.world.MapWorld;

public class GuiPlayerTpCommand extends ScreenBase {
   private MySmallButton confirmButton;
   private class_342 commandFormatTextField;
   private boolean usingDefault;
   private String commandFormat;

   public GuiPlayerTpCommand(class_437 parent, class_437 escape) {
      super(parent, escape, class_2561.method_43471("gui.xaero_wm_player_teleport_command"));
      WorldMapSession session = WorldMapSession.getCurrentSession();
      MapWorld mapWorld = session.getMapProcessor().getMapWorld();
      this.usingDefault = mapWorld.isUsingDefaultPlayerTeleport();
      this.commandFormat = mapWorld.getPlayerTeleportCommandFormat();
      this.canSkipWorldRender = true;
   }

   public void method_25426() {
      super.method_25426();
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      String defaultPlayerTeleportFormat = (String)configManager.getEffective(WorldMapProfiledConfigOptions.DEFAULT_PLAYER_TELEPORT_FORMAT);
      WorldMapSession session = WorldMapSession.getCurrentSession();
      MapWorld mapWorld = session.getMapProcessor().getMapWorld();
      this.commandFormatTextField = new class_342(this.field_22793, this.field_22789 / 2 - 100, this.field_22790 / 7 + 60, 200, 20, class_2561.method_43471("gui.xaero_wm_player_teleport_command"));
      this.commandFormatTextField.method_1852(this.usingDefault ? defaultPlayerTeleportFormat : this.commandFormat);
      this.commandFormatTextField.method_1880(500);
      if (this.usingDefault) {
         this.commandFormatTextField.method_1868(-11184811);
         this.commandFormatTextField.field_22763 = false;
      } else {
         this.commandFormatTextField.method_1863((text) -> {
            this.commandFormat = text;
         });
      }

      this.method_25429(this.commandFormatTextField);
      this.method_37063(this.confirmButton = new MySmallButton(0, this.field_22789 / 2 - 155, this.field_22790 / 6 + 168, class_2561.method_43469("gui.xaero_confirm", new Object[0]), (b) -> {
         if (this.canConfirm()) {
            if (!this.usingDefault && this.commandFormat.equals(defaultPlayerTeleportFormat)) {
               this.usingDefault = true;
            }

            mapWorld.setUseDefaultPlayerTeleport(this.usingDefault);
            mapWorld.setPlayerTeleportCommandFormat(this.commandFormat);
            mapWorld.saveConfig();
            this.goBack();
         }
      }));
      this.method_37063(new MySmallButton(1, this.field_22789 / 2 + 5, this.field_22790 / 6 + 168, class_2561.method_43469("gui.xaero_cancel", new Object[0]), (b) -> {
         this.goBack();
      }));
      this.method_37063(new MySmallButton(202, this.field_22789 / 2 - 75, this.field_22790 / 7 + 20, class_5244.method_32700(class_2561.method_43471("gui.xaero_wm_use_default"), ConfigUtils.getDisplayForBoolean((ConfigOption)null, this.usingDefault)), (b) -> {
         this.usingDefault = !this.usingDefault;
         this.method_25423(this.field_22787, this.field_22789, this.field_22790);
      }));
   }

   public void method_25420(class_332 guiGraphics, int i, int j, float f) {
      super.method_25420(guiGraphics, i, j, f);
      guiGraphics.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 20, 16777215);
      guiGraphics.method_25300(this.field_22793, "{x} {y} {z} {name}", this.field_22789 / 2, this.field_22790 / 7 + 46, -5592406);
   }

   public void method_25394(class_332 guiGraphics, int mouseX, int mouseY, float partial) {
      this.renderEscapeScreen(guiGraphics, mouseX, mouseY, partial);
      super.method_25394(guiGraphics, mouseX, mouseY, partial);
      this.commandFormatTextField.method_25394(guiGraphics, mouseX, mouseY, partial);
   }

   private boolean canConfirm() {
      return this.commandFormat != null && this.commandFormat.length() > 0;
   }

   public void method_25393() {
      super.method_25393();
      this.confirmButton.field_22763 = this.canConfirm();
   }

   public boolean method_25404(int par1, int par2, int par3) {
      if (par1 == 257 && this.commandFormat != null && this.commandFormat.length() > 0) {
         this.confirmButton.method_25348(0.0D, 0.0D);
      }

      return super.method_25404(par1, par2, par3);
   }
}
