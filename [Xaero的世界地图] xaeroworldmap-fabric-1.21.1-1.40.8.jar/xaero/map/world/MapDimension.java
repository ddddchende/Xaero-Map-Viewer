package xaero.map.world;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.class_1132;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2378;
import net.minecraft.class_2874;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3218;
import net.minecraft.class_3532;
import net.minecraft.class_5294;
import net.minecraft.class_5321;
import net.minecraft.class_638;
import net.minecraft.class_7134;
import org.apache.commons.io.FileUtils;
import xaero.lib.client.config.ClientConfigManager;
import xaero.map.MapFullReloader;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.file.RegionDetection;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.highlight.HighlighterRegistry;
import xaero.map.region.LayeredRegionManager;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapLayer;
import xaero.map.region.MapRegion;
import xaero.map.util.linked.LinkedChain;

public class MapDimension {
   private final MapWorld mapWorld;
   private final class_5321<class_1937> dimId;
   private final List<String> multiworldIds;
   private final Hashtable<String, String> multiworldNames;
   private final Hashtable<String, String> autoMultiworldBindings;
   private final DimensionHighlighterHandler highlightHandler;
   private class_2960 dimensionTypeId;
   private class_2874 dimensionType;
   private class_5294 dimensionEffects;
   private float shadowR = 1.0F;
   private float shadowG = 1.0F;
   private float shadowB = 1.0F;
   private String futureAutoMultiworldBinding;
   private String futureCustomSelectedMultiworld;
   public boolean futureMultiworldWritable;
   public boolean futureMultiworldServerBased;
   private String currentMultiworld;
   public boolean currentMultiworldWritable;
   private String confirmedMultiworld;
   private final LayeredRegionManager mapRegions;
   private List<MapRegion> regionBackCompList;
   private final Hashtable<Integer, Hashtable<Integer, RegionDetection>> worldSaveDetectedRegions;
   private final LinkedChain<RegionDetection> worldSaveDetectedRegionsLinked;
   private boolean doneRegionDetection;
   public final ArrayList<LeveledRegion<?>> regionsToCache;
   private MapFullReloader fullReloader;
   private int caveModeType;
   private static final int CAVE_MODE_TYPES = 3;

   public MapDimension(MapWorld mapWorld, class_5321<class_1937> dimId, HighlighterRegistry highlighterRegistry) {
      this.mapWorld = mapWorld;
      this.dimId = dimId;
      this.multiworldIds = new ArrayList();
      this.multiworldNames = new Hashtable();
      this.mapRegions = new LayeredRegionManager(this);
      this.autoMultiworldBindings = new Hashtable();
      this.regionsToCache = new ArrayList();
      this.regionBackCompList = new ArrayList();
      this.highlightHandler = new DimensionHighlighterHandler(this, dimId, highlighterRegistry);
      this.worldSaveDetectedRegions = new Hashtable();
      this.worldSaveDetectedRegionsLinked = new LinkedChain();
      ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
      this.caveModeType = (Integer)configManager.getEffective(WorldMapProfiledConfigOptions.DEFAULT_CAVE_MODE_TYPE);
   }

   public String getCurrentMultiworld() {
      return this.currentMultiworld;
   }

   public boolean isUsingWorldSave() {
      return !this.mapWorld.isMultiplayer() && (this.currentMultiworld == null || this.currentMultiworld.isEmpty());
   }

   public boolean isFutureUsingWorldSaveUnsynced() {
      return !this.mapWorld.isMultiplayer() && (this.getFutureMultiworldUnsynced() == null || this.getFutureMultiworldUnsynced().isEmpty());
   }

   public List<String> getMultiworldIdsCopy() {
      synchronized(this.multiworldIds) {
         return new ArrayList(this.multiworldIds);
      }
   }

   public void updateFutureAutomaticUnsynced(class_310 mc, Object baseObject) {
      if (!this.mapWorld.isMultiplayer()) {
         this.futureAutoMultiworldBinding = "";
         this.futureMultiworldServerBased = false;
      } else if (baseObject != null) {
         if (baseObject instanceof class_2338) {
            class_2338 dimSpawn = (class_2338)baseObject;
            int var10001 = dimSpawn.method_10263() >> 6;
            this.futureAutoMultiworldBinding = "mw" + var10001 + "," + (dimSpawn.method_10264() >> 6) + "," + (dimSpawn.method_10260() >> 6);
            this.futureMultiworldServerBased = false;
         } else if (baseObject instanceof Integer) {
            int levelId = (Integer)baseObject;
            this.futureAutoMultiworldBinding = "mw$" + levelId;
            this.futureMultiworldServerBased = true;
         }
      } else {
         this.futureAutoMultiworldBinding = "unknown";
      }

   }

