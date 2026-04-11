package xaero.map;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import net.minecraft.class_2960;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xaero.lib.XaeroLib;
import xaero.lib.common.config.channel.ConfigChannel;
import xaero.lib.common.config.channel.ConfigChannel.Builder;
import xaero.lib.common.config.channel.register.ConfigChannelRegistry;
import xaero.lib.common.config.primary.option.LibPrimaryCommonConfigOptions;
import xaero.lib.common.packet.IPacketHandler;
import xaero.lib.common.packet.PacketHandlerRegistry;
import xaero.lib.patreon.Patreon;
import xaero.lib.patreon.PatreonMod;
import xaero.map.biome.BiomeGetter;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.cache.UnknownBlockStateCache;
import xaero.map.common.config.LegacyCommonConfigIO;
import xaero.map.common.config.LegacyCommonConfigInit;
import xaero.map.common.config.channel.register.handler.WorldMapChannelCommonRegistryHandler;
import xaero.map.config.channel.register.handler.WorldMapChannelClientRegistryHandler;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.controls.ControlsRegister;
import xaero.map.deallocator.ByteBufferDeallocator;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.events.ClientEvents;
import xaero.map.events.CommonEvents;
import xaero.map.events.ModClientEvents;
import xaero.map.events.ModCommonEvents;
import xaero.map.file.OldFormatSupport;
import xaero.map.file.export.PNGExporter;
import xaero.map.graphics.GLObjectDeleter;
import xaero.map.graphics.TextureUploadBenchmark;
import xaero.map.message.WorldMapMessageRegister;
import xaero.map.misc.Internet;
import xaero.map.mods.SupportMods;
import xaero.map.mods.gui.WaypointSymbolCreator;
import xaero.map.platform.Services;
import xaero.map.pool.MapTilePool;
import xaero.map.pool.TextureUploadPool;
import xaero.map.pool.buffer.TextureDirectBufferPool;
import xaero.map.radar.tracker.PlayerTrackerMapElementRenderer;
import xaero.map.radar.tracker.PlayerTrackerMenuRenderer;
import xaero.map.radar.tracker.system.PlayerTrackerSystemManager;
import xaero.map.radar.tracker.system.impl.SyncedPlayerTrackerSystem;
import xaero.map.region.OverlayManager;
import xaero.map.server.WorldMapServer;
import xaero.map.server.mods.SupportServerMods;
import xaero.map.server.player.ServerPlayerTickHandler;
import xaero.map.settings.ModSettings;

public abstract class WorldMap {
   public static final String MOD_ID = "xaeroworldmap";
   public static boolean loaded = false;
   public static WorldMap INSTANCE;
   public static int MINIMAP_COMPATIBILITY_VERSION = 26;
   public static Logger LOGGER = LogManager.getLogger();
   static final String versionID_minecraft = "1.21.1";
   private String versionID;
   public static int newestUpdateID;
   public static boolean isOutdated;
   public static String latestVersion;
   public static String latestVersionMD5;
   public static ClientEvents events;
   public static ModClientEvents modEvents;
   public static ControlsRegister controlsRegister;
   public static WaypointSymbolCreator waypointSymbolCreator;
   public static ByteBufferDeallocator bufferDeallocator;
   public static TextureUploadBenchmark textureUploadBenchmark;
   public static OverlayManager overlayManager;
   public static OldFormatSupport oldFormatSupport;
   public static PNGExporter pngExporter;
   public static TextureUploadPool.Normal normalTextureUploadPool;
   public static TextureUploadPool.Compressed compressedTextureUploadPool;
   public static TextureUploadPool.BranchUpdate branchUpdatePool;
   public static TextureUploadPool.BranchUpdate branchUpdateAllocatePool;
   public static TextureUploadPool.BranchDownload branchDownloadPool;
   public static TextureUploadPool.SubsequentNormal subsequentNormalTextureUploadPool;
   public static TextureDirectBufferPool textureDirectBufferPool;
   public static MapTilePool tilePool;
   public static MapLimiter mapLimiter;
   public static UnknownBlockStateCache unknownBlockStateCache;
   public static GLObjectDeleter glObjectDeleter;
   public static MapRunner mapRunner;
   public static Thread mapRunnerThread;
   public static CrashHandler crashHandler;
   public static final class_2960 guiTextures = class_2960.method_60655("xaeroworldmap", "gui/gui.png");
   public static ModSettings settings;
   public static WorldMapClientOnly worldMapClientOnly;
   public static WorldMapServer worldmapServer;
   public static MapElementRenderHandler mapElementRenderHandler;
   public static ServerPlayerTickHandler serverPlayerTickHandler;
   public static PlayerTrackerSystemManager playerTrackerSystemManager = new PlayerTrackerSystemManager();
   public static PlayerTrackerMapElementRenderer trackedPlayerRenderer;
   public static PlayerTrackerMenuRenderer trackedPlayerMenuRenderer;
   public static IPacketHandler messageHandler;
   public static CommonEvents commonEvents;
   public static ModCommonEvents modCommonEvents;
   private final Path configSubFolder;
   private final Path defaultConfigsSubFolder;
   private final boolean shouldLoadLegacySettings;
   private final ConfigChannel configChannel;
   public static boolean detailed_debug = false;
   public static boolean pauseRequests = false;
   public static boolean extraDebug = false;
   public static File modJAR = null;
   public static File configFolder;
   public static File optionsFile;
   public static File saveFolder;
   public static LegacyCommonConfigIO commonConfigIO;

