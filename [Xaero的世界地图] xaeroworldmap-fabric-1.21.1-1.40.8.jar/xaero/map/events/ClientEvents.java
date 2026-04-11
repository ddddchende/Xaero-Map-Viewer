package xaero.map.events;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import net.minecraft.class_1041;
import net.minecraft.class_1074;
import net.minecraft.class_1657;
import net.minecraft.class_1936;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_3218;
import net.minecraft.class_332;
import net.minecraft.class_4358;
import net.minecraft.class_437;
import net.minecraft.class_4398;
import net.minecraft.class_4439;
import net.minecraft.class_4877;
import net.minecraft.class_638;
import net.minecraft.class_2556.class_7602;
import xaero.lib.common.reflection.util.ReflectionUtils;
import xaero.lib.patreon.Patreon;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.mods.SupportMods;

public class ClientEvents {
   private class_4877 latestRealm;
   private Field realmsTaskField;
   private Field realmsTaskServerField;

   public class_437 handleGuiOpen(class_437 gui) {
      if (gui instanceof class_4398) {
         try {
            if (this.realmsTaskField == null) {
               this.realmsTaskField = ReflectionUtils.getFieldReflection(class_4398.class, "queuedTasks", "field_46707", "Ljava/util/List;", "f_302752_");
               this.realmsTaskField.setAccessible(true);
            }

            if (this.realmsTaskServerField == null) {
               this.realmsTaskServerField = ReflectionUtils.getFieldReflection(class_4439.class, "server", "field_20224", "Lnet/minecraft/class_4877;", "f_90327_");
               this.realmsTaskServerField.setAccessible(true);
            }

            class_4398 realmsTaskScreen = (class_4398)gui;
            List<class_4358> tasks = (List)this.realmsTaskField.get(realmsTaskScreen);
            Iterator var4 = tasks.iterator();

            while(true) {
               class_4877 realm;
               do {
                  do {
                     class_4358 task;
                     do {
                        if (!var4.hasNext()) {
                           return gui;
                        }

                        task = (class_4358)var4.next();
                     } while(!(task instanceof class_4439));

                     class_4439 realmsTask = (class_4439)task;
                     realm = (class_4877)this.realmsTaskServerField.get(realmsTask);
                  } while(realm == null);
               } while(this.latestRealm != null && realm.field_22599 == this.latestRealm.field_22599);

               this.latestRealm = realm;
            }
         } catch (Exception var8) {
            WorldMap.LOGGER.error("suppressed exception", var8);
         }
      }

      return gui;
   }

   public boolean handleRenderTick(boolean start) {
      if (!WorldMap.loaded) {
         return false;
      } else {
         class_310 mc = class_310.method_1551();
         if (!start) {
            WorldMap.glObjectDeleter.work();
         }

         boolean shouldCancelGameRender = false;
         if (mc.field_1724 != null) {
            WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
            if (worldmapSession != null) {
               MapProcessor mapProcessor = worldmapSession.getMapProcessor();
               if (!start) {
                  mapProcessor.onRenderProcess(mc);
                  mapProcessor.resetRenderStartTime();
                  Queue<Runnable> minecraftScheduledTasks = mapProcessor.getMinecraftScheduledTasks();
                  Runnable task = mapProcessor.getRenderStartTimeUpdater();
                  Runnable[] currentTasks = (Runnable[])minecraftScheduledTasks.toArray(new Runnable[0]);
                  minecraftScheduledTasks.clear();
                  minecraftScheduledTasks.add(task);
                  Runnable[] var9 = currentTasks;
                  int var10 = currentTasks.length;

                  for(int var11 = 0; var11 < var10; ++var11) {
                     Runnable t = var9[var11];
                     minecraftScheduledTasks.add(t);
                  }
               } else {
                  if (!SupportMods.vivecraft && MapProcessor.shouldSkipWorldRender()) {
                     RenderSystem.enableDepthTest();
                     mc.field_1687.method_38534();
                     mc.field_1687.method_2935().method_12130().method_15516();
                     shouldCancelGameRender = true;
                  }

                  if (mapProcessor != null) {
                     mapProcessor.setMainValues();
                  }
               }
            }
         }

         return shouldCancelGameRender;
      }
   }

   public void handleDrawScreen(class_437 gui) {
      if (!Patreon.needsNotification() && WorldMap.isOutdated) {
         WorldMap.isOutdated = false;
      }

   }

