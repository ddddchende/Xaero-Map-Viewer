const fs = require('fs');
const path = require('path');
const JSZip = require('jszip');

function readTagValue(data, view, offset, type) {
  switch (type) {
    case 0:
      return { value: null, newOffset: offset };
    case 1: {
      const value = view.getInt8(offset++);
      return { value, newOffset: offset };
    }
    case 2: {
      const value = view.getInt16(offset, false);
      offset += 2;
      return { value, newOffset: offset };
    }
    case 3: {
      const value = view.getInt32(offset, false);
      offset += 4;
      return { value, newOffset: offset };
    }
    case 4: {
      const value = view.getBigInt64(offset, false);
      offset += 8;
      return { value, newOffset: offset };
    }
    case 5: {
      const value = view.getFloat32(offset, false);
      offset += 4;
      return { value, newOffset: offset };
    }
    case 6: {
      const value = view.getFloat64(offset, false);
      offset += 8;
      return { value, newOffset: offset };
    }
    case 7: {
      const length = view.getInt32(offset, false);
      offset += 4 + length;
      return { value: null, newOffset: offset };
    }
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
      const value = [];
      for (let i = 0; i < listLength; i++) {
        const result = readTagValue(data, view, offset, listType);
        value.push(result.value);
        offset = result.newOffset;
      }
      return { value, newOffset: offset };
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
    case 11: {
      const length = view.getInt32(offset, false);
      offset += 4 + length * 4;
      return { value: null, newOffset: offset };
    }
    case 12: {
      const length = view.getInt32(offset, false);
      offset += 4 + length * 8;
      return { value: null, newOffset: offset };
    }
    default:
      console.log('Unknown NBT type:', type, 'at offset:', offset);
      return { value: null, newOffset: offset };
  }
}

function readNBT(data, offset) {
  const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
  const tagType = view.getUint8(offset++);
  
  if (tagType === 0) {
    return { nbt: null, newOffset: offset };
  }
  
  const nameLen = view.getUint8(offset) << 8 | view.getUint8(offset + 1);
  offset += 2;
  const name = new TextDecoder().decode(data.slice(offset, offset + nameLen));
  offset += nameLen;
  
  const result = readTagValue(data, view, offset, tagType);
  return { nbt: { name, value: result.value }, newOffset: result.newOffset };
}

const basePath = 'E:\\Game\\AC1.21.4基础整合包\\.minecraft\\versions\\1.21.1-Fabric 0.16.14\\xaero\\world-map\\Multiplayer_2b2t.cc\\null\\mw$default';
const files = fs.readdirSync(basePath);
const filePath = path.join(basePath, files[0]);
console.log('Reading:', filePath);

const buffer = fs.readFileSync(filePath);

