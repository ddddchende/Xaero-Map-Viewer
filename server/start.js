import { spawn } from 'child_process';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const configFile = join(__dirname, 'server_config.json');
const serverFile = join(__dirname, 'index.js');

let config = {
  port: 3001,
  maxMemoryMB: 4096,
  maxCacheEntries: 64,
  maxConcurrentLoads: 32,
  maxBatchRegions: 64
};

try {
  const loaded = JSON.parse(readFileSync(configFile, 'utf-8'));
  config = { ...config, ...loaded };
} catch {}

console.log('============================================');
console.log('Xaro Map Server');
console.log('============================================');
console.log(`Memory Limit: ${config.maxMemoryMB} MB`);
console.log(`Cache Entries: ${config.maxCacheEntries}`);
console.log(`Concurrent Loads: ${config.maxConcurrentLoads}`);
console.log(`Batch Regions: ${config.maxBatchRegions}`);
console.log('============================================');
console.log('');

const child = spawn('node', [
  `--max-old-space-size=${config.maxMemoryMB}`,
  serverFile
], {
  stdio: 'inherit',
  env: { ...process.env, PORT: String(config.port) }
});

child.on('error', (err) => {
  console.error('Failed to start server:', err);
  process.exit(1);
});

child.on('exit', (code) => {
  process.exit(code || 0);
});
