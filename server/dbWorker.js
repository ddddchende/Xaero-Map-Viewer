import { parentPort, workerData } from 'worker_threads';
import Database from 'better-sqlite3';
import zlib from 'zlib';

let db = null;
let dbPath = workerData?.dbPath || null;

function initDatabase(path) {
  if (db) {
    db.close();
  }
  dbPath = path;
  db = new Database(path);
  db.pragma('journal_mode = WAL');
  db.pragma('synchronous = OFF');
  db.pragma('cache_size = -256000');
  db.pragma('temp_store = MEMORY');
  db.pragma('mmap_size = 536870912');
  db.pragma('locking_mode = EXCLUSIVE');
  
  db.exec(`
    CREATE TABLE IF NOT EXISTS region_cache (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      cache_key TEXT UNIQUE NOT NULL,
      data BLOB NOT NULL,
      compressed INTEGER DEFAULT 1,
      created_at INTEGER DEFAULT (strftime('%s', 'now')),
      accessed_at INTEGER DEFAULT (strftime('%s', 'now'))
    );
    CREATE INDEX IF NOT EXISTS idx_cache_key ON region_cache(cache_key);
  `);
  
  return true;
}

function compressData(data) {
  return zlib.gzipSync(data, { level: 1 });
}

function decompressData(data) {
  return zlib.gunzipSync(data);
}

parentPort.on('message', (msg) => {
  const { taskId, type, key, data, dbPath: newDbPath } = msg;
  
  try {
    if (type === 'init') {
      const result = initDatabase(newDbPath);
      parentPort.postMessage({ taskId, result });
      return;
    }
    
    if (type === 'read') {
      if (!db) {
        parentPort.postMessage({ taskId, result: null });
        return;
      }
      
      const stmt = db.prepare('SELECT data, compressed FROM region_cache WHERE cache_key = ?');
      const row = stmt.get(key);
      
      if (row && row.data && row.data.length > 0) {
        const decompressed = row.compressed ? decompressData(row.data) : row.data;
        parentPort.postMessage({ taskId, result: { data: decompressed, found: true } }, [decompressed.buffer]);
      } else {
        if (row) {
          const deleteStmt = db.prepare('DELETE FROM region_cache WHERE cache_key = ?');
          deleteStmt.run(key);
        }
        parentPort.postMessage({ taskId, result: { data: null, found: false } });
      }
      return;
    }
    
    if (type === 'write') {
      if (!db) {
        parentPort.postMessage({ taskId, result: false });
        return;
      }
      
      const compressed = compressData(data);
      const stmt = db.prepare('INSERT OR REPLACE INTO region_cache (cache_key, data, compressed) VALUES (?, ?, 1)');
      stmt.run(key, compressed);
      parentPort.postMessage({ taskId, result: true });
      return;
    }
    
    if (type === 'delete') {
      if (!db) {
        parentPort.postMessage({ taskId, result: false });
        return;
      }
      
      const stmt = db.prepare('DELETE FROM region_cache WHERE cache_key = ?');
      stmt.run(key);
      parentPort.postMessage({ taskId, result: true });
      return;
    }
    
    if (type === 'close') {
      if (db) {
        db.close();
        db = null;
      }
      parentPort.postMessage({ taskId, result: true });
      return;
    }
    
    if (type === 'getStats') {
      if (!db) {
        parentPort.postMessage({ taskId, result: { count: 0, totalSize: 0 } });
        return;
      }
      
      const countRow = db.prepare('SELECT COUNT(*) as count FROM region_cache').get();
      const sizeRow = db.prepare('SELECT SUM(LENGTH(data)) as totalSize FROM region_cache').get();
      parentPort.postMessage({ taskId, result: { count: countRow?.count || 0, totalSize: sizeRow?.totalSize || 0 } });
      return;
    }
    
    if (type === 'clearAll') {
      if (!db) {
        parentPort.postMessage({ taskId, result: true });
        return;
      }
      
      db.exec('DELETE FROM region_cache');
      db.exec('VACUUM');
      parentPort.postMessage({ taskId, result: true });
      return;
    }
    
  } catch (error) {
    parentPort.postMessage({ taskId, error: error.message });
  }
});

if (workerData?.dbPath) {
  try {
    initDatabase(workerData.dbPath);
    console.log(`[DB Worker] Database initialized: ${workerData.dbPath}`);
  } catch (error) {
    console.error(`[DB Worker] Failed to initialize database: ${error.message}`);
  }
}
