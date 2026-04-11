package xaero.map;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.api.metadata.ModOrigin.Kind;
import net.minecraft.class_2378;
import net.minecraft.class_7923;
import xaero.lib.common.config.Config;
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.map.biome.BiomeGetter;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.common.config.primary.option.WorldMapPrimaryCommonConfigOptions;
import xaero.map.effects.EffectsRegister;
import xaero.map.events.ClientEventsFabric;
import xaero.map.events.CommonEventsFabric;
import xaero.map.events.ModClientEventsFabric;
import xaero.map.events.ModCommonEventsFabric;
import xaero.map.mods.SupportMods;
import xaero.map.mods.SupportModsFabric;
import xaero.map.region.OverlayManager;
import xaero.map.server.WorldMapServer;
import xaero.map.server.WorldMapServerFabric;

public class WorldMapFabric extends WorldMap implements ClientModInitializer, DedicatedServerModInitializer {
   private final String fileLayoutID = "worldmap_fabric";
   private Throwable firstStageError;
   private boolean loadLaterNeeded;
   private boolean loadLaterDone;

   public void onInitializeClient() {
      try {
         this.loadCommon();
         this.loadClient();
      } catch (Throwable var2) {
         this.firstStageError = var2;
      }

   }

   public void onInitializeServer() {
      try {
         this.loadCommon();
         this.loadServer();
      } catch (Throwable var2) {
         this.firstStageError = var2;
      }

   }

   private void registerClientEvents() {
      events = new ClientEventsFabric();
      modEvents = new ModClientEventsFabric();
   }

   private void registerCommonEvents() {
      CommonEventsFabric commonEventsFabric = new CommonEventsFabric();
      commonEvents = commonEventsFabric;
      modCommonEvents = new ModCommonEventsFabric();
      commonEventsFabric.register();
   }

   void loadCommon() {
      super.loadCommon();
      SingleConfigManager<Config> primaryCommonConfig = this.getConfigs().getPrimaryCommonConfigManager();
      boolean shouldRegisterEffects = (Boolean)primaryCommonConfig.getEffective(WorldMapPrimaryCommonConfigOptions.REGISTER_EFFECTS);
      if (shouldRegisterEffects) {
         (new EffectsRegister()).registerEffects((effect) -> {
            return class_2378.method_47985(class_7923.field_41174, effect.getRegistryName(), effect);
         });
      }

      this.registerCommonEvents();
   }

   void loadClient() throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
      this.registerClientEvents();
      super.loadClient();
      controlsRegister.register(KeyBindingHelper::registerKeyBinding);
      this.loadLaterNeeded = true;
   }

   void loadServer() {
      super.loadServer();
      this.loadLaterNeeded = true;
   }

   public void tryLoadLater() {
      if (!this.loadLaterDone) {
         if (this.firstStageError != null) {
            throw new RuntimeException(this.firstStageError);
         } else if (this.loadLaterNeeded) {
            this.loadLaterDone = true;
            this.loadLater();
         }
      }
   }

   public void tryLoadLaterServer() {
      if (!this.loadLaterDone) {
         if (this.firstStageError != null) {
            throw new RuntimeException(this.firstStageError);
         } else if (this.loadLaterNeeded) {
            this.loadLaterDone = true;
            this.loadLaterServer();
         }
      }
   }

   protected Path fetchModFile() {
      ModContainer modContainer = (ModContainer)FabricLoader.getInstance().getModContainer("xaeroworldmap").orElse((Object)null);
      ModOrigin origin = modContainer.getOrigin();
      Path modFile = origin.getKind() == Kind.PATH ? (Path)origin.getPaths().get(0) : null;
      if (modFile == null) {
         try {
            Class<?> quiltLoaderClass = Class.forName("org.quiltmc.loader.api.QuiltLoader");
            Method quiltGetModContainerMethod = quiltLoaderClass.getDeclaredMethod("getModContainer", String.class);
            Class<?> quiltModContainerAPIClass = Class.forName("org.quiltmc.loader.api.ModContainer");
            Method quiltGetSourcePathsMethod = quiltModContainerAPIClass.getDeclaredMethod("getSourcePaths");
            Object quiltModContainer = ((Optional)quiltGetModContainerMethod.invoke((Object)null, "xaeroworldmap")).orElse((Object)null);
            List<List<Path>> paths = (List)quiltGetSourcePathsMethod.invoke(quiltModContainer);
            if (!paths.isEmpty() && !((List)paths.get(0)).isEmpty()) {
               modFile = (Path)((List)paths.get(0)).get(0);
            }
         } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException var10) {
         }
      }

      return modFile;
   }

   protected String getFileLayoutID() {
      return "worldmap_fabric";
   }

   protected SupportMods createSupportMods() {
      return new SupportModsFabric();
   }

   protected WorldMapClientOnly createClientLoad() {
      return new WorldMapClientOnlyFabric();
   }

   protected WorldMapServer createServerLoad() {
      return new WorldMapServerFabric();
   }

   public MapWriter createWriter(OverlayManager overlayManager, BlockStateShortShapeCache blockStateShortShapeCache, BiomeGetter biomeGetter) {
      return new MapWriterFabric(overlayManager, blockStateShortShapeCache, biomeGetter);
   }

   protected String getModInfoVersion() {
      ModContainer modContainer = (ModContainer)FabricLoader.getInstance().getModContainer("xaeroworldmap").get();
      return modContainer.getMetadata().getVersion().getFriendlyString() + "_fabric";
   }
}
