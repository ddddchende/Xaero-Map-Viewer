package xaero.map.gui;

import java.util.Iterator;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import xaero.lib.client.config.option.ui.ConfigOptionScreenEntry;
import xaero.lib.client.gui.CustomSettingEntry;
import xaero.lib.client.gui.GuiSettings;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.common.config.channel.ConfigChannel;
import xaero.lib.common.config.option.ConfigOption;
import xaero.lib.common.config.util.ConfigConstants;
import xaero.lib.common.gui.widget.TooltipInfo;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.file.export.PNGExportResult;

public class ExportScreen extends GuiSettings {
   private static final class_2561 EXPORTING_MESSAGE = class_2561.method_43471("gui.xaero_export_screen_exporting");
   private final MapProcessor mapProcessor;
   private PNGExportResult result;
   private int stage;
   private final MapTileSelection selection;
   public boolean fullExport;

   public ExportScreen(class_437 backScreen, class_437 escScreen, MapProcessor mapProcessor, MapTileSelection selection) {
      super(class_2561.method_43471("gui.xaero_export_screen"), backScreen, escScreen);
      this.mapProcessor = mapProcessor;
      this.selection = selection;
      ISettingEntry fullExportEntry = new CustomSettingEntry(() -> {
         return false;
      }, class_2561.method_43471("gui.xaero_export_option_full"), new TooltipInfo("gui.xaero_box_export_option_full"), false, () -> {
         return this.fullExport;
      }, 0, 1, (i) -> {
         return i == 1;
      }, (v) -> {
         return v ? ConfigConstants.ON : ConfigConstants.OFF;
      }, (o, n) -> {
         this.fullExport = n;
      }, () -> {
         return true;
      });
      this.entries = new ISettingEntry[]{fullExportEntry, this.primaryOptionEntry(WorldMapPrimaryClientConfigOptions.EXPORT_MULTIPLE_IMAGES), this.primaryOptionEntry(WorldMapPrimaryClientConfigOptions.NIGHT_EXPORT), this.primaryOptionEntry(WorldMapPrimaryClientConfigOptions.EXPORT_HIGHLIGHTS), this.primaryOptionEntry(WorldMapPrimaryClientConfigOptions.EXPORT_SCALE_DOWN_SQUARE)};
      this.canSearch = false;
      this.confirmButton = true;
      this.canSkipWorldRender = true;
   }

   public void method_25426() {
      if (this.stage <= 0) {
         super.method_25426();
      }
   }

   protected void confirm() {
      this.stage = 1;
      this.method_25423(this.field_22787, this.field_22789, this.field_22790);
   }

   public void method_25394(class_332 guiGraphics, int par1, int par2, float par3) {
      this.renderEscapeScreen(guiGraphics, par1, par2, par3);
      super.method_25394(guiGraphics, par1, par2, par3);
      if (this.result != null) {
         guiGraphics.method_27534(this.field_22787.field_1772, this.result.getMessage(), this.field_22789 / 2, this.field_22790 / 7 + 29 + 96, -1);
      }

      if (this.stage > 0) {
         guiGraphics.method_27534(this.field_22787.field_1772, EXPORTING_MESSAGE, this.field_22789 / 2, this.field_22790 / 6 + 68, -1);
         if (this.stage == 1) {
            this.stage = 2;
            return;
         }
      }

      if (this.stage == 2) {
         if (this.mapProcessor.getMapSaveLoad().exportPNG(this, this.fullExport ? null : this.selection)) {
            this.stage = 3;
            this.result = null;
            Iterator var5 = this.method_25396().iterator();

            while(var5.hasNext()) {
               class_364 c = (class_364)var5.next();
               if (c instanceof class_4185) {
                  ((class_4185)c).field_22763 = false;
               }
            }

         } else {
            this.stage = 0;
            this.method_25423(this.field_22787, this.field_22789, this.field_22790);
         }
      }
   }

   public <T> ConfigOptionScreenEntry<T> primaryOptionEntry(ConfigOption<T> option) {
      ConfigChannel channel = WorldMap.INSTANCE.getConfigs();
      return new ConfigOptionScreenEntry(option, () -> {
         return channel.getPrimaryClientConfigManager().getConfig();
      }, () -> {
         return null;
      }, () -> {
         channel.getPrimaryClientConfigManagerIO().save();
      }, channel, true, false);
   }

   public void onExportDone(PNGExportResult result) {
      this.result = result;
      this.stage = 0;
   }

   public MapTileSelection getSelection() {
      return this.selection;
   }
}