   public WorldMap() {
      INSTANCE = this;
      this.configSubFolder = Services.PLATFORM.getConfigDir().resolve("xaero").resolve("world-map");
      this.defaultConfigsSubFolder = Services.PLATFORM.getConfigDir().resolveSibling("defaultconfigs").resolve("xaero").resolve("world-map");
      this.shouldLoadLegacySettings = !Files.exists(this.configSubFolder, new LinkOption[0]);
      this.configChannel = Builder.begin().setId(class_2960.method_60655("xaeroworldmap", "main")).setCommonRegistryHandler(new WorldMapChannelCommonRegistryHandler()).setClientRegistryHandlerSupplier(WorldMapChannelClientRegistryHandler::new).setLogger(LOGGER).setConfigPath(this.configSubFolder).setDefaultConfigsPath(this.defaultConfigsSubFolder).setDefaultEnforcedServerProfileNodePath("xaero.world_map.enforced_server_profile").build();
      ConfigChannelRegistry.INSTANCE.register(this.configChannel);
      (new LegacyCommonConfigInit()).init("xaeroworldmap-common.txt");
   }

   protected abstract Path fetchModFile();

   protected abstract String getFileLayoutID();

   void loadClient() throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
      LOGGER.info("Loading Xaero's World Map - Stage 1/2");
      trackedPlayerRenderer = PlayerTrackerMapElementRenderer.Builder.begin().build();
      trackedPlayerMenuRenderer = PlayerTrackerMenuRenderer.Builder.begin().setRenderer(trackedPlayerRenderer).build();
      Path modFile = this.fetchModFile();
      (worldMapClientOnly = this.createClientLoad()).preInit("xaeroworldmap");
      String fileName = modFile.getFileName().toString();
      if (fileName.endsWith(".jar")) {
         modJAR = modFile.toFile();
      }

      Path gameDir = Services.PLATFORM.getGameDir();
      Path config = Services.PLATFORM.getConfigDir();
      configFolder = config.toFile();
      optionsFile = config.resolve("xaeroworldmap.txt").toFile();
      Path oldSaveFolder4 = gameDir.resolve("XaeroWorldMap");
      Path xaeroFolder = gameDir.resolve("xaero");
      if (!Files.exists(xaeroFolder, new LinkOption[0])) {
         Files.createDirectories(xaeroFolder);
      }

      saveFolder = xaeroFolder.resolve("world-map").toFile();
      if (oldSaveFolder4.toFile().exists() && !saveFolder.exists()) {
         Files.move(oldSaveFolder4, saveFolder.toPath());
      }

      Path oldSaveFolder3 = config.getParent().resolve("XaeroWorldMap");
      File oldOptionsFile = gameDir.resolve("xaeroworldmap.txt").toFile();
      File oldSaveFolder = gameDir.resolve("mods").resolve("XaeroWorldMap").toFile();
      File oldSaveFolder2 = gameDir.resolve("config").resolve("XaeroWorldMap").toFile();
      if (oldOptionsFile.exists() && !optionsFile.exists()) {
         Files.move(oldOptionsFile.toPath(), optionsFile.toPath());
      }

      if (oldSaveFolder.exists() && !saveFolder.exists()) {
         Files.move(oldSaveFolder.toPath(), saveFolder.toPath());
      }

      if (oldSaveFolder2.exists() && !saveFolder.exists()) {
         Files.move(oldSaveFolder2.toPath(), saveFolder.toPath());
      }

      if (oldSaveFolder3.toFile().exists() && !saveFolder.exists()) {
         Files.move(oldSaveFolder3, saveFolder.toPath());
      }

      if (!saveFolder.exists()) {
         Files.createDirectories(saveFolder.toPath());
      }

      settings = new ModSettings();
      if (this.shouldLoadLegacySettings) {
         settings.loadSettings();
         this.configChannel.getClientConfigProfileIO().save(this.configChannel.getClientConfigManager().getCurrentProfile());
         this.configChannel.getPrimaryClientConfigManagerIO().save();
      }

