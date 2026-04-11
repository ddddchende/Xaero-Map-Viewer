package xaero.map.common.config.primary.option;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import xaero.lib.common.config.option.BooleanConfigOption;
import xaero.lib.common.config.option.ConfigOption;
import xaero.lib.common.config.option.ConfigOptionManager;
import xaero.lib.common.config.option.BooleanConfigOption.Builder;

public class WorldMapPrimaryCommonConfigOptions {
   private static final List<ConfigOption<?>> ALL = new ArrayList();
   public static final BooleanConfigOption REGISTER_EFFECTS;

   public static void registerAll(ConfigOptionManager manager) {
      Iterator var1 = ALL.iterator();

      while(var1.hasNext()) {
         ConfigOption<?> option = (ConfigOption)var1.next();
         manager.register(option);
      }

   }

   static {
      REGISTER_EFFECTS = ((Builder)((Builder)Builder.begin().setId("register_status_effects")).setDefaultValue(true)).build(ALL);
   }
}