JSZip.loadAsync(buffer).then(async (contents) => {
  for (const fileName of Object.keys(contents.files)) {
    if (fileName.endsWith('.xaero')) {
      const data = await contents.file(fileName).async('uint8array');
      console.log('Extracted file:', fileName, 'size:', data.length);
      
      const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
      let offset = 0;
      
      const firstByte = view.getUint8(offset++);
      let majorVersion = 0, minorVersion = 0;
      
      if (firstByte === 0xFF) {
        const fullVersion = view.getInt32(offset, false);
        minorVersion = fullVersion & 0xFFFF;
        majorVersion = (fullVersion >> 16) & 0xFFFF;
        console.log('Version:', majorVersion, '.', minorVersion);
        offset += 4;
      }
      
      const chunkCoords = view.getUint8(offset++);
      const chunkX = chunkCoords >> 4;
      const chunkZ = chunkCoords & 0x0F;
      console.log('Chunk:', chunkX, chunkZ);
      
      // Read first tile
      const tileMarker = view.getInt32(offset, false);
      offset += 4;
      console.log('Tile marker:', tileMarker, '(0x' + tileMarker.toString(16) + ')');
      
      if (tileMarker === -1) {
        console.log('Tile is null');
        return;
      }
      
      // Parse first few blocks
      const blockStatePalette = [];
      const biomePalette = [];
      
      for (let i = 0; i < 10; i++) {
        const blockParams = (i === 0) ? tileMarker : view.getInt32(offset, false);
        if (i !== 0) offset += 4;
        
        const hasState = (blockParams & 1) !== 0;
        const hasOverlays = (blockParams & 2) !== 0;
        const hasBiome = (blockParams & 0x100000) !== 0;
        const isNewToPalette = (blockParams & 0x200000) !== 0;
        const hasTopHeight = (blockParams & 0x1000000) !== 0;
        const hasHeightByte = (blockParams & 64) !== 0;
        
        console.log(`\nBlock ${i}:`);
        console.log('  Params:', '0x' + blockParams.toString(16));
        console.log('  hasState:', hasState, 'isNewToPalette:', isNewToPalette);
        console.log('  hasOverlays:', hasOverlays, 'hasBiome:', hasBiome);
        console.log('  hasTopHeight:', hasTopHeight, 'hasHeightByte:', hasHeightByte);
        
        let state = null;
        if (hasState) {
          if (isNewToPalette) {
            try {
              const nbtResult = readNBT(data, offset);
              offset = nbtResult.newOffset;
              state = nbtResult.nbt?.value?.Name || 'minecraft:air';
              console.log('  NBT result:', JSON.stringify(nbtResult.nbt).slice(0, 200));
              blockStatePalette.push(state);
            } catch (e) {
              console.error('  NBT parse error:', e.message);
              state = 'minecraft:air';
            }
          } else {
            const paletteIndex = view.getInt32(offset, false);
            offset += 4;
            state = blockStatePalette[paletteIndex] || 'unknown';
            console.log('  Palette index:', paletteIndex, '->', state);
          }
        } else {
          state = 'minecraft:grass_block';
        }
        
        // Height
        let height = 0;
        if (hasHeightByte) {
          height = view.getUint8(offset++);
        } else {
          const heightSecondPartOffset = minorVersion >= 4 ? 25 : 24;
          const heightBitsCombined = (blockParams >> 12 & 255) | ((blockParams >> heightSecondPartOffset & 15) << 8);
          height = heightBitsCombined << 20 >> 20;
        }
        console.log('  Height:', height);
        
        // Top height
        if (hasTopHeight) {
          const topHeight = view.getUint8(offset++);
          console.log('  Top height:', topHeight);
        }
        
        // Overlays
        if (hasOverlays) {
          const overlayCount = view.getUint8(offset++);
          console.log('  Overlay count:', overlayCount);
          for (let j = 0; j < overlayCount; j++) {
            const overlayParams = view.getInt32(offset, false);
            offset += 4;
            console.log('    Overlay', j, 'params:', '0x' + overlayParams.toString(16));
            
            const overlayHasState = (overlayParams & 1) !== 0;
            if (overlayHasState) {
              const overlayIsNew = (overlayParams & 0x400) !== 0;
              if (overlayIsNew) {
                try {
                  const nbtResult = readNBT(data, offset);
                  offset = nbtResult.newOffset;
                  console.log('    Overlay NBT:', JSON.stringify(nbtResult.nbt).slice(0, 100));
                } catch (e) {
                  console.error('    Overlay NBT error:', e.message);
                }
              } else {
                offset += 4;
              }
            }
            
            const stillUsesColorTypes = minorVersion < 5 || majorVersion <= 2;
            const savedColorType = stillUsesColorTypes ? (overlayParams >> 8) & 3 : 0;
            if (savedColorType === 2 || (overlayParams & 4) !== 0) {
              offset += 4;
            }
            if (minorVersion < 8 && (overlayParams & 8) !== 0) {
              offset += 4;
            }
          }
        }
        
        // Biome
        const stillUsesColorTypes = minorVersion < 5 || majorVersion <= 2;
        const savedColourType = stillUsesColorTypes ? (blockParams >> 2) & 3 : 0;
        
        if ((savedColourType !== 0 && savedColourType !== 3) || hasBiome) {
          const isNewBiomeToPalette = (blockParams & 0x400000) !== 0;
          if (isNewBiomeToPalette) {
            const biomeAsInt = (blockParams & 0x800000) !== 0;
            if (biomeAsInt) {
              const biomeId = view.getInt32(offset, false);
              offset += 4;
              console.log('  Biome (int):', biomeId);
              biomePalette.push(`biome:${biomeId}`);
            } else {
              const strLen = view.getUint8(offset) << 8 | view.getUint8(offset + 1);
              offset += 2;
              const biomeStr = new TextDecoder().decode(data.slice(offset, offset + strLen));
              offset += strLen;
              console.log('  Biome (str):', biomeStr);
              biomePalette.push(biomeStr);
            }
          } else {
            const biomeIndex = view.getInt32(offset, false);
            offset += 4;
            console.log('  Biome (palette):', biomeIndex, '->', biomePalette[biomeIndex]);
          }
        }
        
        console.log('  State:', state);
      }
      
      break;
    }
  }
});
