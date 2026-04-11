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
import xaero.map.mods.SupportMods;
import xaero.map.world.MapWorld;

public class GuiMapTpCommand extends ScreenBase {
   private MySmallButton confirmButton;
   private class_342 commandFormatTextField;
   private class_342 dimensionCommandFormatTextField;
   private boolean usingDefault;
   private String commandFormat;
   private String dimensionCommandFormat;
   private class_2561 waypointCommandHint = class_2561.method_43471("gui.xaero_wm_teleport_command_waypoints_hint");

   public GuiMapTpCommand(class_437 parent, class_437 escape) {
      super(parent, escape, class_2561.method_43471("gui.xaero_wm_teleport_command"));
      WorldMapSession session = WorldMapSession.getCurrentSession();
      MapWorld mapWorld = session.getMapProcessor().getMapWorld();
      this.usingDefault = mapWorld.isUsingDefaultMapTeleport();
      this.commandFormat = mapWorld.getTeleportCommandFormat();
      this.dimensionCommandFormat = mapWorld.getDimensionTeleportCommandFormat();
      this.canSkipWorldRender = true;
   }

   public void method_25426() {
      super.method_25426();
      WorldMapSession session = WorldMapSession.getCurrentSession();
      MapWorld mapWorld = session.getMapProcessor().getMapWorld();
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      String defaultMapTeleportFormat = (String)configManager.getEffective(WorldMapProfiledConfigOptions.DEFAULT_MAP_TELEPORT_FORMAT);
      String defaultMapTeleportDimensionFormat = (String)configManager.getEffective(WorldMapProfiledConfigOptions.DEFAULT_MAP_TELEPORT_DIMENSION_FORMAT);
      this.commandFormatTextField = new class_342(this.field_22793, this.field_22789 / 2 - 100, this.field_22790 / 7 + 60, 200, 20, class_2561.method_43471("gui.xaero_wm_teleport_command"));
      this.commandFormatTextField.method_1880(500);
      this.commandFormatTextField.method_1852(this.usingDefault ? defaultMapTeleportFormat : this.commandFormat);
      this.dimensionCommandFormatTextField = new class_342(this.field_22793, this.field_22789 / 2 - 100, this.field_22790 / 7 + 90, 200, 20, class_2561.method_43471("gui.xaero_wm_dimension_teleport_command"));
      this.dimensionCommandFormatTextField.method_1880(500);
      this.dimensionCommandFormatTextField.method_1852(this.usingDefault ? defaultMapTeleportDimensionFormat : this.dimensionCommandFormat);
      if (this.usingDefault) {
         this.commandFormatTextField.field_22763 = this.dimensionCommandFormatTextField.field_22763 = false;
         this.commandFormatTextField.method_1868(-11184811);
         this.dimensionCommandFormatTextField.method_1868(-11184811);
      } else {
         this.commandFormatTextField.method_1863((text) -> {
            this.commandFormat = text;
         });
         this.dimensionCommandFormatTextField.method_1863((text) -> {
            this.dimensionCommandFormat = text;
         });
      }

      this.method_25429(this.commandFormatTextField);
      this.method_25429(this.dimensionCommandFormatTextField);
      if (SupportMods.minimap()) {
         this.method_37063(new MySmallButton(0, this.field_22789 / 2 - 75, this.field_22790 / 7 + 138, class_2561.method_43471("gui.xaero_wm_teleport_command_waypoints"), (b) -> {
            SupportMods.xaeroMinimap.openWaypointWorldTeleportCommandScreen(this, this.escape);
         }));
      }

      this.method_37063(this.confirmButton = new MySmallButton(1, this.field_22789 / 2 - 155, this.field_22790 / 6 + 168, class_2561.method_43471("gui.xaero_confirm"), (b) -> {
         if (this.canConfirm()) {
            if (!this.usingDefault && this.commandFormat.equals(defaultMapTeleportFormat) && this.dimensionCommandFormat.equals(defaultMapTeleportDimensionFormat)) {
               this.usingDefault = true;
            }

            mapWorld.setTeleportCommandFormat(this.commandFormat);
            mapWorld.setDimensionTeleportCommandFormat(this.dimensionCommandFormat);
            mapWorld.setUseDefaultMapTeleport(this.usingDefault);
            mapWorld.saveConfig();
            this.goBack();
         }

      }));
      this.method_37063(new MySmallButton(2, this.field_22789 / 2 + 5, this.field_22790 / 6 + 168, class_2561.method_43471("gui.xaero_cancel"), (b) -> {
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
      if (SupportMods.minimap()) {
         guiGraphics.method_27534(this.field_22793, this.waypointCommandHint, this.field_22789 / 2, this.field_22790 / 7 + 124, -5592406);
      }

      guiGraphics.method_25300(this.field_22793, "{x} {y} {z} {d}", this.field_22789 / 2, this.field_22790 / 7 + 46, -5592406);
   }

   public void method_25394(class_332 guiGraphics, int mouseX, int mouseY, float partial) {
      this.renderEscapeScreen(guiGraphics, mouseX, mouseY, partial);
      super.method_25394(guiGraphics, mouseX, mouseY, partial);
      this.commandFormatTextField.method_25394(guiGraphics, mouseX, mouseY, partial);
      this.dimensionCommandFormatTextField.method_25394(guiGraphics, mouseX, mouseY, partial);
   }

   private boolean canConfirm() {
      return this.commandFormat != null && this.commandFormat.length() > 0 && this.dimensionCommandFormat != null && this.dimensionCommandFormat.length() > 0;
   }

   public void method_25393() {
      this.confirmButton.field_22763 = this.canConfirm();
   }

   public boolean method_25404(int par1, int par2, int par3) {
      if (par1 == 257 && this.commandFormat != null && this.commandFormat.length() > 0) {
         this.confirmButton.method_25348(0.0D, 0.0D);
      }

      return super.method_25404(par1, par2, par3);
   }
}
