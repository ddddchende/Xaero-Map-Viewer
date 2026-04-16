import JSZip from 'jszip';
import { readFile } from 'fs/promises';
import { existsSync } from 'fs';
import path from 'path';

export const DEFAULT_BIOME = { grass: 0x91bd59, foliage: 0x77ab2f, water: 0x3f76e4 };

const LIGHT_MIN = 9;

function getBlockBrightness(light, sun) {
  return (LIGHT_MIN + Math.max(sun, light)) / (15.0 + LIGHT_MIN);
}

function getPixelLight(light) {
  if (light === 0) return 0.0;
  return getBlockBrightness(light, 0);
}

function getWaterTransparency() {
  return 191;
}

function getWaterColor(biome, biomeColors) {
  const waterBiomeColor = (biomeColors[biome] || DEFAULT_BIOME).water;
  const waterBaseR = 63;
  const waterBaseG = 118;
  const waterBaseB = 228;
  
  const brightnessR = waterBaseR / 255.0;
  const brightnessG = waterBaseG / 255.0;
  const brightnessB = waterBaseB / 255.0;
  
  const biomeR = (waterBiomeColor >> 16) & 0xFF;
  const biomeG = (waterBiomeColor >> 8) & 0xFF;
  const biomeB = waterBiomeColor & 0xFF;
  
  const grayBase = 0.6;
  const r = Math.floor((biomeR * brightnessR + 128 * grayBase) / (1 + grayBase));
  const g = Math.floor((biomeG * brightnessG + 140 * grayBase) / (1 + grayBase));
  const b = Math.floor((biomeB * brightnessB + 160 * grayBase) / (1 + grayBase));
  
  return (r << 16) | (g << 8) | b;
}

