import { parentPort, workerData } from 'worker_threads';
import { existsSync } from 'fs';
import path from 'path';
import { LRUCache } from './lru-cache.js';
import {
  DEFAULT_BIOME,
  computeBlockColor,
  applyShading,
  extractHeightsFromRegion,
  renderRegionToPixels,
  renderCaveLayersToPixels,
  downsamplePixels,
  loadRegion,
  parseRegionFile,
  parseTile,
  parseBlock,
  parseOverlay,
  readNBTInline,
  readTagValue
} from './regionParser.js';

const BLOCK_COLORS = workerData?.blockColors || {};
const BIOME_COLORS = workerData?.biomeColors || {};

const MAX_HEIGHT_CACHE = 256;
const heightCache = new LRUCache(MAX_HEIGHT_CACHE);

function getHeightCacheKey(dimPath, regionX, regionZ, lod) {
  return `${dimPath}:${regionX},${regionZ}:lod${lod}`;
}

function getCachedHeight(key) {
  return heightCache.get(key);
}

function setCachedHeight(key, heights) {
  heightCache.set(key, heights);
}

let currentTaskCancelled = false;
let currentTaskId = null;

parentPort.on('message', (msg) => {
  if (msg.type === 'cancel') {
    if (msg.taskId === currentTaskId) {
      currentTaskCancelled = true;
    }
  }
});

function isCancelled() {
  return currentTaskCancelled;
}

parentPort.on('message', async (task) => {
  const { taskId, dimPath, regionX, regionZ, caveMode, caveStart, lod } = task;

  currentTaskId = taskId;
  currentTaskCancelled = false;

  try {
    let pixels;
    let northRegionHeights = null;
    let westRegionHeights = null;
    let northwestRegionHeights = null;

    if (isCancelled()) {
      parentPort.postMessage({ taskId, result: null });
      return;
    }

    if (caveMode === 0 || caveMode === null) {
      const regionData = await loadRegion(dimPath, regionX, regionZ);
      if (!regionData) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }

      if (isCancelled()) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }

      const northKey = getHeightCacheKey(dimPath, regionX, regionZ - 1, lod);
      const westKey = getHeightCacheKey(dimPath, regionX - 1, regionZ, lod);
      const northwestKey = getHeightCacheKey(dimPath, regionX - 1, regionZ - 1, lod);

      northRegionHeights = getCachedHeight(northKey);
      if (!northRegionHeights) {
        const northRegionPath = path.join(dimPath, `${regionX}_${regionZ - 1}.zip`);
        if (existsSync(northRegionPath)) {
          const northRegionData = await loadRegion(dimPath, regionX, regionZ - 1);
          if (northRegionData) {
            northRegionHeights = extractHeightsFromRegion(northRegionData, lod);
            setCachedHeight(northKey, northRegionHeights);
          }
        }
      }

      if (isCancelled()) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }

      westRegionHeights = getCachedHeight(westKey);
      if (!westRegionHeights) {
        const westRegionPath = path.join(dimPath, `${regionX - 1}_${regionZ}.zip`);
        if (existsSync(westRegionPath)) {
          const westRegionData = await loadRegion(dimPath, regionX - 1, regionZ);
          if (westRegionData) {
            westRegionHeights = extractHeightsFromRegion(westRegionData, lod);
            setCachedHeight(westKey, westRegionHeights);
          }
        }
      }

      northwestRegionHeights = getCachedHeight(northwestKey);
      if (!northwestRegionHeights) {
        const northwestRegionPath = path.join(dimPath, `${regionX - 1}_${regionZ - 1}.zip`);
        if (existsSync(northwestRegionPath)) {
          const northwestRegionData = await loadRegion(dimPath, regionX - 1, regionZ - 1);
          if (northwestRegionData) {
            northwestRegionHeights = extractHeightsFromRegion(northwestRegionData, lod);
            setCachedHeight(northwestKey, northwestRegionHeights);
          }
        }
      }

      if (isCancelled()) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }

      pixels = renderRegionToPixels(regionData, BLOCK_COLORS, BIOME_COLORS, lod, northRegionHeights, westRegionHeights, northwestRegionHeights, isCancelled);
    } else if (caveMode === 1) {
      const maxLayer = caveStart !== null ? Math.floor(caveStart / 16) : 7;
      const layers = [];

      for (let layer = 0; layer <= maxLayer; layer++) {
        if (isCancelled()) break;

        const cavePath = path.join(dimPath, 'caves', String(layer));
        if (existsSync(cavePath)) {
          const regionData = await loadRegion(cavePath, regionX, regionZ);
          if (regionData) {
            layers.push({ layer, data: regionData });
          }
        }
      }

      if (isCancelled()) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }

      if (layers.length === 0) {
        const regionData = await loadRegion(dimPath, regionX, regionZ);
        if (!regionData) {
          parentPort.postMessage({ taskId, result: null });
          return;
        }
        pixels = renderRegionToPixels(regionData, BLOCK_COLORS, BIOME_COLORS, lod, null, null, null, isCancelled);
      } else {
        pixels = renderCaveLayersToPixels(layers, BLOCK_COLORS, BIOME_COLORS, lod);
      }
    } else if (caveMode === 2) {
      const layers = [];

      for (let layer = 0; layer <= 15; layer++) {
        if (isCancelled()) break;

        const cavePath = path.join(dimPath, 'caves', String(layer));
        if (existsSync(cavePath)) {
          const regionData = await loadRegion(cavePath, regionX, regionZ);
          if (regionData) {
            layers.push({ layer, data: regionData });
          }
        }
      }

      if (isCancelled()) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }

      if (layers.length === 0) {
        const regionData = await loadRegion(dimPath, regionX, regionZ);
        if (!regionData) {
          parentPort.postMessage({ taskId, result: null });
          return;
        }
        pixels = renderRegionToPixels(regionData, BLOCK_COLORS, BIOME_COLORS, lod, null, null, null, isCancelled);
      } else {
        pixels = renderCaveLayersToPixels(layers, BLOCK_COLORS, BIOME_COLORS, lod);
      }
    } else {
      const regionData = await loadRegion(dimPath, regionX, regionZ);
      if (!regionData) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }
      pixels = renderRegionToPixels(regionData, BLOCK_COLORS, BIOME_COLORS, lod, null, null, null, isCancelled);
    }

    if (isCancelled() || !pixels) {
      parentPort.postMessage({ taskId, result: null });
      return;
    }

    parentPort.postMessage({ taskId, result: pixels }, [pixels.buffer]);
  } catch (error) {
    parentPort.postMessage({ taskId, error: error.message });
  }
});
