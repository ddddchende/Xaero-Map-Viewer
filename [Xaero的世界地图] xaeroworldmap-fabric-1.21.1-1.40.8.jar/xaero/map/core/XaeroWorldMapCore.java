package xaero.map.core;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_2626;
import net.minecraft.class_2637;
import net.minecraft.class_2666;
import net.minecraft.class_2672;
import net.minecraft.class_2676;
import net.minecraft.class_2678;
import net.minecraft.class_2759;
import net.minecraft.class_2818;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_5218;
import net.minecraft.class_634;
import net.minecraft.class_6603;
import net.minecraft.class_2556.class_7602;
import net.minecraft.class_32.class_5143;
import xaero.lib.common.util.IOUtils;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.file.MapSaveLoad;
import xaero.map.world.MapWorld;

public class XaeroWorldMapCore {
   public static Field chunkCleanField = null;
   public static WorldMapSession currentSession;

   public static void ensureField() {
      if (chunkCleanField == null) {
         try {
            chunkCleanField = class_2818.class.getDeclaredField("xaero_wm_chunkClean");
         } catch (SecurityException | NoSuchFieldException var1) {
            throw new RuntimeException(var1);
         }
      }

   }

   public static void chunkUpdateCallback(int chunkX, int chunkZ) {
      ensureField();
      class_1937 world = class_310.method_1551().field_1687;
      if (world != null) {
         try {
            for(int x = chunkX - 1; x < chunkX + 2; ++x) {
               for(int z = chunkZ - 1; z < chunkZ + 2; ++z) {
                  class_2818 chunk = world.method_8497(x, z);
                  if (chunk != null) {
                     chunkCleanField.set(chunk, false);
                  }
               }
            }
         } catch (IllegalAccessException | IllegalArgumentException var6) {
            throw new RuntimeException(var6);
         }
      }

   }

   public static void onChunkData(int x, int z, class_6603 packetIn) {
      chunkUpdateCallback(x, z);
   }

   public static void onChunkLightData(int x, int z) {
      chunkUpdateCallback(x, z);
   }

   public static void onHandleLevelChunkWithLight(class_2672 packet) {
      onChunkLightData(packet.method_11523(), packet.method_11524());
   }

   public static void onHandleLightUpdatePacket(class_2676 packet) {
      onChunkLightData(packet.method_11558(), packet.method_11554());
   }

   public static void onQueueLightRemoval(class_2666 packet) {
      onChunkLightData(packet.comp_1726().field_9181, packet.comp_1726().field_9180);
   }

   public static void onBlockChange(class_2626 packetIn) {
      chunkUpdateCallback(packetIn.method_11309().method_10263() >> 4, packetIn.method_11309().method_10260() >> 4);
   }

   public static void onMultiBlockChange(class_2637 packetIn) {
      IWorldMapSMultiBlockChangePacket packetAccess = (IWorldMapSMultiBlockChangePacket)packetIn;
      chunkUpdateCallback(packetAccess.xaero_wm_getSectionPos().method_10263(), packetAccess.xaero_wm_getSectionPos().method_10260());
   }

   public static void onPlayNetHandler(class_634 netHandler, class_2678 packet) {
      if (WorldMap.loaded) {
         try {
            IWorldMapClientPlayNetHandler netHandlerAccess = (IWorldMapClientPlayNetHandler)netHandler;
            if (netHandlerAccess.getXaero_worldmapSession() != null) {
               return;
            }

            if (currentSession != null) {
               WorldMap.LOGGER.info("Previous world map session still active. Probably using MenuMobs. Forcing it to end...");
               cleanupCurrentSession();
            }

            WorldMapSession worldmapSession = new WorldMapSession();
            currentSession = worldmapSession;
            worldmapSession.init(netHandler, packet.comp_1727().comp_1555());
            netHandlerAccess.setXaero_worldmapSession(worldmapSession);
            WorldMap.settings.updateRegionCacheHashCode();
         } catch (Throwable var4) {
            if (currentSession != null) {
               cleanupCurrentSession();
            }

            RuntimeException wrappedException = new RuntimeException("Exception initializing Xaero's World Map! ", var4);
            WorldMap.crashHandler.setCrashedBy(wrappedException);
         }

      }
   }

   private static void cleanupCurrentSession() {
      try {
         currentSession.cleanup();
      } catch (Throwable var4) {
         WorldMap.LOGGER.error("suppressed exception", var4);
      } finally {
         currentSession = null;
      }

   }

   public static void onPlayNetHandlerCleanup(class_634 netHandler) {
      if (WorldMap.loaded) {
         try {
            WorldMapSession netHandlerSession = ((IWorldMapClientPlayNetHandler)netHandler).getXaero_worldmapSession();
            if (netHandlerSession == null) {
               return;
            }

            try {
               netHandlerSession.cleanup();
            } finally {
               if (netHandlerSession == currentSession) {
                  currentSession = null;
               }

               ((IWorldMapClientPlayNetHandler)netHandler).setXaero_worldmapSession((WorldMapSession)null);
            }
         } catch (Throwable var6) {
            RuntimeException wrappedException = new RuntimeException("Exception finalizing Xaero's World Map! ", var6);
            WorldMap.crashHandler.setCrashedBy(wrappedException);
         }

      }
   }

   public static void onDeleteWorld(class_5143 levelStorageAccess) {
      if (WorldMap.loaded) {
         String folderName = levelStorageAccess.method_27010(class_5218.field_24188).getParent().getFileName().toString();
         String worldRootId = MapWorld.convertWorldFolderToRootId(5, folderName);
         if (!worldRootId.isEmpty()) {
            Path worldMapCacheFolder = MapSaveLoad.getRootFolder(worldRootId);
            if (worldMapCacheFolder.toFile().exists()) {
               try {
                  IOUtils.deleteFileIf(worldMapCacheFolder, (path) -> {
                     String pathString = worldMapCacheFolder.relativize(path).toString().replace('\\', '/');
                     return pathString.contains("/cache/") || pathString.endsWith("/cache") || pathString.contains("/cache_");
                  }, 20);
                  WorldMap.LOGGER.info(String.format("Deleted world map cache at %s", worldMapCacheFolder));
               } catch (IOException var5) {
                  WorldMap.LOGGER.error(String.format("Failed to delete world map cache at %s!", worldMapCacheFolder), var5);
               }
            }
         }

      }
   }

   public static void onMinecraftRunTick() {
      if (WorldMap.events != null) {
         WorldMap.events.handleClientRunTickStart();
      }

   }

   public static boolean onSystemChat(class_2561 component) {
      return !WorldMap.loaded ? false : WorldMap.events.handleClientSystemChatReceivedEvent(component);
   }

   public static boolean onHandleDisguisedChatMessage(class_7602 chatType, class_2561 component) {
      if (!WorldMap.loaded) {
         return true;
      } else {
         return !WorldMap.events.handleClientPlayerChatReceivedEvent(chatType, component, (GameProfile)null);
      }
   }

   public static boolean onRenderCall(boolean renderingInGame) {
      if (!WorldMap.loaded) {
         return renderingInGame;
      } else {
         return WorldMap.events.handleRenderTick(true) ? false : renderingInGame;
      }
   }

   public static void handlePlayerSetSpawnPacket(class_2759 packet) {
      if (WorldMap.loaded) {
         WorldMap.events.handlePlayerSetSpawnEvent(packet.method_11870(), class_310.method_1551().field_1687);
      }
   }

   public static boolean onRenderCrosshair(class_332 guiGraphics) {
      return !WorldMap.loaded ? false : WorldMap.events.handleRenderCrosshairOverlay(guiGraphics);
   }
}
