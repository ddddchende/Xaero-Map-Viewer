package xaero.map.mixin.plugin;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import xaero.map.platform.Services;

public class MixinPlugin implements IMixinConfigPlugin {
   private static final Map<String, String> MIXIN_MOD_ID_MAP = ImmutableMap.of();

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      String modId = (String)MIXIN_MOD_ID_MAP.get(mixinClassName);
      return modId == null ? true : Services.PLATFORM.checkModForMixin(modId);
   }

   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