   public String getFutureCustomSelectedMultiworld() {
      return this.futureCustomSelectedMultiworld;
   }

   public String getFutureMultiworldUnsynced() {
      return this.futureCustomSelectedMultiworld == null ? this.getFutureAutoMultiworld() : this.futureCustomSelectedMultiworld;
   }

   public void switchToFutureUnsynced() {
      this.currentMultiworld = this.getFutureMultiworldUnsynced();
      this.addMultiworldChecked(this.currentMultiworld);
   }

   public void switchToFutureMultiworldWritableValueUnsynced() {
      this.currentMultiworldWritable = this.futureMultiworldWritable;
   }

   public LayeredRegionManager getLayeredMapRegions() {
      return this.mapRegions;
   }

   public void clear() {
      this.regionsToCache.clear();
      this.mapRegions.clear();
      this.regionBackCompList.clear();
      this.worldSaveDetectedRegions.clear();
      this.worldSaveDetectedRegionsLinked.reset();
      this.doneRegionDetection = false;
      this.clearFullMapReload();
   }

   public void preDetection() {
      this.doneRegionDetection = true;
      this.mapRegions.preDetection();
   }

   public Path getMainFolderPath() {
      return this.mapWorld.getMapProcessor().getMapSaveLoad().getMainFolder(this.mapWorld.getMainId(), this.mapWorld.getMapProcessor().getDimensionName(this.dimId));
   }

   public Path getOldFolderPath() {
      return this.mapWorld.getMapProcessor().getMapSaveLoad().getOldFolder(this.mapWorld.getOldUnfixedMainId(), this.mapWorld.getMapProcessor().getDimensionName(this.dimId));
   }

   public void saveConfigUnsynced() {
      Path dimensionSavePath = this.getMainFolderPath();

      try {
         BufferedOutputStream bufferedOutput = new BufferedOutputStream(new FileOutputStream(dimensionSavePath.resolve("dimension_config.txt").toFile()));

         try {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(bufferedOutput, StandardCharsets.UTF_8));

            try {
               if (this.confirmedMultiworld != null) {
                  writer.println("confirmedMultiworld:" + this.confirmedMultiworld);
               }

               Iterator var4 = this.autoMultiworldBindings.entrySet().iterator();

               String var10001;
               Entry bindingEntry;
               while(var4.hasNext()) {
                  bindingEntry = (Entry)var4.next();
                  var10001 = (String)bindingEntry.getKey();
                  writer.println("autoMWBinding:" + var10001 + ":" + (String)bindingEntry.getValue());
               }

               var4 = this.multiworldNames.entrySet().iterator();

               while(var4.hasNext()) {
                  bindingEntry = (Entry)var4.next();
                  var10001 = (String)bindingEntry.getKey();
                  writer.println("MWName:" + var10001 + ":" + ((String)bindingEntry.getValue()).replace(":", "^col^"));
               }

               writer.println("caveModeType:" + this.caveModeType);
               if (this.dimensionTypeId != null) {
                  writer.println("dimensionTypeId:" + String.valueOf(this.dimensionTypeId));
               }
            } catch (Throwable var8) {
               try {
                  writer.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            writer.close();
         } catch (Throwable var9) {
            try {
               bufferedOutput.close();
            } catch (Throwable var6) {
               var9.addSuppressed(var6);
            }

            throw var9;
         }

         bufferedOutput.close();
      } catch (IOException var10) {
         WorldMap.LOGGER.error("suppressed exception", var10);
      }

   }

   private void loadConfigUnsynced(int attempts) {
      Path dimensionSavePath = this.getMainFolderPath();
      BufferedReader reader = null;

      try {
         try {
            Path oldDimensionSavePath = this.getOldFolderPath();
            if (!Files.exists(dimensionSavePath, new LinkOption[0]) && Files.exists(oldDimensionSavePath, new LinkOption[0])) {
               Files.move(oldDimensionSavePath, dimensionSavePath);
            }

            if (!Files.exists(dimensionSavePath, new LinkOption[0])) {
               Files.createDirectories(dimensionSavePath);
            }

            this.loadMultiworldsList(dimensionSavePath);
            Path configFile = dimensionSavePath.resolve("dimension_config.txt");
            if (Files.exists(configFile, new LinkOption[0])) {
               reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile.toFile()), "UTF8"));

               String line;
               while((line = reader.readLine()) != null) {
                  String[] args = line.split(":");
                  if (args[0].equals("confirmedMultiworld")) {
                     String savedMultiworld = args.length > 1 ? args[1] : "";
                     if (this.multiworldIds.contains(savedMultiworld)) {
                        this.confirmedMultiworld = savedMultiworld;
                     }
                  } else if (args[0].equals("autoMWBinding")) {
                     this.bindAutoMultiworld(args[1], args[2]);
                  } else if (args[0].equals("MWName")) {
                     this.setMultiworldName(args[1], args[2].replace("^col^", ":"));
                  } else if (args[0].equals("dimensionTypeId")) {
                     this.dimensionTypeId = class_2960.method_60654(line.substring(line.indexOf(58) + 1));
                  }

                  if (args[0].equals("caveModeType")) {
                     this.caveModeType = Integer.parseInt(args[1]);
                  }
               }

               return;
            }

            this.saveConfigUnsynced();
            return;
         } catch (IOException var21) {
            if (attempts <= 1) {
               throw new RuntimeException(var21);
            }
         }

