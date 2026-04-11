package xaero.map.file;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_2246;
import net.minecraft.class_2487;
import net.minecraft.class_2507;
import net.minecraft.class_2519;
import net.minecraft.class_2520;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_7923;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.config.util.WorldMapClientConfigUtils;

public class OldFormatSupport {
   private class_2960 vanillaStatesResource = class_2960.method_60655("xaeroworldmap", "vanilla_states.dat");
   private boolean vanillaStatesLoaded;
   private HashMap<Integer, HashMap<Integer, class_2680>> vanilla_states = new HashMap();
   private ImmutableMap<String, String> blockRename1314 = ImmutableMap.of("minecraft:stone_slab", "minecraft:smooth_stone_slab", "minecraft:sign", "minecraft:oak_sign", "minecraft:wall_sign", "minecraft:oak_wall_sign");
   private ImmutableMap<String, OldFormatSupport.Fixer> blockFixes1516;
   private ImmutableMap<String, OldFormatSupport.Fixer> blockFixes1617;
   private Map<String, String> JIGSAW_ORIENTATION_UPDATES16 = ImmutableMap.builder().put("", "north_up").put("down", "down_south").put("up", "up_north").put("north", "north_up").put("south", "south_up").put("west", "west_up").put("east", "east_up").build();
   private OldFormatSupport.Fixer wallFix = (nbt) -> {
      class_2487 properties = nbt.method_10562("Properties");
      properties.method_10582("east", properties.method_10558("east").equals("true") ? "low" : "none");
      properties.method_10582("west", properties.method_10558("west").equals("true") ? "low" : "none");
      properties.method_10582("north", properties.method_10558("north").equals("true") ? "low" : "none");
      properties.method_10582("south", properties.method_10558("south").equals("true") ? "low" : "none");
   };
   public ImmutableMap<String, String> cc2BiomeRenames = ImmutableMap.builder().put("minecraft:badlands_plateau", "minecraft:badlands").put("minecraft:bamboo_jungle_hills", "minecraft:bamboo_jungle").put("minecraft:birch_forest_hills", "minecraft:birch_forest").put("minecraft:dark_forest_hills", "minecraft:dark_forest").put("minecraft:desert_hills", "minecraft:desert").put("minecraft:desert_lakes", "minecraft:desert").put("minecraft:giant_spruce_taiga_hills", "minecraft:old_growth_spruce_taiga").put("minecraft:giant_spruce_taiga", "minecraft:old_growth_spruce_taiga").put("minecraft:giant_tree_taiga_hills", "minecraft:old_growth_pine_taiga").put("minecraft:giant_tree_taiga", "minecraft:old_growth_pine_taiga").put("minecraft:gravelly_mountains", "minecraft:windswept_gravelly_hills").put("minecraft:jungle_edge", "minecraft:sparse_jungle").put("minecraft:jungle_hills", "minecraft:jungle").put("minecraft:modified_badlands_plateau", "minecraft:badlands").put("minecraft:modified_gravelly_mountains", "minecraft:windswept_gravelly_hills").put("minecraft:modified_jungle_edge", "minecraft:sparse_jungle").put("minecraft:modified_jungle", "minecraft:jungle").put("minecraft:modified_wooded_badlands_plateau", "minecraft:wooded_badlands").put("minecraft:mountain_edge", "minecraft:windswept_hills").put("minecraft:mountains", "minecraft:windswept_hills").put("minecraft:mushroom_field_shore", "minecraft:mushroom_fields").put("minecraft:shattered_savanna", "minecraft:windswept_savanna").put("minecraft:shattered_savanna_plateau", "minecraft:windswept_savanna").put("minecraft:snowy_mountains", "minecraft:snowy_plains").put("minecraft:snowy_taiga_hills", "minecraft:snowy_taiga").put("minecraft:snowy_taiga_mountains", "minecraft:snowy_taiga").put("minecraft:snowy_tundra", "minecraft:snowy_plains").put("minecraft:stone_shore", "minecraft:stony_shore").put("minecraft:swamp_hills", "minecraft:swamp").put("minecraft:taiga_hills", "minecraft:taiga").put("minecraft:taiga_mountains", "minecraft:taiga").put("minecraft:tall_birch_forest", "minecraft:old_growth_birch_forest").put("minecraft:tall_birch_hills", "minecraft:old_growth_birch_forest").put("minecraft:wooded_badlands_plateau", "minecraft:wooded_badlands").put("minecraft:wooded_hills", "minecraft:forest").put("minecraft:wooded_mountains", "minecraft:windswept_forest").put("minecraft:lofty_peaks", "minecraft:jagged_peaks").put("minecraft:snowcapped_peaks", "minecraft:frozen_peaks").build();
   private Int2ObjectMap<String> biomesById = new Int2ObjectOpenHashMap();

