package xaero.map.config.option.ui.type;

import xaero.lib.client.config.option.ui.factory.StandardConfigWidgetFactories;
import xaero.lib.client.config.option.ui.factory.ViewEnforcedCondition;
import xaero.lib.client.config.option.ui.type.ConfigOptionUIType;
import xaero.lib.client.config.option.ui.type.ConfigOptionUIType.Builder;
import xaero.lib.common.config.option.ConfigOption;
import xaero.map.gui.EditDefaultTpCommandScreen;

public class WorldMapConfigOptionUITypes {
   public static final ConfigOptionUIType<ConfigOption<String>> DEFAULT_MAP_TP_COMMAND = Builder.begin().setWidgetFactory(StandardConfigWidgetFactories.getOpenScreenFactory((parent, escape, config, enforced, option, onChange, readOnly, includeNullValue) -> {
      return new EditDefaultTpCommandScreen(parent, escape, config, enforced, option, includeNullValue, includeNullValue, onChange);
   }, (ViewEnforcedCondition)null)).build();
}
