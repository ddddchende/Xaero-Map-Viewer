import express from 'express';
import cors from 'cors';
import { readdir, readFile } from 'fs/promises';
import { existsSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import JSZip from 'jszip';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = 3001;

const DEFAULT_DIRECTORY = 'E:\\Game\\AC1.21.4基础整合包\\.minecraft\\versions\\1.21.1-Fabric 0.16.14\\xaero\\world-map\\Multiplayer_2b2t.cc';

app.use(cors());
app.use(express.json());

app.use(express.static(path.join(__dirname, '../dist')));

let mapDirectory = DEFAULT_DIRECTORY;

app.post('/api/set-directory', async (req, res) => {
  const { directory } = req.body;
  
  console.log('Received directory:', directory);
  console.log('Directory exists:', existsSync(directory));
  
  if (!directory) {
    return res.status(400).json({ error: '请提供目录路径' });
  }
  
  if (!existsSync(directory)) {
    return res.status(400).json({ error: '目录不存在', path: directory });
  }
  
  mapDirectory = directory;
  console.log('Map directory set to:', directory);
  res.json({ success: true, directory });
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
    if (worldName !== '当前世界') {
      basePath = path.join(mapDirectory, worldName);
    }
    
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
    if (worldName !== '当前世界') {
      basePath = path.join(mapDirectory, worldName);
    }
    
    const dimPath = path.join(basePath, dimName);
    const mapTypes = await listMapTypes(dimPath);
    res.json(mapTypes);
  } catch (error) {
    res.status(500).json({ error: String(error) });
  }
});

app.get('/api/worlds/:worldName/dimensions/:dimName/regions', async (req, res) => {
  const { worldName, dimName } = req.params;
  const { mapType } = req.query;
  
  try {
    let basePath = mapDirectory;
    if (worldName !== '当前世界') {
      basePath = path.join(mapDirectory, worldName);
    }
    
    let dimPath = path.join(basePath, dimName);
    
    if (mapType) {
      dimPath = path.join(dimPath, mapType);
    } else {
      const mapTypes = await listMapTypes(dimPath);
      if (mapTypes.length > 0) {
        dimPath = path.join(dimPath, mapTypes[0].path);
      }
    }
    
    const regions = await listRegions(dimPath);
    res.json({ regions, mapType: mapType || null });
  } catch (error) {
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
    if (world && world !== '当前世界') {
      basePath = path.join(mapDirectory, String(world));
    }
    
    let dimPath = path.join(basePath, String(dim));
    
    if (mapType) {
      dimPath = path.join(dimPath, String(mapType));
    } else {
      const mapTypes = await listMapTypes(dimPath);
      if (mapTypes.length > 0) {
        dimPath = path.join(dimPath, mapTypes[0].path);
      }
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
      if (dimNames.includes(entry.name)) {
        return 'world';
      }
      
      const subPath = path.join(basePath, entry.name);
      const hasRegions = await hasRegionFiles(subPath);
      if (hasRegions) {
        return 'world';
      }
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
      if (dims.length > 0) {
        worlds.push(entry.name);
      }
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
        dimensions.push({
          name: getDimensionDisplayName(entry.name),
          path: entry.name
        });
      }
    }
  }
  
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
        mapTypes.push({
          name: getMapTypeDisplayName(entry.name),
          path: entry.name
        });
      }
    }
  }
  
  return mapTypes;
}

function getDimensionDisplayName(dimName) {
  const dimNames = {
    'DIM-1': '下界',
    'DIM1': '末地',
    'null': '主世界',
    'minecraft:the_nether': '下界',
    'minecraft:the_end': '末地',
    'overworld': '主世界'
  };
  return dimNames[dimName] || dimName;
}

