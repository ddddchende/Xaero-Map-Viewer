package xaero.map.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import xaero.map.WorldMap;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.server.level.LevelMapProperties;
import xaero.map.server.level.LevelMapPropertiesIO;
import xaero.map.server.radar.tracker.SyncedPlayerTracker;
import xaero.map.server.radar.tracker.SyncedPlayerTrackerSystemManager;

public class MinecraftServerData {
   private final SyncedPlayerTrackerSystemManager syncedPlayerTrackerSystemManager;
   private final SyncedPlayerTracker syncedPlayerTracker;
   private final Map<Path, LevelMapProperties> levelProperties;
   private final LevelMapPropertiesIO propertiesIO;

   public MinecraftServerData(SyncedPlayerTrackerSystemManager syncedPlayerTrackerSystemManager, SyncedPlayerTracker syncedPlayerTracker) {
      this.syncedPlayerTrackerSystemManager = syncedPlayerTrackerSystemManager;
      this.syncedPlayerTracker = syncedPlayerTracker;
      this.levelProperties = new HashMap();
      this.propertiesIO = new LevelMapPropertiesIO();
   }

   public LevelMapProperties getLevelProperties(Path path) {
      LevelMapProperties properties = (LevelMapProperties)this.levelProperties.get(path);
      if (properties == null) {
         properties = new LevelMapProperties();

         try {
            this.propertiesIO.load(path, properties);
         } catch (FileNotFoundException var6) {
            try {
               this.propertiesIO.save(path, properties);
            } catch (IOException var5) {
               properties.setUsable(false);
               WorldMap.LOGGER.warn("Failed to initialize map properties for a world due to an IO exception. This shouldn't be a problem if it's not a \"real\" world. Message: {}", var5.getMessage());
               if (WorldMapClientConfigUtils.getDebug()) {
                  WorldMap.LOGGER.warn("Full exception: ", var5);
               }
            }
         } catch (IOException var7) {
            throw new RuntimeException(var7);
         }

         this.levelProperties.put(path, properties);
      }

      return properties;
   }

   public SyncedPlayerTrackerSystemManager getSyncedPlayerTrackerSystemManager() {
      return this.syncedPlayerTrackerSystemManager;
   }

   public SyncedPlayerTracker getSyncedPlayerTracker() {
      return this.syncedPlayerTracker;
   }

   public static MinecraftServerData get(MinecraftServer server) {
      return ((IMinecraftServer)server).getXaeroWorldMapServerData();
   }
}