      waypointSymbolCreator = new WaypointSymbolCreator();
      if (controlsRegister == null) {
         controlsRegister = new ControlsRegister();
      }

      bufferDeallocator = new ByteBufferDeallocator();
      tilePool = new MapTilePool();
      overlayManager = new OverlayManager();
      oldFormatSupport = new OldFormatSupport();
      pngExporter = new PNGExporter(configFolder.toPath().getParent().resolve("map exports"));
      mapLimiter = new MapLimiter();
      normalTextureUploadPool = new TextureUploadPool.Normal(256);
      compressedTextureUploadPool = new TextureUploadPool.Compressed(256);
      branchUpdatePool = new TextureUploadPool.BranchUpdate(256, false);
      branchUpdateAllocatePool = new TextureUploadPool.BranchUpdate(256, true);
      branchDownloadPool = new TextureUploadPool.BranchDownload(256);
      textureDirectBufferPool = new TextureDirectBufferPool();
      subsequentNormalTextureUploadPool = new TextureUploadPool.SubsequentNormal(256);
      textureUploadBenchmark = new TextureUploadBenchmark(new int[]{512, 512, 512, 256, 256, 256, 256});
      unknownBlockStateCache = new UnknownBlockStateCache();
      glObjectDeleter = new GLObjectDeleter();
      crashHandler = new CrashHandler();
      (mapRunnerThread = new Thread(mapRunner = new MapRunner())).start();
   }

   public void loadLater() {
      LOGGER.info("Loading Xaero's World Map - Stage 2/2");

      try {
         this.loadLaterCommon();
         worldMapClientOnly.postInit();
         settings.updateRegionCacheHashCode();
         Patreon.checkPatreon();
         Internet.checkModVersion();
         if (isOutdated) {
            PatreonMod patreonEntry = (PatreonMod)Patreon.getMods().get(this.getFileLayoutID());
            if (patreonEntry != null) {
               patreonEntry.modJar = modJAR;
               patreonEntry.currentVersion = this.getVersionID();
               patreonEntry.latestVersion = latestVersion;
               patreonEntry.md5 = latestVersionMD5;
               patreonEntry.onVersionIgnore = () -> {
                  this.getConfigs().getPrimaryClientConfigManager().getConfig().set(WorldMapPrimaryClientConfigOptions.IGNORED_UPDATE, newestUpdateID);
                  this.getConfigs().getPrimaryClientConfigManagerIO().save();
               };
               Patreon.addOutdatedMod(patreonEntry);
            }
         }

         playerTrackerSystemManager.register("map_synced", new SyncedPlayerTrackerSystem());
         this.createSupportMods().load();
         mapElementRenderHandler = MapElementRenderHandler.Builder.begin().build();
         oldFormatSupport.loadStates();
         loaded = true;
      } catch (Throwable var2) {
         LOGGER.error("error", var2);
         crashHandler.setCrashedBy(var2);
      }

   }

   void loadServer() {
      worldmapServer = this.createServerLoad();
      worldmapServer.load();
   }

   void loadLaterServer() {
      this.loadLaterCommon();
      worldmapServer.loadLater();
      loaded = true;
   }

   void loadCommon() {
      this.versionID = "1.21.1_" + this.getModInfoVersion();
      messageHandler = PacketHandlerRegistry.INSTANCE.register(class_2960.method_60655("xaeroworldmap", "main"), 1000000, "1.0");
      (new WorldMapMessageRegister()).register(messageHandler);
      serverPlayerTickHandler = new ServerPlayerTickHandler();
      SupportServerMods.check();
   }

   void loadLaterCommon() {
      if (commonConfigIO.shouldEnableEveryoneTracksEveryone()) {
         XaeroLib.INSTANCE.getLibConfigChannel().getPrimaryCommonConfigManager().getConfig().set(LibPrimaryCommonConfigOptions.EVERYONE_TRACKS_EVERYONE, true);
      }

   }

   public String getVersionID() {
      return this.versionID;
   }

   public static void onSessionFinalized() {
      mapLimiter.onSessionFinalized();
      if (SupportMods.minimap()) {
         SupportMods.xaeroMinimap.onSessionFinalized();
      }

   }

   public ConfigChannel getConfigs() {
      return this.configChannel;
   }

   protected abstract SupportMods createSupportMods();

   protected abstract WorldMapClientOnly createClientLoad();

   protected abstract WorldMapServer createServerLoad();

   public abstract MapWriter createWriter(OverlayManager var1, BlockStateShortShapeCache var2, BiomeGetter var3);

   protected abstract String getModInfoVersion();
}