function getMapTypeDisplayName(mapTypeName) {
  const names = {
    'mw$default': '默认地图',
    'mw0,2,0': '地图 v0.2.0'
  };
  if (mapTypeName.startsWith('cache_')) {
    return `缓存 ${mapTypeName.replace('cache_', '')}`;
  }
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

async function listRegions(dimPath) {
  const entries = await readdir(dimPath);
  const regions = [];
  
  for (const entry of entries) {
    const match = entry.match(/^(-?\d+)_(-?\d+)\.(zip|xaero|xwmc)$/);
    if (match) {
      regions.push({
        x: parseInt(match[1], 10),
        z: parseInt(match[2], 10)
      });
    }
  }
  
  return regions;
}

async function loadRegion(dimPath, regionX, regionZ) {
  const zipPath = path.join(dimPath, `${regionX}_${regionZ}.zip`);
  const xaeroPath = path.join(dimPath, `${regionX}_${regionZ}.xaero`);
  
  let filePath = null;
  if (existsSync(zipPath)) {
    filePath = zipPath;
  } else if (existsSync(xaeroPath)) {
    filePath = xaeroPath;
  }
  
  if (!filePath) {
    return null;
  }
  
  const buffer = await readFile(filePath);
  
  // Check if it's a ZIP file
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
    
    console.log(`Parsing region ${regionX},${regionZ}: version ${majorVersion}.${minorVersion}`);
    
    const chunks = Array(8).fill(null).map(() => Array(8).fill(null));
    let totalValidBlocks = 0;
    let totalTiles = 0;
    let sampleBlocks = [];

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
            totalValidBlocks += tile.validBlockCount;
            totalTiles++;
            
            if (sampleBlocks.length < 5 && tile.validBlockCount > 0) {
              for (let bx = 0; bx < 16 && sampleBlocks.length < 5; bx++) {
                for (let bz = 0; bz < 16 && sampleBlocks.length < 5; bz++) {
                  const b = tile.tile.blocks[bx][bz];
                  if (b.s && b.s !== 'minecraft:air') {
                    sampleBlocks.push(b.s);
                  }
                }
              }
            }
          } catch (e) {
            console.error('Tile parse error:', e);
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
    
    console.log(`Region ${regionX},${regionZ}: ${totalTiles} tiles, ${totalValidBlocks} valid blocks`);
    console.log(`Sample blocks: ${sampleBlocks.join(', ')}`);
    console.log(`Block palette size: ${blockStatePalette.length}, Biome palette size: ${biomePalette.length}`);
    
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
  let validBlockCount = 0;
  
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
        
        if (result.block.s && result.block.s !== 'minecraft:air') {
          validBlockCount++;
        }
      } catch (e) {
        blocks[x][z] = { s: 'minecraft:air', h: 0, l: 15, b: null };
      }
    }
  }
  
  if (minorVersion >= 4 && offset + 1 <= data.length) {
    offset++;
  }
  if (minorVersion >= 6 && offset + 4 <= data.length) {
    offset += 4;
  }
  if (minorVersion >= 7 && offset + 1 <= data.length) {
    offset++;
  }
  
  return {
    tile: {
      blocks,
      loaded: true
    },
    newOffset: offset,
    validBlockCount
  };
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
          console.error('NBT parse error:', e);
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
    if (offset + 1 <= data.length) {
      height = view.getUint8(offset++);
    }
  } else {
    const heightSecondPartOffset = minorVersion >= 4 ? 25 : 24;
    const heightBitsCombined = (parametres >> 12 & 255) | ((parametres >> heightSecondPartOffset & 15) << 8);
    height = heightBitsCombined << 20 >> 20;
  }
  
  const hasTopHeight = minorVersion >= 4 && (parametres & 0x1000000) !== 0;
  if (hasTopHeight && offset + 1 <= data.length) {
    offset++;
  }
  
  if (hasOverlays && offset + 1 <= data.length) {
    const overlayCount = view.getUint8(offset++);
    for (let i = 0; i < overlayCount && offset + 4 <= data.length; i++) {
      try {
        const overlayResult = parseOverlay(data, view, offset, minorVersion, majorVersion, blockStatePalette);
        offset = overlayResult.newOffset;
      } catch (e) {
        break;
      }
    }
  }
  
  if (savedColourType === 3 && offset + 4 <= data.length) {
    offset += 4;
  }
  
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
      b: biome
    },
    newOffset: offset
  };
}

function parseOverlay(data, view, offset, minorVersion, majorVersion, blockStatePalette) {
  const overlayParams = view.getInt32(offset, false);
  offset += 4;
  
  const hasState = (overlayParams & 1) !== 0;
  if (hasState) {
    const isNewToPalette = (overlayParams & 0x400) !== 0;
    if (isNewToPalette) {
      try {
        const nbtResult = readNBTInline(data, offset);
        offset = nbtResult.newOffset;
        const overlayState = nbtResult.nbt?.Name || 'minecraft:air';
        blockStatePalette.push(overlayState);
      } catch (e) {}
    } else {
      offset += 4;
    }
  }
  
  if (minorVersion < 1 && (overlayParams & 2) !== 0) {
    offset += 4;
  }
  
  const stillUsesColorTypes = minorVersion < 5 || majorVersion <= 2;
  const savedColorType = stillUsesColorTypes ? (overlayParams >> 8) & 3 : 0;
  if (savedColorType === 2 || (overlayParams & 4) !== 0) {
    offset += 4;
  }
  
  if (minorVersion < 8 && (overlayParams & 8) !== 0) {
    offset += 4;
  }
  
  return { newOffset: offset };
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
      return { value: null, newOffset: offset };
  }
}

app.listen(PORT, () => {
  console.log(`Xaero Map Server running at http://localhost:${PORT}`);
  console.log(`Default directory: ${mapDirectory}`);
});