   public void handlePlayerSetSpawnEvent(class_2338 spawn, class_638 world) {
      WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
      if (worldmapSession != null) {
         MapProcessor mapProcessor = worldmapSession.getMapProcessor();
         mapProcessor.updateWorldSpawn(spawn, world);
      }

   }

   public void handleWorldUnload(class_1936 world) {
      if (class_310.method_1551().field_1724 != null) {
         WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
         if (worldmapSession != null) {
            MapProcessor mapProcessor = worldmapSession.getMapProcessor();
            if (world == mapProcessor.mainWorld) {
               mapProcessor.onWorldUnload();
            }
         }
      }

      if (world instanceof class_3218) {
         class_3218 sw = (class_3218)world;
         WorldDataHandler.onServerWorldUnload(sw);
      }

   }

   public class_4877 getLatestRealm() {
      return this.latestRealm;
   }

   public boolean handleRenderCrosshairOverlay(class_332 guiGraphics) {
      if (class_310.method_1551().field_1690.field_1842) {
         return false;
      } else {
         WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
         MapProcessor mapProcessor = worldmapSession == null ? null : worldmapSession.getMapProcessor();
         String crosshairMessage = mapProcessor == null ? null : mapProcessor.getCrosshairMessage();
         if (crosshairMessage != null) {
            int messageWidth = class_310.method_1551().field_1772.method_1727(crosshairMessage);
            RenderSystem.disableBlend();
            class_1041 window = class_310.method_1551().method_22683();
            guiGraphics.method_25303(class_310.method_1551().field_1772, crosshairMessage, window.method_4486() / 2 - messageWidth / 2, window.method_4502() / 2 + 60, -1);
            RenderSystem.enableBlend();
         }

         return false;
      }
   }

   public boolean handleClientPlayerChatReceivedEvent(class_7602 chatType, class_2561 component, GameProfile gameProfile) {
      return component == null ? false : this.handleChatMessage(gameProfile == null ? null : gameProfile.getName(), component);
   }

   public boolean handleClientSystemChatReceivedEvent(class_2561 component) {
      if (component == null) {
         return false;
      } else {
         String textString = component.getString();
         WorldMapSession worldmapSession;
         if (textString.contains("§r§e§s§e§t§x§a§e§r§o")) {
            worldmapSession = WorldMapSession.getCurrentSession();
            worldmapSession.getMapProcessor().setConsideringNetherFairPlayMessage(false);
            worldmapSession.getMapProcessor().setFairplayMessageReceived(false);
         }

         if (textString.contains("§x§a§e§r§o§w§m§n§e§t§h§e§r§i§s§f§a§i§r")) {
            worldmapSession = WorldMapSession.getCurrentSession();
            worldmapSession.getMapProcessor().setConsideringNetherFairPlayMessage(true);
         }

         if (textString.contains("§f§a§i§r§x§a§e§r§o")) {
            worldmapSession = WorldMapSession.getCurrentSession();
            worldmapSession.getMapProcessor().setFairplayMessageReceived(true);
         }

         return this.handleChatMessage(class_1074.method_4662("gui.xaero_waypoint_server_shared", new Object[0]), component);
      }
   }

   private boolean handleChatMessage(String playerName, class_2561 text) {
      return false;
   }

   public void handlePlayerTickStart(class_1657 player) {
      if (player == class_310.method_1551().field_1724) {
         WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
         if (worldmapSession != null) {
            worldmapSession.getControlsHandler().handleKeyEvents();
         }
      }

   }

   public void handleClientTickStart() {
      if (class_310.method_1551().field_1724 != null) {
         if (!WorldMap.loaded) {
            return;
         }

         WorldMap.crashHandler.checkForCrashes();
         WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
         if (worldmapSession != null) {
            MapProcessor mapProcessor = worldmapSession.getMapProcessor();
            mapProcessor.onClientTickStart();
         }
      }

   }

   public void handleClientRunTickStart() {
      if (class_310.method_1551().field_1724 != null) {
         if (!WorldMap.loaded) {
            return;
         }

         WorldMap.crashHandler.checkForCrashes();
         WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
         if (worldmapSession != null) {
            worldmapSession.getMapProcessor().getWorldDataHandler().handleRenderExecutor();
         }
      }

   }
}