export function computeBlockColor(state, biome, overlays, blockColors, biomeColors, blockLight = 15) {
  if (!state || state === 'minecraft:air') {
    if (overlays && overlays.length > 0) {
      let overlayRed = 0;
      let overlayGreen = 0;
      let overlayBlue = 0;
      let currentTransparencyMultiplier = 1.0;
      let sun = 15;
      
      for (let i = 0; i < overlays.length; i++) {
        const overlay = overlays[i];
        const overlayInfo = blockColors[overlay.state];
        if (!overlayInfo) continue;
        
        let overlayColor = overlayInfo.color;
        let overlayAlpha;
        
        if (overlayInfo.water) {
          overlayColor = getWaterColor(biome, biomeColors);
          overlayAlpha = getWaterTransparency() / 255.0;
        } else {
          overlayAlpha = getPixelLight(overlay.light ?? 15);
        }
        
        const overlayR = (overlayColor >> 16) & 0xFF;
        const overlayG = (overlayColor >> 8) & 0xFF;
        const overlayB = overlayColor & 0xFF;
        
        const brightness = getBlockBrightness(overlay.light ?? 15, sun) * overlayAlpha * currentTransparencyMultiplier;
        
        overlayRed += overlayR * brightness;
        overlayGreen += overlayG * brightness;
        overlayBlue += overlayB * brightness;
        
        sun -= overlay.opacity;
        if (sun < 0) sun = 0;
        
        currentTransparencyMultiplier *= (1.0 - overlayAlpha);
      }
      
      const r = Math.min(255, Math.floor(overlayRed));
      const g = Math.min(255, Math.floor(overlayGreen));
      const b = Math.min(255, Math.floor(overlayBlue));
      
      return { color: (r << 16) | (g << 8) | b, alpha: 255, hasOverlay: true };
    }
    return { color: 0, alpha: 0, hasOverlay: false };
  }

  const info = blockColors[state];
  if (!info) {
    return { color: 0x808080, alpha: 255, hasOverlay: false };
  }

  let color = info.color;
  let alpha = info.alpha ?? 255;

  if (info.grass || info.foliage) {
    const biomeColor = biomeColors[biome] || DEFAULT_BIOME;
    const biomeRGB = info.grass ? biomeColor.grass : biomeColor.foliage;
    
    const baseR = (color >> 16) & 0xFF;
    const baseG = (color >> 8) & 0xFF;
    const baseB = color & 0xFF;
    
    const brightnessR = baseR / 255.0;
    const brightnessG = baseG / 255.0;
    const brightnessB = baseB / 255.0;
    
    const r = Math.floor(((biomeRGB >> 16) & 0xFF) * brightnessR);
    const g = Math.floor(((biomeRGB >> 8) & 0xFF) * brightnessG);
    const b = Math.floor((biomeRGB & 0xFF) * brightnessB);
    
    color = (r << 16) | (g << 8) | b;
  }

  if (overlays && overlays.length > 0) {
    let overlayRed = 0;
    let overlayGreen = 0;
    let overlayBlue = 0;
    let currentTransparencyMultiplier = 1.0;
    let sun = 15;
    
    for (let i = 0; i < overlays.length; i++) {
      const overlay = overlays[i];
      const overlayInfo = blockColors[overlay.state];
      if (!overlayInfo) continue;
      
      let overlayColor = overlayInfo.color;
      let overlayAlpha;
      
      if (overlayInfo.water) {
        overlayColor = getWaterColor(biome, biomeColors);
        overlayAlpha = getWaterTransparency() / 255.0;
      } else {
        overlayAlpha = getPixelLight(overlay.light ?? 15);
      }
      
      const overlayR = (overlayColor >> 16) & 0xFF;
      const overlayG = (overlayColor >> 8) & 0xFF;
      const overlayB = overlayColor & 0xFF;
      
      const brightness = getBlockBrightness(overlay.light ?? 15, sun) * overlayAlpha * currentTransparencyMultiplier;
      
      overlayRed += overlayR * brightness;
      overlayGreen += overlayG * brightness;
      overlayBlue += overlayB * brightness;
      
      sun -= overlay.opacity;
      if (sun < 0) sun = 0;
      
      currentTransparencyMultiplier *= (1.0 - overlayAlpha);
    }
    
    const baseR = (color >> 16) & 0xFF;
    const baseG = (color >> 8) & 0xFF;
    const baseB = color & 0xFF;
    
    const blockBrightness = getBlockBrightness(blockLight, sun);
    
    const r = Math.min(255, Math.floor(baseR * blockBrightness * currentTransparencyMultiplier + overlayRed));
    const g = Math.min(255, Math.floor(baseG * blockBrightness * currentTransparencyMultiplier + overlayGreen));
    const b = Math.min(255, Math.floor(baseB * blockBrightness * currentTransparencyMultiplier + overlayBlue));
    
    color = (r << 16) | (g << 8) | b;
    alpha = 255;
    
    return { color, alpha, hasOverlay: true };
  }

  return { color, alpha, hasOverlay: false };
}

