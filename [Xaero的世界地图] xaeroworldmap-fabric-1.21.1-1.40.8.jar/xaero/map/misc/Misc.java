package xaero.map.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import net.minecraft.class_1041;
import net.minecraft.class_1291;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2371;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_270;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_6880;
import net.minecraft.class_8251;
import net.minecraft.class_327.class_6415;
import net.minecraft.class_4597.class_4598;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import xaero.map.WorldMap;

public class Misc {
   private static final long[] ZERO_LONG_1024 = new long[1024];
   private static long cpuTimerPreTime;
   private static long glTimerPreTime;
   public static final String OUTDATED_FILE_EXT = ".outdated";

   public static class_2680 getStateById(int id) {
      try {
         return class_2248.method_9531(id);
      } catch (Exception var2) {
         return getDefaultBlockStateForStateId(id);
      }
   }

   private static class_2680 getDefaultBlockStateForStateId(int id) {
      try {
         return class_2248.method_9531(id).method_26204().method_9564();
      } catch (Exception var2) {
         return class_2246.field_10124.method_9564();
      }
   }

   public static void glTimerPre() {
      GL11.glFinish();
      glTimerPreTime = System.nanoTime();
   }

   public static int glTimerResult() {
      GL11.glFinish();
      return (int)(System.nanoTime() - glTimerPreTime);
   }

   public static void timerPre() {
      cpuTimerPreTime = System.nanoTime();
   }

   public static int timerResult() {
      return (int)(System.nanoTime() - cpuTimerPreTime);
   }

   public static double getMouseX(class_310 mc, boolean raw) {
      return raw ? mc.field_1729.method_1603() : mc.field_1729.method_1603() * (double)mc.method_22683().method_4489() / (double)mc.method_22683().method_4480();
   }

   public static double getMouseY(class_310 mc, boolean raw) {
      return raw ? mc.field_1729.method_1604() : mc.field_1729.method_1604() * (double)mc.method_22683().method_4506() / (double)mc.method_22683().method_4507();
   }

   public static void minecraftOrtho(class_310 mc, boolean raw) {
      class_1041 mainwindow = mc.method_22683();
      int width = raw ? mc.method_22683().method_4480() : mainwindow.method_4489();
      int height = raw ? mc.method_22683().method_4507() : mainwindow.method_4506();
      Matrix4f ortho = (new Matrix4f()).setOrtho(0.0F, (float)((double)width / mainwindow.method_4495()), (float)((double)height / mainwindow.method_4495()), 0.0F, 1000.0F, 21000.0F);
      RenderSystem.setProjectionMatrix(ortho, class_8251.field_43361);
   }

   public static void clearHeightsData1024(long[] data) {
      System.arraycopy(ZERO_LONG_1024, 0, data, 0, 1024);
   }

   public static <T extends Comparable<? super T>> void addToListOfSmallest(int maxSize, List<T> list, T element) {
      int currentSize = list.size();
      if (currentSize != maxSize || ((Comparable)list.get(currentSize - 1)).compareTo(element) > 0) {
         int iterLimit = currentSize == maxSize ? maxSize : currentSize + 1;

         for(int i = 0; i < iterLimit; ++i) {
            if (i == currentSize || element.compareTo(list.get(i)) < 0) {
               list.add(i, element);
               if (currentSize == maxSize) {
                  list.remove(currentSize);
               }
               break;
            }
         }

      }
   }

   public static Path convertToOutdated(Path path, int attempts) throws IOException {
      if (path.getFileName().toString().endsWith(".outdated")) {
         return path;
      } else {
         Path outdatedPath = path.resolveSibling(path.getFileName().toString() + ".outdated");
         if (Files.exists(path, new LinkOption[0])) {
            convertToOutdated(path, outdatedPath, attempts);
         }

         return outdatedPath;
      }
   }

   private static void convertToOutdated(Path path, Path outdatedPath, int attempts) throws IOException {
      --attempts;

      try {
         Files.move(path, outdatedPath, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException var6) {
         if (attempts <= 0) {
            throw var6;
         }

         WorldMap.LOGGER.info("Failed to convert file to outdated! Retrying... " + attempts);

         try {
            Thread.sleep(50L);
         } catch (InterruptedException var5) {
         }

         convertToOutdated(path, outdatedPath, attempts);
      }

   }

   public static void drawNormalText(class_4587 matrices, String name, float x, float y, int color, boolean shadow, class_4598 renderTypeBuffer) {
      class_310.method_1551().field_1772.method_27521(name, x, y, color, shadow, matrices.method_23760().method_23761(), renderTypeBuffer, class_6415.field_33993, 0, 15728880);
   }

   public static void drawPiercingText(class_4587 matrices, String name, float x, float y, int color, boolean shadow, class_4598 renderTypeBuffer) {
      class_310.method_1551().field_1772.method_27521(name, x, y, color, shadow, matrices.method_23760().method_23761(), renderTypeBuffer, class_6415.field_33994, 0, 15728880);
   }

   public static void drawPiercingText(class_4587 matrices, class_2561 name, float x, float y, int color, boolean shadow, class_4598 renderTypeBuffer) {
      class_310.method_1551().field_1772.method_30882(name, x, y, color, shadow, matrices.method_23760().method_23761(), renderTypeBuffer, class_6415.field_33994, 0, 15728880);
   }

   public static void drawCenteredPiercingText(class_4587 matrices, String name, float x, float y, int color, boolean shadow, class_4598 renderTypeBuffer) {
      drawPiercingText(matrices, name, x - (float)(class_310.method_1551().field_1772.method_1727(name) / 2), y, color, shadow, renderTypeBuffer);
   }

   public static void drawCenteredPiercingText(class_4587 matrices, class_2561 name, float x, float y, int color, boolean shadow, class_4598 renderTypeBuffer) {
      drawPiercingText(matrices, name, x - (float)(class_310.method_1551().field_1772.method_27525(name) / 2), y, color, shadow, renderTypeBuffer);
   }

   public static boolean hasItem(class_1657 player, class_1792 item) {
      return hasItem(player.method_31548().field_7544, -1, item) || hasItem(player.method_31548().field_7548, -1, item) || hasItem(player.method_31548().field_7547, 9, item);
   }

   public static boolean hasItem(class_2371<class_1799> inventory, int limit, class_1792 item) {
      for(int i = 0; i < inventory.size() && (limit == -1 || i < limit); ++i) {
         if (inventory.get(i) != null && ((class_1799)inventory.get(i)).method_7909() == item) {
            return true;
         }
      }

      return false;
   }

   public static int getTeamColour(class_1297 e) {
      Integer teamColour = null;
      class_270 team = e.method_5781();
      if (team != null) {
         teamColour = team.method_1202().method_532();
      }

      return teamColour == null ? -1 : teamColour;
   }

   public static boolean hasEffect(class_1657 player, class_6880<class_1291> effect) {
      return effect != null && player != null && player.method_6059(effect);
   }

   public static boolean hasEffect(class_6880<class_1291> effect) {
      return hasEffect(class_310.method_1551().field_1724, effect);
   }
}
