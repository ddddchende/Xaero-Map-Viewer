const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { unzipSync } = require('zlib');

const basePath = 'E:\\Game\\AC1.21.4基础整合包\\.minecraft\\versions\\1.21.1-Fabric 0.16.14\\xaero\\world-map\\Multiplayer_2b2t.cc\\null\\mw$default';
const files = fs.readdirSync(basePath);
const filePath = path.join(basePath, files[0]);
console.log('Reading:', filePath);

const buffer = fs.readFileSync(filePath);

// Use adm-zip or built-in to extract
const JSZip = require('jszip');
const zip = new JSZip();
zip.loadAsync(buffer).then(async (contents) => {
  const fileNames = Object.keys(contents.files);
  console.log('Files in ZIP:', fileNames);
  
  for (const fileName of fileNames) {
    if (fileName.endsWith('.xaero')) {
      const data = await contents.file(fileName).async('uint8array');
      console.log('Extracted file:', fileName, 'size:', data.length);
      console.log('First 30 bytes:', Array.from(data.slice(0, 30)).map(b => b.toString(16).padStart(2, '0')).join(' '));
      
      // Parse the region data
      const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
      let pos = 0;
      
      const firstByte = view.getUint8(pos++);
      console.log('First byte:', firstByte, '(0x' + firstByte.toString(16) + ')');
      
      if (firstByte === 0xFF) {
        const fullVersion = view.getInt32(pos, false);
        console.log('Full version (hex):', '0x' + fullVersion.toString(16).padStart(8, '0'));
        const minorVersion = fullVersion & 0xFFFF;
        const majorVersion = (fullVersion >> 16) & 0xFFFF;
        console.log('Major version:', majorVersion, 'Minor version:', minorVersion);
        pos += 4;
      } else {
        console.log('No version marker');
        pos = 0;
      }
      
      // Read chunk coords
      const chunkCoords = view.getUint8(pos++);
      console.log('Chunk coords byte:', chunkCoords, '(0x' + chunkCoords.toString(16) + ')');
      console.log('Chunk X:', chunkCoords >> 4, 'Chunk Z:', chunkCoords & 0x0F);
      
      break;
    }
  }
}).catch(err => {
  console.error('Error:', err);
});
