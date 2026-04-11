import { MapRenderer, ViewportBounds } from './renderer/MapRenderer';
import type { MapRegion } from './core/types';

const API_BASE = 'http://localhost:3001/api';
const MAX_LOADED_REGIONS = 100;
const CONCURRENT_LOADS = 4;
const STORAGE_KEY_DIR = 'xaero_map_directory';

class XaeroMapViewer {
  private renderer: MapRenderer;
  private currentWorld: string | null = null;
  private currentDim: string | null = null;
  private currentMapType: string | null = null;
  private allRegions: {x: number, z: number}[] = [];
  private loadingRegions: Set<string> = new Set();
  private mapTypes: {name: string, path: string}[] = [];
  private regionAccessTime: Map<string, number> = new Map();
  private isLoading: boolean = false;
  private loadQueue: Array<() => Promise<void>> = [];
  private activeLoads: number = 0;

  constructor() {
    const canvas = document.getElementById('mapCanvas') as HTMLCanvasElement;
    this.renderer = new MapRenderer(canvas);
    
    this.renderer.setOnViewportChange((bounds) => this.onViewportChange(bounds));
    
    this.setupUI();
    this.loadCachedDirectory();
    setInterval(() => this.unloadOldRegions(), 10000);
  }

  private loadCachedDirectory(): void {
    const cachedDir = localStorage.getItem(STORAGE_KEY_DIR);
    const dirInput = document.getElementById('dirInput') as HTMLInputElement;
    
    if (cachedDir) {
      if (dirInput) dirInput.value = cachedDir;
      this.setDirectory(cachedDir, true);
    } else {
      this.checkServerConnection();
    }
  }

  private unloadOldRegions(): void {
    if (this.allRegions.length === 0) return;
    
    const now = Date.now();
    const bounds = this.renderer.getViewportBounds();
    const visibleKeys = new Set<string>();
    
    for (let x = bounds.startX; x <= bounds.endX; x++) {
      for (let z = bounds.startZ; z <= bounds.endZ; z++) {
        visibleKeys.add(`${x},${z}`);
      }
    }
    
    const toUnload: string[] = [];
    for (const [key, time] of this.regionAccessTime) {
      if (!visibleKeys.has(key) && now - time > 30000) {
        toUnload.push(key);
      }
    }
    
    if (toUnload.length > 0) {
      console.log(`Unloading ${toUnload.length} old regions`);
      for (const key of toUnload) {
        this.regionAccessTime.delete(key);
      }
    }
  }

  private async checkServerConnection(): Promise<void> {
    try {
      const response = await fetch(`${API_BASE}/worlds`);
      if (response.ok || response.status === 400) {
        this.updateStatus('已连接到服务器，请输入地图目录');
      }
    } catch {
      this.updateStatus('服务器未启动，请运行 node server/index.js');
    }
  }

  private setupUI(): void {
    const worldSelector = document.getElementById('worldSelector') as HTMLSelectElement;
    const dimSelector = document.getElementById('dimSelector') as HTMLSelectElement;
    const mapTypeSelector = document.getElementById('mapTypeSelector') as HTMLSelectElement;
    const zoomSlider = document.getElementById('zoomSlider') as HTMLInputElement;
    const gotoBtn = document.getElementById('gotoCoords') as HTMLButtonElement;
    const setDirBtn = document.getElementById('setDirBtn') as HTMLButtonElement;
    const dirInput = document.getElementById('dirInput') as HTMLInputElement;
    
    worldSelector?.addEventListener('change', () => this.handleWorldChange());
    dimSelector?.addEventListener('change', () => this.handleDimChange());
    mapTypeSelector?.addEventListener('change', () => this.handleMapTypeChange());
    zoomSlider?.addEventListener('input', (e) => {
      const value = parseInt((e.target as HTMLInputElement).value, 10);
      this.renderer.setScale(Math.pow(2, value));
    });
    gotoBtn?.addEventListener('click', () => this.handleGotoCoords());
    setDirBtn?.addEventListener('click', () => this.setDirectory(dirInput.value, false));
    
    this.showLoading(false);
  }

