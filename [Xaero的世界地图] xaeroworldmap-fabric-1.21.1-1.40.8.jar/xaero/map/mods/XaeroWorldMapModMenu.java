package xaero.map.mods;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import xaero.lib.client.gui.GuiSettings;
import xaero.lib.client.gui.config.context.BuiltInEditConfigScreenContexts;
import xaero.map.gui.GuiWorldMapSettings;

public class XaeroWorldMapModMenu implements ModMenuApi {
   public ConfigScreenFactory<GuiSettings> getModConfigScreenFactory() {
      return (current) -> {
         return new GuiWorldMapSettings(current, BuiltInEditConfigScreenContexts.CLIENT);
      };
   }
}