   public OldFormatSupport() {
      this.blockFixes1516 = ImmutableMap.builder().put("minecraft:jigsaw", (nbt) -> {
         class_2487 properties = nbt.method_10562("Properties");
         String facing = properties.method_10558("facing");
         properties.method_10551("facing");
         properties.method_10582("orientation", (String)this.JIGSAW_ORIENTATION_UPDATES16.get(facing));
      }).put("minecraft:redstone_wire", (nbt) -> {
         class_2487 properties = nbt.method_10562("Properties");
         String east = properties.method_10558("east");
         String west = properties.method_10558("west");
         String north = properties.method_10558("north");
         String south = properties.method_10558("south");
         if (east.equals("")) {
            east = "none";
         }

         if (west.equals("")) {
            west = "none";
         }

         if (north.equals("")) {
            north = "none";
         }

         if (south.equals("")) {
            south = "none";
         }

         boolean hasEast = !east.equals("none");
         boolean hasWest = !west.equals("none");
         boolean hasNorth = !north.equals("none");
         boolean hasSouth = !south.equals("none");
         boolean hasHorizontal = hasWest || hasEast;
         boolean hasVertical = hasNorth || hasSouth;
         east = !hasEast && !hasVertical ? "side" : east;
         west = !hasWest && !hasVertical ? "side" : west;
         north = !hasNorth && !hasHorizontal ? "side" : north;
         south = !hasSouth && !hasHorizontal ? "side" : south;
         properties.method_10582("east", east);
         properties.method_10582("west", west);
         properties.method_10582("north", north);
         properties.method_10582("south", south);
      }).put("minecraft:andesite_wall", this.wallFix).put("minecraft:brick_wall", this.wallFix).put("minecraft:cobblestone_wall", this.wallFix).put("minecraft:diorite_wall", this.wallFix).put("minecraft:end_stone_brick_wall", this.wallFix).put("minecraft:granite_wall", this.wallFix).put("minecraft:mossy_cobblestone_wall", this.wallFix).put("minecraft:mossy_stone_brick_wall", this.wallFix).put("minecraft:nether_brick_wall", this.wallFix).put("minecraft:prismarine_wall", this.wallFix).put("minecraft:red_nether_brick_wall", this.wallFix).put("minecraft:red_sandstone_wall", this.wallFix).put("minecraft:sandstone_wall", this.wallFix).put("minecraft:stone_brick_wall", this.wallFix).build();
      this.blockFixes1617 = ImmutableMap.builder().put("minecraft:cauldron", (nbt) -> {
         class_2487 properties = nbt.method_10562("Properties");
         if (!properties.method_33133()) {
            class_2520 level = properties.method_10580("level");
            if (level != null && !level.method_10714().equals("0")) {
               nbt.method_10566("Name", class_2519.method_23256("minecraft:water_cauldron"));
            } else {
               nbt.method_10551("Properties");
            }
         }

      }).put("minecraft:grass_path", (nbt) -> {
         nbt.method_10566("Name", class_2519.method_23256("minecraft:dirt_path"));
      }).build();
      this.biomesById.put(0, "minecraft:ocean");
      this.biomesById.put(1, "minecraft:plains");
      this.biomesById.put(2, "minecraft:desert");
      this.biomesById.put(3, "minecraft:mountains");
      this.biomesById.put(4, "minecraft:forest");
      this.biomesById.put(5, "minecraft:taiga");
      this.biomesById.put(6, "minecraft:swamp");
      this.biomesById.put(7, "minecraft:river");
      this.biomesById.put(8, "minecraft:nether_wastes");
      this.biomesById.put(9, "minecraft:the_end");
      this.biomesById.put(10, "minecraft:frozen_ocean");
      this.biomesById.put(11, "minecraft:frozen_river");
      this.biomesById.put(12, "minecraft:snowy_tundra");
      this.biomesById.put(13, "minecraft:snowy_mountains");
      this.biomesById.put(14, "minecraft:mushroom_fields");
      this.biomesById.put(15, "minecraft:mushroom_field_shore");
      this.biomesById.put(16, "minecraft:beach");
      this.biomesById.put(17, "minecraft:desert_hills");
      this.biomesById.put(18, "minecraft:wooded_hills");
      this.biomesById.put(19, "minecraft:taiga_hills");
      this.biomesById.put(20, "minecraft:mountain_edge");
      this.biomesById.put(21, "minecraft:jungle");
      this.biomesById.put(22, "minecraft:jungle_hills");
      this.biomesById.put(23, "minecraft:jungle_edge");
      this.biomesById.put(24, "minecraft:deep_ocean");
      this.biomesById.put(25, "minecraft:stone_shore");
      this.biomesById.put(26, "minecraft:snowy_beach");
      this.biomesById.put(27, "minecraft:birch_forest");
      this.biomesById.put(28, "minecraft:birch_forest_hills");
      this.biomesById.put(29, "minecraft:dark_forest");
      this.biomesById.put(30, "minecraft:snowy_taiga");
      this.biomesById.put(31, "minecraft:snowy_taiga_hills");
      this.biomesById.put(32, "minecraft:giant_tree_taiga");
      this.biomesById.put(33, "minecraft:giant_tree_taiga_hills");
      this.biomesById.put(34, "minecraft:wooded_mountains");
      this.biomesById.put(35, "minecraft:savanna");
      this.biomesById.put(36, "minecraft:savanna_plateau");
      this.biomesById.put(37, "minecraft:badlands");
      this.biomesById.put(38, "minecraft:wooded_badlands_plateau");
      this.biomesById.put(39, "minecraft:badlands_plateau");
      this.biomesById.put(40, "minecraft:small_end_islands");
      this.biomesById.put(41, "minecraft:end_midlands");
      this.biomesById.put(42, "minecraft:end_highlands");
      this.biomesById.put(43, "minecraft:end_barrens");
      this.biomesById.put(44, "minecraft:warm_ocean");
      this.biomesById.put(45, "minecraft:lukewarm_ocean");
      this.biomesById.put(46, "minecraft:cold_ocean");
      this.biomesById.put(47, "minecraft:deep_warm_ocean");
      this.biomesById.put(48, "minecraft:deep_lukewarm_ocean");
      this.biomesById.put(49, "minecraft:deep_cold_ocean");
      this.biomesById.put(50, "minecraft:deep_frozen_ocean");
      this.biomesById.put(127, "minecraft:the_void");
      this.biomesById.put(129, "minecraft:sunflower_plains");
      this.biomesById.put(130, "minecraft:desert_lakes");
      this.biomesById.put(131, "minecraft:gravelly_mountains");
      this.biomesById.put(132, "minecraft:flower_forest");
      this.biomesById.put(133, "minecraft:taiga_mountains");
      this.biomesById.put(134, "minecraft:swamp_hills");
      this.biomesById.put(140, "minecraft:ice_spikes");
      this.biomesById.put(149, "minecraft:modified_jungle");
      this.biomesById.put(151, "minecraft:modified_jungle_edge");
      this.biomesById.put(155, "minecraft:tall_birch_forest");
      this.biomesById.put(156, "minecraft:tall_birch_hills");
      this.biomesById.put(157, "minecraft:dark_forest_hills");
      this.biomesById.put(158, "minecraft:snowy_taiga_mountains");
      this.biomesById.put(160, "minecraft:giant_spruce_taiga");
      this.biomesById.put(161, "minecraft:giant_spruce_taiga_hills");
      this.biomesById.put(162, "minecraft:modified_gravelly_mountains");
      this.biomesById.put(163, "minecraft:shattered_savanna");
      this.biomesById.put(164, "minecraft:shattered_savanna_plateau");
      this.biomesById.put(165, "minecraft:eroded_badlands");
      this.biomesById.put(166, "minecraft:modified_wooded_badlands_plateau");
      this.biomesById.put(167, "minecraft:modified_badlands_plateau");
      this.biomesById.put(168, "minecraft:bamboo_jungle");
      this.biomesById.put(169, "minecraft:bamboo_jungle_hills");
      this.biomesById.put(170, "minecraft:soul_sand_valley");
      this.biomesById.put(171, "minecraft:crimson_forest");
      this.biomesById.put(172, "minecraft:warped_forest");
      this.biomesById.put(173, "minecraft:basalt_deltas");
      this.biomesById.put(174, "minecraft:dripstone_caves");
      this.biomesById.put(175, "minecraft:lush_caves");
      this.biomesById.put(177, "minecraft:meadow");
      this.biomesById.put(178, "minecraft:grove");
      this.biomesById.put(179, "minecraft:snowy_slopes");
      this.biomesById.put(180, "minecraft:snowcapped_peaks");
      this.biomesById.put(181, "minecraft:lofty_peaks");
      this.biomesById.put(182, "minecraft:stony_peaks");
   }

