import express from 'express';
import cors from 'cors';
import compression from 'compression';
import { readdir, mkdir } from 'fs/promises';
import { existsSync, writeFileSync, readFileSync, mkdirSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { cpus, totalmem } from 'os';
import { Worker } from 'worker_threads';
import { createServer } from 'http';
import { WebSocketServer } from 'ws';
import { LRUCache } from './lru-cache.js';
import {
  extractHeightsFromRegion,
  renderRegionToPixels,
  renderCaveLayersToPixels,
  loadRegion
} from './regionParser.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3001;

app.use(cors());

const SERVER_CONFIG_FILE = path.join(__dirname, 'server_config.json');

let mapDirectory = '';
let cacheDirectory = path.join(__dirname, 'cache');
let currentMapDbPath = '';
let currentWorld = null;

const totalMemoryGB = totalmem() / (1024 * 1024 * 1024);
let maxMemoryCacheEntries = Math.min(256, Math.floor(totalMemoryGB * 16));
if (maxMemoryCacheEntries < 64) maxMemoryCacheEntries = 64;
let maxConcurrentLoads = 32;
let maxBatchRegions = 64;
let compressionLevel = 6;

loadServerConfig();

app.use(compression({ level: compressionLevel, threshold: 1024 }));
app.use(express.json());
app.use(express.static(path.join(__dirname, '../dist')));

const bufferPool = [];
const BUFFER_POOL_MAX_SIZE = 64;

function getPooledBuffer(size) {
  for (let i = 0; i < bufferPool.length; i++) {
    if (bufferPool[i].length >= size) {
      const buf = bufferPool.splice(i, 1)[0];
      return buf.slice(0, size);
    }
  }
  return Buffer.alloc(size);
}

function releaseBuffer(buf) {
  if (bufferPool.length < BUFFER_POOL_MAX_SIZE) {
    bufferPool.push(buf);
  }
}

function loadServerConfig() {
  try {
    if (existsSync(SERVER_CONFIG_FILE)) {
      let content = readFileSync(SERVER_CONFIG_FILE, 'utf-8');
      if (content.charCodeAt(0) === 0xFEFF) {
        content = content.slice(1);
      }
      const config = JSON.parse(content);
      if (config.mapDirectory) {
        mapDirectory = config.mapDirectory;
      }
      if (config.cacheDirectory) {
        cacheDirectory = config.cacheDirectory;
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
      if (config.compressionLevel !== undefined) {
        compressionLevel = config.compressionLevel;
      }
      console.log('Config loaded:', { mapDirectory, compressionLevel, maxCacheEntries: maxMemoryCacheEntries });
    } else {
      saveServerConfig();
    }
  } catch (e) {
    console.log('Failed to load server config:', e.message);
  }
}

function saveServerConfig() {
  try {
    writeFileSync(SERVER_CONFIG_FILE, JSON.stringify({
      mapDirectory,
      cacheDirectory,
      maxCacheEntries: maxMemoryCacheEntries,
      maxConcurrentLoads,
      maxBatchRegions,
      compressionLevel
    }, null, 2));
  } catch (e) {
    console.log('Failed to save server config:', e.message);
  }
}

const MAX_MEMORY_CACHE_ENTRIES = maxMemoryCacheEntries;
const MAX_CONCURRENT_LOADS = maxConcurrentLoads;
const MAX_BATCH_REGIONS = maxBatchRegions;
const pixelCache = new LRUCache(MAX_MEMORY_CACHE_ENTRIES);

const NUM_WORKERS = cpus().length;
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

const MAX_HEIGHT_CACHE = 4096;
const sharedHeightCache = new LRUCache(MAX_HEIGHT_CACHE);

let dbWorker = null;
let dbWorkerTaskId = 0;
const dbWorkerPendingTasks = new Map();

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

function findInsertIndex(priority) {
  let left = 0;
  let right = taskQueue.length;
  while (left < right) {
    const mid = (left + right) >>> 1;
    if (taskQueue[mid].priority > priority) {
      left = mid + 1;
    } else {
      right = mid;
    }
  }
  return left;
}

function initWorkerPool() {
  for (let i = 0; i < NUM_WORKERS; i++) {
    const worker = new Worker(path.join(__dirname, 'regionWorker.js'), {
      workerData: { blockColors: BLOCK_COLORS, biomeColors: BIOME_COLORS }
    });
    
    worker.on('message', (msg) => {
      if (msg.type === 'getHeight') {
        const { requestId, dimPath, regionX, regionZ, lod } = msg;
        const key = `${dimPath}:${regionX},${regionZ}:lod${lod}`;
        const heights = sharedHeightCache.get(key);
        worker.postMessage({ type: 'heightResponse', requestId, heights, cacheKey: key });
        return;
      }
      
      if (msg.type === 'setHeight') {
        const { cacheKey, heights } = msg;
        if (heights && heights.length > 0) {
          sharedHeightCache.set(cacheKey, heights);
        }
        return;
      }
      
      const { taskId, result, error } = msg;
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

function initDbWorker(dbPath) {
  if (dbWorker) {
    try {
      dbWorker.terminate();
    } catch {}
    dbWorker = null;
  }
  
  dbWorker = new Worker(path.join(__dirname, 'dbWorker.js'), {
    workerData: { dbPath }
  });
  
  dbWorker.on('message', (msg) => {
    const { taskId, result, error } = msg;
    const task = dbWorkerPendingTasks.get(taskId);
    if (task) {
      dbWorkerPendingTasks.delete(taskId);
      if (error) {
        task.reject(new Error(error));
      } else {
        task.resolve(result);
      }
    }
  });
  
  dbWorker.on('error', (err) => {
    console.error('DB Worker error:', err);
  });
  
  console.log(`Database worker initialized for: ${dbPath}`);
}

function dbWorkerCall(type, data) {
  return new Promise((resolve, reject) => {
    if (!dbWorker) {
      reject(new Error('DB worker not initialized'));
      return;
    }
    
    const taskId = ++dbWorkerTaskId;
    dbWorkerPendingTasks.set(taskId, { resolve, reject });
    
    dbWorker.postMessage({ taskId, type, ...data });
  });
}

function processNextTask() {
  while (taskQueue.length > 0) {
    const freeWorkerIndex = workerBusy.findIndex(busy => !busy);
    if (freeWorkerIndex === -1) return;
    
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
    
    const task = {
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
    };
    
    const insertIndex = findInsertIndex(priority);
    taskQueue.splice(insertIndex, 0, task);
    
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

function getDbNameForWorld(worldName) {
  if (!worldName) return 'default';
  const normalized = worldName.replace(/[\\/:*?"<>|]/g, '_');
  const hash = normalized.split('').reduce((acc, char) => ((acc << 5) - acc + char.charCodeAt(0)) | 0, 0);
  return `${Math.abs(hash).toString(16)}_${normalized.substring(0, 48)}`;
}

function initDatabaseForWorld(worldName = null) {
  if (currentWorld === worldName && dbWorker) {
    return;
  }
  
  currentWorld = worldName;
  
  if (dbWorker) {
    try {
      dbWorker.terminate();
    } catch {}
    dbWorker = null;
  }
  
  clearMemoryCache();
  
  if (!existsSync(cacheDirectory)) {
    try {
      mkdirSync(cacheDirectory, { recursive: true });
    } catch (e) {
      console.error('Failed to create cache directory:', e.message);
    }
  }
  
  const dbName = getDbNameForWorld(worldName);
  const dbPath = path.join(cacheDirectory, `${dbName}.db`);
  currentMapDbPath = dbPath;
  
  initDbWorker(dbPath);
  
  //console.log(`Database initialized for world "${worldName}": ${dbPath}`);
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
  
  if (dbWorker) {
    try {
      const result = await dbWorkerCall('read', { key });
      if (result && result.found && result.data && result.data.length > 0) {
        pixelCache.set(key, result.data);
        return result.data;
      }
    } catch (e) {
      console.error('Database read error:', e.message);
    }
  }
  
  return null;
}

async function batchReadPixelCache(requests) {
  if (!requests || requests.length === 0) return new Map();
  
  const keyToCoord = new Map();
  const keysToFetch = [];
  const results = new Map();
  
  for (const req of requests) {
    const key = getCacheKey(req.dimPath, req.regionX, req.regionZ, req.yHeight, req.lod);
    keyToCoord.set(key, req);
    
    const cached = pixelCache.get(key);
    if (cached !== null) {
      if (cached && cached.length > 0) {
        results.set(key, cached);
      } else {
        pixelCache.delete(key);
        keysToFetch.push(key);
      }
    } else {
      keysToFetch.push(key);
    }
  }
  
  if (keysToFetch.length > 0 && dbWorker) {
    try {
      const dbResults = await dbWorkerCall('batchRead', { keys: keysToFetch });
      if (dbResults) {
        for (const [key, result] of Object.entries(dbResults)) {
          if (result && result.found && result.data && result.data.length > 0) {
            pixelCache.set(key, result.data);
            results.set(key, result.data);
          }
        }
      }
    } catch (e) {
      console.error('Database batch read error:', e.message);
    }
  }
  
  return results;
}

async function writePixelCache(dimPath, regionX, regionZ, pixels, yHeight = null, lod = 0) {
  if (!pixels || pixels.length === 0) return;
  
  const key = getCacheKey(dimPath, regionX, regionZ, yHeight, lod);
  pixelCache.set(key, pixels);
  
  if (dbWorker) {
    try {
      await dbWorkerCall('write', { key, data: pixels });
    } catch (e) {
      console.error('Database write error:', e.message);
    }
  }
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
  'minecraft:grass_path': { color: 0x947a41 },
  'minecraft:farmland': { color: 0x8f6747 },
  'minecraft:farmland#moisture=7': { color: 0x6a5338 },
  
  'minecraft:white_carpet': { color: 0xeaeced },
  'minecraft:orange_carpet': { color: 0xf17614 },
  'minecraft:magenta_carpet': { color: 0xbe45b4 },
  'minecraft:light_blue_carpet': { color: 0x3aafd9 },
  'minecraft:yellow_carpet': { color: 0xf9c628 },
  'minecraft:lime_carpet': { color: 0x70b91a },
  'minecraft:pink_carpet': { color: 0xee8dac },
  'minecraft:gray_carpet': { color: 0x3f4448 },
  'minecraft:light_gray_carpet': { color: 0x8e8e87 },
  'minecraft:cyan_carpet': { color: 0x158a91 },
  'minecraft:purple_carpet': { color: 0x7a2aad },
  'minecraft:blue_carpet': { color: 0x35399d },
  'minecraft:brown_carpet': { color: 0x724829 },
  'minecraft:green_carpet': { color: 0x556e1c },
  'minecraft:red_carpet': { color: 0xa12723 },
  'minecraft:black_carpet': { color: 0x15151a },
  
  'minecraft:white_wool': { color: 0xeaeced },
  'minecraft:orange_wool': { color: 0xf17614 },
  'minecraft:magenta_wool': { color: 0xbe45b4 },
  'minecraft:light_blue_wool': { color: 0x3aafd9 },
  'minecraft:yellow_wool': { color: 0xf9c628 },
  'minecraft:lime_wool': { color: 0x70b91a },
  'minecraft:pink_wool': { color: 0xee8dac },
  'minecraft:gray_wool': { color: 0x3f4448 },
  'minecraft:light_gray_wool': { color: 0x8e8e87 },
  'minecraft:cyan_wool': { color: 0x158a91 },
  'minecraft:purple_wool': { color: 0x7a2aad },
  'minecraft:blue_wool': { color: 0x35399d },
  'minecraft:brown_wool': { color: 0x724829 },
  'minecraft:green_wool': { color: 0x556e1c },
  'minecraft:red_wool': { color: 0xa12723 },
  'minecraft:black_wool': { color: 0x15151a },
  
  'minecraft:white_concrete': { color: 0xcfd5d6 },
  'minecraft:orange_concrete': { color: 0xe06101 },
  'minecraft:magenta_concrete': { color: 0xa9309f },
  'minecraft:light_blue_concrete': { color: 0x2489c7 },
  'minecraft:yellow_concrete': { color: 0xf1af15 },
  'minecraft:lime_concrete': { color: 0x5ea919 },
  'minecraft:pink_concrete': { color: 0xd6658f },
  'minecraft:gray_concrete': { color: 0x373a3e },
  'minecraft:light_gray_concrete': { color: 0x7d7d73 },
  'minecraft:cyan_concrete': { color: 0x157788 },
  'minecraft:purple_concrete': { color: 0x64209c },
  'minecraft:blue_concrete': { color: 0x2d2f8f },
  'minecraft:brown_concrete': { color: 0x603c20 },
  'minecraft:green_concrete': { color: 0x495b24 },
  'minecraft:red_concrete': { color: 0x8e2121 },
  'minecraft:black_concrete': { color: 0x080a0f },
  
  'minecraft:white_terracotta': { color: 0xd2b2a1 },
  'minecraft:orange_terracotta': { color: 0xa25426 },
  'minecraft:magenta_terracotta': { color: 0x96586d },
  'minecraft:light_blue_terracotta': { color: 0x716d8a },
  'minecraft:yellow_terracotta': { color: 0xba8523 },
  'minecraft:lime_terracotta': { color: 0x687635 },
  'minecraft:pink_terracotta': { color: 0xa24e4f },
  'minecraft:gray_terracotta': { color: 0x3a2a24 },
  'minecraft:light_gray_terracotta': { color: 0x876b62 },
  'minecraft:cyan_terracotta': { color: 0x575b5b },
  'minecraft:purple_terracotta': { color: 0x764656 },
  'minecraft:blue_terracotta': { color: 0x4a3c5b },
  'minecraft:brown_terracotta': { color: 0x4d3324 },
  'minecraft:green_terracotta': { color: 0x4c532a },
  'minecraft:red_terracotta': { color: 0x8f3d2f },
  'minecraft:black_terracotta': { color: 0x251710 },
  'minecraft:terracotta': { color: 0x985e44 },
  
  'minecraft:white_glazed_terracotta': { color: 0xd2b2a1 },
  'minecraft:orange_glazed_terracotta': { color: 0xa25426 },
  'minecraft:magenta_glazed_terracotta': { color: 0x96586d },
  'minecraft:light_blue_glazed_terracotta': { color: 0x716d8a },
  'minecraft:yellow_glazed_terracotta': { color: 0xba8523 },
  'minecraft:lime_glazed_terracotta': { color: 0x687635 },
  'minecraft:pink_glazed_terracotta': { color: 0xa24e4f },
  'minecraft:gray_glazed_terracotta': { color: 0x3a2a24 },
  'minecraft:light_gray_glazed_terracotta': { color: 0x876b62 },
  'minecraft:cyan_glazed_terracotta': { color: 0x575b5b },
  'minecraft:purple_glazed_terracotta': { color: 0x764656 },
  'minecraft:blue_glazed_terracotta': { color: 0x4a3c5b },
  'minecraft:brown_glazed_terracotta': { color: 0x4d3324 },
  'minecraft:green_glazed_terracotta': { color: 0x4c532a },
  'minecraft:red_glazed_terracotta': { color: 0x8f3d2f },
  'minecraft:black_glazed_terracotta': { color: 0x251710 },
  
  'minecraft:white_concrete_powder': { color: 0xcfd5d6 },
  'minecraft:orange_concrete_powder': { color: 0xe06101 },
  'minecraft:magenta_concrete_powder': { color: 0xa9309f },
  'minecraft:light_blue_concrete_powder': { color: 0x2489c7 },
  'minecraft:yellow_concrete_powder': { color: 0xf1af15 },
  'minecraft:lime_concrete_powder': { color: 0x5ea919 },
  'minecraft:pink_concrete_powder': { color: 0xd6658f },
  'minecraft:gray_concrete_powder': { color: 0x373a3e },
  'minecraft:light_gray_concrete_powder': { color: 0x7d7d73 },
  'minecraft:cyan_concrete_powder': { color: 0x157788 },
  'minecraft:purple_concrete_powder': { color: 0x64209c },
  'minecraft:blue_concrete_powder': { color: 0x2d2f8f },
  'minecraft:brown_concrete_powder': { color: 0x603c20 },
  'minecraft:green_concrete_powder': { color: 0x495b24 },
  'minecraft:red_concrete_powder': { color: 0x8e2121 },
  'minecraft:black_concrete_powder': { color: 0x080a0f },
  
  'minecraft:white_shulker_box': { color: 0xd8dddd },
  'minecraft:orange_shulker_box': { color: 0xf17614 },
  'minecraft:magenta_shulker_box': { color: 0xbe45b4 },
  'minecraft:light_blue_shulker_box': { color: 0x3aafd9 },
  'minecraft:yellow_shulker_box': { color: 0xf9c628 },
  'minecraft:lime_shulker_box': { color: 0x70b91a },
  'minecraft:pink_shulker_box': { color: 0xee8dac },
  'minecraft:gray_shulker_box': { color: 0x3f4448 },
  'minecraft:light_gray_shulker_box': { color: 0x8e8e87 },
  'minecraft:cyan_shulker_box': { color: 0x158a91 },
  'minecraft:purple_shulker_box': { color: 0x7a2aad },
  'minecraft:blue_shulker_box': { color: 0x35399d },
  'minecraft:brown_shulker_box': { color: 0x724829 },
  'minecraft:green_shulker_box': { color: 0x556e1c },
  'minecraft:red_shulker_box': { color: 0xa12723 },
  'minecraft:black_shulker_box': { color: 0x19191e },
  'minecraft:shulker_box': { color: 0x8b618b },
  
  'minecraft:white_candle': { color: 0xd2d9da },
  'minecraft:orange_candle': { color: 0xdb6409 },
  'minecraft:magenta_candle': { color: 0xa12e99 },
  'minecraft:light_blue_candle': { color: 0x238ac5 },
  'minecraft:yellow_candle': { color: 0xd1a532 },
  'minecraft:lime_candle': { color: 0x62ac17 },
  'minecraft:pink_candle': { color: 0xd0668e },
  'minecraft:gray_candle': { color: 0x505e61 },
  'minecraft:light_gray_candle': { color: 0x767970 },
  'minecraft:cyan_candle': { color: 0x117c7c },
  'minecraft:purple_candle': { color: 0x69229f },
  'minecraft:blue_candle': { color: 0x394ca1 },
  'minecraft:brown_candle': { color: 0x704629 },
  'minecraft:green_candle': { color: 0x496015 },
  'minecraft:red_candle': { color: 0x992724 },
  'minecraft:black_candle': { color: 0x26253a },
  'minecraft:candle': { color: 0xe8c999 },
  'minecraft:candle#lit=true': { color: 0xffd700 },
  
  'minecraft:anvil': { color: 0x494949 },
  'minecraft:chipped_anvil': { color: 0x494949 },
  'minecraft:damaged_anvil': { color: 0x484848 },
  
  'minecraft:grindstone': { color: 0x8a8a8a },
  'minecraft:stonecutter': { color: 0x7b776f },
  'minecraft:smithing_table': { color: 0x393b47 },
  'minecraft:fletching_table': { color: 0xc5b485 },
  'minecraft:cartography_table': { color: 0x675743 },
  'minecraft:loom': { color: 0x8e775c },
  'minecraft:barrel': { color: 0x87653a },
  'minecraft:smoker': { color: 0x555451 },
  'minecraft:blast_furnace': { color: 0x515051 },
  'minecraft:composter': { color: 0x996334 },
  'minecraft:lectern': { color: 0xae8a53 },
  'minecraft:bell': { color: 0xfdeb6f },
  'minecraft:lantern': { color: 0x6a5b54 },
  'minecraft:soul_lantern': { color: 0x486373 },
  'minecraft:campfire': { color: 0x4f4b44 },
  'minecraft:soul_campfire': { color: 0x4a6b6b },
  
  'minecraft:beacon': { color: 0x76ddd7 },
  'minecraft:conduit': { color: 0xa08c71 },
  'minecraft:end_portal_frame': { color: 0x5b7961 },
  'minecraft:respawn_anchor': { color: 0x4c1896 },
  'minecraft:lodestone': { color: 0x939599 },
  'minecraft:lightning_rod': { color: 0xc56f53 },
  
  'minecraft:enchanting_table': { color: 0x814b55 },
  'minecraft:brewing_stand': { color: 0x7a6551 },
  'minecraft:cauldron': { color: 0x4a4a4a },
  'minecraft:water_cauldron': { color: 0x3f76e4, water: true },
  'minecraft:lava_cauldron': { color: 0xcf4919 },
  'minecraft:powder_snow_cauldron': { color: 0xf9fefe },
  
  'minecraft:flower_pot': { color: 0x7c4535 },
  'minecraft:potted_oak_sapling': { color: 0x7c4535 },
  'minecraft:potted_spruce_sapling': { color: 0x7c4535 },
  'minecraft:potted_birch_sapling': { color: 0x7c4535 },
  'minecraft:potted_jungle_sapling': { color: 0x7c4535 },
  'minecraft:potted_acacia_sapling': { color: 0x7c4535 },
  'minecraft:potted_dark_oak_sapling': { color: 0x7c4535 },
  'minecraft:potted_mangrove_propagule': { color: 0x7c4535 },
  'minecraft:potted_cherry_sapling': { color: 0x7c4535 },
  'minecraft:potted_bamboo': { color: 0x7c4535 },
  'minecraft:potted_fern': { color: 0x7c4535 },
  'minecraft:potted_dandelion': { color: 0x7c4535 },
  'minecraft:potted_poppy': { color: 0x7c4535 },
  'minecraft:potted_blue_orchid': { color: 0x7c4535 },
  'minecraft:potted_allium': { color: 0x7c4535 },
  'minecraft:potted_azure_bluet': { color: 0x7c4535 },
  'minecraft:potted_red_tulip': { color: 0x7c4535 },
  'minecraft:potted_orange_tulip': { color: 0x7c4535 },
  'minecraft:potted_white_tulip': { color: 0x7c4535 },
  'minecraft:potted_pink_tulip': { color: 0x7c4535 },
  'minecraft:potted_oxeye_daisy': { color: 0x7c4535 },
  'minecraft:potted_cornflower': { color: 0x7c4535 },
  'minecraft:potted_lily_of_the_valley': { color: 0x7c4535 },
  'minecraft:potted_wither_rose': { color: 0x7c4535 },
  'minecraft:potted_torchflower': { color: 0x7c4535 },
  'minecraft:potted_cactus': { color: 0x7c4535 },
  'minecraft:potted_dead_bush': { color: 0x7c4535 },
  'minecraft:potted_crimson_fungus': { color: 0x7c4535 },
  'minecraft:potted_warped_fungus': { color: 0x7c4535 },
  'minecraft:potted_crimson_roots': { color: 0x7c4535 },
  'minecraft:potted_warped_roots': { color: 0x7c4535 },
  'minecraft:potted_mangrove_propagule': { color: 0x7c4535 },
  
  'minecraft:dandelion': { color: 0x94ac2b },
  'minecraft:poppy': { color: 0x814126 },
  'minecraft:blue_orchid': { color: 0x2fa3a8 },
  'minecraft:allium': { color: 0x9f89b8 },
  'minecraft:azure_bluet': { color: 0xa9cd7f },
  'minecraft:red_tulip': { color: 0x5a8121 },
  'minecraft:orange_tulip': { color: 0x5d8e1f },
  'minecraft:white_tulip': { color: 0x5ea547 },
  'minecraft:pink_tulip': { color: 0x639d4e },
  'minecraft:oxeye_daisy': { color: 0xb3ca8f },
  'minecraft:cornflower': { color: 0x507993 },
  'minecraft:lily_of_the_valley': { color: 0x7baf5f },
  'minecraft:wither_rose': { color: 0x292d17 },
  'minecraft:torchflower': { color: 0x65654d },
  'minecraft:sunflower': { color: 0x32811b },
  'minecraft:lilac': { color: 0x9b7d93 },
  'minecraft:rose_bush': { color: 0x834225 },
  'minecraft:peony': { color: 0x827f8b },
  'minecraft:pink_petals': { color: 0xf7b5db },
  'minecraft:pitcher_plant': { color: 0x5a7b6e },
  
  'minecraft:brown_mushroom': { color: 0x9a755d },
  'minecraft:red_mushroom': { color: 0xd94b44 },
  'minecraft:brown_mushroom_block': { color: 0x957051 },
  'minecraft:red_mushroom_block': { color: 0xc82f2d },
  'minecraft:mushroom_stem': { color: 0xcbc5ba },
  
  'minecraft:crimson_fungus': { color: 0x8d2c1e },
  'minecraft:warped_fungus': { color: 0x4a6d58 },
  'minecraft:crimson_roots': { color: 0x7e082a },
  'minecraft:warped_roots': { color: 0x148a7c },
  'minecraft:nether_sprouts': { color: 0x149785 },
  'minecraft:weeping_vines': { color: 0x690100 },
  'minecraft:twisting_vines': { color: 0x148f7c },
  
  'minecraft:sea_pickle': { color: 0x5a6128 },
  'minecraft:sea_lantern': { color: 0xacc8be },
  'minecraft:sea_grass': { color: 0x337f08, grass: true },
  'minecraft:tall_seagrass': { color: 0x3b8b0e, grass: true },
  'minecraft:kelp': { color: 0x578c2d },
  'minecraft:kelp_plant': { color: 0x57822b },
  
  'minecraft:turtle_egg': { color: 0xe4e3c0 },
  'minecraft:frogspawn': { color: 0x6e5f57 },
  'minecraft:sniffer_egg': { color: 0x7a6b5e },
  
  'minecraft:dragon_egg': { color: 0x0d0910 },
  
  'minecraft:sugar_cane': { color: 0x95c165 },
  'minecraft:bamboo': { color: 0x49761a },
  'minecraft:cactus': { color: 0x567f2b },
  'minecraft:pumpkin_stem': { color: 0x7a8b4a },
  'minecraft:attached_pumpkin_stem': { color: 0x7a8b4a },
  'minecraft:melon_stem': { color: 0x7a8b4a },
  'minecraft:attached_melon_stem': { color: 0x7a8b4a },
  'minecraft:cocoa': { color: 0x8b5a2b },
  'minecraft:sweet_berry_bush': { color: 0x5a8a3a },
  'minecraft:cave_vines': { color: 0x5a6d29 },
  'minecraft:cave_vines_plant': { color: 0x5a6d29 },
  'minecraft:spore_blossom': { color: 0xcf619f },
  'minecraft:big_dripleaf': { color: 0x708e34 },
  'minecraft:big_dripleaf_stem': { color: 0x5a7a2a },
  'minecraft:small_dripleaf': { color: 0x5f782e },
  'minecraft:azalea': { color: 0x667d30 },
  'minecraft:flowering_azalea': { color: 0x707a40 },
  'minecraft:moss_carpet': { color: 0x596e2d },
  'minecraft:hanging_roots': { color: 0xa1735c },
  
  'minecraft:redstone_wire': { color: 0xf0f0f0 },
  'minecraft:redstone_torch': { color: 0xff0000 },
  'minecraft:redstone_wall_torch': { color: 0xff0000 },
  'minecraft:repeater': { color: 0xa09d9c },
  'minecraft:comparator': { color: 0xa6a2a0 },
  'minecraft:observer': { color: 0x6b6b6b },
  'minecraft:daylight_detector': { color: 0x8a7a6a },
  'minecraft:target': { color: 0xe2aa9e },
  'minecraft:sculk_sensor': { color: 0x074654 },
  'minecraft:calibrated_sculk_sensor': { color: 0x1c4f65 },
  'minecraft:sculk_shrieker': { color: 0xc7cdaa },
  
  'minecraft:lever': { color: 0x6f5d43 },
  'minecraft:tripwire_hook': { color: 0x8a8a8a },
  'minecraft:tripwire': { color: 0x888888 },
  
  'minecraft:fire': { color: 0xd48c36 },
  'minecraft:soul_fire': { color: 0x33c1c5 },
  
  'minecraft:torch': { color: 0xffd700 },
  'minecraft:wall_torch': { color: 0xffd700 },
  'minecraft:soul_torch': { color: 0x4ac7c7 },
  'minecraft:soul_wall_torch': { color: 0x4ac7c7 },
  'minecraft:redstone_ore#lit=true': { color: 0xff0000 },
  'minecraft:deepslate_redstone_ore#lit=true': { color: 0xff0000 },
  
  'minecraft:glow_lichen': { color: 0x70837a },
  'minecraft:glowstone': { color: 0xac8354 },
  'minecraft:shroomlight': { color: 0xf19347 },
  'minecraft:ochre_froglight': { color: 0xfbf5cf },
  'minecraft:pearlescent_froglight': { color: 0xf6f0f0 },
  'minecraft:verdant_froglight': { color: 0xe5f4e4 },
  'minecraft:sea_lantern': { color: 0xacc8be },
  'minecraft:end_rod': { color: 0xfafafa },
  
  'minecraft:coral_block': { color: 0x6a6a6a },
  'minecraft:tube_coral': { color: 0x3053c5 },
  'minecraft:brain_coral': { color: 0xc65598 },
  'minecraft:bubble_coral': { color: 0xa118a0 },
  'minecraft:fire_coral': { color: 0xa7262f },
  'minecraft:horn_coral': { color: 0xd1ba3f },
  'minecraft:tube_coral_fan': { color: 0x3053c5 },
  'minecraft:brain_coral_fan': { color: 0xc65598 },
  'minecraft:bubble_coral_fan': { color: 0xa118a0 },
  'minecraft:fire_coral_fan': { color: 0xa7262f },
  'minecraft:horn_coral_fan': { color: 0xd1ba3f },
  'minecraft:dead_tube_coral': { color: 0x766f6c },
  'minecraft:dead_brain_coral': { color: 0x867d79 },
  'minecraft:dead_bubble_coral': { color: 0x857d79 },
  'minecraft:dead_fire_coral': { color: 0x89807c },
  'minecraft:dead_horn_coral': { color: 0x8f8782 },
  'minecraft:dead_tube_coral_fan': { color: 0x766f6c },
  'minecraft:dead_brain_coral_fan': { color: 0x867d79 },
  'minecraft:dead_bubble_coral_fan': { color: 0x857d79 },
  'minecraft:dead_fire_coral_fan': { color: 0x89807c },
  'minecraft:dead_horn_coral_fan': { color: 0x8f8782 },
  
  'minecraft:lily_pad': { color: 0x868686 },
  
  'minecraft:cobweb': { color: 0xe5e9ea },
  
  'minecraft:bedrock': { color: 0x555555 },
  'minecraft:obsidian': { color: 0x0f0b19 },
  'minecraft:crying_obsidian': { color: 0x210a3c },
  'minecraft:glowstone': { color: 0xac8354 },
  'minecraft:netherrack': { color: 0x622626 },
  'minecraft:basalt': { color: 0x515156 },
  'minecraft:blackstone': { color: 0x2a242a },
  'minecraft:gilded_blackstone': { color: 0x382b26 },
  'minecraft:ancient_debris': { color: 0x5f423a },
  'minecraft:netherite_block': { color: 0x433d40 },
  'minecraft:crying_obsidian': { color: 0x210a3c },
  'minecraft:respawn_anchor': { color: 0x4c1896 },
  
  'minecraft:end_stone': { color: 0xdcdf9e },
  'minecraft:end_portal_frame': { color: 0x5b7961 },
  'minecraft:purpur_block': { color: 0xaa7eaa },
  'minecraft:purpur_pillar': { color: 0xac80ab },
  'minecraft:end_stone_bricks': { color: 0xdcdf9e },
  'minecraft:chorus_flower': { color: 0x977998 },
  'minecraft:chorus_plant': { color: 0x5e395e },
  
  'minecraft:sculk': { color: 0x0d1e24 },
  'minecraft:sculk_vein': { color: 0x08303a },
  'minecraft:sculk_catalyst': { color: 0x0f2026 },
  'minecraft:sculk_shrieker': { color: 0xc7cdaa },
  'minecraft:reinforced_deepslate': { color: 0x50534f },
  
  'minecraft:dispenser': { color: 0x7a7a7a },
  'minecraft:dropper': { color: 0x7a7a7a },
  'minecraft:hopper': { color: 0x4c4a4c },
  'minecraft:chest': { color: 0x8a6b3a },
  'minecraft:trapped_chest': { color: 0x8a6b3a },
  'minecraft:ender_chest': { color: 0x1a1a2e },
  
  'minecraft:crafting_table': { color: 0x78492a },
  'minecraft:furnace': { color: 0x6e6e6e },
  'minecraft:jukebox': { color: 0x6a4c2e },
  'minecraft:note_block': { color: 0x6a4c2e },
  
  'minecraft:tnt': { color: 0x8f3e36 },
  'minecraft:tnt#unstable=true': { color: 0x8f3e36 },
  
  'minecraft:dragon_head': { color: 0x1a1a1a },
  'minecraft:dragon_wall_head': { color: 0x1a1a1a },
  'minecraft:player_head': { color: 0x8b8b8b },
  'minecraft:player_wall_head': { color: 0x8b8b8b },
  'minecraft:zombie_head': { color: 0x4a7a4a },
  'minecraft:zombie_wall_head': { color: 0x4a7a4a },
  'minecraft:skeleton_skull': { color: 0xe0e0e0 },
  'minecraft:skeleton_wall_skull': { color: 0xe0e0e0 },
  'minecraft:wither_skeleton_skull': { color: 0x3a3a3a },
  'minecraft:wither_skeleton_wall_skull': { color: 0x3a3a3a },
  'minecraft:creeper_head': { color: 0x0da70b },
  'minecraft:creeper_wall_head': { color: 0x0da70b },
  'minecraft:piglin_head': { color: 0xd4a574 },
  'minecraft:piglin_wall_head': { color: 0xd4a574 },
  
  'minecraft:armor_stand': { color: 0x8b8b8b },
  
  'minecraft:item_frame': { color: 0x8a6b3a },
  'minecraft:glow_item_frame': { color: 0xac8354 },
  'minecraft:painting': { color: 0x8a6b3a },
  
  'minecraft:oak_sign': { color: 0xa2834f },
  'minecraft:spruce_sign': { color: 0x735531 },
  'minecraft:birch_sign': { color: 0xc0af79 },
  'minecraft:jungle_sign': { color: 0xa07351 },
  'minecraft:acacia_sign': { color: 0xa85a32 },
  'minecraft:dark_oak_sign': { color: 0x432b14 },
  'minecraft:mangrove_sign': { color: 0x763631 },
  'minecraft:cherry_sign': { color: 0xe3b3ad },
  'minecraft:bamboo_sign': { color: 0xc1ad50 },
  'minecraft:crimson_sign': { color: 0x653147 },
  'minecraft:warped_sign': { color: 0x2b6963 },
  'minecraft:oak_hanging_sign': { color: 0xa2834f },
  'minecraft:spruce_hanging_sign': { color: 0x735531 },
  'minecraft:birch_hanging_sign': { color: 0xc0af79 },
  'minecraft:jungle_hanging_sign': { color: 0xa07351 },
  'minecraft:acacia_hanging_sign': { color: 0xa85a32 },
  'minecraft:dark_oak_hanging_sign': { color: 0x432b14 },
  'minecraft:mangrove_hanging_sign': { color: 0x763631 },
  'minecraft:cherry_hanging_sign': { color: 0xe3b3ad },
  'minecraft:bamboo_hanging_sign': { color: 0xc1ad50 },
  'minecraft:crimson_hanging_sign': { color: 0x653147 },
  'minecraft:warped_hanging_sign': { color: 0x2b6963 },
  'minecraft:oak_wall_sign': { color: 0xa2834f },
  'minecraft:spruce_wall_sign': { color: 0x735531 },
  'minecraft:birch_wall_sign': { color: 0xc0af79 },
  'minecraft:jungle_wall_sign': { color: 0xa07351 },
  'minecraft:acacia_wall_sign': { color: 0xa85a32 },
  'minecraft:dark_oak_wall_sign': { color: 0x432b14 },
  'minecraft:mangrove_wall_sign': { color: 0x763631 },
  'minecraft:cherry_wall_sign': { color: 0xe3b3ad },
  'minecraft:bamboo_wall_sign': { color: 0xc1ad50 },
  'minecraft:crimson_wall_sign': { color: 0x653147 },
  'minecraft:warped_wall_sign': { color: 0x2b6963 },
  
  'minecraft:white_banner': { color: 0xeaeced },
  'minecraft:orange_banner': { color: 0xf17614 },
  'minecraft:magenta_banner': { color: 0xbe45b4 },
  'minecraft:light_blue_banner': { color: 0x3aafd9 },
  'minecraft:yellow_banner': { color: 0xf9c628 },
  'minecraft:lime_banner': { color: 0x70b91a },
  'minecraft:pink_banner': { color: 0xee8dac },
  'minecraft:gray_banner': { color: 0x3f4448 },
  'minecraft:light_gray_banner': { color: 0x8e8e87 },
  'minecraft:cyan_banner': { color: 0x158a91 },
  'minecraft:purple_banner': { color: 0x7a2aad },
  'minecraft:blue_banner': { color: 0x35399d },
  'minecraft:brown_banner': { color: 0x724829 },
  'minecraft:green_banner': { color: 0x556e1c },
  'minecraft:red_banner': { color: 0xa12723 },
  'minecraft:black_banner': { color: 0x15151a },
  'minecraft:white_wall_banner': { color: 0xeaeced },
  'minecraft:orange_wall_banner': { color: 0xf17614 },
  'minecraft:magenta_wall_banner': { color: 0xbe45b4 },
  'minecraft:light_blue_wall_banner': { color: 0x3aafd9 },
  'minecraft:yellow_wall_banner': { color: 0xf9c628 },
  'minecraft:lime_wall_banner': { color: 0x70b91a },
  'minecraft:pink_wall_banner': { color: 0xee8dac },
  'minecraft:gray_wall_banner': { color: 0x3f4448 },
  'minecraft:light_gray_wall_banner': { color: 0x8e8e87 },
  'minecraft:cyan_wall_banner': { color: 0x158a91 },
  'minecraft:purple_wall_banner': { color: 0x7a2aad },
  'minecraft:blue_wall_banner': { color: 0x35399d },
  'minecraft:brown_wall_banner': { color: 0x724829 },
  'minecraft:green_wall_banner': { color: 0x556e1c },
  'minecraft:red_wall_banner': { color: 0xa12723 },
  'minecraft:black_wall_banner': { color: 0x15151a },
  
  'minecraft:structure_block': { color: 0x594a5a },
  'minecraft:jigsaw': { color: 0x504651 },
  'minecraft:command_block': { color: 0xb5886c },
  'minecraft:chain_command_block': { color: 0x84a597 },
  'minecraft:repeating_command_block': { color: 0x816fb0 },
  'minecraft:barrier': { color: 0x000000, alpha: 0 },
  'minecraft:light': { color: 0xffffff },
  'minecraft:structure_void': { color: 0x000000, alpha: 0 },
  
  'minecraft:moving_piston': { color: 0x8b8b8b },
  'minecraft:piston': { color: 0x8b8b8b },
  'minecraft:piston_head': { color: 0x8b8b8b },
  'minecraft:sticky_piston': { color: 0x8b8b8b },
  
  'minecraft:rail': { color: 0x7e7059 },
  'minecraft:powered_rail': { color: 0x8a6e4a },
  'minecraft:detector_rail': { color: 0x7b695a },
  'minecraft:activator_rail': { color: 0x73574a },
  
  'minecraft:scaffolding': { color: 0xaa8449 },
  'minecraft:ladder': { color: 0x7d6137 },
  
  'minecraft:hay_block': { color: 0xa68b0c },
  'minecraft:bone_block': { color: 0xd2ceb3 },
  'minecraft:honeycomb_block': { color: 0xe5941e },
  'minecraft:honey_block': { color: 0xfab935 },
  'minecraft:slime_block': { color: 0x6fc05b },
  
  'minecraft:beehive': { color: 0x9f804e },
  'minecraft:bee_nest': { color: 0xcaa04b },
  
  'minecraft:pointed_dripstone': { color: 0x866c5d },
  'minecraft:dripstone_block': { color: 0x866c5d },
  
  'minecraft:calcite': { color: 0xdfe0dd },
  'minecraft:amethyst_block': { color: 0x8662bf },
  'minecraft:budding_amethyst': { color: 0x8460bb },
  'minecraft:amethyst_cluster': { color: 0xa47fcf },
  'minecraft:large_amethyst_bud': { color: 0xa17ecb },
  'minecraft:medium_amethyst_bud': { color: 0x9e78ca },
  'minecraft:small_amethyst_bud': { color: 0x8463c0 },
  
  'minecraft:raw_iron_block': { color: 0xa6886b },
  'minecraft:raw_copper_block': { color: 0x9a6a4f },
  'minecraft:raw_gold_block': { color: 0xdea92f },
  
  'minecraft:copper_block': { color: 0xc06c50 },
  'minecraft:exposed_copper': { color: 0xa17e68 },
  'minecraft:weathered_copper': { color: 0x6c996e },
  'minecraft:oxidized_copper': { color: 0x52a385 },
  'minecraft:waxed_copper_block': { color: 0xc06c50 },
  'minecraft:waxed_exposed_copper': { color: 0xa17e68 },
  'minecraft:waxed_weathered_copper': { color: 0x6c996e },
  'minecraft:waxed_oxidized_copper': { color: 0x52a385 },
  'minecraft:copper_grate': { color: 0xc06c4f },
  'minecraft:exposed_copper_grate': { color: 0xa27e69 },
  'minecraft:weathered_copper_grate': { color: 0x6c996e },
  'minecraft:oxidized_copper_grate': { color: 0x52a385 },
  'minecraft:waxed_copper_grate': { color: 0xc06c4f },
  'minecraft:waxed_exposed_copper_grate': { color: 0xa27e69 },
  'minecraft:waxed_weathered_copper_grate': { color: 0x6c996e },
  'minecraft:waxed_oxidized_copper_grate': { color: 0x52a385 },
  'minecraft:copper_bulb': { color: 0x9c5739 },
  'minecraft:exposed_copper_bulb': { color: 0x876c5a },
  'minecraft:weathered_copper_bulb': { color: 0x6c996e },
  'minecraft:oxidized_copper_bulb': { color: 0x52a385 },
  'minecraft:waxed_copper_bulb': { color: 0x9c5739 },
  'minecraft:waxed_exposed_copper_bulb': { color: 0x876c5a },
  'minecraft:waxed_weathered_copper_bulb': { color: 0x6c996e },
  'minecraft:waxed_oxidized_copper_bulb': { color: 0x52a385 },
  'minecraft:chiseled_copper': { color: 0xb8654a },
  'minecraft:waxed_chiseled_copper': { color: 0xb8654a },
  'minecraft:copper_door': { color: 0xc06c50 },
  'minecraft:exposed_copper_door': { color: 0xa17e68 },
  'minecraft:weathered_copper_door': { color: 0x6c996e },
  'minecraft:oxidized_copper_door': { color: 0x52a385 },
  'minecraft:waxed_copper_door': { color: 0xc06c50 },
  'minecraft:waxed_exposed_copper_door': { color: 0xa17e68 },
  'minecraft:waxed_weathered_copper_door': { color: 0x6c996e },
  'minecraft:waxed_oxidized_copper_door': { color: 0x52a385 },
  'minecraft:copper_trapdoor': { color: 0xc06c50 },
  'minecraft:exposed_copper_trapdoor': { color: 0xa17e68 },
  'minecraft:weathered_copper_trapdoor': { color: 0x6c996e },
  'minecraft:oxidized_copper_trapdoor': { color: 0x52a385 },
  'minecraft:waxed_copper_trapdoor': { color: 0xc06c50 },
  'minecraft:waxed_exposed_copper_trapdoor': { color: 0xa17e68 },
  'minecraft:waxed_weathered_copper_trapdoor': { color: 0x6c996e },
  'minecraft:waxed_oxidized_copper_trapdoor': { color: 0x52a385 },
  'minecraft:cut_copper': { color: 0xbf6b51 },
  'minecraft:exposed_cut_copper': { color: 0x9b7a65 },
  'minecraft:weathered_cut_copper': { color: 0x6c996e },
  'minecraft:oxidized_cut_copper': { color: 0x52a385 },
  'minecraft:waxed_cut_copper': { color: 0xbf6b51 },
  'minecraft:waxed_exposed_cut_copper': { color: 0x9b7a65 },
  'minecraft:waxed_weathered_cut_copper': { color: 0x6c996e },
  'minecraft:waxed_oxidized_cut_copper': { color: 0x52a385 },
  
  'minecraft:tuff': { color: 0x6c6d67 },
  'minecraft:tuff_bricks': { color: 0x62675f },
  'minecraft:chiseled_tuff': { color: 0x595e57 },
  'minecraft:chiseled_tuff_bricks': { color: 0x636760 },
  'minecraft:polished_tuff': { color: 0x6c6d67 },
  'minecraft:tuff_slab': { color: 0x6c6d67 },
  'minecraft:tuff_stairs': { color: 0x6c6d67 },
  'minecraft:tuff_wall': { color: 0x6c6d67 },
  'minecraft:tuff_brick_slab': { color: 0x62675f },
  'minecraft:tuff_brick_stairs': { color: 0x62675f },
  'minecraft:tuff_brick_wall': { color: 0x62675f },
  'minecraft:polished_tuff_slab': { color: 0x6c6d67 },
  'minecraft:polished_tuff_stairs': { color: 0x6c6d67 },
  'minecraft:polished_tuff_wall': { color: 0x6c6d67 },
  
  'minecraft:mud': { color: 0x3c393d },
  'minecraft:packed_mud': { color: 0x8e6b50 },
  'minecraft:mud_bricks': { color: 0x89684f },
  'minecraft:muddy_mangrove_roots': { color: 0x463b2d },
  'minecraft:mud_brick_slab': { color: 0x89684f },
  'minecraft:mud_brick_stairs': { color: 0x89684f },
  'minecraft:mud_brick_wall': { color: 0x89684f },
  
  'minecraft:cherry_log': { color: 0xb98d89 },
  'minecraft:cherry_wood': { color: 0xb98d89 },
  'minecraft:stripped_cherry_log': { color: 0xb98d89 },
  'minecraft:stripped_cherry_wood': { color: 0xb98d89 },
  'minecraft:cherry_planks': { color: 0xe3b3ad },
  'minecraft:cherry_sapling': { color: 0xe5adc2 },
  'minecraft:cherry_leaves': { color: 0xe5adc2, foliage: true },
  'minecraft:cherry_slab': { color: 0xe3b3ad },
  'minecraft:cherry_stairs': { color: 0xe3b3ad },
  'minecraft:cherry_fence': { color: 0xe3b3ad },
  'minecraft:cherry_fence_gate': { color: 0xe3b3ad },
  'minecraft:cherry_door': { color: 0xe3b3ad },
  'minecraft:cherry_trapdoor': { color: 0xe3b3ad },
  'minecraft:cherry_button': { color: 0xe3b3ad },
  'minecraft:cherry_pressure_plate': { color: 0xe3b3ad },
  'minecraft:cherry_sign': { color: 0xe3b3ad },
  'minecraft:cherry_wall_sign': { color: 0xe3b3ad },
  'minecraft:cherry_hanging_sign': { color: 0xe3b3ad },
  'minecraft:cherry_wall_hanging_sign': { color: 0xe3b3ad },
  'minecraft:potted_cherry_sapling': { color: 0x7c4535 },
  
  'minecraft:bamboo_block': { color: 0x7f903a },
  'minecraft:stripped_bamboo_block': { color: 0x7f903a },
  'minecraft:bamboo_planks': { color: 0xc1ad50 },
  'minecraft:bamboo_mosaic': { color: 0xbeaa4e },
  'minecraft:bamboo_slab': { color: 0xc1ad50 },
  'minecraft:bamboo_stairs': { color: 0xc1ad50 },
  'minecraft:bamboo_mosaic_slab': { color: 0xbeaa4e },
  'minecraft:bamboo_mosaic_stairs': { color: 0xbeaa4e },
  'minecraft:bamboo_fence': { color: 0xc1ad50 },
  'minecraft:bamboo_fence_gate': { color: 0xc1ad50 },
  'minecraft:bamboo_door': { color: 0xc1ad50 },
  'minecraft:bamboo_trapdoor': { color: 0xc1ad50 },
  'minecraft:bamboo_button': { color: 0xc1ad50 },
  'minecraft:bamboo_pressure_plate': { color: 0xc1ad50 },
  'minecraft:bamboo_sign': { color: 0xc1ad50 },
  'minecraft:bamboo_wall_sign': { color: 0xc1ad50 },
  'minecraft:bamboo_hanging_sign': { color: 0xc1ad50 },
  'minecraft:bamboo_wall_hanging_sign': { color: 0xc1ad50 },
  'minecraft:potted_bamboo': { color: 0x7c4535 },
  
  'minecraft:chiseled_bookshelf': { color: 0xb29159 },
  
  'minecraft:decorated_pot': { color: 0x8a7a6a },
  
  'minecraft:sniffer_egg': { color: 0x7a6b5e },
  
  'minecraft:pitcher_plant': { color: 0x5a7b6e },
  'minecraft:pitcher_crop': { color: 0x5a7b6e },
  'minecraft:potted_pitcher_plant': { color: 0x7c4535 },
  
  'minecraft:torchflower': { color: 0x65654d },
  'minecraft:torchflower_crop': { color: 0x65654d },
  'minecraft:potted_torchflower': { color: 0x7c4535 },
  
  'minecraft:pink_petals': { color: 0xf7b5db },
  
  'minecraft:calibrated_sculk_sensor': { color: 0x1c4f65 },
  
  'minecraft:vault': { color: 0x37464f },
  
  'minecraft:crafter': { color: 0x706364 },
  
  'minecraft:heavy_core': { color: 0x2a2a2a },
  
  'minecraft:trial_spawner': { color: 0x4a4a4a },
  'minecraft:trial_spawner#ominous=true': { color: 0x4a4a4a },
  
  'minecraft:vault': { color: 0x37464f },
  'minecraft:vault#ominous=true': { color: 0x37464f },
  
  'minecraft:breeze_rod': { color: 0x8ab4b4 },
  'minecraft:wind_charge': { color: 0x8ab4b4 },
  
  'minecraft:polished_tuff': { color: 0x6c6d67 },
  'minecraft:chiseled_tuff': { color: 0x595e57 },
  'minecraft:tuff_bricks': { color: 0x62675f },
  'minecraft:chiseled_tuff_bricks': { color: 0x636760 },
  
  'minecraft:polished_tuff_slab': { color: 0x6c6d67 },
  'minecraft:polished_tuff_stairs': { color: 0x6c6d67 },
  'minecraft:polished_tuff_wall': { color: 0x6c6d67 },
  'minecraft:tuff_slab': { color: 0x6c6d67 },
  'minecraft:tuff_stairs': { color: 0x6c6d67 },
  'minecraft:tuff_wall': { color: 0x6c6d67 },
  'minecraft:tuff_brick_slab': { color: 0x62675f },
  'minecraft:tuff_brick_stairs': { color: 0x62675f },
  'minecraft:tuff_brick_wall': { color: 0x62675f },
  
  'minecraft:chiseled_copper': { color: 0xb8654a },
  'minecraft:exposed_chiseled_copper': { color: 0xa17e68 },
  'minecraft:weathered_chiseled_copper': { color: 0x6c996e },
  'minecraft:oxidized_chiseled_copper': { color: 0x52a385 },
  'minecraft:waxed_chiseled_copper': { color: 0xb8654a },
  'minecraft:waxed_exposed_chiseled_copper': { color: 0xa17e68 },
  'minecraft:waxed_weathered_chiseled_copper': { color: 0x6c996e },
  'minecraft:waxed_oxidized_chiseled_copper': { color: 0x52a385 },
  
  'minecraft:copper_grate': { color: 0xc06c4f },
  'minecraft:exposed_copper_grate': { color: 0xa27e69 },
  'minecraft:weathered_copper_grate': { color: 0x6c996e },
  'minecraft:oxidized_copper_grate': { color: 0x52a385 },
  'minecraft:waxed_copper_grate': { color: 0xc06c4f },
  'minecraft:waxed_exposed_copper_grate': { color: 0xa27e69 },
  'minecraft:waxed_weathered_copper_grate': { color: 0x6c996e },
  'minecraft:waxed_oxidized_copper_grate': { color: 0x52a385 },
  
  'minecraft:copper_bulb': { color: 0x9c5739 },
  'minecraft:exposed_copper_bulb': { color: 0x876c5a },
  'minecraft:weathered_copper_bulb': { color: 0x6c996e },
  'minecraft:oxidized_copper_bulb': { color: 0x52a385 },
  'minecraft:waxed_copper_bulb': { color: 0x9c5739 },
  'minecraft:waxed_exposed_copper_bulb': { color: 0x876c5a },
  'minecraft:waxed_weathered_copper_bulb': { color: 0x6c996e },
  'minecraft:waxed_oxidized_copper_bulb': { color: 0x52a385 },
  'minecraft:copper_bulb#lit=true': { color: 0xffa500 },
  'minecraft:exposed_copper_bulb#lit=true': { color: 0xffa500 },
  'minecraft:weathered_copper_bulb#lit=true': { color: 0xffa500 },
  'minecraft:oxidized_copper_bulb#lit=true': { color: 0xffa500 },
  
  'minecraft:copper_door': { color: 0xc06c50 },
  'minecraft:exposed_copper_door': { color: 0xa17e68 },
  'minecraft:weathered_copper_door': { color: 0x6c996e },
  'minecraft:oxidized_copper_door': { color: 0x52a385 },
  'minecraft:waxed_copper_door': { color: 0xc06c50 },
  'minecraft:waxed_exposed_copper_door': { color: 0xa17e68 },
  'minecraft:waxed_weathered_copper_door': { color: 0x6c996e },
  'minecraft:waxed_oxidized_copper_door': { color: 0x52a385 },
  
  'minecraft:copper_trapdoor': { color: 0xc06c50 },
  'minecraft:exposed_copper_trapdoor': { color: 0xa17e68 },
  'minecraft:weathered_copper_trapdoor': { color: 0x6c996e },
  'minecraft:oxidized_copper_trapdoor': { color: 0x52a385 },
  'minecraft:waxed_copper_trapdoor': { color: 0xc06c50 },
  'minecraft:waxed_exposed_copper_trapdoor': { color: 0xa17e68 },
  'minecraft:waxed_weathered_copper_trapdoor': { color: 0x6c996e },
  'minecraft:waxed_oxidized_copper_trapdoor': { color: 0x52a385 },
  
  'minecraft:polished_tuff': { color: 0x6c6d67 },
  
  'minecraft:stone': { color: 0x7e7e7e },
  'minecraft:granite': { color: 0x956756 },
  'minecraft:polished_granite': { color: 0x956756 },
  'minecraft:diorite': { color: 0xbdbcbd },
  'minecraft:polished_diorite': { color: 0xbdbcbd },
  'minecraft:andesite': { color: 0x888889 },
  'minecraft:polished_andesite': { color: 0x888889 },
  
  'minecraft:stone_slab': { color: 0x7e7e7e },
  'minecraft:smooth_stone_slab': { color: 0x7e7e7e },
  'minecraft:sandstone_slab': { color: 0xe0d6aa },
  'minecraft:cut_sandstone_slab': { color: 0xe0d6aa },
  'minecraft:red_sandstone_slab': { color: 0xb5621f },
  'minecraft:cut_red_sandstone_slab': { color: 0xb5621f },
  'minecraft:cobblestone_slab': { color: 0x807f80 },
  'minecraft:brick_slab': { color: 0x976253 },
  'minecraft:stone_brick_slab': { color: 0x7a7a7a },
  'minecraft:nether_brick_slab': { color: 0x2c161a },
  'minecraft:quartz_slab': { color: 0xece6df },
  'minecraft:smooth_quartz_slab': { color: 0xece6df },
  'minecraft:purpur_slab': { color: 0xaa7eaa },
  'minecraft:prismarine_slab': { color: 0x639c97 },
  'minecraft:dark_prismarine_slab': { color: 0x345c4c },
  'minecraft:prismarine_brick_slab': { color: 0x63ac9e },
  'minecraft:mossy_cobblestone_slab': { color: 0x6e775f },
  'minecraft:mossy_stone_brick_slab': { color: 0x737969 },
  'minecraft:smooth_sandstone_slab': { color: 0xe0d6aa },
  'minecraft:smooth_red_sandstone_slab': { color: 0xb5621f },
  'minecraft:red_nether_brick_slab': { color: 0x4a2c2c },
  'minecraft:end_stone_brick_slab': { color: 0xdcdf9e },
  
  'minecraft:stone_stairs': { color: 0x7e7e7e },
  'minecraft:cobblestone_stairs': { color: 0x807f80 },
  'minecraft:sandstone_stairs': { color: 0xe0d6aa },
  'minecraft:red_sandstone_stairs': { color: 0xb5621f },
  'minecraft:brick_stairs': { color: 0x976253 },
  'minecraft:stone_brick_stairs': { color: 0x7a7a7a },
  'minecraft:nether_brick_stairs': { color: 0x2c161a },
  'minecraft:quartz_stairs': { color: 0xece6df },
  'minecraft:smooth_quartz_stairs': { color: 0xece6df },
  'minecraft:purpur_stairs': { color: 0xaa7eaa },
  'minecraft:prismarine_stairs': { color: 0x639c97 },
  'minecraft:dark_prismarine_stairs': { color: 0x345c4c },
  'minecraft:prismarine_brick_stairs': { color: 0x63ac9e },
  'minecraft:mossy_cobblestone_stairs': { color: 0x6e775f },
  'minecraft:mossy_stone_brick_stairs': { color: 0x737969 },
  'minecraft:smooth_sandstone_stairs': { color: 0xe0d6aa },
  'minecraft:smooth_red_sandstone_stairs': { color: 0xb5621f },
  'minecraft:red_nether_brick_stairs': { color: 0x4a2c2c },
  'minecraft:end_stone_brick_stairs': { color: 0xdcdf9e },
  
  'minecraft:stone_wall': { color: 0x7e7e7e },
  'minecraft:cobblestone_wall': { color: 0x807f80 },
  'minecraft:mossy_cobblestone_wall': { color: 0x6e775f },
  'minecraft:brick_wall': { color: 0x976253 },
  'minecraft:stone_brick_wall': { color: 0x7a7a7a },
  'minecraft:mossy_stone_brick_wall': { color: 0x737969 },
  'minecraft:nether_brick_wall': { color: 0x2c161a },
  'minecraft:red_nether_brick_wall': { color: 0x4a2c2c },
  'minecraft:end_stone_brick_wall': { color: 0xdcdf9e },
  'minecraft:prismarine_wall': { color: 0x639c97 },
  'minecraft:sandstone_wall': { color: 0xe0d6aa },
  'minecraft:red_sandstone_wall': { color: 0xb5621f },
  'minecraft:granite_wall': { color: 0x956756 },
  'minecraft:andesite_wall': { color: 0x888889 },
  'minecraft:diorite_wall': { color: 0xbdbcbd },
  'minecraft:blackstone_wall': { color: 0x2a242a },
  'minecraft:polished_blackstone_wall': { color: 0x2a242a },
  'minecraft:polished_blackstone_brick_wall': { color: 0x2a242a },
  'minecraft:cobbled_deepslate_wall': { color: 0x4d4d51 },
  'minecraft:polished_deepslate_wall': { color: 0x484949 },
  'minecraft:deepslate_tile_wall': { color: 0x373737 },
  'minecraft:deepslate_brick_wall': { color: 0x474747 },
  
  'minecraft:smooth_stone': { color: 0x7e7e7e },
  'minecraft:smooth_sandstone': { color: 0xe0d6aa },
  'minecraft:smooth_red_sandstone': { color: 0xb5621f },
  'minecraft:smooth_quartz': { color: 0xece6df },
  
  'minecraft:chiseled_stone_bricks': { color: 0x787778 },
  'minecraft:cracked_stone_bricks': { color: 0x767676 },
  'minecraft:chiseled_sandstone': { color: 0xe0d6aa },
  'minecraft:chiseled_red_sandstone': { color: 0xb5621f },
  'minecraft:chiseled_quartz_block': { color: 0xe8e3d9 },
  'minecraft:quartz_pillar': { color: 0xebe6df },
  'minecraft:quartz_bricks': { color: 0xece6df },
  
  'minecraft:stone_bricks': { color: 0x7a7a7a },
  'minecraft:mossy_stone_bricks': { color: 0x737969 },
  'minecraft:sandstone': { color: 0xe0d6aa },
  'minecraft:red_sandstone': { color: 0xb5621f },
  'minecraft:quartz_block': { color: 0xece6df },
  'minecraft:bricks': { color: 0x976253 },
  'minecraft:nether_bricks': { color: 0x2c161a },
  'minecraft:red_nether_bricks': { color: 0x4a2c2c },
  'minecraft:end_stone_bricks': { color: 0xdcdf9e },
  'minecraft:prismarine_bricks': { color: 0x63ac9e },
  'minecraft:dark_prismarine': { color: 0x345c4c },
  
  'minecraft:polished_blackstone': { color: 0x2a242a },
  'minecraft:polished_blackstone_bricks': { color: 0x2a242a },
  'minecraft:cracked_polished_blackstone_bricks': { color: 0x2a242a },
  'minecraft:chiseled_polished_blackstone': { color: 0x2a242a },
  'minecraft:gilded_blackstone': { color: 0x382b26 },
  
  'minecraft:cobbled_deepslate': { color: 0x4d4d51 },
  'minecraft:polished_deepslate': { color: 0x484949 },
  'minecraft:deepslate_tiles': { color: 0x373737 },
  'minecraft:deepslate_bricks': { color: 0x474747 },
  'minecraft:chiseled_deepslate': { color: 0x363637 },
  'minecraft:cracked_deepslate_tiles': { color: 0x373737 },
  'minecraft:cracked_deepslate_bricks': { color: 0x474747 },
  
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
  'minecraft:packed_ice': { color: 0x8eb4fa },
  'minecraft:blue_ice': { color: 0x74a8fd },
  'minecraft:frosted_ice': { color: 0x91b7fd },
  
  'minecraft:glass': { color: 0xb0d6db },
  'minecraft:glass_pane': { color: 0xaad2d9 },
  'minecraft:tinted_glass': { color: 0x2d2d2d },
  'minecraft:white_stained_glass': { color: 0xffffff },
  'minecraft:orange_stained_glass': { color: 0xd77f32 },
  'minecraft:magenta_stained_glass': { color: 0xb14bd7 },
  'minecraft:light_blue_stained_glass': { color: 0x6698d7 },
  'minecraft:yellow_stained_glass': { color: 0xe5e532 },
  'minecraft:lime_stained_glass': { color: 0x7fcd19 },
  'minecraft:pink_stained_glass': { color: 0xf27fa5 },
  'minecraft:gray_stained_glass': { color: 0x4b4b4b },
  'minecraft:light_gray_stained_glass': { color: 0x989898 },
  'minecraft:cyan_stained_glass': { color: 0x4b7f98 },
  'minecraft:purple_stained_glass': { color: 0x7f3eb1 },
  'minecraft:blue_stained_glass': { color: 0x324bb1 },
  'minecraft:brown_stained_glass': { color: 0x664b32 },
  'minecraft:green_stained_glass': { color: 0x667f32 },
  'minecraft:red_stained_glass': { color: 0x983232 },
  'minecraft:black_stained_glass': { color: 0x191919 },
  
  'minecraft:white_stained_glass_pane': { color: 0xffffff },
  'minecraft:orange_stained_glass_pane': { color: 0xd77f32 },
  'minecraft:magenta_stained_glass_pane': { color: 0xb14bd7 },
  'minecraft:light_blue_stained_glass_pane': { color: 0x6698d7 },
  'minecraft:yellow_stained_glass_pane': { color: 0xe5e532 },
  'minecraft:lime_stained_glass_pane': { color: 0x7fcd19 },
  'minecraft:pink_stained_glass_pane': { color: 0xf27fa5 },
  'minecraft:gray_stained_glass_pane': { color: 0x4b4b4b },
  'minecraft:light_gray_stained_glass_pane': { color: 0x989898 },
  'minecraft:cyan_stained_glass_pane': { color: 0x4b7f98 },
  'minecraft:purple_stained_glass_pane': { color: 0x7f3eb1 },
  'minecraft:blue_stained_glass_pane': { color: 0x324bb1 },
  'minecraft:brown_stained_glass_pane': { color: 0x664b32 },
  'minecraft:green_stained_glass_pane': { color: 0x667f32 },
  'minecraft:red_stained_glass_pane': { color: 0x983232 },
  'minecraft:black_stained_glass_pane': { color: 0x191919 },
  
  'minecraft:oak_slab': { color: 0xa2834f },
  'minecraft:spruce_slab': { color: 0x735531 },
  'minecraft:birch_slab': { color: 0xc0af79 },
  'minecraft:jungle_slab': { color: 0xa07351 },
  'minecraft:acacia_slab': { color: 0xa85a32 },
  'minecraft:dark_oak_slab': { color: 0x432b14 },
  'minecraft:mangrove_slab': { color: 0x763631 },
  'minecraft:cherry_slab': { color: 0xe3b3ad },
  'minecraft:bamboo_slab': { color: 0xc1ad50 },
  'minecraft:crimson_slab': { color: 0x653147 },
  'minecraft:warped_slab': { color: 0x2b6963 },
  
  'minecraft:stone_slab': { color: 0x7e7e7e },
  'minecraft:smooth_stone_slab': { color: 0x7e7e7e },
  'minecraft:cobblestone_slab': { color: 0x807f80 },
  'minecraft:mossy_cobblestone_slab': { color: 0x6e775f },
  'minecraft:stone_bricks_slab': { color: 0x7a7a7a },
  'minecraft:mossy_stone_bricks_slab': { color: 0x737969 },
  'minecraft:andesite_slab': { color: 0x888889 },
  'minecraft:diorite_slab': { color: 0xbdbcbd },
  'minecraft:granite_slab': { color: 0x956756 },
  'minecraft:polished_andesite_slab': { color: 0x888889 },
  'minecraft:polished_diorite_slab': { color: 0xbdbcbd },
  'minecraft:polished_granite_slab': { color: 0x956756 },
  
  'minecraft:sandstone_slab': { color: 0xe0d6aa },
  'minecraft:red_sandstone_slab': { color: 0xb5621f },
  'minecraft:cut_sandstone_slab': { color: 0xe0d6aa },
  'minecraft:cut_red_sandstone_slab': { color: 0xb5621f },
  
  'minecraft:brick_slab': { color: 0x976253 },
  'minecraft:stone_brick_slab': { color: 0x7a7a7a },
  'minecraft:nether_brick_slab': { color: 0x2c161a },
  'minecraft:red_nether_brick_slab': { color: 0x4a2c2c },
  'minecraft:quartz_slab': { color: 0xece6df },
  'minecraft:smooth_quartz_slab': { color: 0xece6df },
  'minecraft:purpur_slab': { color: 0xaa7eaa },
  'minecraft:prismarine_slab': { color: 0x639c97 },
  'minecraft:prismarine_brick_slab': { color: 0x63ac9e },
  'minecraft:dark_prismarine_slab': { color: 0x345c4c },
  
  'minecraft:deepslate_tile_slab': { color: 0x373737 },
  'minecraft:deepslate_brick_slab': { color: 0x474747 },
  'minecraft:polished_deepslate_slab': { color: 0x484949 },
  'minecraft:cobbled_deepslate_slab': { color: 0x4d4d51 },
  'minecraft:tuff_slab': { color: 0x6c6d67 },
  'minecraft:tuff_brick_slab': { color: 0x62675f },
  
  'minecraft:mud_brick_slab': { color: 0x89684f },
  
  'minecraft:cut_copper_slab': { color: 0xbf6b51 },
  'minecraft:exposed_cut_copper_slab': { color: 0x9b7a65 },
  'minecraft:weathered_cut_copper_slab': { color: 0x6c996e },
  'minecraft:oxidized_cut_copper_slab': { color: 0x52a385 },
  'minecraft:waxed_cut_copper_slab': { color: 0xbf6b51 },
  'minecraft:waxed_exposed_cut_copper_slab': { color: 0x9b7a65 },
  'minecraft:waxed_weathered_cut_copper_slab': { color: 0x6c996e },
  'minecraft:waxed_oxidized_cut_copper_slab': { color: 0x52a385 },
  
  'minecraft:oak_stairs': { color: 0xa2834f },
  'minecraft:spruce_stairs': { color: 0x735531 },
  'minecraft:birch_stairs': { color: 0xc0af79 },
  'minecraft:jungle_stairs': { color: 0xa07351 },
  'minecraft:acacia_stairs': { color: 0xa85a32 },
  'minecraft:dark_oak_stairs': { color: 0x432b14 },
  'minecraft:mangrove_stairs': { color: 0x763631 },
  'minecraft:cherry_stairs': { color: 0xe3b3ad },
  'minecraft:bamboo_stairs': { color: 0xc1ad50 },
  'minecraft:bamboo_mosaic_stairs': { color: 0xbeaa4e },
  'minecraft:crimson_stairs': { color: 0x653147 },
  'minecraft:warped_stairs': { color: 0x2b6963 },
  
  'minecraft:stone_stairs': { color: 0x7e7e7e },
  'minecraft:cobblestone_stairs': { color: 0x807f80 },
  'minecraft:mossy_cobblestone_stairs': { color: 0x6e775f },
  'minecraft:stone_brick_stairs': { color: 0x7a7a7a },
  'minecraft:mossy_stone_brick_stairs': { color: 0x737969 },
  'minecraft:andesite_stairs': { color: 0x888889 },
  'minecraft:diorite_stairs': { color: 0xbdbcbd },
  'minecraft:granite_stairs': { color: 0x956756 },
  'minecraft:polished_andesite_stairs': { color: 0x888889 },
  'minecraft:polished_diorite_stairs': { color: 0xbdbcbd },
  'minecraft:polished_granite_stairs': { color: 0x956756 },
  
  'minecraft:sandstone_stairs': { color: 0xe0d6aa },
  'minecraft:red_sandstone_stairs': { color: 0xb5621f },
  'minecraft:smooth_sandstone_stairs': { color: 0xe0d6aa },
  'minecraft:smooth_red_sandstone_stairs': { color: 0xb5621f },
  
  'minecraft:brick_stairs': { color: 0x976253 },
  'minecraft:nether_brick_stairs': { color: 0x2c161a },
  'minecraft:red_nether_brick_stairs': { color: 0x4a2c2c },
  'minecraft:quartz_stairs': { color: 0xece6df },
  'minecraft:smooth_quartz_stairs': { color: 0xece6df },
  'minecraft:purpur_stairs': { color: 0xaa7eaa },
  'minecraft:prismarine_stairs': { color: 0x639c97 },
  'minecraft:prismarine_brick_stairs': { color: 0x63ac9e },
  'minecraft:dark_prismarine_stairs': { color: 0x345c4c },
  
  'minecraft:deepslate_tile_stairs': { color: 0x373737 },
  'minecraft:deepslate_brick_stairs': { color: 0x474747 },
  'minecraft:polished_deepslate_stairs': { color: 0x484949 },
  'minecraft:cobbled_deepslate_stairs': { color: 0x4d4d51 },
  'minecraft:tuff_stairs': { color: 0x6c6d67 },
  'minecraft:tuff_brick_stairs': { color: 0x62675f },
  
  'minecraft:mud_brick_stairs': { color: 0x89684f },
  
  'minecraft:cut_copper_stairs': { color: 0xbf6b51 },
  'minecraft:exposed_cut_copper_stairs': { color: 0x9b7a65 },
  'minecraft:weathered_cut_copper_stairs': { color: 0x6c996e },
  'minecraft:oxidized_cut_copper_stairs': { color: 0x52a385 },
  'minecraft:waxed_cut_copper_stairs': { color: 0xbf6b51 },
  'minecraft:waxed_exposed_cut_copper_stairs': { color: 0x9b7a65 },
  'minecraft:waxed_weathered_cut_copper_stairs': { color: 0x6c996e },
  'minecraft:waxed_oxidized_cut_copper_stairs': { color: 0x52a385 },
  
  'minecraft:snow': { color: 0xf9fefe },
  'minecraft:snow_block': { color: 0xf9fefe },
  
  'minecraft:iron_bars': { color: 0x898b88 },
  'minecraft:iron_door': { color: 0xc2c1c1 },
  'minecraft:iron_trapdoor': { color: 0xcbcaca },
  
  'minecraft:oak_door': { color: 0x8d6f42 },
  'minecraft:spruce_door': { color: 0x6d502f },
  'minecraft:birch_door': { color: 0xc0af79 },
  'minecraft:jungle_door': { color: 0xa07351 },
  'minecraft:acacia_door': { color: 0xa85a32 },
  'minecraft:dark_oak_door': { color: 0x432b14 },
  'minecraft:mangrove_door': { color: 0x763631 },
  'minecraft:cherry_door': { color: 0xe3b3ad },
  'minecraft:bamboo_door': { color: 0xc1ad50 },
  'minecraft:crimson_door': { color: 0x653147 },
  'minecraft:warped_door': { color: 0x2b6963 },
  
  'minecraft:oak_trapdoor': { color: 0x7d6339 },
  'minecraft:spruce_trapdoor': { color: 0x6d502f },
  'minecraft:birch_trapdoor': { color: 0xc0af79 },
  'minecraft:jungle_trapdoor': { color: 0xa07351 },
  'minecraft:acacia_trapdoor': { color: 0xa85a32 },
  'minecraft:dark_oak_trapdoor': { color: 0x432b14 },
  'minecraft:mangrove_trapdoor': { color: 0x763631 },
  'minecraft:cherry_trapdoor': { color: 0xe3b3ad },
  'minecraft:bamboo_trapdoor': { color: 0xc1ad50 },
  'minecraft:crimson_trapdoor': { color: 0x653147 },
  'minecraft:warped_trapdoor': { color: 0x2b6963 },
  
  'minecraft:oak_fence': { color: 0xa2834f },
  'minecraft:spruce_fence': { color: 0x735531 },
  'minecraft:birch_fence': { color: 0xc0af79 },
  'minecraft:jungle_fence': { color: 0xa07351 },
  'minecraft:acacia_fence': { color: 0xa85a32 },
  'minecraft:dark_oak_fence': { color: 0x432b14 },
  'minecraft:mangrove_fence': { color: 0x763631 },
  'minecraft:cherry_fence': { color: 0xe3b3ad },
  'minecraft:bamboo_fence': { color: 0xc1ad50 },
  'minecraft:crimson_fence': { color: 0x653147 },
  'minecraft:warped_fence': { color: 0x2b6963 },
  
  'minecraft:oak_fence_gate': { color: 0xa2834f },
  'minecraft:spruce_fence_gate': { color: 0x735531 },
  'minecraft:birch_fence_gate': { color: 0xc0af79 },
  'minecraft:jungle_fence_gate': { color: 0xa07351 },
  'minecraft:acacia_fence_gate': { color: 0xa85a32 },
  'minecraft:dark_oak_fence_gate': { color: 0x432b14 },
  'minecraft:mangrove_fence_gate': { color: 0x763631 },
  'minecraft:cherry_fence_gate': { color: 0xe3b3ad },
  'minecraft:bamboo_fence_gate': { color: 0xc1ad50 },
  'minecraft:crimson_fence_gate': { color: 0x653147 },
  'minecraft:warped_fence_gate': { color: 0x2b6963 },
  
  'minecraft:oak_pressure_plate': { color: 0xa2834f },
  'minecraft:spruce_pressure_plate': { color: 0x735531 },
  'minecraft:birch_pressure_plate': { color: 0xc0af79 },
  'minecraft:jungle_pressure_plate': { color: 0xa07351 },
  'minecraft:acacia_pressure_plate': { color: 0xa85a32 },
  'minecraft:dark_oak_pressure_plate': { color: 0x432b14 },
  'minecraft:mangrove_pressure_plate': { color: 0x763631 },
  'minecraft:cherry_pressure_plate': { color: 0xe3b3ad },
  'minecraft:bamboo_pressure_plate': { color: 0xc1ad50 },
  'minecraft:crimson_pressure_plate': { color: 0x653147 },
  'minecraft:warped_pressure_plate': { color: 0x2b6963 },
  'minecraft:stone_pressure_plate': { color: 0x7e7e7e },
  'minecraft:polished_blackstone_pressure_plate': { color: 0x2a242a },
  'minecraft:heavy_weighted_pressure_plate': { color: 0xdcdcdc },
  'minecraft:light_weighted_pressure_plate': { color: 0xf6d03e },
  
  'minecraft:oak_button': { color: 0xa2834f },
  'minecraft:spruce_button': { color: 0x735531 },
  'minecraft:birch_button': { color: 0xc0af79 },
  'minecraft:jungle_button': { color: 0xa07351 },
  'minecraft:acacia_button': { color: 0xa85a32 },
  'minecraft:dark_oak_button': { color: 0x432b14 },
  'minecraft:mangrove_button': { color: 0x763631 },
  'minecraft:cherry_button': { color: 0xe3b3ad },
  'minecraft:bamboo_button': { color: 0xc1ad50 },
  'minecraft:crimson_button': { color: 0x653147 },
  'minecraft:warped_button': { color: 0x2b6963 },
  'minecraft:stone_button': { color: 0x7e7e7e },
  'minecraft:polished_blackstone_button': { color: 0x2a242a },
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

function extractHeightsFromRegionLocal(regionData) {
  return extractHeightsFromRegion(regionData, 0);
}

function renderRegionToPixelsLocal(regionData, northRegionHeights = null, westRegionHeights = null, northwestRegionHeights = null) {
  return renderRegionToPixels(regionData, BLOCK_COLORS, BIOME_COLORS, 0, northRegionHeights, westRegionHeights, northwestRegionHeights);
}

function renderCaveLayersToPixelsLocal(layers) {
  return renderCaveLayersToPixels(layers, BLOCK_COLORS, BIOME_COLORS, 0);
}

app.get('/api/config', (req, res) => {
  res.json({
    numWorkers: NUM_WORKERS,
    maxConcurrentLoads: MAX_CONCURRENT_LOADS,
    maxBatchRegions: MAX_BATCH_REGIONS
  });
});

app.get('/api/cache-size', async (req, res) => {
  try {
    if (dbWorker) {
      const stats = await dbWorkerCall('getStats', {});
      const count = stats?.count || 0;
      const totalSize = stats?.totalSize || 0;
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
    
    if (dbWorker) {
      await dbWorkerCall('clearAll', {});
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
    const worldMapPath = path.join(mapDirectory, 'world-map');
    if (!existsSync(worldMapPath)) {
      return res.json([]);
    }
    const worlds = await listWorlds(worldMapPath);
    res.json(worlds);
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/worlds/:worldName/dimensions', async (req, res) => {
  const { worldName } = req.params;
  try {
    initDatabaseForWorld(worldName);
    const worldMapPath = path.join(mapDirectory, 'world-map', worldName);
    const dimensions = await listDimensions(worldMapPath);
    res.json(dimensions);
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/worlds/:worldName/dimensions/:dimName/map-types', async (req, res) => {
  const { worldName, dimName } = req.params;
  try {
    initDatabaseForWorld(worldName);
    const dimPath = path.join(mapDirectory, 'world-map', worldName, dimName);
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
    initDatabaseForWorld(worldName);
    let dimPath = path.join(mapDirectory, 'world-map', worldName, dimName);
    //console.log('Loading regions from:', dimPath, 'mapDirectory:', mapDirectory);
    if (mapType) {
      dimPath = path.join(dimPath, mapType);
    } else {
      const mapTypes = await listMapTypes(dimPath);
      if (mapTypes.length > 0) dimPath = path.join(dimPath, mapTypes[0].path);
    }
    
    const caveModeValue = caveMode !== undefined ? parseInt(String(caveMode)) : null;
    const caveStartValue = caveStart !== undefined ? parseInt(String(caveStart)) : null;
    const regions = await listRegions(dimPath, caveModeValue, caveStartValue);
    //console.log('Found', regions.length, 'regions in', dimPath);
    res.json({ regions, mapType: mapType || null });
  } catch (error) {
    console.error('Error loading regions:', error);
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/region-pixels', async (req, res) => {
  const { world, dim, x, z, mapType, caveMode, caveStart, lod } = req.query;
  
  if (!dim || x === undefined || z === undefined) {
    return res.status(400).json({ error: '缺少参数' });
  }
  
  try {
    initDatabaseForWorld(String(world));
    let dimPath = path.join(mapDirectory, 'world-map', String(world), String(dim));
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
  
  initDatabaseForWorld(String(world));
  
  const requestId = clientRequestId !== undefined ? parseInt(String(clientRequestId)) : requestIdCounter++;
  let isCancelled = false;
  
  const cancelHandler = () => {
    isCancelled = true;
    cancelRequest(requestId);
  };
  req.on('close', cancelHandler);
  
  try {
    let dimPath = path.join(mapDirectory, 'world-map', String(world), String(dim));
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
    
    const cacheRequests = filteredPairs.map(coord => ({
      dimPath,
      regionX: coord.x,
      regionZ: coord.z,
      yHeight: caveModeValue !== null ? `cave_${caveModeValue}_${caveStartValue || 'auto'}` : null,
      lod: lodValue
    }));
    
    const cachedResults = await batchReadPixelCache(cacheRequests);
    
    const results = await Promise.all(
      filteredPairs.map(coord => {
        const key = getCacheKey(dimPath, coord.x, coord.z, 
          caveModeValue !== null ? `cave_${caveModeValue}_${caveStartValue || 'auto'}` : null, 
          lodValue);
        const cached = cachedResults.get(key);
        
        if (cached) {
          return cached;
        }
        
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
    
    const buffer = getPooledBuffer(totalSize);
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
    
    req.off('close', cancelHandler);
    res.setHeader('Content-Type', 'application/octet-stream');
    res.setHeader('Content-Length', buffer.length);
    res.on('finish', () => releaseBuffer(buffer));
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

const WAYPOINT_COLORS = [
  0xFF0000, 0xFF8800, 0xFFFF00, 0x88FF00,
  0x00FF00, 0x00FF88, 0x00FFFF, 0x0088FF,
  0x0000FF, 0x8800FF, 0xFF00FF, 0xFF0088,
  0xFFFFFF, 0x888888, 0x444444, 0x000000
];

function parseWaypointLine(line, defaultSetName = 'default', dimension = '') {
  const parts = line.split(':');
  
  if (parts[0] === 'waypoint' && parts.length >= 9) {
    const name = parts[1].replace(/§§/g, ':') || 'Unnamed';
    const initials = parts[2].replace(/§§/g, ':') || '!';
    const x = parseInt(parts[3], 10) || 0;
    const yIncluded = parts[4] !== '~';
    const y = yIncluded ? (parseInt(parts[4], 10) || 64) : 64;
    const z = parseInt(parts[5], 10) || 0;
    const colorIndex = parseInt(parts[6], 10) || 0;
    const disabled = parts[7] === 'true';
    const type = parseInt(parts[8], 10) || 0;
    const setName = parts[9] || defaultSetName;
    const rotation = parts[10] === 'true';
    const yaw = parseInt(parts[11], 10) || 0;
    const global = parts[12] === 'true' || parts[12] === 'GLOBAL';
    
    const color = WAYPOINT_COLORS[colorIndex] || WAYPOINT_COLORS[0];
    
    return {
      id: `${x}_${y}_${z}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      name,
      x,
      y,
      z,
      color,
      symbol: initials.substring(0, 2),
      type,
      disabled,
      rotation,
      yaw,
      temporary: false,
      global,
      setName,
      yIncluded,
      dimension
    };
  }
  
  if (parts.length >= 6 && parts[0] !== 'sets' && parts[0] !== '#' && parts[0] !== 'slime_chunk_seed') {
    const name = parts[0].replace(/§§/g, ':') || 'Unnamed';
    const x = parseInt(parts[1], 10) || 0;
    const y = parseInt(parts[2], 10) || 64;
    const z = parseInt(parts[3], 10) || 0;
    const colorIndex = parseInt(parts[4], 10) || 0;
    const symbol = parts[5] || '!';
    const type = parseInt(parts[6], 10) || 0;
    const disabled = parts[7] === '1';
    const rotation = parts[8] === '1';
    const yaw = parseInt(parts[9], 10) || 0;
    const yIncluded = parts.length > 10 ? parts[10] !== '0' : true;
    
    const color = WAYPOINT_COLORS[colorIndex] || WAYPOINT_COLORS[0];
    
    return {
      id: `${x}_${y}_${z}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      name,
      x,
      y,
      z,
      color,
      symbol: symbol.substring(0, 2),
      type,
      disabled,
      rotation,
      yaw,
      temporary: false,
      global: false,
      setName: defaultSetName,
      yIncluded,
      dimension
    };
  }
  
  return null;
}

async function findXaeroMinimapWaypoints(xaeroPath) {
  const servers = [];
  
  const minimapPath = path.join(xaeroPath, 'minimap');
  console.log('Looking for waypoints in:', minimapPath);
  if (!existsSync(minimapPath)) {
    console.log('Minimap path does not exist');
    return servers;
  }
  
  try {
    const serverEntries = await readdir(minimapPath, { withFileTypes: true });
    console.log('Found server entries:', serverEntries.map(e => e.name));
    for (const serverEntry of serverEntries) {
      if (serverEntry.isDirectory()) {
        const serverPath = path.join(minimapPath, serverEntry.name);
        const dimensions = {};
        
        const dimEntries = await readdir(serverPath, { withFileTypes: true });
        for (const dimEntry of dimEntries) {
          if (dimEntry.isDirectory() && dimEntry.name.startsWith('dim%')) {
            const dimPath = path.join(serverPath, dimEntry.name);
            const dimNum = dimEntry.name.replace('dim%', '');
            
            let dimName;
            if (dimNum === '0') dimName = 'null';
            else if (dimNum === '-1') dimName = 'DIM-1';
            else if (dimNum === '1') dimName = 'DIM1';
            else dimName = dimEntry.name;
            
            const files = await readdir(dimPath);
            const waypointFiles = files.filter(f => f.startsWith('mw$') && f.endsWith('.txt'));
            
            if (waypointFiles.length > 0) {
              dimensions[dimName] = {
                path: dimPath,
                files: waypointFiles
              };
            }
          }
        }
        
        if (Object.keys(dimensions).length > 0) {
          servers.push({
            name: serverEntry.name,
            path: serverPath,
            dimensions
          });
        }
      }
    }
  } catch (e) {
    console.log('Error finding Xaero minimap waypoints:', e.message);
  }
  
  return servers;
}

async function loadWaypointsFromXaeroFile(filePath, setName = 'default', dimension = '') {
  try {
    //console.log('Loading waypoints from:', filePath);
    const content = readFileSync(filePath, 'utf-8');
    const lines = content.split('\n').filter(line => line.trim());
    //console.log('Found', lines.length, 'lines in file');
    const waypoints = [];
    
    for (const line of lines) {
      const waypoint = parseWaypointLine(line.trim(), setName, dimension);
      if (waypoint) {
        waypoints.push(waypoint);
      }
    }
    
    //console.log('Parsed', waypoints.length, 'waypoints from', filePath);
    return waypoints;
  } catch (e) {
    console.log('Error loading waypoints from Xaero file:', e.message);
    return [];
  }
}

app.get('/api/waypoints', async (req, res) => {
  if (!mapDirectory) {
    return res.status(400).json({ error: '请先设置地图目录' });
  }
  
  try {
    const xaeroServers = await findXaeroMinimapWaypoints(mapDirectory);
    res.json({ xaeroServers });
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/waypoints/servers', async (req, res) => {
  if (!mapDirectory) {
    return res.status(400).json({ error: '请先设置地图目录' });
  }
  
  try {
    const xaeroServers = await findXaeroMinimapWaypoints(mapDirectory);
    res.json({ servers: xaeroServers });
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/waypoints/server/:serverName', async (req, res) => {
  const { serverName } = req.params;
  
  if (!mapDirectory) {
    return res.status(400).json({ error: '请先设置地图目录' });
  }
  
  try {
    const minimapPath = path.join(mapDirectory, 'minimap', serverName);
    
    if (!existsSync(minimapPath)) {
      return res.json({ waypoints: {}, dimensions: [] });
    }
    
    const dimensions = [];
    const dimWaypoints = {};
    
    const dimEntries = await readdir(minimapPath, { withFileTypes: true });
    for (const dimEntry of dimEntries) {
      if (dimEntry.isDirectory() && dimEntry.name.startsWith('dim%')) {
        const dimPath = path.join(minimapPath, dimEntry.name);
        const dimNum = dimEntry.name.replace('dim%', '');
        
        let dimName;
        if (dimNum === '0') dimName = 'null';
        else if (dimNum === '-1') dimName = 'DIM-1';
        else if (dimNum === '1') dimName = 'DIM1';
        else dimName = dimEntry.name;
        
        const files = await readdir(dimPath);
        const waypointFiles = files.filter(f => f.startsWith('mw$') && f.endsWith('.txt'));
        
        if (waypointFiles.length > 0) {
          dimensions.push(dimName);
          
          let allWaypoints = [];
          for (const wpFile of waypointFiles) {
            const filePath = path.join(dimPath, wpFile);
            const setName = wpFile.replace('.txt', '').replace('mw$', '');
            const wps = await loadWaypointsFromXaeroFile(filePath, setName, dimName);
            allWaypoints = allWaypoints.concat(wps);
          }
          dimWaypoints[dimName] = allWaypoints;
        }
      }
    }
    
    res.json({ waypoints: dimWaypoints, dimensions });
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/waypoints/server/:serverName/:dimension', async (req, res) => {
  const { serverName, dimension } = req.params;
  
  if (!mapDirectory) {
    return res.status(400).json({ error: '请先设置地图目录' });
  }
  
  try {
    let dimNum;
    if (dimension === 'null' || dimension === 'overworld') dimNum = '0';
    else if (dimension === 'DIM-1' || dimension === 'nether') dimNum = '-1';
    else if (dimension === 'DIM1' || dimension === 'end') dimNum = '1';
    else dimNum = dimension.replace('dim%', '');
    
    const dimPath = path.join(mapDirectory, 'minimap', serverName, `dim%${dimNum}`);
    
    if (!existsSync(dimPath)) {
      return res.json({ waypoints: [] });
    }
    
    const files = await readdir(dimPath);
    const waypointFiles = files.filter(f => f.startsWith('mw$') && f.endsWith('.txt'));
    
    let allWaypoints = [];
    for (const wpFile of waypointFiles) {
      const filePath = path.join(dimPath, wpFile);
      const setName = wpFile.replace('.txt', '').replace('mw$', '');
      const wps = await loadWaypointsFromXaeroFile(filePath, setName, dimension);
      allWaypoints = allWaypoints.concat(wps);
    }
    
    res.json({ waypoints: allWaypoints });
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

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
      mapDirectory
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
    
    initDatabaseForWorld(world);
    
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
      let dimPath = path.join(mapDirectory, 'world-map', world, dim);
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
      
      const buffer = getPooledBuffer(totalSize);
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
      
      ws.send(buffer, () => releaseBuffer(buffer));
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
  initWorkerPool();
  console.log(`Xaero Map Server running at http://localhost:${PORT}`);
  console.log(`WebSocket server ready at ws://localhost:${PORT}`);
  console.log(`Default directory: ${mapDirectory || '(not set)'}`);
  console.log(`Cache directory: ${cacheDirectory}`);
  console.log(`Config: maxCacheEntries=${MAX_MEMORY_CACHE_ENTRIES}, maxConcurrentLoads=${MAX_CONCURRENT_LOADS}, maxBatchRegions=${MAX_BATCH_REGIONS}`);
});
