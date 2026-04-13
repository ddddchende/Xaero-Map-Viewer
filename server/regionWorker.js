import { parentPort, workerData } from 'worker_threads';
import JSZip from 'jszip';
import { readFile } from 'fs/promises';
import { existsSync } from 'fs';
import path from 'path';
import zlib from 'zlib';

const BLOCK_COLORS = workerData?.blockColors || {};
const BIOME_COLORS = workerData?.biomeColors || {};
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
  
  if (prevHeight !== 32767 && prevDiagHeight !== 32767) {
    const verticalSlope = height - prevHeight;
    if (verticalSlope > 0) {
      depthBrightness *= 1.3;
    } else if (verticalSlope < 0) {
      depthBrightness *= 0.7;
    }
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

function renderRegionToPixels(regionData, lod = 0) {
  const step = 1 << lod;
  const srcSize = 512;
  const dstSize = srcSize >> lod;
  const pixels = Buffer.alloc(dstSize * dstSize * 4);
  const heights = new Int16Array(dstSize * dstSize);
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
            
            const srcPixelX = pixelBaseX + x;
            if (srcPixelX % step !== 0) continue;
            if (srcPixelX >= srcSize) continue;
            const dstPixelX = srcPixelX >> lod;
            
            for (let z = 0; z < 16; z++) {
              const block = blockRow[z];
              if (!block || !block.s) continue;
              
              const srcPixelZ = pixelBaseZ + z;
              if (srcPixelZ % step !== 0) continue;
              if (srcPixelZ >= srcSize) continue;
              const dstPixelZ = srcPixelZ >> lod;
              
              const idx = dstPixelZ * dstSize + dstPixelX;
              const height = block.h ?? 32767;
              heights[idx] = height;
              
              const prevHeight = dstPixelZ > 0 ? heights[idx - dstSize] : 32767;
              const prevDiagHeight = (dstPixelZ > 0 && dstPixelX > 0) ? heights[idx - dstSize - 1] : 32767;
              
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

function renderCaveLayersToPixels(layers, lod = 0) {
  const step = 1 << lod;
  const srcSize = 512;
  const dstSize = srcSize >> lod;
  const pixels = Buffer.alloc(dstSize * dstSize * 4);
  
  for (let i = 0; i < layers.length; i++) {
    const layerData = layers[i].data;
    const layerPixels = renderRegionToPixels(layerData, lod);
    
    for (let dz = 0; dz < dstSize; dz++) {
      for (let dx = 0; dx < dstSize; dx++) {
        const idx = dz * dstSize + dx;
        const srcAlpha = layerPixels[idx * 4 + 3];
        if (srcAlpha > 0 && pixels[idx * 4 + 3] === 0) {
          pixels[idx * 4] = layerPixels[idx * 4];
          pixels[idx * 4 + 1] = layerPixels[idx * 4 + 1];
          pixels[idx * 4 + 2] = layerPixels[idx * 4 + 2];
          pixels[idx * 4 + 3] = srcAlpha;
        }
      }
    }
  }
  
  return pixels;
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

parentPort.on('message', async (task) => {
  const { taskId, dimPath, regionX, regionZ, caveMode, caveStart, lod } = task;
  
  try {
    let pixels;
    
    if (caveMode === 0 || caveMode === null) {
      const regionData = await loadRegion(dimPath, regionX, regionZ);
      if (!regionData) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }
      pixels = renderRegionToPixels(regionData, lod);
    } else if (caveMode === 1) {
      const maxLayer = caveStart !== null ? Math.floor(caveStart / 16) : 7;
      const layers = [];
      
      for (let layer = 0; layer <= maxLayer; layer++) {
        const cavePath = path.join(dimPath, 'caves', String(layer));
        if (existsSync(cavePath)) {
          const regionData = await loadRegion(cavePath, regionX, regionZ);
          if (regionData) {
            layers.push({ layer, data: regionData });
          }
        }
      }
      
      if (layers.length === 0) {
        const regionData = await loadRegion(dimPath, regionX, regionZ);
        if (!regionData) {
          parentPort.postMessage({ taskId, result: null });
          return;
        }
        pixels = renderRegionToPixels(regionData, lod);
      } else {
        pixels = renderCaveLayersToPixels(layers, lod);
      }
    } else if (caveMode === 2) {
      const layers = [];
      const maxLayer = 15;
      
      for (let layer = 0; layer <= maxLayer; layer++) {
        const cavePath = path.join(dimPath, 'caves', String(layer));
        if (existsSync(cavePath)) {
          const regionData = await loadRegion(cavePath, regionX, regionZ);
          if (regionData) {
            layers.push({ layer, data: regionData });
          }
        }
      }
      
      if (layers.length === 0) {
        const regionData = await loadRegion(dimPath, regionX, regionZ);
        if (!regionData) {
          parentPort.postMessage({ taskId, result: null });
          return;
        }
        pixels = renderRegionToPixels(regionData, lod);
      } else {
        pixels = renderCaveLayersToPixels(layers, lod);
      }
    } else {
      const regionData = await loadRegion(dimPath, regionX, regionZ);
      if (!regionData) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }
      pixels = renderRegionToPixels(regionData, lod);
    }
    
    parentPort.postMessage({ taskId, result: pixels }, [pixels.buffer]);
  } catch (error) {
    parentPort.postMessage({ taskId, error: error.message });
  }
});