   public void loadVanillaStates() throws IOException, CommandSyntaxException {
      if (WorldMapClientConfigUtils.getDebug()) {
         WorldMap.LOGGER.info("Loading vanilla states...");
      }

      this.loadStates(this.vanilla_states, ((class_3298)class_310.method_1551().method_1478().method_14486(this.vanillaStatesResource).get()).method_14482());
      this.vanillaStatesLoaded = true;
   }

   public void loadModdedStates(MapProcessor mapProcessor, String worldId, String dimId, String mwId) throws FileNotFoundException, IOException, CommandSyntaxException {
      if (worldId != null) {
         if (WorldMapClientConfigUtils.getDebug()) {
            WorldMap.LOGGER.info("Loading modded states for the world...");
         }

         Path mainFolder = mapProcessor.getMapSaveLoad().getMainFolder(worldId, dimId);
         Path subFolder = mapProcessor.getMapSaveLoad().getMWSubFolder(worldId, mainFolder, mwId);
         Path inputPath = subFolder.resolve("states.dat");
         if (!Files.exists(subFolder, new LinkOption[0])) {
            Files.createDirectories(subFolder);
         }

      }
   }

   private void loadStates(HashMap<Integer, HashMap<Integer, class_2680>> stateMap, InputStream inputStream) throws IOException, CommandSyntaxException {
      DataInputStream input = new DataInputStream(new BufferedInputStream(inputStream));

      try {
         while(true) {
            int stateId = input.readInt();
            int blockId = stateId & 4095;
            int meta = stateId >> 12 & 1048575;
            class_2487 nbtCompound = class_2507.method_10627(input);
            this.fixBlock(nbtCompound, 1);
            class_2680 blockState = WorldMap.unknownBlockStateCache.getBlockStateFromNBT(class_7923.field_41175.method_46771(), nbtCompound);
            this.putState(stateMap, blockId, meta, blockState);
         }
      } catch (EOFException var9) {
         if (WorldMapClientConfigUtils.getDebug()) {
            WorldMap.LOGGER.info("Done.");
         }

         input.close();
      }
   }

