package xaero.map.file.worldsave;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2378;
import net.minecraft.class_2874;
import net.minecraft.class_310;
import net.minecraft.class_3218;
import net.minecraft.class_3611;
import net.minecraft.class_5218;
import net.minecraft.class_5321;
import net.minecraft.class_7225;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.capabilities.CapabilityGetter;
import xaero.map.capabilities.ServerWorldCapabilities;
import xaero.map.executor.Executor;
import xaero.map.region.MapRegion;
import xaero.map.world.MapWorld;

public class WorldDataHandler {
   private final Executor renderExecutor;
   private WorldDataReader reader;
   private class_3218 worldServer;
   private Path worldDir;

   public WorldDataHandler(WorldDataReader reader, Executor renderExecutor) throws NoSuchFieldException, SecurityException {
      this.reader = reader;
      this.renderExecutor = renderExecutor;
   }

   public void handleRenderExecutor() {
      this.renderExecutor.method_5383();
   }

   public void prepareSingleplayer(class_1937 world, MapProcessor mapProcessor) {
      MapWorld mapWorld = mapProcessor.getMapWorld();
      if (world != null && mapWorld.getCurrentDimension().isUsingWorldSave()) {
         class_5321<class_1937> dimId = mapWorld.getCurrentDimensionId();
         this.worldServer = class_310.method_1551().method_1576().method_3847(dimId);
         if (this.worldServer != null) {
            Path overworldDir = this.worldServer.method_8503().method_27050(class_5218.field_24188);
            this.worldDir = class_2874.method_12488(dimId, overworldDir);
         } else {
            this.worldDir = null;
         }
      } else {
         this.worldServer = null;
         this.worldDir = null;
      }

   }

   public WorldDataHandler.Result buildRegion(MapRegion region, class_7225<class_2248> blockLookup, class_2378<class_2248> blockRegistry, class_2378<class_3611> fluidRegistry, boolean loading, int[] chunkCountDest) throws IOException {
      if (this.worldServer == null) {
         WorldMap.LOGGER.info("Tried loading a region for a null server world!");
         return WorldDataHandler.Result.CANCEL;
      } else {
         ServerWorldCapabilities serverCaps;
         try {
            serverCaps = CapabilityGetter.getServerWorldCapabilities(this.worldServer);
         } catch (Exception var12) {
            throw new RuntimeException(var12);
         }

         boolean shouldCancel = false;
         synchronized(serverCaps) {
            if (serverCaps.loaded) {
               boolean buildResult = this.reader.buildRegion(region, this.worldServer, blockLookup, blockRegistry, fluidRegistry, loading, chunkCountDest, this.renderExecutor);
               return buildResult ? WorldDataHandler.Result.SUCCESS : WorldDataHandler.Result.FAIL;
            }

            shouldCancel = true;
         }

         if (shouldCancel) {
            WorldMap.LOGGER.info("Tried loading a region for an unloaded server world!");
            return WorldDataHandler.Result.CANCEL;
         } else {
            WorldMap.LOGGER.info("Server world capability required for Xaero's World Map not present!");
            return WorldDataHandler.Result.FAIL;
         }
      }
   }

   public static void onServerWorldUnload(class_3218 sw) {
      ServerWorldCapabilities serverCaps;
      try {
         serverCaps = CapabilityGetter.getServerWorldCapabilities(sw);
      } catch (Exception var5) {
         throw new RuntimeException(var5);
      }

      synchronized(serverCaps) {
         serverCaps.loaded = false;
      }
   }

   public class_3218 getWorldServer() {
      return this.worldServer;
   }

   public WorldDataReader getWorldDataReader() {
      return this.reader;
   }

   public Path getWorldDir() {
      return this.worldDir;
   }

   Executor getWorldDataRenderExecutor() {
      return this.renderExecutor;
   }

   public static enum Result {
      SUCCESS,
      FAIL,
      CANCEL;

      // $FF: synthetic method
      private static WorldDataHandler.Result[] $values() {
         return new WorldDataHandler.Result[]{SUCCESS, FAIL, CANCEL};
      }
   }
}