export function applyShading(color, height, prevHeight, prevDiagHeight, light, glowing = false, slopes = 2, shadowR = 1.0, shadowG = 1.0, shadowB = 1.0, hasOverlay = false) {
  let r = (color >> 16) & 0xFF;
  let g = (color >> 8) & 0xFF;
  let b = color & 0xFF;

  let brightnessR = 1.0;
  let brightnessG = 1.0;
  let brightnessB = 1.0;

  let depthBrightness = 1.0;

  if (!hasOverlay && height >= 0 && height <= 63) {
    depthBrightness = 0.7 + (height / 63.0) * 0.3;
  }

  if (hasOverlay) {
    r = Math.min(255, r);
    g = Math.min(255, g);
    b = Math.min(255, b);
    return (r << 16) | (g << 8) | b;
  }

  const effectivePrevHeight = prevHeight === 32767 ? height : prevHeight;
  const effectivePrevDiagHeight = prevDiagHeight === 32767 ? height : prevDiagHeight;

  const verticalSlope = height - effectivePrevHeight;
  const diagonalSlope = height - effectivePrevDiagHeight;

  if (slopes > 0) {
    if (slopes === 1) {
      if (verticalSlope > 0) {
        depthBrightness *= 1.15;
      } else if (verticalSlope < 0) {
        depthBrightness *= 0.85;
      }
    } else {
      const ambientLightColored = glowing ? 0.0 : 0.2;
      const ambientLightWhite = glowing ? 1.0 : 0.5;
      const maxDirectLight = glowing ? 0.22222224 : 0.6666667;

      let cos = 0.0;

      if (slopes === 2) {
        const directLightClamped = -verticalSlope;
        if (directLightClamped < 1.0) {
          if (verticalSlope === 1 && diagonalSlope === 1) {
            cos = 1.0;
          } else {
            const whiteLight = verticalSlope - diagonalSlope;
            const cast = 1.0 - directLightClamped;
            const crossMagnitude = Math.sqrt(whiteLight * whiteLight + 1.0 + directLightClamped * directLightClamped);
            cos = (cast / crossMagnitude) / Math.sqrt(2.0);
          }
        }
      } else if (verticalSlope >= 0) {
        if (verticalSlope === 1) {
          cos = 1.0;
        } else {
          const directLightClamped = Math.sqrt(verticalSlope * verticalSlope + 1);
          const whiteLight = verticalSlope + 1;
          cos = (whiteLight / directLightClamped) / Math.sqrt(2.0);
        }
      }

      let directLight = 0.0;
      if (cos === 1.0) {
        directLight = maxDirectLight;
      } else if (cos > 0.0) {
        directLight = Math.ceil(cos * 10.0) / 10.0 * maxDirectLight * 0.88388;
      }

      const whiteLight = ambientLightWhite + directLight;
      brightnessR *= shadowR * ambientLightColored + whiteLight;
      brightnessG *= shadowG * ambientLightColored + whiteLight;
      brightnessB *= shadowB * ambientLightColored + whiteLight;
    }
  }

  brightnessR *= depthBrightness;
  brightnessG *= depthBrightness;
  brightnessB *= depthBrightness;

  if (light < 15) {
    const lightFactor = (9 + light) / 24;
    r = (r * lightFactor) | 0;
    g = (g * lightFactor) | 0;
    b = (b * lightFactor) | 0;
  }

  r = Math.min(255, (r * brightnessR) | 0);
  g = Math.min(255, (g * brightnessG) | 0);
  b = Math.min(255, (b * brightnessB) | 0);

  return (r << 16) | (g << 8) | b;
}

export function extractHeightsFromRegion(regionData, lod = 0) {
  const step = 1 << lod;
  const srcSize = 512;
  const dstSize = srcSize >> lod;
  const heights = new Int16Array(dstSize * dstSize);
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

            const srcPixelX = pixelBaseX + x;
            if (lod > 0 && srcPixelX % step !== 0) continue;
            if (srcPixelX >= srcSize) continue;
            const dstPixelX = srcPixelX >> lod;

            for (let z = 0; z < 16; z++) {
              const block = blockRow[z];
              if (!block || !block.s) continue;

              const srcPixelZ = pixelBaseZ + z;
              if (lod > 0 && srcPixelZ % step !== 0) continue;
              if (srcPixelZ >= srcSize) continue;
              const dstPixelZ = srcPixelZ >> lod;

              const idx = dstPixelZ * dstSize + dstPixelX;
              heights[idx] = block.h ?? 32767;
            }
          }
        }
      }
    }
  }

  return heights;
}