   public void loadStates() throws IOException, CommandSyntaxException {
      if (!this.vanillaStatesLoaded) {
         this.loadVanillaStates();
      }

   }

   private void putState(HashMap<Integer, HashMap<Integer, class_2680>> stateMap, int blockId, int meta, class_2680 blockState) {
      HashMap<Integer, class_2680> blockStates = (HashMap)stateMap.get(blockId);
      if (blockStates == null) {
         stateMap.put(blockId, blockStates = new HashMap());
      }

      blockStates.put(meta, blockState);
   }

   private class_2680 getStateForId(int stateId, HashMap<Integer, HashMap<Integer, class_2680>> stateMap) {
      int blockId = stateId & 4095;
      HashMap<Integer, class_2680> blockStates = (HashMap)stateMap.get(blockId);
      if (blockStates == null) {
         return null;
      } else {
         int meta = stateId >> 12 & 1048575;
         return (class_2680)blockStates.getOrDefault(meta, (Object)null);
      }
   }

   public class_2680 getStateForId(int stateId) {
      class_2680 vanillaState = this.getStateForId(stateId, this.vanilla_states);
      return vanillaState != null ? vanillaState : class_2246.field_10124.method_9564();
   }

   public void fixBlock(class_2487 nbt, int version) {
      if (version == 1) {
         this.fixBlockName1314(nbt);
      }

      if (version < 3) {
         this.fixBlock1516(nbt);
      }

      if (version < 5) {
         this.fixBlock1617(nbt);
      }

   }

   private void fixBlockName1314(class_2487 nbt) {
      String name = nbt.method_10558("Name");
      nbt.method_10582("Name", (String)this.blockRename1314.getOrDefault(name, name));
   }

   private void fixBlock1516(class_2487 nbt) {
      String name = nbt.method_10558("Name");
      OldFormatSupport.Fixer fixer = (OldFormatSupport.Fixer)this.blockFixes1516.get(name);
      if (fixer != null) {
         fixer.fix(nbt);
      }

   }

   private void fixBlock1617(class_2487 nbt) {
      String name = nbt.method_10558("Name");
      OldFormatSupport.Fixer fixer = (OldFormatSupport.Fixer)this.blockFixes1617.get(name);
      if (fixer != null) {
         fixer.fix(nbt);
      }

   }

   public String fixBiome(int id, int version) {
      return this.fixBiome(id, version, "minecraft:plains");
   }

   public String fixBiome(int id, int version, String defaultValue) {
      String biomeStringId = (String)this.biomesById.get(id);
      if (biomeStringId == null) {
         biomeStringId = defaultValue;
      }

      return biomeStringId == null ? null : this.fixBiome(biomeStringId, version);
   }

   public String fixBiome(String id, int version) {
      return version < 6 ? this.fixBiome1718(id) : id;
   }

   private String fixBiome1718(String id) {
      return "minecraft:deep_warm_ocean".equals(id) ? "minecraft:warm_ocean" : (String)this.cc2BiomeRenames.getOrDefault(id, id);
   }

   public interface Fixer {
      void fix(class_2487 var1);
   }
}
