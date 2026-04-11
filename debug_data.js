const http = require('http');

const url = 'http://localhost:3001/api/region?world=%E5%BD%93%E5%89%8D%E4%B8%96%E7%95%8C&dim=null&x=-1000&z=-1169&mapType=mw%24default';

http.get(url, (res) => {
  let data = '';
  res.on('data', chunk => data += chunk);
  res.on('end', () => {
    const region = JSON.parse(data);
    
    let nullStateBlocks = [];
    
    for (let cx = 0; cx < 8; cx++) {
      for (let cz = 0; cz < 8; cz++) {
        const chunk = region.chunks[cx]?.[cz];
        if (!chunk) continue;
        
        for (let tx = 0; tx < 4; tx++) {
          for (let tz = 0; tz < 4; tz++) {
            const tile = chunk.tiles[tx]?.[tz];
            if (!tile || !tile.blocks) continue;
            
            for (let x = 0; x < 16; x++) {
              for (let z = 0; z < 16; z++) {
                const block = tile.blocks[x]?.[z];
                if (!block) {
                  nullStateBlocks.push({ type: 'no-block', cx, cz, tx, tz, x, z });
                } else if (!block.s) {
                  nullStateBlocks.push({ type: 'null-state', cx, cz, tx, tz, x, z, block });
                }
              }
            }
          }
        }
      }
    }
    
    console.log('Null state blocks:', nullStateBlocks.length);
    console.log('Sample null blocks:', nullStateBlocks.slice(0, 5));
  });
});