export function renderRegionToPixels(regionData, blockColors, biomeColors, lod = 0, northHeights = null, westHeights = null, northwestHeights = null, isCancelledFn = null) {
  const step = 1 << lod;
  const srcSize = 512;
  const dstSize = srcSize >> lod;
  const pixels = Buffer.alloc(dstSize * dstSize * 4);
  const heights = new Int16Array(dstSize * dstSize);
  heights.fill(32767);

  const chunks = regionData.chunks;

  for (let chunkX = 0; chunkX < 8; chunkX++) {
    if (isCancelledFn && isCancelledFn()) return null;

    const chunkRow = chunks[chunkX];
    if (!chunkRow) continue;

    for (let chunkZ = 0; chunkZ < 8; chunkZ++) {
      if (isCancelledFn && isCancelledFn()) return null;

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
            if (lod > 0 && srcPixelX % step !== 0) continue;
            if (srcPixelX >= srcSize) continue;
            const dstPixelX = srcPixelX >> lod;

            for (let z = 0; z < 16; z++) {
              const block = blockRow[z];
              if (!block || !block.s) continue;

              const srcPixelZ = pixelBaseZ + z;
              if (lod > 0 && srcPixelZ % step !== 0) continue;
              if (srcPixelZ >= srcSize) continue;
              const dstPixelZ = srcPixelZ >> lod;

              const idx = dstPixelZ * dstSize + dstPixelX;
              const height = block.h ?? 32767;
              heights[idx] = height;

              let prevHeight, prevDiagHeight;

              if (dstPixelZ > 0) {
                prevHeight = heights[idx - dstSize];
                if (dstPixelX > 0) {
                  prevDiagHeight = heights[idx - dstSize - 1];
                } else {
                  prevDiagHeight = westHeights ? westHeights[(dstPixelZ - 1) * dstSize + (dstSize - 1)] : 32767;
                }
              } else {
                if (northHeights) {
                  prevHeight = northHeights[(dstSize - 1) * dstSize + dstPixelX];
                  if (dstPixelX > 0) {
                    prevDiagHeight = northHeights[(dstSize - 1) * dstSize + (dstPixelX - 1)];
                  } else if (northwestHeights) {
                    prevDiagHeight = northwestHeights[(dstSize - 1) * dstSize + (dstSize - 1)];
                  } else {
                    prevDiagHeight = 32767;
                  }
                } else {
                  prevHeight = 32767;
                  prevDiagHeight = 32767;
                }
              }

              const { color, alpha, hasOverlay } = computeBlockColor(block.s, block.b, block.o, blockColors, biomeColors, block.l ?? 15);
              const shadedColor = applyShading(color, height, prevHeight, prevDiagHeight, block.l ?? 15, false, 2, 1.0, 1.0, 1.0, hasOverlay);
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

export function renderCaveLayersToPixels(layers, blockColors, biomeColors, lod = 0) {
  const step = 1 << lod;
  const srcSize = 512;
  const dstSize = srcSize >> lod;
  const pixels = Buffer.alloc(dstSize * dstSize * 4);

  for (let i = layers.length - 1; i >= 0; i--) {
    const layerData = layers[i].data;
    const layerPixels = renderRegionToPixels(layerData, blockColors, biomeColors, lod);

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

export function downsamplePixels(pixels, srcSize, dstSize) {
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

export async function loadRegion(dimPath, regionX, regionZ) {
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

export function parseRegionFile(buffer, regionX, regionZ) {
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

export function parseTile(data, view, offset, tileMarker, minorVersion, majorVersion, blockStatePalette, biomePalette) {
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

export function parseBlock(data, view, offset, parametres, minorVersion, majorVersion, blockStatePalette, biomePalette) {
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

  let overlays = [];

  if (hasOverlays && offset + 1 <= data.length) {
    const overlayCount = view.getUint8(offset++);
    for (let i = 0; i < overlayCount && offset + 4 <= data.length; i++) {
      try {
        const overlayResult = parseOverlay(data, view, offset, minorVersion, majorVersion, blockStatePalette);
        offset = overlayResult.newOffset;
        if (overlayResult.state) {
          overlays.push({
            state: overlayResult.state,
            opacity: overlayResult.opacity || 15,
            light: overlayResult.light || 15
          });
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
      o: overlays.length > 0 ? overlays : undefined
    },
    newOffset: offset
  };
}

export function parseOverlay(data, view, offset, minorVersion, majorVersion, blockStatePalette) {
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

  const opacity = (overlayParams >> 11) & 15;
  const light = (overlayParams >> 4) & 15;

  return { newOffset: offset, state: overlayState, opacity, light };
}

export function readNBTInline(data, offset) {
  const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
  const tagType = view.getUint8(offset++);
  if (tagType === 0) return { nbt: null, newOffset: offset };

  const nameLen = view.getUint8(offset) << 8 | view.getUint8(offset + 1);
  offset += 2 + nameLen;

  const nbt = readTagValue(data, view, offset, tagType);
  return { nbt: nbt.value, newOffset: nbt.newOffset };
}

export function readTagValue(data, view, offset, type) {
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
