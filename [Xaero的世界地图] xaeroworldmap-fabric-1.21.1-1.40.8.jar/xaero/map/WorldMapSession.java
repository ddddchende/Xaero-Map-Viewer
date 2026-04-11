package xaero.map;

import java.util.HashSet;
import net.minecraft.class_310;
import net.minecraft.class_634;
import net.minecraft.class_746;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.BiomeGetter;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.cache.BrokenBlockTintCache;
import xaero.map.controls.ControlsHandler;
import xaero.map.core.IWorldMapClientPlayNetHandler;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.executor.Executor;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.file.worldsave.WorldDataReader;
import xaero.map.file.worldsave.biome.WorldDataBiomeManager;
import xaero.map.graphics.TextureUploader;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.message.MessageBox;
import xaero.map.gui.message.render.MessageBoxRenderer;
import xaero.map.highlight.HighlighterRegistry;
import xaero.map.highlight.MapRegionHighlightsPreparer;
import xaero.map.misc.CaveStartCalculator;
import xaero.map.mods.SupportMods;
import xaero.map.radar.tracker.synced.ClientSyncedTrackedPlayerManager;

public class WorldMapSession {
   private ControlsHandler controlsHandler;
   private MapProcessor mapProcessor;
   private MapWriter mapWriter;
   private MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers;
   private boolean usable;

   public void init(class_634 connection, long biomeZoomSeed) throws NoSuchFieldException {
      BlockStateShortShapeCache blockStateShortShapeCache = new BlockStateShortShapeCache();
      MapSaveLoad mapSaveLoad = new MapSaveLoad(WorldMap.overlayManager, WorldMap.pngExporter, WorldMap.oldFormatSupport, blockStateShortShapeCache);
      TextureUploader textureUploader = new TextureUploader(WorldMap.normalTextureUploadPool, WorldMap.compressedTextureUploadPool, WorldMap.branchUpdatePool, WorldMap.branchUpdateAllocatePool, WorldMap.branchDownloadPool, WorldMap.subsequentNormalTextureUploadPool, WorldMap.textureUploadBenchmark);
      BiomeGetter biomeGetter = new BiomeGetter();
      BrokenBlockTintCache brokenBlockTintCache = new BrokenBlockTintCache(new HashSet(32));
      BiomeColorCalculator biomeColorCalculator = new BiomeColorCalculator();
      WorldDataBiomeManager worldDataBiomeManager = new WorldDataBiomeManager();
      WorldDataReader worldDataReader = new WorldDataReader(WorldMap.overlayManager, blockStateShortShapeCache, worldDataBiomeManager, biomeZoomSeed);
      Executor worldDataRenderExecutor = new Executor("world data render executor", Thread.currentThread());
      WorldDataHandler worldDataHandler = new WorldDataHandler(worldDataReader, worldDataRenderExecutor);
      this.multiTextureRenderTypeRenderers = new MultiTextureRenderTypeRendererProvider(2);
      this.mapWriter = WorldMap.INSTANCE.createWriter(WorldMap.overlayManager, blockStateShortShapeCache, biomeGetter);
      HighlighterRegistry highlightRegistry = new HighlighterRegistry();
      if (SupportMods.pac()) {
         SupportMods.xaeroPac.registerHighlighters(highlightRegistry);
      }

      highlightRegistry.end();
      MapRegionHighlightsPreparer mapRegionHighlightsPreparer = new MapRegionHighlightsPreparer();
      ClientSyncedTrackedPlayerManager clientSyncedTrackedPlayerManager = new ClientSyncedTrackedPlayerManager();
      this.mapProcessor = new MapProcessor(mapSaveLoad, this.mapWriter, WorldMap.mapLimiter, WorldMap.bufferDeallocator, WorldMap.tilePool, WorldMap.overlayManager, textureUploader, worldDataHandler, WorldMap.worldMapClientOnly.branchTextureRenderer, this.multiTextureRenderTypeRenderers, WorldMap.worldMapClientOnly.customVertexConsumers, biomeColorCalculator, blockStateShortShapeCache, biomeGetter, brokenBlockTintCache, highlightRegistry, mapRegionHighlightsPreparer, MessageBox.Builder.begin().build(), new MessageBoxRenderer(), new CaveStartCalculator(this.mapWriter), clientSyncedTrackedPlayerManager);
      this.mapWriter.setMapProcessor(this.mapProcessor);
      mapSaveLoad.setMapProcessor(this.mapProcessor);
      worldDataReader.setMapProcessor(this.mapProcessor);
      this.controlsHandler = new ControlsHandler(this.mapProcessor);
      this.mapProcessor.onInit(connection);
      this.usable = true;
      WorldMap.LOGGER.info("New world map session initialized!");
   }

   public void cleanup() {
      try {
         if (this.usable) {
            this.mapProcessor.stop();
            WorldMap.LOGGER.info("Finalizing world map session...");
            WorldMap.mapRunnerThread.interrupt();

            while(!this.mapProcessor.isFinished()) {
               this.mapProcessor.waitForLoadingToFinish(() -> {
               });

               try {
                  Thread.sleep(20L);
               } catch (InterruptedException var2) {
                  WorldMap.LOGGER.error("suppressed exception", var2);
               }
            }
         }

         WorldMap.LOGGER.info("World map session finalized.");
         WorldMap.onSessionFinalized();
      } catch (Throwable var3) {
         WorldMap.LOGGER.error("World map session failed to finalize properly.", var3);
      }

      this.usable = false;
   }

   public ControlsHandler getControlsHandler() {
      return this.controlsHandler;
   }

   public MapProcessor getMapProcessor() {
      return this.mapProcessor;
   }

   public static WorldMapSession getCurrentSession() {
      WorldMapSession session = getForPlayer(class_310.method_1551().field_1724);
      if (session == null && XaeroWorldMapCore.currentSession != null && XaeroWorldMapCore.currentSession.usable) {
         session = XaeroWorldMapCore.currentSession;
      }

      return session;
   }

   public static WorldMapSession getForPlayer(class_746 player) {
      return player != null && player.field_3944 != null ? ((IWorldMapClientPlayNetHandler)player.field_3944).getXaero_worldmapSession() : null;
   }

   public boolean isUsable() {
      return this.usable;
   }
}