  private updateStatus(message: string): void {
    const statusEl = document.getElementById('status');
    if (statusEl) {
      statusEl.textContent = message;
    }
  }

  private async setDirectory(directory: string, isAutoLoad: boolean): Promise<void> {
    if (!directory) {
      if (!isAutoLoad) alert('请输入地图目录路径');
      return;
    }
    
    if (this.isLoading) return;
    this.isLoading = true;
    
    this.showLoading(true);
    this.updateStatus('正在设置目录...');
    
    try {
      const response = await fetch(`${API_BASE}/set-directory`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ directory })
      });
      
      if (!response.ok) {
        const error = await response.json();
        if (!isAutoLoad) alert(error.error || '设置目录失败');
        this.updateStatus('目录设置失败');
        return;
      }
      
      localStorage.setItem(STORAGE_KEY_DIR, directory);
      this.updateStatus(`目录已设置`);
      await this.loadWorlds(true);
    } catch (e) {
      if (!isAutoLoad) alert('连接服务器失败');
      this.updateStatus('连接服务器失败');
    } finally {
      this.showLoading(false);
      this.isLoading = false;
    }
  }

  private async loadWorlds(autoSelect: boolean = false): Promise<void> {
    try {
      const response = await fetch(`${API_BASE}/worlds`);
      const worlds: string[] = await response.json();
      
      const selector = document.getElementById('worldSelector') as HTMLSelectElement;
      const container = document.getElementById('worldSelect') as HTMLElement;
      
      if (!selector || !container) return;
      
      selector.innerHTML = '<option value="">选择世界</option>';
      
      if (worlds.length === 0) {
        container.style.display = 'none';
        this.updateStatus('未找到世界');
        return;
      }
      
      container.style.display = 'block';
      
      for (const world of worlds) {
        const option = document.createElement('option');
        option.value = world;
        option.textContent = world;
        selector.appendChild(option);
      }
      
      this.updateStatus(`找到 ${worlds.length} 个世界`);
      
      if (autoSelect && worlds.length === 1) {
        selector.value = worlds[0];
        await this.handleWorldChange(true);
      }
    } catch (e) {
      console.error('Failed to load worlds:', e);
    }
  }

  private async handleWorldChange(autoSelect: boolean = false): Promise<void> {
    const selector = document.getElementById('worldSelector') as HTMLSelectElement;
    this.currentWorld = selector.value;
    
    if (!this.currentWorld) return;
    
    this.updateStatus('正在加载维度...');
    
    try {
      const response = await fetch(`${API_BASE}/worlds/${encodeURIComponent(this.currentWorld)}/dimensions`);
      const dimensions: {name: string, path: string}[] = await response.json();
      
      const dimSelector = document.getElementById('dimSelector') as HTMLSelectElement;
      const container = document.getElementById('dimSelect') as HTMLElement;
      
      if (!dimSelector || !container) return;
      
      dimSelector.innerHTML = '<option value="">选择维度</option>';
      
      if (dimensions.length === 0) {
        container.style.display = 'none';
        this.updateStatus('未找到维度');
        return;
      }
      
      container.style.display = 'block';
      
      for (const dim of dimensions) {
        const option = document.createElement('option');
        option.value = dim.path;
        option.textContent = dim.name;
        dimSelector.appendChild(option);
      }
      
      this.updateStatus(`找到 ${dimensions.length} 个维度`);
      
      if (autoSelect || dimensions.length === 1) {
        const overworld = dimensions.find(d => d.path === 'null' || d.name.includes('主世界') || d.name === 'Overworld');
        const toSelect = overworld || dimensions[0];
        dimSelector.value = toSelect.path;
        await this.handleDimChange(true);
      }
    } catch (e) {
      console.error('Failed to load dimensions:', e);
    }
  }

  private async handleDimChange(autoSelect: boolean = false): Promise<void> {
    const selector = document.getElementById('dimSelector') as HTMLSelectElement;
    this.currentDim = selector.value;
    
    if (!this.currentWorld || !this.currentDim) return;
    
    this.updateStatus('正在加载地图类型...');
    
    try {
      const response = await fetch(`${API_BASE}/worlds/${encodeURIComponent(this.currentWorld)}/dimensions/${encodeURIComponent(this.currentDim)}/map-types`);
      this.mapTypes = await response.json();
      
      const mapTypeSelector = document.getElementById('mapTypeSelector') as HTMLSelectElement;
      const container = document.getElementById('mapTypeSelect') as HTMLElement;
      
      if (!mapTypeSelector || !container) return;
      
      mapTypeSelector.innerHTML = '<option value="">选择地图类型</option>';
      
      if (this.mapTypes.length === 0) {
        container.style.display = 'none';
        await this.loadRegionList();
        return;
      }
      
      container.style.display = 'block';
      
      for (const mt of this.mapTypes) {
        const option = document.createElement('option');
        option.value = mt.path;
        option.textContent = mt.name;
        mapTypeSelector.appendChild(option);
      }
      
      if (autoSelect || this.mapTypes.length === 1) {
        const defaultMap = this.mapTypes.find(m => m.path.includes('default') || m.name === '默认') || this.mapTypes[0];
        mapTypeSelector.value = defaultMap.path;
        this.currentMapType = defaultMap.path;
        await this.loadRegionList();
      } else {
        this.updateStatus(`找到 ${this.mapTypes.length} 个地图类型`);
      }
    } catch (e) {
      console.error('Failed to load map types:', e);
      this.updateStatus('获取地图类型失败');
    }
  }

  private async handleMapTypeChange(): Promise<void> {
    const selector = document.getElementById('mapTypeSelector') as HTMLSelectElement;
    this.currentMapType = selector.value;
    
    if (this.currentMapType) {
      await this.loadRegionList();
    }
  }

  private async loadRegionList(): Promise<void> {
    if (!this.currentWorld || !this.currentDim) return;
    
    this.showLoading(true);
    this.renderer.clearRegions();
    this.allRegions = [];
    this.loadingRegions.clear();
    this.regionAccessTime.clear();
    this.loadQueue = [];
    
    try {
      let url = `${API_BASE}/worlds/${encodeURIComponent(this.currentWorld)}/dimensions/${encodeURIComponent(this.currentDim)}/regions`;
      if (this.currentMapType) {
        url += `?mapType=${encodeURIComponent(this.currentMapType)}`;
      }
      
      const response = await fetch(url);
      const data = await response.json();
      this.allRegions = data.regions || [];
      
      if (data.mapType) {
        this.currentMapType = data.mapType;
      }
      
      this.updateStatus(`发现 ${this.allRegions.length} 个区域文件`);
      
      this.renderer.centerOn(0, 0);
      
      this.loadVisibleRegions();
    } catch (e) {
      console.error('Failed to load regions:', e);
    } finally {
      this.showLoading(false);
    }
  }

  private onViewportChange(_bounds: ViewportBounds): void {
    this.loadVisibleRegions();
  }

  private loadVisibleRegions(): void {
    if (!this.currentWorld || !this.currentDim) return;
    
    const stats = this.renderer.getStats();
    const currentCount = stats.regionCount;
    
    if (currentCount >= MAX_LOADED_REGIONS) {
      const keys = Array.from(this.regionAccessTime.entries())
        .filter(([key]) => {
          const bounds = this.renderer.getViewportBounds();
          const [x, z] = key.split(',').map(Number);
          return x < bounds.startX || x > bounds.endX || z < bounds.startZ || z > bounds.endZ;
        })
        .sort((a, b) => a[1] - b[1]);
      
      const toUnload = keys.slice(0, Math.min(10, keys.length));
      for (const [key] of toUnload) {
        this.regionAccessTime.delete(key);
      }
    }
    
    const bounds = this.renderer.getViewportBounds();
    
    for (let x = bounds.startX; x <= bounds.endX; x++) {
      for (let z = bounds.startZ; z <= bounds.endZ; z++) {
        const key = `${x},${z}`;
        if (this.regionAccessTime.has(key)) {
          this.regionAccessTime.set(key, Date.now());
        }
      }
    }
    
    const regionsToLoad: {x: number, z: number}[] = [];
    
    for (let x = bounds.startX; x <= bounds.endX; x++) {
      for (let z = bounds.startZ; z <= bounds.endZ; z++) {
        const key = `${x},${z}`;
        if (!this.renderer.hasRegion(x, z) && !this.loadingRegions.has(key)) {
          if (this.allRegions.length === 0) {
            regionsToLoad.push({x, z});
          } else {
            const existsInList = this.allRegions.some(r => r.x === x && r.z === z);
            if (existsInList) {
              regionsToLoad.push({x, z});
            }
          }
        }
      }
    }
    
    if (regionsToLoad.length === 0) return;
    
    const availableSlots = MAX_LOADED_REGIONS - this.renderer.getStats().regionCount;
    if (availableSlots <= 0) return;
    
    const toLoad = regionsToLoad.slice(0, Math.min(availableSlots, CONCURRENT_LOADS * 2));
    
    for (const region of toLoad) {
      this.queueLoad(region.x, region.z);
    }
    
    this.processQueue();
  }

  private queueLoad(x: number, z: number): void {
    const key = `${x},${z}`;
    if (this.loadingRegions.has(key)) return;
    
    this.loadingRegions.add(key);
    
    this.loadQueue.push(async () => {
      await this.loadSingleRegion(x, z);
    });
  }

  private async processQueue(): Promise<void> {
    while (this.loadQueue.length > 0 && this.activeLoads < CONCURRENT_LOADS) {
      const task = this.loadQueue.shift();
      if (task) {
        this.activeLoads++;
        task().finally(() => {
          this.activeLoads--;
          this.processQueue();
        });
      }
    }
  }

  private async loadSingleRegion(x: number, z: number): Promise<void> {
    const key = `${x},${z}`;
    
    try {
      let url = `${API_BASE}/region?world=${encodeURIComponent(this.currentWorld!)}&dim=${encodeURIComponent(this.currentDim!)}&x=${x}&z=${z}`;
      if (this.currentMapType) {
        url += `&mapType=${encodeURIComponent(this.currentMapType)}`;
      }
      
      const response = await fetch(url);
      if (response.ok) {
        const region: MapRegion = await response.json();
        this.renderer.addRegion(region);
        this.regionAccessTime.set(key, Date.now());
        this.renderer.render();
        this.updateStats();
      }
    } catch (e) {
      console.error(`Failed to load region ${x},${z}:`, e);
    } finally {
      this.loadingRegions.delete(key);
    }
  }

  private handleGotoCoords(): void {
    const xInput = document.getElementById('targetX') as HTMLInputElement;
    const zInput = document.getElementById('targetZ') as HTMLInputElement;
    
    if (!xInput || !zInput) return;
    
    const x = parseInt(xInput.value, 10);
    const z = parseInt(zInput.value, 10);
    
    if (!isNaN(x) && !isNaN(z)) {
      this.renderer.centerOn(x, z);
    }
  }

  private showLoading(show: boolean): void {
    const loading = document.getElementById('loading');
    if (loading) {
      loading.style.display = show ? 'flex' : 'none';
    }
  }

  private updateStats(): void {
    const stats = document.getElementById('stats');
    if (stats) {
      const { regionCount, loadedPixels, cacheSize } = this.renderer.getStats();
      stats.innerHTML = `
        <div>已加载区域: ${regionCount} / ${this.allRegions.length}</div>
        <div>缓存: ${cacheSize}</div>
        <div>像素数: ${loadedPixels.toLocaleString()}</div>
      `;
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  new XaeroMapViewer();
});