         if (reader != null) {
            try {
               reader.close();
            } catch (IOException var20) {
               throw new RuntimeException(var20);
            }
         }

         WorldMap.LOGGER.warn("IO exception while loading world map dimension config. Retrying... " + attempts);

         try {
            Thread.sleep(20L);
         } catch (InterruptedException var19) {
         }

         this.loadConfigUnsynced(attempts - 1);
      } finally {
         if (reader != null) {
            try {
               reader.close();
            } catch (IOException var18) {
               WorldMap.LOGGER.error("suppressed exception", var18);
            }
         }

      }

   }

   public void pickDefaultCustomMultiworldUnsynced() {
      if (this.multiworldIds.isEmpty()) {
         this.futureCustomSelectedMultiworld = "mw$default";
         this.multiworldIds.add(this.futureCustomSelectedMultiworld);
         this.setMultiworldName(this.futureCustomSelectedMultiworld, "Default");
      } else {
         int indexOfAuto = this.multiworldIds.indexOf(this.getFutureAutoMultiworld());
         this.futureCustomSelectedMultiworld = (String)this.multiworldIds.get(indexOfAuto != -1 ? indexOfAuto : 0);
      }

   }

   private void loadMultiworldsList(Path dimensionSavePath) {
      if (!this.mapWorld.isMultiplayer()) {
         this.multiworldIds.add("");
      }

      try {
         Stream<Path> subFolders = Files.list(dimensionSavePath);
         Iterator iter = subFolders.iterator();

         while(true) {
            String folderName;
            boolean autoMultiworldFormat;
            boolean levelIdMultiworldFormat;
            boolean customMultiworldFormat;
            do {
               Path path;
               do {
                  if (!iter.hasNext()) {
                     subFolders.close();
                     return;
                  }

                  path = (Path)iter.next();
               } while(!path.toFile().isDirectory());

               folderName = path.getFileName().toString();
               autoMultiworldFormat = folderName.matches("^mw(-?\\d+),(-?\\d+),(-?\\d+)$");
               levelIdMultiworldFormat = folderName.startsWith("mw$");
               customMultiworldFormat = folderName.startsWith("cm$");
            } while(!autoMultiworldFormat && !levelIdMultiworldFormat && !customMultiworldFormat);

            this.multiworldIds.add(folderName);
         }
      } catch (IOException var9) {
         WorldMap.LOGGER.error("suppressed exception", var9);
      }
   }

   public void confirmMultiworldUnsynced() {
      if (!this.futureMultiworldWritable) {
         this.futureMultiworldWritable = true;
         if (this.mapWorld.getFutureMultiworldType(this) == 2 && this.futureCustomSelectedMultiworld != null) {
            this.makeCustomSelectedMultiworldAutoUnsynced();
         }

         this.confirmedMultiworld = this.getFutureMultiworldUnsynced();
         this.saveConfigUnsynced();
      }

   }

   private void makeCustomSelectedMultiworldAutoUnsynced() {
      String currentAutoMultiworld = this.getFutureAutoMultiworld();
      boolean currentBindingFound = false;
      Iterator var3 = this.autoMultiworldBindings.entrySet().iterator();

      while(var3.hasNext()) {
         Entry<String, String> bindingEntry = (Entry)var3.next();
         if (((String)bindingEntry.getValue()).equals(this.futureCustomSelectedMultiworld)) {
            this.bindAutoMultiworld((String)bindingEntry.getKey(), currentAutoMultiworld);
            currentBindingFound = true;
            break;
         }
      }

      if (!currentBindingFound && !this.futureCustomSelectedMultiworld.startsWith("cm$")) {
         this.bindAutoMultiworld(this.futureCustomSelectedMultiworld, currentAutoMultiworld);
      }

      this.bindAutoMultiworld(this.futureAutoMultiworldBinding, this.futureCustomSelectedMultiworld);
      this.futureCustomSelectedMultiworld = null;
      this.saveConfigUnsynced();
   }

   private void bindAutoMultiworld(String binding, String multiworld) {
      if (binding.equals(multiworld)) {
         this.autoMultiworldBindings.remove(binding);
      } else {
         this.autoMultiworldBindings.put(binding, multiworld);
      }

   }

   public void resetCustomMultiworldUnsynced() {
      this.futureCustomSelectedMultiworld = this.mapWorld.getFutureMultiworldType(this) == 2 ? null : this.confirmedMultiworld;
      if (this.futureCustomSelectedMultiworld == null && this.mapWorld.isMultiplayer() && this.mapWorld.getFutureMultiworldType(this) < 2) {
         this.pickDefaultCustomMultiworldUnsynced();
      }

      this.futureMultiworldWritable = this.mapWorld.getFutureMultiworldType(this) != 1 && this.mapWorld.isFutureMultiworldTypeConfirmed(this);
   }

   public void setMultiworldUnsynced(String nextMW) {
      String cmw = this.futureCustomSelectedMultiworld == null ? this.getFutureMultiworldUnsynced() : this.futureCustomSelectedMultiworld;
      this.futureCustomSelectedMultiworld = nextMW;
      this.futureMultiworldWritable = false;
      WorldMap.LOGGER.info(cmw + " -> " + this.futureCustomSelectedMultiworld);
   }

   private boolean multiworldExists(String mw) {
      synchronized(this.multiworldIds) {
         return this.multiworldIds.contains(mw);
      }
   }

   public boolean addMultiworldChecked(String mw) {
      synchronized(this.multiworldIds) {
         if (!this.multiworldIds.contains(mw)) {
            this.multiworldIds.add(mw);
            return true;
         } else {
            return false;
         }
      }
   }

   public String getMultiworldName(String mwId) {
      if (mwId.isEmpty()) {
         return "gui.xaero_world_save";
      } else {
         String tableName = (String)this.multiworldNames.get(mwId);
         if (tableName != null) {
            return tableName;
         } else if (!this.multiworldExists(mwId)) {
            return mwId;
         } else {
            int var4 = 1;

            int var10001;
            String automaticName;
            do {
               var10001 = var4++;
            } while(this.multiworldNames.containsValue(automaticName = "Map " + var10001));

            this.setMultiworldName(mwId, automaticName);
            synchronized(this.mapWorld.getMapProcessor().uiSync) {
               this.saveConfigUnsynced();
               return automaticName;
            }
         }
      }
   }

   public void setMultiworldName(String mwId, String mwName) {
      this.multiworldNames.put(mwId, mwName);
   }

   private String getFutureAutoMultiworld() {
      String futureAutoMultiworldBinding = this.futureAutoMultiworldBinding;
      if (futureAutoMultiworldBinding == null) {
         return null;
      } else {
         String boundMultiworld = (String)this.autoMultiworldBindings.get(futureAutoMultiworldBinding);
         return boundMultiworld == null ? futureAutoMultiworldBinding : boundMultiworld;
      }
   }

   public MapWorld getMapWorld() {
      return this.mapWorld;
   }

   public void deleteMultiworldMapDataUnsynced(String mwId) {
      try {
         Path currentDimFolder = this.getMainFolderPath();
         Path currentMWFolder = currentDimFolder.resolve(mwId);
         Path binFolder = currentDimFolder.resolve("last deleted");
         Path binMWFolder = binFolder.resolve(mwId);
         if (!Files.exists(binFolder, new LinkOption[0])) {
            Files.createDirectories(binFolder);
         }

         FileUtils.cleanDirectory(binFolder.toFile());
         Files.move(currentMWFolder, binMWFolder);
      } catch (Exception var6) {
         WorldMap.LOGGER.error("suppressed exception", var6);
      }

   }

   public void deleteMultiworldId(String mwId) {
      synchronized(this.multiworldIds) {
         this.multiworldIds.remove(mwId);
         this.multiworldNames.remove(mwId);
         if (mwId.equals(this.confirmedMultiworld)) {
            this.confirmedMultiworld = null;
         }

      }
   }

   public class_5321<class_1937> getDimId() {
      return this.dimId;
   }

   public boolean hasConfirmedMultiworld() {
      return this.confirmedMultiworld != null;
   }

   public boolean isFutureMultiworldServerBased() {
      return this.futureMultiworldServerBased;
   }

   public DimensionHighlighterHandler getHighlightHandler() {
      return this.highlightHandler;
   }

   public void onClearCachedHighlightHash(int regionX, int regionZ) {
      this.mapRegions.onClearCachedHighlightHash(regionX, regionZ);
   }

   public void onClearCachedHighlightHashes() {
      this.mapRegions.onClearCachedHighlightHashes();
   }

   public boolean hasDoneRegionDetection() {
      return this.doneRegionDetection;
   }

   public void addWorldSaveRegionDetection(RegionDetection regionDetection) {
      synchronized(this.worldSaveDetectedRegions) {
         Hashtable<Integer, RegionDetection> column = (Hashtable)this.worldSaveDetectedRegions.get(regionDetection.getRegionX());
         if (column == null) {
            this.worldSaveDetectedRegions.put(regionDetection.getRegionX(), column = new Hashtable());
         }

         column.put(regionDetection.getRegionZ(), regionDetection);
         this.worldSaveDetectedRegionsLinked.add(regionDetection);
      }
   }

   public RegionDetection getWorldSaveRegionDetection(int x, int z) {
      if (this.worldSaveDetectedRegions == null) {
         return null;
      } else {
         Hashtable<Integer, RegionDetection> column = (Hashtable)this.worldSaveDetectedRegions.get(x);
         return column != null ? (RegionDetection)column.get(z) : null;
      }
   }

   public int getCaveModeType() {
      return this.caveModeType;
   }

   public void toggleCaveModeType(boolean forward) {
      this.caveModeType += forward ? 1 : -1;
      if (forward) {
         if (this.caveModeType >= 3) {
            this.caveModeType = 0;
         }

      } else {
         if (this.caveModeType < 0) {
            this.caveModeType = 2;
         }

      }
   }

   public Iterable<Hashtable<Integer, RegionDetection>> getWorldSaveDetectedRegions() {
      return this.worldSaveDetectedRegions.values();
   }

   public Iterable<RegionDetection> getLinkedWorldSaveDetectedRegions() {
      return this.worldSaveDetectedRegionsLinked;
   }

   public MapFullReloader getFullReloader() {
      return this.fullReloader;
   }

   public void startFullMapReload(int caveLayer, boolean resave, MapProcessor mapProcessor) {
      MapLayer layer = this.mapRegions.getLayer(caveLayer);
      this.fullReloader = new MapFullReloader(caveLayer, resave, layer.getLinkedCompleteWorldSaveDetectedRegions().iterator(), this, mapProcessor);
   }

   public void clearFullMapReload() {
      this.fullReloader = null;
   }

   public class_2874 getDimensionType(class_2378<class_2874> dimensionTypes) {
      if (this.dimensionType != null) {
         return this.dimensionType;
      } else {
         this.dimensionType = getDimensionType(this.dimId, this.dimensionTypeId, dimensionTypes);
         return this.dimensionType;
      }
   }

   private static class_2874 getDimensionType(class_5321<class_1937> dimId, class_2960 dimensionTypeId, class_2378<class_2874> dimensionTypes) {
      if (dimensionTypeId == null) {
         if (dimId == class_1937.field_25180) {
            dimensionTypeId = class_7134.field_37671;
         } else if (dimId == class_1937.field_25179) {
            dimensionTypeId = class_7134.field_37670;
         } else {
            if (dimId != class_1937.field_25181) {
               class_1132 integratedServer = class_310.method_1551().method_1576();
               if (integratedServer == null) {
                  return null;
               }

               class_3218 serverLevel = integratedServer.method_3847(dimId);
               if (serverLevel == null) {
                  return null;
               }

               return serverLevel.method_8597();
            }

            dimensionTypeId = class_7134.field_37672;
         }
      }

      return dimensionTypes == null ? null : (class_2874)dimensionTypes.method_10223(dimensionTypeId);
   }

   public static class_2874 getDimensionType(MapDimension dim, class_5321<class_1937> dimId, class_2378<class_2874> dimensionTypes) {
      return dim == null ? getDimensionType((class_5321)dimId, (class_2960)null, dimensionTypes) : dim.getDimensionType(dimensionTypes);
   }

   public void onWorldChangeUnsynced(class_1937 newWorld) {
      if (newWorld != null && this.dimId.equals(newWorld.method_27983())) {
         class_5321<class_2874> dimTypeId = (class_5321)newWorld.method_40134().method_40230().get();
         if (!dimTypeId.method_29177().equals(this.dimensionTypeId)) {
            this.dimensionTypeId = dimTypeId.method_29177();
            this.saveConfigUnsynced();
         }
      }

      this.dimensionType = null;
      this.dimensionEffects = null;
   }

   public boolean isUsingUnknownDimensionType(class_2378<class_2874> dimensionTypes) {
      return this.getDimensionType(dimensionTypes) == null;
   }

   public boolean isCacheOnlyMode(class_2378<class_2874> dimensionTypes) {
      return this.isUsingUnknownDimensionType(dimensionTypes);
   }

   public void onCreationUnsynced() {
      this.loadConfigUnsynced(10);
      if (this.dimId != class_1937.field_25179 && !class_7134.field_37670.equals(this.dimensionTypeId)) {
         if (this.dimId == class_1937.field_25180 || class_7134.field_37671.equals(this.dimensionTypeId)) {
            this.shadowR = 1.0F;
            this.shadowG = 0.0F;
            this.shadowB = 0.0F;
         }
      } else {
         this.shadowR = 0.518F;
         this.shadowG = 0.678F;
         this.shadowB = 1.0F;
      }

   }

   public float getShadowR() {
      return this.shadowR;
   }

   public float getShadowG() {
      return this.shadowG;
   }

   public float getShadowB() {
      return this.shadowB;
   }

   public class_5294 getDimensionEffects(class_2378<class_2874> dimensionTypes) {
      if (this.dimensionEffects == null) {
         class_2874 type = this.getDimensionType(dimensionTypes);
         if (type == null) {
            return null;
         }

         this.dimensionEffects = class_5294.method_28111(type);
      }

      return this.dimensionEffects;
   }

   public float getSkyDarken(float partial, class_638 world, class_2378<class_2874> dimensionTypes) {
      if (this.dimId == world.method_27983()) {
         return world.method_23783(1.0F);
      } else {
         class_2874 dimType = this.getDimensionType(dimensionTypes);
         if (dimType == null) {
            return 1.0F;
         } else {
            float timeOfDay = dimType.method_28528(world.method_30271());
            float brightness = 1.0F - (class_3532.method_15362(timeOfDay * 6.2831855F) * 2.0F + 0.2F);
            brightness = 1.0F - class_3532.method_15363(brightness, 0.0F, 1.0F);
            return brightness * 0.8F + 0.2F;
         }
      }
   }

   public double calculateDimScale(class_2378<class_2874> dimensionTypes) {
      class_2874 dimType = this.getDimensionType(dimensionTypes);
      return dimType == null ? 1.0D : dimType.comp_646();
   }

   public double calculateDimDiv(class_2378<class_2874> dimensionTypes, class_2874 actualDimension) {
      return this.calculateDimScale(dimensionTypes) / (actualDimension == null ? 1.0D : actualDimension.comp_646());
   }

   public MapConnectionNode getPlayerMapKey() {
      String playerMW = this.mapWorld.getFutureMultiworldType(this) == 1 ? null : this.getFutureAutoMultiworld();
      if (playerMW == null) {
         playerMW = this.confirmedMultiworld;
      }

      return playerMW == null ? null : new MapConnectionNode(this.dimId, playerMW);
   }

   public MapConnectionNode getSelectedMapKeyUnsynced() {
      String selectedMW = this.getFutureMultiworldUnsynced();
      if (selectedMW == null) {
         selectedMW = this.getCurrentMultiworld();
      }

      return selectedMW == null ? null : new MapConnectionNode(this.dimId, selectedMW);
   }

   public boolean isAutoSelected() {
      String selectedMW = this.getFutureCustomSelectedMultiworld();
      return selectedMW == null || selectedMW.equals(this.getFutureAutoMultiworld());
   }

   public class_2960 getDimensionTypeId() {
      return this.dimensionTypeId;
   }
}
