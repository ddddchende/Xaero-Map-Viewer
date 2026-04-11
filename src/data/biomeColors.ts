export interface BiomeColorInfo {
  grass: number;
  foliage: number;
  water: number;
}

export const BIOME_COLORS: Record<string, BiomeColorInfo> = {
  'minecraft:ocean': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
  'minecraft:plains': { grass: 0x91bd59, foliage: 0x77ab2f, water: 0x3f76e4 },
  'minecraft:desert': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:windswept_hills': { grass: 0x8ab689, foliage: 0x6da36b, water: 0x3f76e4 },
  'minecraft:forest': { grass: 0x79c05a, foliage: 0x59ae30, water: 0x3f76e4 },
  'minecraft:taiga': { grass: 0x86b783, foliage: 0x68a464, water: 0x3f76e4 },
  'minecraft:swamp': { grass: 0x6a7039, foliage: 0x6a7039, water: 0x617b64 },
  'minecraft:river': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
  'minecraft:nether_wastes': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:the_end': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
  'minecraft:frozen_ocean': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3938c9 },
  'minecraft:frozen_river': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3938c9 },
  'minecraft:snowy_plains': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
  'minecraft:mushroom_fields': { grass: 0x55c93f, foliage: 0x2bbb0f, water: 0x3f76e4 },
  'minecraft:beach': { grass: 0x91bd59, foliage: 0x77ab2f, water: 0x3f76e4 },
  'minecraft:jungle': { grass: 0x59c93c, foliage: 0x30bb0b, water: 0x3f76e4 },
  'minecraft:sparse_jungle': { grass: 0x64c73f, foliage: 0x3eb80f, water: 0x3f76e4 },
  'minecraft:deep_ocean': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
  'minecraft:stony_shore': { grass: 0x8ab689, foliage: 0x6da36b, water: 0x3f76e4 },
  'minecraft:snowy_beach': { grass: 0x83b593, foliage: 0x64a278, water: 0x3f76e4 },
  'minecraft:birch_forest': { grass: 0x88bb67, foliage: 0x6ba941, water: 0x3f76e4 },
  'minecraft:dark_forest': { grass: 0x507a32, foliage: 0x3b8015, water: 0x3f76e4 },
  'minecraft:snowy_taiga': { grass: 0x86b87a, foliage: 0x60a17b, water: 0x3f76e4 },
  'minecraft:old_growth_pine_taiga': { grass: 0x86b87a, foliage: 0x68a464, water: 0x3f76e4 },
  'minecraft:windswept_forest': { grass: 0x8ab689, foliage: 0x6da36b, water: 0x3f76e4 },
  'minecraft:savanna': { grass: 0x9fb955, foliage: 0xa5a51d, water: 0x3f76e4 },
  'minecraft:savanna_plateau': { grass: 0xa5a51d, foliage: 0x9fb955, water: 0x3f76e4 },
  'minecraft:badlands': { grass: 0x90814d, foliage: 0x9e814d, water: 0x3f76e4 },
  'minecraft:wooded_badlands': { grass: 0x90814d, foliage: 0x9e814d, water: 0x3f76e4 },
  'minecraft:meadow': { grass: 0x7cbd6b, foliage: 0x63a948, water: 0x3f76e4 },
  'minecraft:grove': { grass: 0x8ab689, foliage: 0x6da36b, water: 0x3f76e4 },
  'minecraft:snowy_slopes': { grass: 0x86b89f, foliage: 0x68a48c, water: 0x3f76e4 },
  'minecraft:frozen_peaks': { grass: 0x8ab689, foliage: 0x6da36b, water: 0x3f76e4 },
  'minecraft:jagged_peaks': { grass: 0x8ab689, foliage: 0x6da36b, water: 0x3f76e4 },
  'minecraft:stony_peaks': { grass: 0x8ab689, foliage: 0x6da36b, water: 0x3f76e4 },
  'minecraft:lush_caves': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
  'minecraft:dripstone_caves': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
  'minecraft:deep_dark': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
  'minecraft:mangrove_swamp': { grass: 0x6a7039, foliage: 0x8db127, water: 0x3a7a6a },
  'minecraft:cherry_grove': { grass: 0x91bd59, foliage: 0xe49fbd, water: 0x3f76e4 },
  'minecraft:crimson_forest': { grass: 0xbfb755, foliage: 0xff0000, water: 0x3f76e4 },
  'minecraft:warped_forest': { grass: 0xbfb755, foliage: 0x00ffaa, water: 0x3f76e4 },
  'minecraft:soul_sand_valley': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:basalt_deltas': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:the_void': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
};

const DEFAULT_BIOME: BiomeColorInfo = { grass: 0x91bd59, foliage: 0x77ab2f, water: 0x3f76e4 };

export function getBiomeColors(biomeName: string | null): BiomeColorInfo {
  if (!biomeName) return DEFAULT_BIOME;
  
  const normalizedName = biomeName.startsWith('minecraft:') 
    ? biomeName 
    : `minecraft:${biomeName}`;
  
  return BIOME_COLORS[normalizedName] || DEFAULT_BIOME;
}

export function getGrassColor(biomeName: string | null): number {
  return getBiomeColors(biomeName).grass;
}

export function getFoliageColor(biomeName: string | null): number {
  return getBiomeColors(biomeName).foliage;
}

export function getWaterColor(biomeName: string | null): number {
  return getBiomeColors(biomeName).water;
}
