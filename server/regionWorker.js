import { parentPort, workerData } from 'worker_threads';
import { existsSync } from 'fs';
import path from 'path';
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

let heightRequestIdCounter = 0;
const pendingHeightRequests = new Map();

function getHeightCacheKey(dimPath, regionX, regionZ, lod) {
  return `${dimPath}:${regionX},${regionZ}:lod${lod}`;
}

function requestHeightFromMain(dimPath, regionX, regionZ, lod) {
  return new Promise((resolve) => {
    const requestId = heightRequestIdCounter++;
    pendingHeightRequests.set(requestId, resolve);
    
    parentPort.postMessage({
      type: 'getHeight',
      requestId,
      dimPath,
      regionX,
      regionZ,
      lod
    });
  });
}

function setHeightToMain(cacheKey, heights) {
  parentPort.postMessage({
    type: 'setHeight',
    cacheKey,
    heights
  });
}

let currentTaskCancelled = false;
let currentTaskId = null;

parentPort.on('message', (msg) => {
  if (msg.type === 'cancel') {
    if (msg.taskId === currentTaskId) {
      currentTaskCancelled = true;
    }
  } else if (msg.type === 'heightResponse') {
    const { requestId, heights, cacheKey } = msg;
    const resolve = pendingHeightRequests.get(requestId);
    if (resolve) {
      pendingHeightRequests.delete(requestId);
      resolve(heights);
    }
  }
});

function isCancelled() {
  return currentTaskCancelled;
}

parentPort.on('message', async (task) => {
  if (task.type === 'cancel' || task.type === 'heightResponse') return;
  
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

      northRegionHeights = await requestHeightFromMain(dimPath, regionX, regionZ - 1, lod);
      if (!northRegionHeights) {
        const northRegionPath = path.join(dimPath, `${regionX}_${regionZ - 1}.zip`);
        if (existsSync(northRegionPath)) {
          const northRegionData = await loadRegion(dimPath, regionX, regionZ - 1);
          if (northRegionData) {
            northRegionHeights = extractHeightsFromRegion(northRegionData, lod);
            setHeightToMain(northKey, northRegionHeights);
          }
        }
      }

      if (isCancelled()) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }

      westRegionHeights = await requestHeightFromMain(dimPath, regionX - 1, regionZ, lod);
      if (!westRegionHeights) {
        const westRegionPath = path.join(dimPath, `${regionX - 1}_${regionZ}.zip`);
        if (existsSync(westRegionPath)) {
          const westRegionData = await loadRegion(dimPath, regionX - 1, regionZ);
          if (westRegionData) {
            westRegionHeights = extractHeightsFromRegion(westRegionData, lod);
            setHeightToMain(westKey, westRegionHeights);
          }
        }
      }

      if (isCancelled()) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }

      northwestRegionHeights = await requestHeightFromMain(dimPath, regionX - 1, regionZ - 1, lod);
      if (!northwestRegionHeights) {
        const northwestRegionPath = path.join(dimPath, `${regionX - 1}_${regionZ - 1}.zip`);
        if (existsSync(northwestRegionPath)) {
          const northwestRegionData = await loadRegion(dimPath, regionX - 1, regionZ - 1);
          if (northwestRegionData) {
            northwestRegionHeights = extractHeightsFromRegion(northwestRegionData, lod);
            setHeightToMain(northwestKey, northwestRegionHeights);
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
