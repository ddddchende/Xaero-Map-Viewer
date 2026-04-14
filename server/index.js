import express from 'express';
import cors from 'cors';
import compression from 'compression';
import { readdir, readFile, mkdir } from 'fs/promises';
import { existsSync, writeFileSync, readFileSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import JSZip from 'jszip';
import { cpus } from 'os';
import { Worker } from 'worker_threads';
import Database from 'better-sqlite3';
import zlib from 'zlib';
import { createServer } from 'http';
import { WebSocketServer } from 'ws';
import { LRUCache } from './lru-cache.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3001;

app.use(cors());
app.use(compression({ level: 9, threshold: 1024 }));
app.use(express.json());
app.use(express.static(path.join(__dirname, '../dist')));

let baseMapDirectory = '';
let mapDirectory = '';
let cacheDirectory = path.join(__dirname, 'cache');
let db = null;
let currentMapDbPath = '';

const USER_CONFIG_FILE = path.join(__dirname, 'config.json');
const SERVER_CONFIG_FILE = path.join(__dirname, 'server_config.json');

let maxMemoryCacheEntries = 64;
let maxConcurrentLoads = 32;
let maxBatchRegions = 64;

function loadUserConfig() {
  try {
    if (existsSync(USER_CONFIG_FILE)) {
      const config = JSON.parse(readFileSync(USER_CONFIG_FILE, 'utf-8'));
      if (config.cacheDirectory) {
        cacheDirectory = config.cacheDirectory;
      }
      if (config.selectedSubDir && baseMapDirectory) {
        mapDirectory = path.join(baseMapDirectory, config.selectedSubDir);
      }
    } else {
      saveUserConfig();
    }
  } catch (e) {
    console.log('Failed to load user config:', e.message);
    saveUserConfig();
  }
}

function saveUserConfig() {
  try {
    const config = {
      cacheDirectory
    };
    if (baseMapDirectory && mapDirectory && mapDirectory.startsWith(baseMapDirectory)) {
      config.selectedSubDir = path.relative(baseMapDirectory, mapDirectory);
    }
    writeFileSync(USER_CONFIG_FILE, JSON.stringify(config, null, 2));
  } catch (e) {
    console.log('Failed to save user config:', e.message);
  }
}

function loadServerConfig() {
  try {
    if (existsSync(SERVER_CONFIG_FILE)) {
      const config = JSON.parse(readFileSync(SERVER_CONFIG_FILE, 'utf-8'));
      if (config.mapDirectory) {
        baseMapDirectory = config.mapDirectory;
      }
      if (config.maxCacheEntries) {
        maxMemoryCacheEntries = config.maxCacheEntries;
      }
      if (config.maxConcurrentLoads) {
        maxConcurrentLoads = config.maxConcurrentLoads;
      }
      if (config.maxBatchRegions) {
        maxBatchRegions = config.maxBatchRegions;
      }
    } else {
      saveServerConfig();
    }
  } catch (e) {
    console.log('Failed to load server config:', e.message);
    saveServerConfig();
  }
}

function saveServerConfig() {
  try {
    writeFileSync(SERVER_CONFIG_FILE, JSON.stringify({
      mapDirectory: baseMapDirectory,
      maxCacheEntries: maxMemoryCacheEntries,
      maxConcurrentLoads,
      maxBatchRegions
    }, null, 2));
  } catch (e) {
    console.log('Failed to save server config:', e.message);
  }
}

loadServerConfig();
loadUserConfig();

const MAX_MEMORY_CACHE_ENTRIES = maxMemoryCacheEntries;
const MAX_CONCURRENT_LOADS = maxConcurrentLoads;
const MAX_BATCH_REGIONS = maxBatchRegions;
const pixelCache = new LRUCache(MAX_MEMORY_CACHE_ENTRIES);
const dbStatements = {};

const NUM_WORKERS = Math.max(1, Math.min(cpus().length - 1, 4));
const workers = [];
const workerBusy = [];
const taskQueue = [];
const workerTaskId = new Map();
const cancelledRequests = new Set();
const clientPendingRequests = new Map();
const clientViewports = new Map();
let taskIdCounter = 0;
let requestIdCounter = 0;
let clientIdCounter = 0;

function isRegionInAnyViewport(regionX, regionZ) {
  for (const [clientId, viewport] of clientViewports) {
    if (viewport && 
        regionX >= viewport.startX && regionX <= viewport.endX &&
        regionZ >= viewport.startZ && regionZ <= viewport.endZ) {
      return true;
    }
  }
  return false;
}

function initWorkerPool() {
  for (let i = 0; i < NUM_WORKERS; i++) {
    const worker = new Worker(path.join(__dirname, 'regionWorker.js'), {
      workerData: { blockColors: BLOCK_COLORS, biomeColors: BIOME_COLORS }
    });
    
    worker.on('message', ({ taskId, result, error }) => {
      const task = workerTaskId.get(taskId);
      if (task) {
        workerTaskId.delete(taskId);
        workerBusy[task.workerIndex] = false;
        
        if (!task.cancelled) {
          if (error) {
            task.reject(new Error(error));
          } else {
            task.resolve(result);
          }
        }
        processNextTask();
      }
    });
    
    worker.on('error', (err) => {
      console.error('Worker error:', err);
    });
    
    workers.push(worker);
    workerBusy.push(false);
  }
  
  console.log(`Worker pool initialized with ${NUM_WORKERS} workers`);
}

function processNextTask() {
  if (taskQueue.length === 0) return;
  
  const freeWorkerIndex = workerBusy.findIndex(busy => !busy);
  if (freeWorkerIndex === -1) return;
  
  while (taskQueue.length > 0) {
    taskQueue.sort((a, b) => b.priority - a.priority);
    
    const task = taskQueue.shift();
    
    if (cancelledRequests.has(task.requestId)) {
      task.resolve(null);
      continue;
    }
    
    if (!isRegionInAnyViewport(task.regionX, task.regionZ)) {
      task.resolve(null);
      continue;
    }
    
    workerBusy[freeWorkerIndex] = true;
    
    workerTaskId.set(task.taskId, { 
      resolve: task.resolve, 
      reject: task.reject, 
      workerIndex: freeWorkerIndex,
      requestId: task.requestId,
      regionX: task.regionX,
      regionZ: task.regionZ,
      cancelled: false
    });
    
    workers[freeWorkerIndex].postMessage({
      taskId: task.taskId,
      dimPath: task.dimPath,
      regionX: task.regionX,
      regionZ: task.regionZ,
      caveMode: task.caveMode,
      caveStart: task.caveStart,
      lod: task.lod
    });
    break;
  }
}

function cancelRequest(requestId) {
  cancelledRequests.add(requestId);
  
  for (let i = taskQueue.length - 1; i >= 0; i--) {
    if (taskQueue[i].requestId === requestId) {
      taskQueue[i].resolve(null);
      taskQueue.splice(i, 1);
    }
  }
  
  for (const [taskId, task] of workerTaskId.entries()) {
    if (task.requestId === requestId) {
      task.cancelled = true;
      workers[task.workerIndex].postMessage({ type: 'cancel', taskId });
    }
  }
}

function cancelTasksNotInViewport() {
  for (const [taskId, task] of workerTaskId.entries()) {
    if (!task.cancelled && !isRegionInAnyViewport(task.regionX, task.regionZ)) {
      task.cancelled = true;
      workers[task.workerIndex].postMessage({ type: 'cancel', taskId });
    }
  }
  
  for (let i = taskQueue.length - 1; i >= 0; i--) {
    if (!isRegionInAnyViewport(taskQueue[i].regionX, taskQueue[i].regionZ)) {
      taskQueue[i].resolve(null);
      taskQueue.splice(i, 1);
    }
  }
}

function runWorkerTask(dimPath, regionX, regionZ, caveMode, caveStart, lod, priority = 0, requestId = null) {
  return new Promise((resolve, reject) => {
    const taskId = taskIdCounter++;
    
    taskQueue.push({
      taskId,
      requestId,
      dimPath,
      regionX,
      regionZ,
      caveMode,
      caveStart,
      lod,
      priority,
      resolve,
      reject
    });
    
    processNextTask();
  });
}

function clearMemoryCache() {
  pixelCache.clear();
}

async function ensureCacheDir() {
  if (!existsSync(cacheDirectory)) {
    await mkdir(cacheDirectory, { recursive: true });
  }
}

function getDbNameForDirectory(dirPath) {
  if (!dirPath) return 'default';
  const normalized = path.resolve(dirPath).replace(/[\\/:*?"<>|]/g, '_');
  const hash = normalized.split('').reduce((acc, char) => ((acc << 5) - acc + char.charCodeAt(0)) | 0, 0);
  return `${Math.abs(hash).toString(16)}_${path.basename(dirPath).substring(0, 32)}`;
}

function initDatabase(forMapDirectory = null) {
  if (db) {
    try {
      db.close();
    } catch {}
    db = null;
  }
  
  clearMemoryCache();
  
  const dbName = getDbNameForDirectory(forMapDirectory);
  const dbPath = path.join(cacheDirectory, `${dbName}.db`);
  currentMapDbPath = dbPath;
  
  db = new Database(dbPath);
  db.pragma('journal_mode = WAL');
  db.pragma('synchronous = OFF');
  db.pragma('cache_size = -256000');
  db.pragma('temp_store = MEMORY');
  db.pragma('mmap_size = 536870912');
  db.pragma('locking_mode = EXCLUSIVE');
  
  db.exec(`
    CREATE TABLE IF NOT EXISTS region_cache (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      cache_key TEXT UNIQUE NOT NULL,
      data BLOB NOT NULL,
      compressed INTEGER DEFAULT 1,
      created_at INTEGER DEFAULT (strftime('%s', 'now')),
      accessed_at INTEGER DEFAULT (strftime('%s', 'now'))
    );
    CREATE INDEX IF NOT EXISTS idx_cache_key ON region_cache(cache_key);
  `);
  
  dbStatements.read = db.prepare('SELECT data, compressed FROM region_cache WHERE cache_key = ?');
  dbStatements.write = db.prepare('INSERT OR REPLACE INTO region_cache (cache_key, data, compressed) VALUES (?, ?, 1)');
  dbStatements.delete = db.prepare('DELETE FROM region_cache WHERE cache_key = ?');
  
  console.log(`Database initialized: ${dbPath}`);
}

function compressData(data) {
  return zlib.gzipSync(data, { level: 1 });
}

function decompressData(data) {
  return zlib.gunzipSync(data);
}

function getCacheKey(dimPath, regionX, regionZ, yHeight = null, lod = 0) {
  const relativePath = mapDirectory ? path.relative(mapDirectory, dimPath) : dimPath;
  const heightSuffix = yHeight !== null ? `_y${yHeight}` : '';
  const lodSuffix = lod > 0 ? `_lod${lod}` : '';
  return `${relativePath}/${regionX}_${regionZ}${heightSuffix}${lodSuffix}`;
}

async function readPixelCache(dimPath, regionX, regionZ, yHeight = null, lod = 0) {
  const key = getCacheKey(dimPath, regionX, regionZ, yHeight, lod);
  
  const cached = pixelCache.get(key);
  if (cached !== null) {
    if (cached && cached.length > 0) {
      return cached;
    }
    pixelCache.delete(key);
  }
  
  if (db && dbStatements.read) {
    try {
      const row = dbStatements.read.get(key);
      if (row && row.data && row.data.length > 0) {
        const data = row.compressed ? decompressData(row.data) : row.data;
        if (!data || data.length === 0) {
          dbStatements.delete.run(key);
          return null;
        }
        pixelCache.set(key, data);
        return data;
      } else if (row) {
        dbStatements.delete.run(key);
      }
    } catch (e) {
      console.error('Database read error:', e.message);
      try {
        dbStatements.delete.run(key);
      } catch {}
    }
  }
  
  return null;
}

async function writePixelCache(dimPath, regionX, regionZ, pixels, yHeight = null, lod = 0) {
  if (!pixels || pixels.length === 0) return;
  
  const key = getCacheKey(dimPath, regionX, regionZ, yHeight, lod);
  pixelCache.set(key, pixels);
  
  if (db && dbStatements.write) {
    try {
      const compressed = compressData(pixels);
      dbStatements.write.run(key, compressed);
    } catch (e) {
      console.error('Database write error:', e.message);
    }
  }
}

function downsamplePixels(pixels, srcSize, dstSize) {
  const result = Buffer.alloc(dstSize * dstSize * 4);
  const ratio = srcSize / dstSize;
  
  for (let dy = 0; dy < dstSize; dy++) {
    for (let dx = 0; dx < dstSize; dx++) {
      const sx = Math.floor(dx * ratio);
      const sy = Math.floor(dy * ratio);
      const srcIdx = (sy * srcSize + sx) * 4;
      const dstIdx = (dy * dstSize + dx) * 4;
      
      result[dstIdx] = pixels[srcIdx];
      result[dstIdx + 1] = pixels[srcIdx + 1];
      result[dstIdx + 2] = pixels[srcIdx + 2];
      result[dstIdx + 3] = pixels[srcIdx + 3];
    }
  }
  
  return result;
}

async function loadRegionPixels(dimPath, regionX, regionZ, caveMode = null, caveStart = null, lod = 0, priority = 0, requestId = null) {
  const cacheKey = caveMode !== null ? `cave_${caveMode}_${caveStart || 'auto'}` : null;
  
  const cached = await readPixelCache(dimPath, regionX, regionZ, cacheKey, lod);
  if (cached) return cached;
  
  if (workers.length > 0) {
    const pixels = await runWorkerTask(dimPath, regionX, regionZ, caveMode, caveStart, lod, priority, requestId);
    if (!pixels) return null;
    const pixelBuffer = Buffer.isBuffer(pixels) ? pixels : Buffer.from(pixels);
    await writePixelCache(dimPath, regionX, regionZ, pixelBuffer, cacheKey, lod);
    return pixelBuffer;
  }
  
  return null;
}

function renderCaveLayersToPixels(layers) {
  const size = 512;
  const pixels = Buffer.alloc(size * size * 4);
  
  for (let i = 0; i < layers.length; i++) {
    const layerData = layers[i].data;
    const layerPixels = renderRegionToPixels(layerData);
    
    for (let idx = 0; idx < size * size; idx++) {
      const srcAlpha = layerPixels[idx * 4 + 3];
      if (srcAlpha > 0) {
        pixels[idx * 4] = layerPixels[idx * 4];
        pixels[idx * 4 + 1] = layerPixels[idx * 4 + 1];
        pixels[idx * 4 + 2] = layerPixels[idx * 4 + 2];
        pixels[idx * 4 + 3] = srcAlpha;
      }
    }
  }
  
  return pixels;
}

const BLOCK_COLORS = {
  'minecraft:air': { color: 0x000000, alpha: 0 },
  'minecraft:cave_air': { color: 0x000000, alpha: 0 },
  'minecraft:void_air': { color: 0x000000, alpha: 0 },
  'minecraft:water': { color: 0x3f76e4, water: true },
  'minecraft:lava': { color: 0xcf4919 },
  'minecraft:snow': { color: 0xf9fefe },
  'minecraft:acacia_leaves': { color: 0x959595, foliage: true },
  'minecraft:acacia_log': { color: 0x975937 },
  'minecraft:acacia_planks': { color: 0xa85a32 },
  'minecraft:activator_rail': { color: 0x73574a },
  'minecraft:allium': { color: 0x9f89b8 },
  'minecraft:amethyst_block': { color: 0x8662bf },
  'minecraft:amethyst_cluster': { color: 0xa47fcf },
  'minecraft:ancient_debris': { color: 0x5f423a },
  'minecraft:andesite': { color: 0x888889 },
  'minecraft:anvil': { color: 0x494949 },
  'minecraft:azalea_leaves': { color: 0x5a732c, foliage: true },
  'minecraft:azalea': { color: 0x667d30 },
  'minecraft:azure_bluet': { color: 0xa9cd7f },
  'minecraft:bamboo_block': { color: 0x7f903a },
  'minecraft:bamboo': { color: 0x49761a },
  'minecraft:bamboo_mosaic': { color: 0xbeaa4e },
  'minecraft:bamboo_planks': { color: 0xc1ad50 },
  'minecraft:barrel': { color: 0x87653a },
  'minecraft:basalt': { color: 0x515156 },
  'minecraft:beacon': { color: 0x76ddd7 },
  'minecraft:bedrock': { color: 0x555555 },
  'minecraft:beehive': { color: 0x9f804e },
  'minecraft:bee_nest': { color: 0xcaa04b },
  'minecraft:bell': { color: 0xfdeb6f },
  'minecraft:big_dripleaf': { color: 0x708e34 },
  'minecraft:birch_leaves': { color: 0x838182, foliage: true },
  'minecraft:birch_log': { color: 0xc1b387 },
  'minecraft:birch_planks': { color: 0xc0af79 },
  'minecraft:blackstone': { color: 0x2a242a },
  'minecraft:black_candle': { color: 0x26253a },
  'minecraft:black_concrete': { color: 0x080a0f },
  'minecraft:black_shulker_box': { color: 0x19191e },
  'minecraft:black_stained_glass': { color: 0x191919 },
  'minecraft:black_terracotta': { color: 0x251710 },
  'minecraft:black_wool': { color: 0x15151a },
  'minecraft:blast_furnace': { color: 0x515051 },
  'minecraft:blue_candle': { color: 0x394ca1 },
  'minecraft:blue_concrete': { color: 0x2d2f8f },
  'minecraft:blue_ice': { color: 0x74a8fd },
  'minecraft:blue_orchid': { color: 0x2fa3a8 },
  'minecraft:blue_stained_glass': { color: 0x324bb1 },
  'minecraft:blue_terracotta': { color: 0x4a3c5b },
  'minecraft:blue_wool': { color: 0x35399d },
  'minecraft:bone_block': { color: 0xd2ceb3 },
  'minecraft:bookshelf': { color: 0x755f3c },
  'minecraft:brain_coral': { color: 0xc65598 },
  'minecraft:brain_coral_block': { color: 0xcf5b9f },
  'minecraft:brewing_stand': { color: 0x7a6551 },
  'minecraft:bricks': { color: 0x976253 },
  'minecraft:brown_candle': { color: 0x704629 },
  'minecraft:brown_concrete': { color: 0x603c20 },
  'minecraft:brown_mushroom': { color: 0x9a755d },
  'minecraft:brown_mushroom_block': { color: 0x957051 },
  'minecraft:brown_stained_glass': { color: 0x664b32 },
  'minecraft:brown_terracotta': { color: 0x4d3324 },
  'minecraft:brown_wool': { color: 0x724829 },
  'minecraft:bubble_coral': { color: 0xa118a0 },
  'minecraft:bubble_coral_block': { color: 0xa51aa2 },
  'minecraft:budding_amethyst': { color: 0x8460bb },
  'minecraft:cactus': { color: 0x567f2b },
  'minecraft:calcite': { color: 0xdfe0dd },
  'minecraft:calibrated_sculk_sensor': { color: 0x1c4f65 },
  'minecraft:campfire': { color: 0x4f4b44 },
  'minecraft:candle': { color: 0xe8c999 },
  'minecraft:cartography_table': { color: 0x675743 },
  'minecraft:carved_pumpkin': { color: 0x965411 },
  'minecraft:cave_vines': { color: 0x5a6d29 },
  'minecraft:chain': { color: 0x333a4a },
  'minecraft:chain_command_block': { color: 0x84a597 },
  'minecraft:cherry_leaves': { color: 0xe5adc2, foliage: true },
  'minecraft:cherry_log': { color: 0xb98d89 },
  'minecraft:cherry_planks': { color: 0xe3b3ad },
  'minecraft:chipped_anvil': { color: 0x494949 },
  'minecraft:chiseled_bookshelf': { color: 0xb29159 },
  'minecraft:chiseled_copper': { color: 0xb8654a },
  'minecraft:chiseled_deepslate': { color: 0x363637 },
  'minecraft:chiseled_quartz_block': { color: 0xe8e3d9 },
  'minecraft:chiseled_stone_bricks': { color: 0x787778 },
  'minecraft:chiseled_tuff': { color: 0x595e57 },
  'minecraft:chiseled_tuff_bricks': { color: 0x636760 },
  'minecraft:chorus_flower': { color: 0x977998 },
  'minecraft:chorus_plant': { color: 0x5e395e },
  'minecraft:clay': { color: 0xa1a6b3 },
  'minecraft:coal_block': { color: 0x101010 },
  'minecraft:coal_ore': { color: 0x6a6a69 },
  'minecraft:coarse_dirt': { color: 0x77563b },
  'minecraft:cobbled_deepslate': { color: 0x4d4d51 },
  'minecraft:cobblestone': { color: 0x807f80 },
  'minecraft:cobweb': { color: 0xe5e9ea },
  'minecraft:command_block': { color: 0xb5886c },
  'minecraft:comparator': { color: 0xa6a2a0 },
  'minecraft:composter': { color: 0x996334 },
  'minecraft:conduit': { color: 0xa08c71 },
  'minecraft:copper_block': { color: 0xc06c50 },
  'minecraft:copper_bulb': { color: 0x9c5739 },
  'minecraft:copper_grate': { color: 0xc06c4f },
  'minecraft:copper_ore': { color: 0x7d7e78 },
  'minecraft:cornflower': { color: 0x507993 },
  'minecraft:cracked_stone_bricks': { color: 0x767676 },
  'minecraft:crafter': { color: 0x706364 },
  'minecraft:crafting_table': { color: 0x78492a },
  'minecraft:crimson_fungus': { color: 0x8d2c1e },
  'minecraft:crimson_nylium': { color: 0x831f1f },
  'minecraft:crimson_planks': { color: 0x653147 },
  'minecraft:crimson_roots': { color: 0x7e082a },
  'minecraft:crimson_stem': { color: 0x713246 },
  'minecraft:crying_obsidian': { color: 0x210a3c },
  'minecraft:cut_copper': { color: 0xbf6b51 },
  'minecraft:cyan_candle': { color: 0x117c7c },
  'minecraft:cyan_concrete': { color: 0x157788 },
  'minecraft:cyan_stained_glass': { color: 0x4b7f98 },
  'minecraft:cyan_terracotta': { color: 0x575b5b },
  'minecraft:cyan_wool': { color: 0x158a91 },
  'minecraft:damaged_anvil': { color: 0x484848 },
  'minecraft:dandelion': { color: 0x94ac2b },
  'minecraft:dark_oak_leaves': { color: 0x979797, foliage: true },
  'minecraft:dark_oak_log': { color: 0x442d16 },
  'minecraft:dark_oak_planks': { color: 0x432b14 },
  'minecraft:dark_prismarine': { color: 0x345c4c },
  'minecraft:dead_brain_coral': { color: 0x867d79 },
  'minecraft:dead_bubble_coral': { color: 0x857d79 },
  'minecraft:dead_bush': { color: 0x6b4f29 },
  'minecraft:dead_fire_coral': { color: 0x89807c },
  'minecraft:dead_horn_coral': { color: 0x8f8782 },
  'minecraft:dead_tube_coral': { color: 0x766f6c },
  'minecraft:deepslate': { color: 0x505053 },
  'minecraft:deepslate_bricks': { color: 0x474747 },
  'minecraft:deepslate_coal_ore': { color: 0x4a4a4c },
  'minecraft:deepslate_copper_ore': { color: 0x5c5d59 },
  'minecraft:deepslate_diamond_ore': { color: 0x536a6b },
  'minecraft:deepslate_emerald_ore': { color: 0x4e6858 },
  'minecraft:deepslate_gold_ore': { color: 0x73674e },
  'minecraft:deepslate_iron_ore': { color: 0x6b645f },
  'minecraft:deepslate_lapis_ore': { color: 0x505b73 },
  'minecraft:deepslate_redstone_ore': { color: 0x69494b },
  'minecraft:deepslate_tiles': { color: 0x373737 },
  'minecraft:detector_rail': { color: 0x7b695a },
  'minecraft:diamond_block': { color: 0x62ede4 },
  'minecraft:diamond_ore': { color: 0x798d8d },
  'minecraft:diorite': { color: 0xbdbcbd },
  'minecraft:dirt': { color: 0x866043 },
  'minecraft:dirt_path': { color: 0x947a41 },
  'minecraft:dispenser': { color: 0x7a7a7a },
  'minecraft:dragon_egg': { color: 0x0d0910 },
  'minecraft:dried_kelp_block': { color: 0x323b27 },
  'minecraft:dripstone_block': { color: 0x866c5d },
  'minecraft:dropper': { color: 0x7a7a7a },
  'minecraft:emerald_block': { color: 0x2acb58 },
  'minecraft:emerald_ore': { color: 0x6c8874 },
  'minecraft:enchanting_table': { color: 0x814b55 },
  'minecraft:end_portal_frame': { color: 0x5b7961 },
  'minecraft:end_stone': { color: 0xdcdf9e },
  'minecraft:exposed_copper': { color: 0xa17e68 },
  'minecraft:exposed_copper_bulb': { color: 0x876c5a },
  'minecraft:exposed_copper_grate': { color: 0xa27e69 },
  'minecraft:exposed_cut_copper': { color: 0x9b7a65 },
  'minecraft:farmland': { color: 0x8f6747 },
  'minecraft:fern': { color: 0x7c7d7c },
  'minecraft:fire': { color: 0xd48c36 },
  'minecraft:fire_coral': { color: 0xa7262f },
  'minecraft:fire_coral_block': { color: 0xa4232f },
  'minecraft:fletching_table': { color: 0xc5b485 },
  'minecraft:flowering_azalea_leaves': { color: 0x646f3d, foliage: true },
  'minecraft:flowering_azalea': { color: 0x707a40 },
  'minecraft:flower_pot': { color: 0x7c4535 },
  'minecraft:frogspawn': { color: 0x6e5f57 },
  'minecraft:furnace': { color: 0x6e6e6e },
  'minecraft:gilded_blackstone': { color: 0x382b26 },
  'minecraft:glass': { color: 0xb0d6db },
  'minecraft:glass_pane': { color: 0xaad2d9 },
  'minecraft:glowstone': { color: 0xac8354 },
  'minecraft:glow_lichen': { color: 0x70837a },
  'minecraft:gold_block': { color: 0xf6d03e },
  'minecraft:gold_ore': { color: 0x91866b },
  'minecraft:granite': { color: 0x956756 },
  'minecraft:grass_block': { color: 0x939393, grass: true },
  'minecraft:grass': { color: 0x939393, grass: true },
  'minecraft:gravel': { color: 0x847f7f },
  'minecraft:gray_candle': { color: 0x505e61 },
  'minecraft:gray_concrete': { color: 0x373a3e },
  'minecraft:gray_stained_glass': { color: 0x4b4b4b },
  'minecraft:gray_terracotta': { color: 0x3a2a24 },
  'minecraft:gray_wool': { color: 0x3f4448 },
  'minecraft:green_candle': { color: 0x496015 },
  'minecraft:green_concrete': { color: 0x495b24 },
  'minecraft:green_stained_glass': { color: 0x667f32 },
  'minecraft:green_terracotta': { color: 0x4c532a },
  'minecraft:green_wool': { color: 0x556e1c },
  'minecraft:hanging_roots': { color: 0xa1735c },
  'minecraft:hay_block': { color: 0xa68b0c },
  'minecraft:honeycomb_block': { color: 0xe5941e },
  'minecraft:honey_block': { color: 0xfab935 },
  'minecraft:hopper': { color: 0x4c4a4c },
  'minecraft:horn_coral': { color: 0xd1ba3f },
  'minecraft:horn_coral_block': { color: 0xd8c842 },
  'minecraft:ice': { color: 0x91b7fd },
  'minecraft:iron_bars': { color: 0x898b88 },
  'minecraft:iron_block': { color: 0xdcdcdc },
  'minecraft:iron_door': { color: 0xc2c1c1 },
  'minecraft:iron_ore': { color: 0x88817b },
  'minecraft:iron_trapdoor': { color: 0xcbcaca },
  'minecraft:jack_o_lantern': { color: 0xd79835 },
  'minecraft:jigsaw': { color: 0x504651 },
  'minecraft:jungle_leaves': { color: 0x9d9a90, foliage: true },
  'minecraft:jungle_log': { color: 0x966d47 },
  'minecraft:jungle_planks': { color: 0xa07351 },
  'minecraft:kelp': { color: 0x578c2d },
  'minecraft:kelp_plant': { color: 0x57822b },
  'minecraft:ladder': { color: 0x7d6137 },
  'minecraft:lantern': { color: 0x6a5b54 },
  'minecraft:lapis_block': { color: 0x1f438c },
  'minecraft:lapis_ore': { color: 0x6b768d },
  'minecraft:large_amethyst_bud': { color: 0xa17ecb },
  'minecraft:lectern': { color: 0xae8a53 },
  'minecraft:lever': { color: 0x6f5d43 },
  'minecraft:lightning_rod': { color: 0xc56f53 },
  'minecraft:light_blue_candle': { color: 0x238ac5 },
  'minecraft:light_blue_concrete': { color: 0x2489c7 },
  'minecraft:light_blue_stained_glass': { color: 0x6698d7 },
  'minecraft:light_blue_terracotta': { color: 0x716d8a },
  'minecraft:light_blue_wool': { color: 0x3aafd9 },
  'minecraft:light_gray_candle': { color: 0x767970 },
  'minecraft:light_gray_concrete': { color: 0x7d7d73 },
  'minecraft:light_gray_stained_glass': { color: 0x989898 },
  'minecraft:light_gray_terracotta': { color: 0x876b62 },
  'minecraft:light_gray_wool': { color: 0x8e8e87 },
  'minecraft:lilac': { color: 0x9b7d93 },
  'minecraft:lily_of_the_valley': { color: 0x7baf5f },
  'minecraft:lily_pad': { color: 0x868686 },
  'minecraft:lime_candle': { color: 0x62ac17 },
  'minecraft:lime_concrete': { color: 0x5ea919 },
  'minecraft:lime_stained_glass': { color: 0x7fcd19 },
  'minecraft:lime_terracotta': { color: 0x687635 },
  'minecraft:lime_wool': { color: 0x70b91a },
  'minecraft:lodestone': { color: 0x939599 },
  'minecraft:loom': { color: 0x8e775c },
  'minecraft:magenta_candle': { color: 0xa12e99 },
  'minecraft:magenta_concrete': { color: 0xa9309f },
  'minecraft:magenta_stained_glass': { color: 0xb14bd7 },
  'minecraft:magenta_terracotta': { color: 0x96586d },
  'minecraft:magenta_wool': { color: 0xbe45b4 },
  'minecraft:mangrove_leaves': { color: 0x828181, foliage: true },
  'minecraft:mangrove_log': { color: 0x67312a },
  'minecraft:mangrove_planks': { color: 0x763631 },
  'minecraft:mangrove_roots': { color: 0x4b3c27 },
  'minecraft:medium_amethyst_bud': { color: 0x9e78ca },
  'minecraft:melon': { color: 0x6f911f },
  'minecraft:mossy_cobblestone': { color: 0x6e775f },
  'minecraft:mossy_stone_bricks': { color: 0x737969 },
  'minecraft:moss_block': { color: 0x596e2d },
  'minecraft:moss_carpet': { color: 0x596e2d },
  'minecraft:mud': { color: 0x3c393d },
  'minecraft:muddy_mangrove_roots': { color: 0x463b2d },
  'minecraft:mud_bricks': { color: 0x89684f },
  'minecraft:mushroom_stem': { color: 0xcbc5ba },
  'minecraft:mycelium': { color: 0x6f6365 },
  'minecraft:netherite_block': { color: 0x433d40 },
  'minecraft:netherrack': { color: 0x622626 },
  'minecraft:nether_bricks': { color: 0x2c161a },
  'minecraft:nether_gold_ore': { color: 0x73372a },
  'minecraft:nether_sprouts': { color: 0x149785 },
  'minecraft:nether_wart_block': { color: 0x730302, foliage: true },
  'minecraft:oak_door': { color: 0x8d6f42 },
  'minecraft:oak_leaves': { color: 0x909090, foliage: true },
  'minecraft:oak_log': { color: 0x977a49 },
  'minecraft:oak_planks': { color: 0xa2834f },
  'minecraft:oak_trapdoor': { color: 0x7d6339 },
  'minecraft:obsidian': { color: 0x0f0b19 },
  'minecraft:ochre_froglight': { color: 0xfbf5cf },
  'minecraft:orange_candle': { color: 0xdb6409 },
  'minecraft:orange_concrete': { color: 0xe06101 },
  'minecraft:orange_stained_glass': { color: 0xd77f32 },
  'minecraft:orange_terracotta': { color: 0xa25426 },
  'minecraft:orange_tulip': { color: 0x5d8e1f },
  'minecraft:orange_wool': { color: 0xf17614 },
  'minecraft:oxeye_daisy': { color: 0xb3ca8f },
  'minecraft:oxidized_copper': { color: 0x52a385 },
  'minecraft:packed_ice': { color: 0x8eb4fa },
  'minecraft:packed_mud': { color: 0x8e6b50 },
  'minecraft:pearlescent_froglight': { color: 0xf6f0f0 },
  'minecraft:peony': { color: 0x827f8b },
  'minecraft:pink_candle': { color: 0xd0668e },
  'minecraft:pink_concrete': { color: 0xd6658f },
  'minecraft:pink_petals': { color: 0xf7b5db },
  'minecraft:pink_stained_glass': { color: 0xf27fa5 },
  'minecraft:pink_terracotta': { color: 0xa24e4f },
  'minecraft:pink_tulip': { color: 0x639d4e },
  'minecraft:pink_wool': { color: 0xee8dac },
  'minecraft:podzol': { color: 0x5c3f18 },
  'minecraft:polished_deepslate': { color: 0x484949 },
  'minecraft:poppy': { color: 0x814126 },
  'minecraft:powered_rail': { color: 0x8a6e4a },
  'minecraft:prismarine': { color: 0x639c97 },
  'minecraft:prismarine_bricks': { color: 0x63ac9e },
  'minecraft:pumpkin': { color: 0xc67718 },
  'minecraft:purple_candle': { color: 0x69229f },
  'minecraft:purple_concrete': { color: 0x64209c },
  'minecraft:purple_stained_glass': { color: 0x7f3eb1 },
  'minecraft:purple_terracotta': { color: 0x764656 },
  'minecraft:purple_wool': { color: 0x7a2aad },
  'minecraft:purpur_block': { color: 0xaa7eaa },
  'minecraft:purpur_pillar': { color: 0xac80ab },
  'minecraft:quartz_block': { color: 0xece6df },
  'minecraft:quartz_pillar': { color: 0xebe6df },
  'minecraft:rail': { color: 0x7e7059 },
  'minecraft:raw_copper_block': { color: 0x9a6a4f },
  'minecraft:raw_gold_block': { color: 0xdea92f },
  'minecraft:raw_iron_block': { color: 0xa6886b },
  'minecraft:redstone_block': { color: 0xb01905 },
  'minecraft:redstone_wire': { color: 0xf0f0f0 },
  'minecraft:redstone_lamp': { color: 0x5f371e },
  'minecraft:redstone_ore': { color: 0x8c6e6e },
  'minecraft:red_candle': { color: 0x992724 },
  'minecraft:red_concrete': { color: 0x8e2121 },
  'minecraft:red_mushroom': { color: 0xd94b44 },
  'minecraft:red_mushroom_block': { color: 0xc82f2d },
  'minecraft:red_sand': { color: 0xbf6721 },
  'minecraft:red_sandstone': { color: 0xb5621f },
  'minecraft:red_stained_glass': { color: 0x983232 },
  'minecraft:red_terracotta': { color: 0x8f3d2f },
  'minecraft:red_tulip': { color: 0x5a8121 },
  'minecraft:red_wool': { color: 0xa12723 },
  'minecraft:reinforced_deepslate': { color: 0x50534f },
  'minecraft:repeater': { color: 0xa09d9c },
  'minecraft:repeating_command_block': { color: 0x816fb0 },
  'minecraft:respawn_anchor': { color: 0x4c1896 },
  'minecraft:rooted_dirt': { color: 0x90684d },
  'minecraft:rose_bush': { color: 0x834225 },
  'minecraft:sand': { color: 0xdbcfa3 },
  'minecraft:sandstone': { color: 0xe0d6aa },
  'minecraft:scaffolding': { color: 0xaa8449 },
  'minecraft:sculk': { color: 0x0d1e24 },
  'minecraft:sculk_catalyst': { color: 0x0f2026 },
  'minecraft:sculk_sensor': { color: 0x074654 },
  'minecraft:sculk_shrieker': { color: 0xc7cdaa },
  'minecraft:sculk_vein': { color: 0x08303a },
  'minecraft:seagrass': { color: 0x337f08, grass: true },
  'minecraft:sea_lantern': { color: 0xacc8be },
  'minecraft:sea_pickle': { color: 0x5a6128 },
  'minecraft:shroomlight': { color: 0xf19347 },
  'minecraft:shulker_box': { color: 0x8b618b },
  'minecraft:slime_block': { color: 0x6fc05b },
  'minecraft:small_amethyst_bud': { color: 0x8463c0 },
  'minecraft:small_dripleaf': { color: 0x5f782e },
  'minecraft:smithing_table': { color: 0x393b47 },
  'minecraft:smoker': { color: 0x555451 },
  'minecraft:snow_block': { color: 0xf9fefe },
  'minecraft:soul_fire': { color: 0x33c1c5 },
  'minecraft:soul_lantern': { color: 0x486373 },
  'minecraft:soul_sand': { color: 0x513e33 },
  'minecraft:soul_soil': { color: 0x4c3a2f },
  'minecraft:spore_blossom': { color: 0xcf619f },
  'minecraft:spruce_leaves': { color: 0x7e7e7e, foliage: true },
  'minecraft:spruce_log': { color: 0x6d502f },
  'minecraft:spruce_planks': { color: 0x735531 },
  'minecraft:stone': { color: 0x7e7e7e },
  'minecraft:stonecutter': { color: 0x7b776f },
  'minecraft:stone_bricks': { color: 0x7a7a7a },
  'minecraft:structure_block': { color: 0x594a5a },
  'minecraft:sugar_cane': { color: 0x95c165 },
  'minecraft:sunflower': { color: 0x32811b },
  'minecraft:tall_grass': { color: 0x979597, grass: true },
  'minecraft:tall_seagrass': { color: 0x3b8b0e, grass: true },
  'minecraft:target': { color: 0xe2aa9e },
  'minecraft:terracotta': { color: 0x985e44 },
  'minecraft:tnt': { color: 0x8f3e36 },
  'minecraft:torchflower': { color: 0x65654d },
  'minecraft:tube_coral': { color: 0x3053c5 },
  'minecraft:tube_coral_block': { color: 0x3157cf },
  'minecraft:tuff': { color: 0x6c6d67 },
  'minecraft:tuff_bricks': { color: 0x62675f },
  'minecraft:turtle_egg': { color: 0xe4e3c0 },
  'minecraft:twisting_vines': { color: 0x148f7c },
  'minecraft:vault': { color: 0x37464f },
  'minecraft:verdant_froglight': { color: 0xe5f4e4 },
  'minecraft:vine': { color: 0x747474, foliage: true },
  'minecraft:warped_fungus': { color: 0x4a6d58 },
  'minecraft:warped_nylium': { color: 0x2b7265 },
  'minecraft:warped_planks': { color: 0x2b6963 },
  'minecraft:warped_roots': { color: 0x148a7c },
  'minecraft:warped_stem': { color: 0x356e6e },
  'minecraft:warped_wart_block': { color: 0x177879, foliage: true },
  'minecraft:weathered_copper': { color: 0x6c996e },
  'minecraft:weeping_vines': { color: 0x690100 },
  'minecraft:white_candle': { color: 0xd2d9da },
  'minecraft:white_concrete': { color: 0xcfd5d6 },
  'minecraft:white_shulker_box': { color: 0xd8dddd },
  'minecraft:white_stained_glass': { color: 0xffffff },
  'minecraft:white_terracotta': { color: 0xd2b2a1 },
  'minecraft:white_tulip': { color: 0x5ea547 },
  'minecraft:white_wool': { color: 0xeaeced },
  'minecraft:wither_rose': { color: 0x292d17 },
  'minecraft:yellow_candle': { color: 0xd1a532 },
  'minecraft:yellow_concrete': { color: 0xf1af15 },
  'minecraft:yellow_stained_glass': { color: 0xe5e532 },
  'minecraft:yellow_terracotta': { color: 0xba8523 },
  'minecraft:yellow_wool': { color: 0xf9c628 }
};

const BIOME_COLORS = {
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
  'minecraft:old_growth_pine_taiga': { grass: 0x86b783, foliage: 0x68a464, water: 0x3f76e4 },
  'minecraft:old_growth_spruce_taiga': { grass: 0x86b783, foliage: 0x68a464, water: 0x3f76e4 },
  'minecraft:windswept_forest': { grass: 0x8ab689, foliage: 0x6da36b, water: 0x3f76e4 },
  'minecraft:savanna': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:savanna_plateau': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:windswept_savanna': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:badlands': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:wooded_badlands': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:eroded_badlands': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:meadow': { grass: 0x91bd59, foliage: 0x77ab2f, water: 0x3f76e4 },
  'minecraft:grove': { grass: 0x86b87a, foliage: 0x60a17b, water: 0x3f76e4 },
  'minecraft:snowy_slopes': { grass: 0x86b87a, foliage: 0x60a17b, water: 0x3f76e4 },
  'minecraft:frozen_peaks': { grass: 0x86b87a, foliage: 0x60a17b, water: 0x3f76e4 },
  'minecraft:jagged_peaks': { grass: 0x86b87a, foliage: 0x60a17b, water: 0x3f76e4 },
  'minecraft:stony_peaks': { grass: 0x91bd59, foliage: 0x77ab2f, water: 0x3f76e4 },
  'minecraft:cherry_grove': { grass: 0x91bd59, foliage: 0xe49fbd, water: 0x3f76e4 },
  'minecraft:mangrove_swamp': { grass: 0x6a7039, foliage: 0x8db127, water: 0x3a7a6a },
  'minecraft:deep_dark': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
  'minecraft:crimson_forest': { grass: 0xbfb755, foliage: 0xff0000, water: 0x3f76e4 },
  'minecraft:warped_forest': { grass: 0xbfb755, foliage: 0x00ffaa, water: 0x3f76e4 },
  'minecraft:soul_sand_valley': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:basalt_deltas': { grass: 0xbfb755, foliage: 0xaea42a, water: 0x3f76e4 },
  'minecraft:the_void': { grass: 0x8eb971, foliage: 0x71a74d, water: 0x3f76e4 },
};

const DEFAULT_BIOME = { grass: 0x91bd59, foliage: 0x77ab2f, water: 0x3f76e4 };

function computeBlockColor(state, biome, hasWaterOverlay) {
  if (!state || state === 'minecraft:air') {
    if (hasWaterOverlay) {
      const biomeColor = BIOME_COLORS[biome] || DEFAULT_BIOME;
      return { color: biomeColor.water, alpha: 191 };
    }
    return { color: 0, alpha: 0 };
  }
  
  const info = BLOCK_COLORS[state];
  if (!info) {
    return { color: 0x808080, alpha: 255 };
  }
  
  let color = info.color;
  let alpha = info.alpha ?? 255;
  
  if (info.grass || info.foliage) {
    const biomeColor = BIOME_COLORS[biome] || DEFAULT_BIOME;
    color = info.grass ? biomeColor.grass : biomeColor.foliage;
  }
  
  if (hasWaterOverlay) {
    const biomeColor = BIOME_COLORS[biome] || DEFAULT_BIOME;
    const waterColor = biomeColor.water;
    const r = (((color >> 16) & 0xFF) * 64 + ((waterColor >> 16) & 0xFF) * 191) >> 8;
    const g = (((color >> 8) & 0xFF) * 64 + ((waterColor >> 8) & 0xFF) * 191) >> 8;
    const b = ((color & 0xFF) * 64 + (waterColor & 0xFF) * 191) >> 8;
    color = (r << 16) | (g << 8) | b;
    alpha = 255;
  }
  
  return { color, alpha };
}

function applyShading(color, height, prevHeight, prevDiagHeight, light) {
  let r = (color >> 16) & 0xFF;
  let g = (color >> 8) & 0xFF;
  let b = color & 0xFF;
  
  let depthBrightness = 1.0;
  
  if (height >= 0 && height <= 63) {
    depthBrightness = 0.6 + (height / 63.0) * 0.4;
  }
  
  const effectivePrevHeight = prevHeight === 32767 ? height : prevHeight;
  const effectivePrevDiagHeight = prevDiagHeight === 32767 ? height : prevDiagHeight;
  
  const verticalSlope = height - effectivePrevHeight;
  if (verticalSlope > 0) {
    depthBrightness *= 1.3;
  } else if (verticalSlope < 0) {
    depthBrightness *= 0.7;
  }
  
  const brightness = 0.8 * depthBrightness;
  
  if (light < 15) {
    const lightFactor = (9 + light) / 24;
    r = (r * lightFactor) | 0;
    g = (g * lightFactor) | 0;
    b = (b * lightFactor) | 0;
  }
  
  r = Math.min(255, (r * brightness) | 0);
  g = Math.min(255, (g * brightness) | 0);
  b = Math.min(255, (b * brightness) | 0);
  
  return (r << 16) | (g << 8) | b;
}

function extractHeightsFromRegion(regionData) {
  const size = 512;
  const heights = new Int16Array(size * size);
  heights.fill(32767);
  
  if (!regionData || !regionData.chunks) return heights;
  
  const chunks = regionData.chunks;
  
  for (let chunkX = 0; chunkX < 8; chunkX++) {
    const chunkRow = chunks[chunkX];
    if (!chunkRow) continue;
    
    for (let chunkZ = 0; chunkZ < 8; chunkZ++) {
      const chunk = chunkRow[chunkZ];
      if (!chunk) continue;
      
      const tiles = chunk.tiles;
      const baseX = chunkX * 64;
      const baseZ = chunkZ * 64;
      
      for (let tileX = 0; tileX < 4; tileX++) {
        const tileRow = tiles[tileX];
        if (!tileRow) continue;
        
        for (let tileZ = 0; tileZ < 4; tileZ++) {
          const tile = tileRow[tileZ];
          if (!tile || !tile.blocks) continue;
          
          const blocks = tile.blocks;
          const pixelBaseX = baseX + tileX * 16;
          const pixelBaseZ = baseZ + tileZ * 16;
          
          for (let x = 0; x < 16; x++) {
            const blockRow = blocks[x];
            if (!blockRow) continue;
            
            const pixelX = pixelBaseX + x;
            if (pixelX >= size) continue;
            
            for (let z = 0; z < 16; z++) {
              const block = blockRow[z];
              if (!block || !block.s) continue;
              
              const pixelZ = pixelBaseZ + z;
              if (pixelZ >= size) continue;
              
              const idx = pixelZ * size + pixelX;
              heights[idx] = block.h ?? 32767;
            }
          }
        }
      }
    }
  }
  
  return heights;
}

function renderRegionToPixels(regionData, northRegionHeights = null, westRegionHeights = null, northwestRegionHeights = null) {
  const size = 512;
  const pixels = Buffer.alloc(size * size * 4);
  const heights = new Int16Array(size * size);
  heights.fill(32767);
  
  const chunks = regionData.chunks;
  
  for (let chunkX = 0; chunkX < 8; chunkX++) {
    const chunkRow = chunks[chunkX];
    if (!chunkRow) continue;
    
    for (let chunkZ = 0; chunkZ < 8; chunkZ++) {
      const chunk = chunkRow[chunkZ];
      if (!chunk) continue;
      
      const tiles = chunk.tiles;
      const baseX = chunkX * 64;
      const baseZ = chunkZ * 64;
      
      for (let tileX = 0; tileX < 4; tileX++) {
        const tileRow = tiles[tileX];
        if (!tileRow) continue;
        
        for (let tileZ = 0; tileZ < 4; tileZ++) {
          const tile = tileRow[tileZ];
          if (!tile || !tile.blocks) continue;
          
          const blocks = tile.blocks;
          const pixelBaseX = baseX + tileX * 16;
          const pixelBaseZ = baseZ + tileZ * 16;
          
          for (let x = 0; x < 16; x++) {
            const blockRow = blocks[x];
            if (!blockRow) continue;
            
            const pixelX = pixelBaseX + x;
            if (pixelX >= size) continue;
            
            for (let z = 0; z < 16; z++) {
              const block = blockRow[z];
              if (!block || !block.s) continue;
              
              const pixelZ = pixelBaseZ + z;
              if (pixelZ >= size) continue;
              
              const idx = pixelZ * size + pixelX;
              const height = block.h ?? 32767;
              heights[idx] = height;
              
              let prevHeight, prevDiagHeight;
              
              if (pixelZ > 0) {
                prevHeight = heights[idx - size];
                if (pixelX > 0) {
                  prevDiagHeight = heights[idx - size - 1];
                } else {
                  prevDiagHeight = westRegionHeights ? westRegionHeights[(pixelZ - 1) * size + (size - 1)] : 32767;
                }
              } else {
                if (northRegionHeights) {
                  prevHeight = northRegionHeights[(size - 1) * size + pixelX];
                  if (pixelX > 0) {
                    prevDiagHeight = northRegionHeights[(size - 1) * size + (pixelX - 1)];
                  } else if (northwestRegionHeights) {
                    prevDiagHeight = northwestRegionHeights[(size - 1) * size + (size - 1)];
                  } else {
                    prevDiagHeight = 32767;
                  }
                } else {
                  prevHeight = 32767;
                  prevDiagHeight = 32767;
                }
              }
              
              const { color, alpha } = computeBlockColor(block.s, block.b, block.w);
              const shadedColor = applyShading(color, height, prevHeight, prevDiagHeight, block.l ?? 15);
              const pixelIdx = idx << 2;
              
              pixels[pixelIdx] = (shadedColor >> 16) & 0xFF;
              pixels[pixelIdx + 1] = (shadedColor >> 8) & 0xFF;
              pixels[pixelIdx + 2] = shadedColor & 0xFF;
              pixels[pixelIdx + 3] = alpha;
            }
          }
        }
      }
    }
  }
  
  return pixels;
}

app.get('/api/map-subdirectories', async (req, res) => {
  if (!baseMapDirectory) {
    return res.status(400).json({ error: '未配置地图基础目录，请在 server_config.json 中设置 mapDirectory' });
  }
  
  if (!existsSync(baseMapDirectory)) {
    return res.status(400).json({ error: '地图基础目录不存在', path: baseMapDirectory });
  }
  
  try {
    const entries = await readdir(baseMapDirectory, { withFileTypes: true });
    const subDirs = [];
    for (const entry of entries) {
      if (entry.isDirectory()) {
        subDirs.push(entry.name);
      }
    }
    subDirs.sort();
    res.json({ baseDirectory: baseMapDirectory, subdirectories: subDirs });
  } catch (error) {
    res.status(500).json({ error: `读取目录失败: ${error.message}` });
  }
});

app.post('/api/set-map-subdirectory', async (req, res) => {
  const { subdirectory } = req.body;
  
  if (!baseMapDirectory) {
    return res.status(400).json({ error: '未配置地图基础目录，请在 server_config.json 中设置 mapDirectory' });
  }
  
  if (!subdirectory) {
    return res.status(400).json({ error: '请提供子目录名称' });
  }
  
  const fullDir = path.join(baseMapDirectory, subdirectory);
  
  if (!existsSync(fullDir)) {
    return res.status(400).json({ error: '子目录不存在', path: fullDir });
  }
  
  mapDirectory = fullDir;
  initDatabase(fullDir);
  saveUserConfig();
  res.json({ success: true, directory: fullDir, subdirectory });
});

app.post('/api/set-directory', async (req, res) => {
  const { directory } = req.body;
  
  if (!directory) {
    return res.status(400).json({ error: '请提供目录路径' });
  }
  
  if (!existsSync(directory)) {
    return res.status(400).json({ error: '目录不存在', path: directory });
  }
  
  mapDirectory = directory;
  initDatabase(directory);
  saveUserConfig();
  res.json({ success: true, directory });
});

app.post('/api/set-cache-directory', async (req, res) => {
  const { directory } = req.body;
  
  if (!directory) {
    return res.status(400).json({ error: '请提供缓存目录路径' });
  }
  
  try {
    if (!existsSync(directory)) {
      await mkdir(directory, { recursive: true });
    }
    
    cacheDirectory = directory;
    
    initDatabase(mapDirectory);
    
    saveUserConfig();
    
    res.json({ success: true, directory: cacheDirectory });
  } catch (error) {
    res.status(500).json({ error: `无法创建缓存目录: ${error.message}` });
  }
});

app.get('/api/cache-directory', (req, res) => {
  res.json({ directory: cacheDirectory });
});

app.get('/api/cache-size', async (req, res) => {
  try {
    if (db) {
      const row = db.prepare('SELECT COUNT(*) as count FROM region_cache').get();
      const sizeRow = db.prepare('SELECT SUM(LENGTH(data)) as totalSize FROM region_cache').get();
      const count = row?.count || 0;
      const totalSize = sizeRow?.totalSize || 0;
      const sizeMB = (totalSize / (1024 * 1024)).toFixed(2);
      res.json({ count, sizeMB: parseFloat(sizeMB) });
    } else {
      res.json({ count: 0, sizeMB: 0 });
    }
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.delete('/api/cache-directory', async (req, res) => {
  try {
    pixelCache.clear();
    
    if (db) {
      db.exec('DELETE FROM region_cache');
      db.exec('VACUUM');
    }
    
    res.json({ success: true, message: '缓存已清除' });
  } catch (error) {
    res.status(500).json({ error: `清除缓存失败: ${error.message}` });
  }
});

app.get('/api/worlds', async (req, res) => {
  if (!mapDirectory) {
    return res.status(400).json({ error: '请先设置地图目录' });
  }
  
  try {
    const structure = await detectDirectoryStructure(mapDirectory);
    if (structure === 'world') {
      res.json(['当前世界']);
    } else {
      const worlds = await listWorlds(mapDirectory);
      res.json(worlds);
    }
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/worlds/:worldName/dimensions', async (req, res) => {
  const { worldName } = req.params;
  try {
    let basePath = mapDirectory;
    if (worldName !== '当前世界') basePath = path.join(mapDirectory, worldName);
    const dimensions = await listDimensions(basePath);
    res.json(dimensions);
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/worlds/:worldName/dimensions/:dimName/map-types', async (req, res) => {
  const { worldName, dimName } = req.params;
  try {
    let basePath = mapDirectory;
    if (worldName !== '当前世界') basePath = path.join(mapDirectory, worldName);
    const dimPath = path.join(basePath, dimName);
    const mapTypes = await listMapTypes(dimPath);
    res.json(mapTypes);
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/worlds/:worldName/dimensions/:dimName/regions', async (req, res) => {
  const { worldName, dimName } = req.params;
  const { mapType, caveMode, caveStart } = req.query;
  
  try {
    let basePath = mapDirectory;
    if (worldName !== '当前世界') basePath = path.join(mapDirectory, worldName);
    
    let dimPath = path.join(basePath, dimName);
    if (mapType) {
      dimPath = path.join(dimPath, mapType);
    } else {
      const mapTypes = await listMapTypes(dimPath);
      if (mapTypes.length > 0) dimPath = path.join(dimPath, mapTypes[0].path);
    }
    
    const caveModeValue = caveMode !== undefined ? parseInt(String(caveMode)) : null;
    const caveStartValue = caveStart !== undefined ? parseInt(String(caveStart)) : null;
    const regions = await listRegions(dimPath, caveModeValue, caveStartValue);
    res.json({ regions, mapType: mapType || null });
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/region-pixels', async (req, res) => {
  const { world, dim, x, z, mapType, caveMode, caveStart, lod } = req.query;
  
  if (!dim || x === undefined || z === undefined) {
    return res.status(400).json({ error: '缺少参数' });
  }
  
  try {
    let basePath = mapDirectory;
    if (world && world !== '当前世界') basePath = path.join(mapDirectory, String(world));
    
    let dimPath = path.join(basePath, String(dim));
    if (mapType) {
      dimPath = path.join(dimPath, String(mapType));
    } else {
      const mapTypes = await listMapTypes(dimPath);
      if (mapTypes.length > 0) dimPath = path.join(dimPath, mapTypes[0].path);
    }
    
    const caveModeValue = caveMode !== undefined ? parseInt(String(caveMode)) : null;
    const caveStartValue = caveStart !== undefined ? parseInt(String(caveStart)) : null;
    const lodValue = lod !== undefined ? parseInt(String(lod)) : 0;
    const pixels = await loadRegionPixels(dimPath, parseInt(String(x)), parseInt(String(z)), caveModeValue, caveStartValue, lodValue);
    if (!pixels) {
      return res.status(404).json({ error: '区域不存在' });
    }
    
    const pixelBuffer = Buffer.isBuffer(pixels) ? pixels : Buffer.from(pixels);
    
    res.setHeader('Content-Type', 'application/octet-stream');
    res.setHeader('Content-Length', pixelBuffer.length);
    res.send(pixelBuffer);
  } catch (error) {
    console.error('Error loading region pixels:', error);
    res.status(500).json({ error: String(error) });
  }
});

app.post('/api/cancel-request/:requestId', (req, res) => {
  const requestId = parseInt(req.params.requestId);
  if (!isNaN(requestId)) {
    cancelRequest(requestId);
  }
  res.json({ success: true });
});

app.get('/api/batch-regions', async (req, res) => {
  const { requestId: clientRequestId, world, dim, coords, mapType, caveMode, caveStart, lod, viewStartX, viewStartZ, viewEndX, viewEndZ } = req.query;
  
  if (!dim || !coords) {
    return res.status(400).json({ error: '缺少参数' });
  }
  
  const requestId = clientRequestId !== undefined ? parseInt(String(clientRequestId)) : requestIdCounter++;
  let isCancelled = false;
  
  const cancelHandler = () => {
    isCancelled = true;
    cancelRequest(requestId);
  };
  req.on('close', cancelHandler);
  
  try {
    let basePath = mapDirectory;
    if (world && world !== '当前世界') basePath = path.join(mapDirectory, String(world));
    
    let dimPath = path.join(basePath, String(dim));
    if (mapType) {
      dimPath = path.join(dimPath, String(mapType));
    } else {
      const mapTypes = await listMapTypes(dimPath);
      if (mapTypes.length > 0) dimPath = path.join(dimPath, mapTypes[0].path);
    }
    
    const caveModeValue = caveMode !== undefined ? parseInt(String(caveMode)) : null;
    const caveStartValue = caveStart !== undefined ? parseInt(String(caveStart)) : null;
    const lodValue = lod !== undefined ? parseInt(String(lod)) : 0;
    
    const hasViewport = viewStartX !== undefined && viewStartZ !== undefined && 
                        viewEndX !== undefined && viewEndZ !== undefined;
    const vx0 = hasViewport ? parseInt(String(viewStartX)) : null;
    const vz0 = hasViewport ? parseInt(String(viewStartZ)) : null;
    const vx1 = hasViewport ? parseInt(String(viewEndX)) : null;
    const vz1 = hasViewport ? parseInt(String(viewEndZ)) : null;
    
    const coordPairs = String(coords).split(';').map(s => {
      const parts = s.split(',');
      return { x: parseInt(parts[0]), z: parseInt(parts[1]) };
    }).filter(c => !isNaN(c.x) && !isNaN(c.z)).slice(0, MAX_BATCH_REGIONS);
    
    const filteredPairs = hasViewport 
      ? coordPairs.filter(c => c.x >= vx0 && c.x <= vx1 && c.z >= vz0 && c.z <= vz1)
      : coordPairs;
    
    const totalRegions = filteredPairs.length;
    if (totalRegions === 0) {
      req.off('close', cancelHandler);
      cancelledRequests.delete(requestId);
      const emptyBuffer = Buffer.alloc(8);
      emptyBuffer.writeUInt32LE(0, 0);
      emptyBuffer.writeUInt32LE(lodValue, 4);
      res.setHeader('Content-Type', 'application/octet-stream');
      res.setHeader('Content-Length', emptyBuffer.length);
      return res.send(emptyBuffer);
    }
    
    const lodSize = 512 >> lodValue;
    const REGION_PIXEL_SIZE = lodSize * lodSize * 4;
    
    const centerX = hasViewport ? (vx0 + vx1) / 2 : 0;
    const centerZ = hasViewport ? (vz0 + vz1) / 2 : 0;
    const maxDist = hasViewport ? Math.max(vx1 - vx0, vz1 - vz0) / 2 : 1;
    
    const results = await Promise.all(
      filteredPairs.map(coord => {
        const dist = hasViewport 
          ? Math.sqrt(Math.pow(coord.x - centerX, 2) + Math.pow(coord.z - centerZ, 2))
          : 0;
        const priority = Math.max(0, 1000 - Math.floor(dist / maxDist * 1000));
        return loadRegionPixels(dimPath, coord.x, coord.z, caveModeValue, caveStartValue, lodValue, priority, requestId);
      })
    );
    
    if (isCancelled) {
      req.off('close', cancelHandler);
      cancelledRequests.delete(requestId);
      return;
    }
    
    cancelledRequests.delete(requestId);
    
    let totalSize = 4 + 4;
    for (let i = 0; i < totalRegions; i++) {
      totalSize += 12 + (results[i] ? results[i].length : 0);
    }
    
    const buffer = Buffer.alloc(totalSize);
    let offset = 0;
    
    buffer.writeUInt32LE(totalRegions, offset);
    offset += 4;
    buffer.writeUInt32LE(lodValue, offset);
    offset += 4;
    
    for (let i = 0; i < totalRegions; i++) {
      buffer.writeInt32LE(filteredPairs[i].x, offset);
      buffer.writeInt32LE(filteredPairs[i].z, offset + 4);
      
      if (results[i]) {
        const resultBuffer = Buffer.isBuffer(results[i]) ? results[i] : Buffer.from(results[i]);
        buffer.writeUInt32LE(resultBuffer.length, offset + 8);
        resultBuffer.copy(buffer, offset + 12);
        offset += 12 + resultBuffer.length;
      } else {
        buffer.writeUInt32LE(0, offset + 8);
        offset += 12;
      }
    }
    
    results.length = 0;
    if (global.gc) global.gc();
    
    req.off('close', cancelHandler);
    res.setHeader('Content-Type', 'application/octet-stream');
    res.setHeader('Content-Length', buffer.length);
    res.send(buffer);
  } catch (error) {
    req.off('close', cancelHandler);
    console.error('Error batch loading regions:', error);
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/region', async (req, res) => {
  const { world, dim, x, z, mapType } = req.query;
  
  if (!dim || x === undefined || z === undefined) {
    return res.status(400).json({ error: '缺少参数' });
  }
  
  try {
    let basePath = mapDirectory;
    if (world && world !== '当前世界') basePath = path.join(mapDirectory, String(world));
    
    let dimPath = path.join(basePath, String(dim));
    if (mapType) {
      dimPath = path.join(dimPath, String(mapType));
    } else {
      const mapTypes = await listMapTypes(dimPath);
      if (mapTypes.length > 0) dimPath = path.join(dimPath, mapTypes[0].path);
    }
    
    const regionData = await loadRegion(dimPath, parseInt(String(x)), parseInt(String(z)));
    if (!regionData) {
      return res.status(404).json({ error: '区域不存在' });
    }
    
    res.json(regionData);
  } catch (error) {
    console.error('Error loading region:', error);
    res.status(500).json({ error: String(error) });
  }
});

async function detectDirectoryStructure(basePath) {
  const entries = await readdir(basePath, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.isDirectory()) {
      const dimNames = ['null', 'DIM-1', 'DIM1', 'overworld'];
      if (dimNames.includes(entry.name)) return 'world';
      const subPath = path.join(basePath, entry.name);
      const hasRegions = await hasRegionFiles(subPath);
      if (hasRegions) return 'world';
    }
  }
  return 'root';
}

async function listWorlds(basePath) {
  const entries = await readdir(basePath, { withFileTypes: true });
  const worlds = [];
  for (const entry of entries) {
    if (entry.isDirectory()) {
      const worldPath = path.join(basePath, entry.name);
      const dims = await listDimensions(worldPath);
      if (dims.length > 0) worlds.push(entry.name);
    }
  }
  return worlds;
}

async function listDimensions(worldPath) {
  const entries = await readdir(worldPath, { withFileTypes: true });
  const dimensions = [];
  for (const entry of entries) {
    if (entry.isDirectory()) {
      const dimPath = path.join(worldPath, entry.name);
      const mapTypes = await listMapTypes(dimPath);
      if (mapTypes.length > 0) {
        dimensions.push({ name: getDimensionDisplayName(entry.name), path: entry.name });
      }
    }
  }
  
  const order = { '主世界': 0, 'null': 0, 'overworld': 0, '下界': 1, 'DIM-1': 1, 'minecraft:the_nether': 1, '末地': 2, 'DIM1': 2, 'minecraft:the_end': 2 };
  dimensions.sort((a, b) => {
    const orderA = order[a.name] ?? order[a.path] ?? 99;
    const orderB = order[b.name] ?? order[b.path] ?? 99;
    return orderA - orderB;
  });
  
  return dimensions;
}

async function listMapTypes(dimPath) {
  const entries = await readdir(dimPath, { withFileTypes: true });
  const mapTypes = [];
  for (const entry of entries) {
    if (entry.isDirectory()) {
      const mapTypePath = path.join(dimPath, entry.name);
      const hasRegions = await hasRegionFiles(mapTypePath);
      if (hasRegions) {
        mapTypes.push({ name: getMapTypeDisplayName(entry.name), path: entry.name });
      }
    }
  }
  return mapTypes;
}

function getDimensionDisplayName(dimName) {
  const dimNames = {
    'DIM-1': '下界', 'DIM1': '末地', 'null': '主世界',
    'minecraft:the_nether': '下界', 'minecraft:the_end': '末地', 'overworld': '主世界'
  };
  return dimNames[dimName] || dimName;
}

function getMapTypeDisplayName(mapTypeName) {
  const names = { 'mw$default': '默认地图', 'mw0,2,0': '地图 v0.2.0' };
  if (mapTypeName.startsWith('cache_')) return `缓存 ${mapTypeName.replace('cache_', '')}`;
  return names[mapTypeName] || mapTypeName;
}

async function hasRegionFiles(dirPath) {
  try {
    const entries = await readdir(dirPath);
    return entries.some(e => e.endsWith('.zip') || e.endsWith('.xaero') || e.endsWith('.xwmc'));
  } catch {
    return false;
  }
}

async function listRegions(dimPath, caveMode = null, caveStart = null) {
  if (caveMode === 0 || caveMode === null) {
    const entries = await readdir(dimPath);
    const regions = [];
    for (const entry of entries) {
      const match = entry.match(/^(-?\d+)_(-?\d+)\.(zip|xaero|xwmc)$/);
      if (match) {
        regions.push({ x: parseInt(match[1], 10), z: parseInt(match[2], 10) });
      }
    }
    return regions;
  }
  
  const regionSet = new Set();
  const maxLayer = caveMode === 2 ? 15 : (caveStart !== null ? Math.floor(caveStart / 16) : 7);
  
  for (let layer = 0; layer <= maxLayer; layer++) {
    const cavePath = path.join(dimPath, 'caves', String(layer));
    try {
      const entries = await readdir(cavePath);
      for (const entry of entries) {
        const match = entry.match(/^(-?\d+)_(-?\d+)\.(zip|xaero|xwmc)$/);
        if (match) {
          const key = `${match[1]},${match[2]}`;
          if (!regionSet.has(key)) {
            regionSet.add(key);
          }
        }
      }
    } catch {}
  }
  
  return Array.from(regionSet).map(key => {
    const [x, z] = key.split(',').map(Number);
    return { x, z };
  });
}

async function loadRegion(dimPath, regionX, regionZ) {
  const zipPath = path.join(dimPath, `${regionX}_${regionZ}.zip`);
  const xaeroPath = path.join(dimPath, `${regionX}_${regionZ}.xaero`);
  
  let filePath = null;
  if (existsSync(zipPath)) filePath = zipPath;
  else if (existsSync(xaeroPath)) filePath = xaeroPath;
  
  if (!filePath) return null;
  
  const buffer = await readFile(filePath);
  
  if (buffer[0] === 0x50 && buffer[1] === 0x4b) {
    const zip = new JSZip();
    const contents = await zip.loadAsync(buffer);
    const fileNames = Object.keys(contents.files);
    for (const fileName of fileNames) {
      if (fileName.endsWith('.xaero')) {
        const data = await contents.file(fileName).async('uint8array');
        return parseRegionFile(data, regionX, regionZ);
      }
    }
  }
  
  return parseRegionFile(new Uint8Array(buffer), regionX, regionZ);
}

function parseRegionFile(buffer, regionX, regionZ) {
  try {
    const data = new Uint8Array(buffer);
    const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
    let offset = 0;
    
    const blockStatePalette = [];
    const biomePalette = [];
    
    const firstByte = view.getUint8(offset++);
    let majorVersion = 0;
    let minorVersion = 0;
    
    if (firstByte === 0xFF) {
      const fullVersion = view.getInt32(offset, false);
      offset += 4;
      minorVersion = fullVersion & 0xFFFF;
      majorVersion = (fullVersion >> 16) & 0xFFFF;
    } else {
      offset = 0;
    }
    
    const chunks = Array(8).fill(null).map(() => Array(8).fill(null));
    
    while (offset < data.length - 1) {
      const chunkCoords = view.getUint8(offset++);
      if (chunkCoords === 0xFF) break;
      
      const chunkX = chunkCoords >> 4;
      const chunkZ = chunkCoords & 0x0F;
      
      if (chunkX >= 8 || chunkZ >= 8) break;
      
      const tiles = Array(4).fill(null).map(() => Array(4).fill(null));
      
      for (let i = 0; i < 4; i++) {
        for (let j = 0; j < 4; j++) {
          if (offset + 4 > data.length) break;
          
          const tileMarker = view.getInt32(offset, false);
          offset += 4;
          
          if (tileMarker === -1) {
            tiles[i][j] = null;
            continue;
          }
          
          try {
            const tile = parseTile(data, view, offset, tileMarker, minorVersion, majorVersion, blockStatePalette, biomePalette);
            offset = tile.newOffset;
            tiles[i][j] = tile.tile;
          } catch (e) {
            tiles[i][j] = null;
          }
        }
      }
      
      chunks[chunkX][chunkZ] = {
        x: regionX * 8 + chunkX,
        z: regionZ * 8 + chunkZ,
        tiles,
        loadState: 2
      };
    }
    
    return {
      regionX,
      regionZ,
      worldId: '',
      dimId: '',
      mwId: '',
      caveLayer: Number.MAX_SAFE_INTEGER,
      chunks,
      version: majorVersion * 1000 + minorVersion
    };
  } catch (e) {
    console.error('Error parsing region file:', e);
    return {
      regionX,
      regionZ,
      worldId: '',
      dimId: '',
      mwId: '',
      caveLayer: Number.MAX_SAFE_INTEGER,
      chunks: Array(8).fill(null).map(() => Array(8).fill(null)),
      version: 0
    };
  }
}

function parseTile(data, view, offset, tileMarker, minorVersion, majorVersion, blockStatePalette, biomePalette) {
  const blocks = [];
  
  for (let x = 0; x < 16; x++) {
    blocks[x] = [];
    for (let z = 0; z < 16; z++) {
      try {
        if (offset + 4 > data.length) {
          blocks[x][z] = { s: 'minecraft:air', h: 0, l: 15, b: null };
          continue;
        }
        
        const isFirstBlock = (x === 0 && z === 0);
        const blockParams = isFirstBlock ? tileMarker : view.getInt32(offset, false);
        if (!isFirstBlock) offset += 4;
        
        const result = parseBlock(data, view, offset, blockParams, minorVersion, majorVersion, blockStatePalette, biomePalette);
        blocks[x][z] = result.block;
        offset = result.newOffset;
      } catch (e) {
        blocks[x][z] = { s: 'minecraft:air', h: 0, l: 15, b: null };
      }
    }
  }
  
  if (minorVersion >= 4 && offset + 1 <= data.length) offset++;
  if (minorVersion >= 6 && offset + 4 <= data.length) offset += 4;
  if (minorVersion >= 7 && offset + 1 <= data.length) offset++;
  
  return { tile: { blocks, loaded: true }, newOffset: offset };
}

function parseBlock(data, view, offset, parametres, minorVersion, majorVersion, blockStatePalette, biomePalette) {
  const hasState = (parametres & 1) !== 0;
  const hasOverlays = (parametres & 2) !== 0;
  const stillUsesColorTypes = minorVersion < 5 || majorVersion <= 2;
  const savedColourType = stillUsesColorTypes ? (parametres >> 2) & 3 : 0;
  
  let state = null;
  
  if (hasState) {
    if (majorVersion === 0) {
      const stateId = view.getInt32(offset, false);
      offset += 4;
      state = `minecraft:unknown_${stateId}`;
    } else {
      const isNewToPalette = (parametres & 0x200000) !== 0;
      if (isNewToPalette) {
        try {
          const nbtResult = readNBTInline(data, offset);
          offset = nbtResult.newOffset;
          state = nbtResult.nbt?.Name || 'minecraft:air';
          blockStatePalette.push(state);
        } catch (e) {
          state = 'minecraft:air';
        }
      } else {
        if (offset + 4 <= data.length) {
          const paletteIndex = view.getInt32(offset, false);
          offset += 4;
          state = blockStatePalette[paletteIndex] || null;
        }
      }
    }
  } else {
    state = 'minecraft:grass_block';
  }
  
  let height = 0;
  if ((parametres & 64) !== 0) {
    if (offset + 1 <= data.length) height = view.getUint8(offset++);
  } else {
    const heightSecondPartOffset = minorVersion >= 4 ? 25 : 24;
    const heightBitsCombined = (parametres >> 12 & 255) | ((parametres >> heightSecondPartOffset & 15) << 8);
    height = heightBitsCombined << 20 >> 20;
  }
  
  const hasTopHeight = minorVersion >= 4 && (parametres & 0x1000000) !== 0;
  if (hasTopHeight && offset + 1 <= data.length) offset++;
  
  let hasWaterOverlay = false;
  
  if (hasOverlays && offset + 1 <= data.length) {
    const overlayCount = view.getUint8(offset++);
    for (let i = 0; i < overlayCount && offset + 4 <= data.length; i++) {
      try {
        const overlayResult = parseOverlay(data, view, offset, minorVersion, majorVersion, blockStatePalette);
        offset = overlayResult.newOffset;
        if (overlayResult.state === 'minecraft:water') {
          hasWaterOverlay = true;
        }
      } catch (e) {
        break;
      }
    }
  }
  
  if (savedColourType === 3 && offset + 4 <= data.length) offset += 4;
  
  let biome = null;
  const hasBiome = (parametres & 0x100000) !== 0;
  if ((savedColourType !== 0 && savedColourType !== 3) || hasBiome) {
    if (offset + 4 <= data.length) {
      const isNewBiomeToPalette = (parametres & 0x400000) !== 0;
      if (isNewBiomeToPalette) {
        const biomeAsInt = (parametres & 0x800000) !== 0;
        if (biomeAsInt) {
          const biomeId = view.getInt32(offset, false);
          offset += 4;
          biome = `biome:${biomeId}`;
        } else if (offset + 2 <= data.length) {
          const strLen = view.getUint8(offset) << 8 | view.getUint8(offset + 1);
          offset += 2;
          if (offset + strLen <= data.length) {
            const strBytes = data.slice(offset, offset + strLen);
            offset += strLen;
            biome = new TextDecoder().decode(strBytes);
          }
        }
        if (biome) biomePalette.push(biome);
      } else {
        const biomeIndex = view.getInt32(offset, false);
        offset += 4;
        biome = biomePalette[biomeIndex] || null;
      }
    }
  }
  
  return { 
    block: { 
      s: state, 
      h: height, 
      l: 15, 
      b: biome,
      w: hasWaterOverlay 
    }, 
    newOffset: offset 
  };
}

function parseOverlay(data, view, offset, minorVersion, majorVersion, blockStatePalette) {
  const overlayParams = view.getInt32(offset, false);
  offset += 4;
  
  let overlayState = null;
  const hasState = (overlayParams & 1) !== 0;
  
  if (!hasState) {
    overlayState = 'minecraft:water';
  } else {
    const isNewToPalette = (overlayParams & 0x400) !== 0;
    if (isNewToPalette) {
      try {
        const nbtResult = readNBTInline(data, offset);
        offset = nbtResult.newOffset;
        overlayState = nbtResult.nbt?.Name || 'minecraft:air';
        blockStatePalette.push(overlayState);
      } catch (e) {}
    } else {
      if (offset + 4 <= data.length) {
        const paletteIndex = view.getInt32(offset, false);
        offset += 4;
        overlayState = blockStatePalette[paletteIndex] || null;
      }
    }
  }
  
  if (minorVersion < 1 && (overlayParams & 2) !== 0) offset += 4;
  
  const stillUsesColorTypes = minorVersion < 5 || majorVersion <= 2;
  const savedColorType = stillUsesColorTypes ? (overlayParams >> 8) & 3 : 0;
  if (savedColorType === 2 || (overlayParams & 4) !== 0) offset += 4;
  if (minorVersion < 8 && (overlayParams & 8) !== 0) offset += 4;
  
  return { newOffset: offset, state: overlayState };
}

function readNBTInline(data, offset) {
  const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
  const tagType = view.getUint8(offset++);
  if (tagType === 0) return { nbt: null, newOffset: offset };
  
  const nameLen = view.getUint8(offset) << 8 | view.getUint8(offset + 1);
  offset += 2 + nameLen;
  
  const nbt = readTagValue(data, view, offset, tagType);
  return { nbt: nbt.value, newOffset: nbt.newOffset };
}

function readTagValue(data, view, offset, type) {
  switch (type) {
    case 0: return { value: null, newOffset: offset };
    case 1: { const value = view.getInt8(offset++); return { value, newOffset: offset }; }
    case 2: { const value = view.getInt16(offset, false); offset += 2; return { value, newOffset: offset }; }
    case 3: { const value = view.getInt32(offset, false); offset += 4; return { value, newOffset: offset }; }
    case 4: { offset += 8; return { value: null, newOffset: offset }; }
    case 5: { offset += 4; return { value: null, newOffset: offset }; }
    case 6: { offset += 8; return { value: null, newOffset: offset }; }
    case 7: { const length = view.getInt32(offset, false); offset += 4 + length; return { value: null, newOffset: offset }; }
    case 8: {
      const length = view.getUint8(offset) << 8 | view.getUint8(offset + 1);
      offset += 2;
      const value = new TextDecoder().decode(data.slice(offset, offset + length));
      offset += length;
      return { value, newOffset: offset };
    }
    case 9: {
      const listType = view.getUint8(offset++);
      const listLength = view.getInt32(offset, false);
      offset += 4;
      for (let i = 0; i < listLength; i++) {
        const result = readTagValue(data, view, offset, listType);
        offset = result.newOffset;
      }
      return { value: null, newOffset: offset };
    }
    case 10: {
      const value = {};
      while (true) {
        const childType = view.getUint8(offset++);
        if (childType === 0) break;
        const nameLen = view.getUint8(offset) << 8 | view.getUint8(offset + 1);
        offset += 2;
        const name = new TextDecoder().decode(data.slice(offset, offset + nameLen));
        offset += nameLen;
        const result = readTagValue(data, view, offset, childType);
        value[name] = result.value;
        offset = result.newOffset;
      }
      return { value, newOffset: offset };
    }
    case 11: { const length = view.getInt32(offset, false); offset += 4 + length * 4; return { value: null, newOffset: offset }; }
    case 12: { const length = view.getInt32(offset, false); offset += 4 + length * 8; return { value: null, newOffset: offset }; }
    default: return { value: null, newOffset: offset };
  }
}

const server = createServer(app);
const wss = new WebSocketServer({ server });

const wsClients = new Set();

wss.on('connection', (ws) => {
  wsClients.add(ws);
  
  const clientId = clientIdCounter++;
  ws.clientId = clientId;
  clientPendingRequests.set(clientId, { requestId: null, lod: null });
  
  ws.send(JSON.stringify({ 
    type: 'server-config', 
    config: {
      cacheDirectory,
      mapDirectory,
      baseMapDirectory
    }
  }));
  
  ws.on('close', () => {
    wsClients.delete(ws);
    clientPendingRequests.delete(clientId);
    clientViewports.delete(clientId);
  });
  ws.on('message', async (data) => {
    try {
      const msg = JSON.parse(data.toString());
      await handleWsMessage(ws, msg);
    } catch (e) {
      ws.send(JSON.stringify({ error: e.message }));
    }
  });
});

async function handleWsMessage(ws, msg) {
  const { type, requestId, payload } = msg;
  
  if (type === 'batch-regions') {
    const { world, dim, coords, mapType, caveMode, caveStart, lod, viewStartX, viewStartZ, viewEndX, viewEndZ } = payload;
    
    if (!dim || !coords) {
      ws.send(JSON.stringify({ type: 'error', requestId, error: '缺少参数' }));
      return;
    }
    
    const clientId = ws.clientId;
    const lodValue = lod !== undefined ? parseInt(lod) : 0;
    
    if (clientId !== undefined) {
      const pending = clientPendingRequests.get(clientId);
      if (pending && pending.requestId !== null && pending.lod !== null && pending.lod !== lodValue) {
        cancelRequest(pending.requestId);
      }
      clientPendingRequests.set(clientId, { requestId, lod: lodValue });
      
      if (viewStartX !== undefined && viewStartZ !== undefined && 
          viewEndX !== undefined && viewEndZ !== undefined) {
        clientViewports.set(clientId, {
          startX: parseInt(viewStartX),
          startZ: parseInt(viewStartZ),
          endX: parseInt(viewEndX),
          endZ: parseInt(viewEndZ)
        });
        cancelTasksNotInViewport();
      }
    }
    
    try {
      let basePath = mapDirectory;
      if (world && world !== '当前世界') basePath = path.join(mapDirectory, world);
      
      let dimPath = path.join(basePath, dim);
      if (mapType) {
        dimPath = path.join(dimPath, mapType);
      } else {
        const mapTypes = await listMapTypes(dimPath);
        if (mapTypes.length > 0) dimPath = path.join(dimPath, mapTypes[0].path);
      }
      
      const caveModeValue = caveMode !== undefined ? parseInt(caveMode) : null;
      const caveStartValue = caveStart !== undefined ? parseInt(caveStart) : null;
      
      const coordPairs = coords.split(';').map(c => {
        const [x, z] = c.split(',').map(Number);
        return { x, z };
      }).filter(c => !isNaN(c.x) && !isNaN(c.z)).slice(0, MAX_BATCH_REGIONS);
      
      const totalRegions = coordPairs.length;
      if (totalRegions === 0) {
        const emptyBuffer = Buffer.alloc(12);
        emptyBuffer.writeUInt32LE(requestId, 0);
        emptyBuffer.writeUInt32LE(0, 4);
        emptyBuffer.writeUInt32LE(lodValue, 8);
        ws.send(emptyBuffer);
        return;
      }
      
      const results = await Promise.all(
        coordPairs.map(coord => {
          if (!isRegionInAnyViewport(coord.x, coord.z)) {
            return null;
          }
          return loadRegionPixels(dimPath, coord.x, coord.z, caveModeValue, caveStartValue, lodValue, 0, requestId);
        })
      );
      
      let totalSize = 12;
      for (let i = 0; i < totalRegions; i++) {
        totalSize += 12 + (results[i] ? results[i].length : 0);
      }
      
      const buffer = Buffer.alloc(totalSize);
      let offset = 0;
      
      buffer.writeUInt32LE(requestId, offset);
      offset += 4;
      buffer.writeUInt32LE(totalRegions, offset);
      offset += 4;
      buffer.writeUInt32LE(lodValue, offset);
      offset += 4;
      
      for (let i = 0; i < totalRegions; i++) {
        buffer.writeInt32LE(coordPairs[i].x, offset);
        buffer.writeInt32LE(coordPairs[i].z, offset + 4);
        
        if (results[i]) {
          const resultBuffer = Buffer.isBuffer(results[i]) ? results[i] : Buffer.from(results[i]);
          buffer.writeUInt32LE(resultBuffer.length, offset + 8);
          resultBuffer.copy(buffer, offset + 12);
          offset += 12 + resultBuffer.length;
        } else {
          buffer.writeUInt32LE(0, offset + 8);
          offset += 12;
        }
      }
      
      results.length = 0;
      if (global.gc) global.gc();
      
      ws.send(buffer);
    } catch (e) {
      console.error('WebSocket batch-regions error:', e.message);
      ws.send(JSON.stringify({ type: 'error', requestId, error: e.message }));
    } finally {
      if (clientId !== undefined) {
        const pending = clientPendingRequests.get(clientId);
        if (pending && pending.requestId === requestId) {
          clientPendingRequests.set(clientId, { requestId: null, lod: null });
        }
      }
    }
  }
}

server.listen(PORT, async () => {
  await ensureCacheDir();
  initDatabase(mapDirectory || null);
  initWorkerPool();
  console.log(`Xaero Map Server running at http://localhost:${PORT}`);
  console.log(`WebSocket server ready at ws://localhost:${PORT}`);
  console.log(`Default directory: ${mapDirectory || '(not set)'}`);
  console.log(`Cache database: ${currentMapDbPath}`);
  console.log(`Config: maxCacheEntries=${MAX_MEMORY_CACHE_ENTRIES}, maxConcurrentLoads=${MAX_CONCURRENT_LOADS}, maxBatchRegions=${MAX_BATCH_REGIONS}`);
});
