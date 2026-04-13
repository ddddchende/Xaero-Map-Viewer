import { MapRenderer, ViewportBounds } from './renderer/MapRenderer';

const DEFAULT_SERVER = 'localhost:3001';
const STORAGE_KEY_DIR = 'xaero_map_directory';
const STORAGE_KEY_SERVER = 'xaero_map_server';
const STORAGE_KEY_MAP_STATE = 'xaero_map_state';
const STORAGE_KEY_SECTIONS = 'xaero_map_sections';
const STORAGE_KEY_CONCURRENT = 'xaero_map_concurrent';
const DEFAULT_CONCURRENT_LOADS = 256;

let API_BASE = `http://${DEFAULT_SERVER}/api`;

interface MapState {
  world: string | null;
  dim: string | null;
  mapType: string | null;
  caveMode: number;
  caveStart: number;
  scale: number;
  centerX: number;
  centerZ: number;
}

class XaeroMapViewer {
  private renderer: MapRenderer;
  private currentWorld: string | null = null;
  private currentDim: string | null = null;
  private currentMapType: string | null = null;
  private currentCaveMode: number = 0;
  private currentCaveStart: number = 64;
  private concurrentLoads: number = DEFAULT_CONCURRENT_LOADS;
  private allRegions: {x: number, z: number}[] = [];
  private allRegionSet: Set<string> = new Set();
  private loadingRegions: Set<string> = new Set();
  private mapTypes: {name: string, path: string}[] = [];
  private regionAccessTime: Map<string, number> = new Map();
  private isLoading: boolean = false;
  private abortController: AbortController | null = null;
  private savedMapState: MapState | null = null;
  private cacheSizeTimeout: ReturnType<typeof setTimeout> | null = null;
  private viewportChangeTimeout: ReturnType<typeof setTimeout> | null = null;
  private lastViewportBounds: ViewportBounds | null = null;
  private currentRequestId: number = 0;

  constructor() {
    this.loadServerConfig();
    this.loadMapState();
    this.loadConcurrentSetting();
    
    const canvas = document.getElementById('mapCanvas') as HTMLCanvasElement;
    this.renderer = new MapRenderer(canvas);
    
    this.renderer.setOnViewportChange((bounds) => this.onViewportChange(bounds));
    this.renderer.setOnLodChange(() => this.onLodChange());
    
    this.setupUI();
    this.loadCachedDirectory();
    this.startAutoLoad();
  }
  
  private loadServerConfig(): void {
    const savedServer = localStorage.getItem(STORAGE_KEY_SERVER) || DEFAULT_SERVER;
    API_BASE = `http://${savedServer}/api`;
    const serverInput = document.getElementById('serverInput') as HTMLInputElement;
    if (serverInput) serverInput.value = savedServer;
    this.updateCurrentServerDisplay(savedServer);
  }
  
  private loadMapState(): void {
    try {
      const saved = localStorage.getItem(STORAGE_KEY_MAP_STATE);
      if (saved) {
        this.savedMapState = JSON.parse(saved);
      }
    } catch {
      this.savedMapState = null;
    }
  }
  
  private saveMapState(): void {
    const viewState = this.renderer.getViewState();
    const state: MapState = {
      world: this.currentWorld,
      dim: this.currentDim,
      mapType: this.currentMapType,
      caveMode: this.currentCaveMode,
      caveStart: this.currentCaveStart,
      scale: viewState.scale,
      centerX: viewState.centerX,
      centerZ: viewState.centerZ
    };
    localStorage.setItem(STORAGE_KEY_MAP_STATE, JSON.stringify(state));
  }
  
  private loadConcurrentSetting(): void {
    try {
      const saved = localStorage.getItem(STORAGE_KEY_CONCURRENT);
      if (saved) {
        const value = parseInt(saved, 10);
        if (!isNaN(value) && value >= 16 && value <= 512) {
          this.concurrentLoads = value;
        }
      }
    } catch {}
  }
  
  private saveConcurrentSetting(): void {
    localStorage.setItem(STORAGE_KEY_CONCURRENT, String(this.concurrentLoads));
  }
  
  private updateCurrentServerDisplay(server: string): void {
    const currentServerEl = document.getElementById('currentServer');
    if (currentServerEl) {
      currentServerEl.textContent = `当前: ${server}`;
    }
  }
  
  private setServerAddress(address: string): void {
    if (!address) {
      alert('请输入服务端地址');
      return;
    }
    
    API_BASE = `http://${address}/api`;
    localStorage.setItem(STORAGE_KEY_SERVER, address);
    this.updateCurrentServerDisplay(address);
    this.updateStatus('正在连接服务器...');
    this.checkServerConnection();
  }

  private startAutoLoad(): void {
    window.setInterval(() => {
      if (this.currentWorld && this.currentDim) {
        this.loadVisibleRegions();
      }
    }, 500);
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

  private async checkServerConnection(): Promise<void> {
    try {
      const response = await fetch(`${API_BASE}/worlds`);
      if (response.ok || response.status === 400) {
        this.updateStatus('已连接到服务器');
      }
    } catch {
      this.updateStatus('无法连接服务器，请检查地址');
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
    const setServerBtn = document.getElementById('setServerBtn') as HTMLButtonElement;
    const serverInput = document.getElementById('serverInput') as HTMLInputElement;
    const toggleSidebar = document.getElementById('toggleSidebar');
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    
    worldSelector?.addEventListener('change', () => this.handleWorldChange());
    dimSelector?.addEventListener('change', () => this.handleDimChange());
    mapTypeSelector?.addEventListener('change', () => this.handleMapTypeChange());
    zoomSlider?.addEventListener('input', (e) => {
      const value = parseFloat((e.target as HTMLInputElement).value);
      this.renderer.setScale(Math.pow(2, value));
    });
    gotoBtn?.addEventListener('click', () => this.handleGotoCoords());
    setDirBtn?.addEventListener('click', () => this.setDirectory(dirInput.value, false));
    setServerBtn?.addEventListener('click', () => this.setServerAddress(serverInput.value));
    
    toggleSidebar?.addEventListener('click', () => {
      sidebar?.classList.toggle('open');
      overlay?.classList.toggle('open');
    });
    
    overlay?.addEventListener('click', () => {
      sidebar?.classList.remove('open');
      overlay?.classList.remove('open');
    });
    
    this.setupSectionToggle();
    
    const yHeightSlider = document.getElementById('yHeightSlider') as HTMLInputElement;
    yHeightSlider?.addEventListener('input', (e) => {
      const value = parseInt((e.target as HTMLInputElement).value);
      this.currentCaveStart = value;
      const yHeightValue = document.getElementById('yHeightValue');
      if (yHeightValue) yHeightValue.textContent = value.toString();
      this.renderer.clearRegions();
      this.loadVisibleRegions();
    });
    
    const caveModeSelector = document.getElementById('caveModeSelector') as HTMLSelectElement;
    caveModeSelector?.addEventListener('change', (e) => {
      this.currentCaveMode = parseInt((e.target as HTMLSelectElement).value);
      const caveStartGroup = document.getElementById('caveStartGroup');
      if (caveStartGroup) {
        caveStartGroup.style.display = this.currentCaveMode === 1 ? 'block' : 'none';
      }
      this.saveMapState();
      this.loadRegionList();
    });
    
    const caveStartSlider = document.getElementById('caveStartSlider') as HTMLInputElement;
    caveStartSlider?.addEventListener('input', (e) => {
      const value = parseInt((e.target as HTMLInputElement).value);
      this.currentCaveStart = value;
      const caveStartValue = document.getElementById('caveStartValue');
      if (caveStartValue) caveStartValue.textContent = value.toString();
      this.saveMapState();
      this.loadRegionList();
    });
    
    const setCacheDirBtn = document.getElementById('setCacheDirBtn') as HTMLButtonElement;
    const cacheDirInput = document.getElementById('cacheDirInput') as HTMLInputElement;
    const clearCacheBtn = document.getElementById('clearCacheBtn') as HTMLButtonElement;
    
    setCacheDirBtn?.addEventListener('click', () => this.setCacheDirectory(cacheDirInput.value));
    clearCacheBtn?.addEventListener('click', () => this.clearCache());
    
    const concurrentSlider = document.getElementById('concurrentSlider') as HTMLInputElement;
    const concurrentValue = document.getElementById('concurrentValue');
    if (concurrentSlider && concurrentValue) {
      concurrentSlider.value = String(this.concurrentLoads);
      concurrentValue.textContent = String(this.concurrentLoads);
      
      concurrentSlider.addEventListener('input', (e) => {
        const value = parseInt((e.target as HTMLInputElement).value);
        this.concurrentLoads = value;
        concurrentValue.textContent = String(value);
        this.saveConcurrentSetting();
      });
    }
    
    this.loadCacheDirectory();
    this.loadCacheSize();
    
    this.showLoading(false);
  }

  private setupSectionToggle(): void {
    const savedSections = this.loadSectionStates();
    
    document.querySelectorAll('.section-title[data-toggle]').forEach(title => {
      const sectionId = title.getAttribute('data-toggle');
      const section = title.closest('.section') as HTMLElement;
      const content = section?.querySelector('.section-content') as HTMLElement;
      
      if (section && content) {
        if (savedSections[sectionId!] === false) {
          section.classList.add('collapsed');
          content.style.maxHeight = '0';
        } else {
          content.style.maxHeight = 'none';
        }
        
        title.addEventListener('click', () => {
          const isCollapsed = section.classList.toggle('collapsed');
          if (isCollapsed) {
            content.style.maxHeight = '0';
          } else {
            content.style.maxHeight = 'none';
          }
          this.saveSectionStates();
        });
      }
    });
  }
  
  private updateSectionContentHeight(sectionElement: HTMLElement): void {
    const content = sectionElement.querySelector('.section-content') as HTMLElement;
    if (content && !sectionElement.classList.contains('collapsed')) {
      content.style.maxHeight = 'none';
    }
  }

  private loadSectionStates(): Record<string, boolean> {
    try {
      const saved = localStorage.getItem(STORAGE_KEY_SECTIONS);
      return saved ? JSON.parse(saved) : {};
    } catch {
      return {};
    }
  }

  private saveSectionStates(): void {
    const states: Record<string, boolean> = {};
    document.querySelectorAll('.section[data-section]').forEach(section => {
      const sectionId = section.getAttribute('data-section');
      if (sectionId) {
        states[sectionId] = !section.classList.contains('collapsed');
      }
    });
    localStorage.setItem(STORAGE_KEY_SECTIONS, JSON.stringify(states));
  }

  private updateStatus(message: string): void {
    const statusEl = document.getElementById('status');
    if (statusEl) {
      statusEl.textContent = message;
    }
  }

  private async loadCacheDirectory(): Promise<void> {
    try {
      const response = await fetch(`${API_BASE}/cache-directory`);
      if (response.ok) {
        const data = await response.json();
        const cacheDirInput = document.getElementById('cacheDirInput') as HTMLInputElement;
        if (cacheDirInput && data.directory) {
          cacheDirInput.value = data.directory;
        }
      }
    } catch {
      // ignore
    }
  }

  private formatCacheSize(sizeMB: number): string {
    if (sizeMB < 1024) {
      return `${sizeMB} MB`;
    }
    const gb = sizeMB / 1024;
    if (gb < 10) {
      return `${gb.toFixed(1)} GB`;
    }
    return `${Math.round(gb)} GB`;
  }

  private async loadCacheSize(): Promise<void> {
    if (this.cacheSizeTimeout) {
      clearTimeout(this.cacheSizeTimeout);
    }
    
    this.cacheSizeTimeout = setTimeout(async () => {
      try {
        const response = await fetch(`${API_BASE}/cache-size`);
        if (response.ok) {
          const data = await response.json();
          const cacheSizeEl = document.getElementById('cacheSize');
          if (cacheSizeEl) {
            cacheSizeEl.textContent = `缓存: ${this.formatCacheSize(data.sizeMB)}`;
          }
        }
      } catch {
        const cacheSizeEl = document.getElementById('cacheSize');
        if (cacheSizeEl) {
          cacheSizeEl.textContent = '缓存: 计算失败';
        }
      }
    }, 500);
  }

  private async setCacheDirectory(directory: string): Promise<void> {
    if (!directory) {
      alert('请输入缓存目录路径');
      return;
    }
    
    try {
      const response = await fetch(`${API_BASE}/set-cache-directory`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ directory })
      });
      
      if (!response.ok) {
        const error = await response.json();
        alert(error.error || '设置缓存目录失败');
        return;
      }
      
      alert('缓存目录已设置');
    } catch (error) {
      alert('设置缓存目录失败: ' + error);
    }
  }

  private async clearCache(): Promise<void> {
    if (!confirm('确定要清除所有缓存吗？')) return;
    
    try {
      const response = await fetch(`${API_BASE}/cache-directory`, {
        method: 'DELETE'
      });
      
      if (!response.ok) {
        const error = await response.json();
        alert(error.error || '清除缓存失败');
        return;
      }
      
      this.renderer.clearRegions();
      this.loadCacheSize();
      alert('缓存已清除');
    } catch (error) {
      alert('清除缓存失败: ' + error);
    }
  }

  private async setDirectory(directory: string, isAutoLoad: boolean): Promise<void> {
    if (!directory) {
      if (!isAutoLoad) alert('请输入地图目录路径');
      return;
    }
    
    if (this.isLoading) return;
    this.isLoading = true;
    
    this.cancelServerRequest();
    
    if (this.abortController) {
      this.abortController.abort();
    }
    this.abortController = new AbortController();
    
    this.currentWorld = null;
    this.currentDim = null;
    this.currentMapType = null;
    this.allRegions = [];
    this.allRegionSet.clear();
    this.loadingRegions.clear();
    this.regionAccessTime.clear();
    this.mapTypes = [];
    
    this.renderer.clearAllRegions();
    
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
      const container = document.getElementById('worldSection') as HTMLElement;
      
      if (!selector || !container) return;
      
      selector.innerHTML = '';
      
      if (worlds.length === 0) {
        container.style.display = 'none';
        this.updateStatus('未找到世界');
        return;
      }
      
      container.style.display = 'block';
      this.updateSectionContentHeight(container);
      
      for (const world of worlds) {
        const option = document.createElement('option');
        option.value = world;
        option.textContent = world;
        selector.appendChild(option);
      }
      
      this.updateStatus(`找到 ${worlds.length} 个世界`);
      
      const savedWorld = this.savedMapState?.world;
      const savedWorldExists = savedWorld && worlds.includes(savedWorld);
      
      if (savedWorldExists) {
        selector.value = savedWorld!;
        await this.handleWorldChange(true);
      } else if (autoSelect && worlds.length >= 1) {
        selector.value = worlds[0];
        await this.handleWorldChange(true);
      }
    } catch (e) {
      console.error('Failed to load worlds:', e);
    }
  }

  private async handleWorldChange(autoSelect: boolean = false): Promise<void> {
    if (this.abortController) {
      this.abortController.abort();
    }
    this.abortController = new AbortController();
    
    const selector = document.getElementById('worldSelector') as HTMLSelectElement;
    this.currentWorld = selector.value;
    
    if (!this.currentWorld) return;
    
    this.saveMapState();
    this.updateStatus('正在加载维度...');
    
    const signal = this.abortController?.signal;
    
    try {
      const response = await fetch(`${API_BASE}/worlds/${encodeURIComponent(this.currentWorld)}/dimensions`, { signal });
      const dimensions: {name: string, path: string}[] = await response.json();
      
      const dimSelector = document.getElementById('dimSelector') as HTMLSelectElement;
      const container = document.getElementById('worldSection') as HTMLElement;
      
      if (!dimSelector || !container) return;
      
      dimSelector.innerHTML = '';
      
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
      
      const savedDim = this.savedMapState?.world === this.currentWorld ? this.savedMapState.dim : null;
      const savedDimExists = savedDim && dimensions.some(d => d.path === savedDim);
      
      if (savedDimExists) {
        dimSelector.value = savedDim!;
        await this.handleDimChange();
      } else if (autoSelect || dimensions.length === 1) {
        const overworld = dimensions.find(d => d.path === 'null' || d.name.includes('主世界') || d.name === 'Overworld');
        const toSelect = overworld || dimensions[0];
        dimSelector.value = toSelect.path;
        await this.handleDimChange();
      }
    } catch (e: unknown) {
      if (e instanceof Error && e.name === 'AbortError') {
        return;
      }
      console.error('Failed to load dimensions:', e);
    }
  }

  private async handleDimChange(): Promise<void> {
    if (this.abortController) {
      this.abortController.abort();
    }
    this.abortController = new AbortController();
    
    this.currentRequestId++;
    
    const selector = document.getElementById('dimSelector') as HTMLSelectElement;
    this.currentDim = selector.value;
    
    this.renderer.setCurrentDimension(this.currentDim);
    
    if (!this.currentWorld || !this.currentDim) return;
    
    this.saveMapState();
    
    this.renderer.clearAllRegions();
    this.allRegions = [];
    this.allRegionSet.clear();
    this.loadingRegions.clear();
    this.regionAccessTime.clear();
    
    const isNether = this.currentDim.toLowerCase().includes('nether') || 
                     this.currentDim.includes('DIM-1') || 
                     this.currentDim.includes('下界');
    
    const caveModeGroup = document.getElementById('caveModeGroup') as HTMLElement;
    const caveStartGroup = document.getElementById('caveStartGroup') as HTMLElement;
    
    if (caveModeGroup) {
      caveModeGroup.style.display = 'block';
    }
    
    const savedCaveMode = this.savedMapState?.dim === this.currentDim ? this.savedMapState.caveMode : null;
    const savedCaveStart = this.savedMapState?.dim === this.currentDim ? this.savedMapState.caveStart : null;
    
    if (savedCaveMode !== null) {
      this.currentCaveMode = savedCaveMode;
      const caveModeSelector = document.getElementById('caveModeSelector') as HTMLSelectElement;
      if (caveModeSelector) caveModeSelector.value = String(savedCaveMode);
    } else {
      if (isNether) {
        this.currentCaveMode = 2;
        const caveModeSelector = document.getElementById('caveModeSelector') as HTMLSelectElement;
        if (caveModeSelector) caveModeSelector.value = '2';
      } else {
        this.currentCaveMode = 0;
        const caveModeSelector = document.getElementById('caveModeSelector') as HTMLSelectElement;
        if (caveModeSelector) caveModeSelector.value = '0';
      }
    }
    
    if (savedCaveStart !== null && this.currentCaveMode === 1) {
      this.currentCaveStart = savedCaveStart;
      const caveStartSlider = document.getElementById('caveStartSlider') as HTMLInputElement;
      const caveStartValue = document.getElementById('caveStartValue');
      if (caveStartSlider) caveStartSlider.value = String(savedCaveStart);
      if (caveStartValue) caveStartValue.textContent = String(savedCaveStart);
    }
    
    if (caveStartGroup) {
      caveStartGroup.style.display = this.currentCaveMode === 1 ? 'block' : 'none';
    }
    
    this.updateStatus('正在加载地图类型...');
    
    const signal = this.abortController?.signal;
    
    try {
      const response = await fetch(`${API_BASE}/worlds/${encodeURIComponent(this.currentWorld)}/dimensions/${encodeURIComponent(this.currentDim)}/map-types`, { signal });
      this.mapTypes = await response.json();
      
      const mapTypeSelector = document.getElementById('mapTypeSelector') as HTMLSelectElement;
      const container = document.getElementById('worldSection') as HTMLElement;
      
      if (!mapTypeSelector || !container) return;
      
      mapTypeSelector.innerHTML = '';
      
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
      
      const savedMapType = this.savedMapState?.dim === this.currentDim ? this.savedMapState.mapType : null;
      const savedMapTypeExists = savedMapType && this.mapTypes.some(m => m.path === savedMapType);
      
      if (savedMapTypeExists) {
        mapTypeSelector.value = savedMapType!;
        this.currentMapType = savedMapType;
        await this.loadRegionList();
      } else if (this.mapTypes.length === 1) {
        const defaultMap = this.mapTypes[0];
        mapTypeSelector.value = defaultMap.path;
        this.currentMapType = defaultMap.path;
        await this.loadRegionList();
      } else {
        const defaultMap = this.mapTypes.find(m => m.path.includes('default') || m.name === '默认') || this.mapTypes[0];
        mapTypeSelector.value = defaultMap.path;
        this.currentMapType = defaultMap.path;
        await this.loadRegionList();
      }
    } catch (e: unknown) {
      if (e instanceof Error && e.name === 'AbortError') {
        return;
      }
      console.error('Failed to load map types:', e);
      this.updateStatus('获取地图类型失败');
    }
  }

  private handleMapTypeChange(): void {
    const selector = document.getElementById('mapTypeSelector') as HTMLSelectElement;
    this.currentMapType = selector.value;
    
    this.saveMapState();
    
    if (this.currentMapType) {
      this.loadRegionList();
    }
  }

  private async loadRegionList(): Promise<void> {
    if (!this.currentWorld || !this.currentDim) return;
    
    this.showLoading(true);
    this.renderer.clearRegions();
    this.loadingRegions.clear();
    this.regionAccessTime.clear();
    
    const signal = this.abortController?.signal;
    
    try {
      let url = `${API_BASE}/worlds/${encodeURIComponent(this.currentWorld)}/dimensions/${encodeURIComponent(this.currentDim)}/regions`;
      const params = new URLSearchParams();
      if (this.currentMapType) {
        params.set('mapType', this.currentMapType);
      }
      if (this.currentCaveMode > 0) {
        params.set('caveMode', String(this.currentCaveMode));
        if (this.currentCaveMode === 1) {
          params.set('caveStart', String(this.currentCaveStart));
        }
      }
      if (params.toString()) {
        url += '?' + params.toString();
      }
      
      const response = await fetch(url, { signal });
      const data = await response.json();
      this.allRegions = data.regions || [];
      
      this.allRegionSet.clear();
      for (const r of this.allRegions) {
        this.allRegionSet.add(`${r.x},${r.z}`);
      }
      
      this.renderer.setAllRegions(this.allRegionSet);
      
      if (data.mapType) {
        this.currentMapType = data.mapType;
      }
      
      this.updateStatus(`发现 ${this.allRegions.length} 个区域文件`);
      
      if (this.savedMapState?.scale && this.savedMapState?.centerX !== undefined && this.savedMapState?.centerZ !== undefined) {
        this.renderer.setViewState(this.savedMapState.scale, this.savedMapState.centerX, this.savedMapState.centerZ);
        this.savedMapState = null;
      } else {
        this.renderer.centerOn(0, 0);
      }
      
      this.loadVisibleRegions();
    } catch (e: unknown) {
      if (e instanceof Error && e.name === 'AbortError') {
        return;
      }
      console.error('Failed to load regions:', e);
    } finally {
      this.showLoading(false);
    }
  }

  private onViewportChange(bounds: ViewportBounds): void {
    const shouldCancelRequest = this.shouldCancelPendingRequests(bounds);
    
    if (shouldCancelRequest) {
      this.cancelServerRequest();
      if (this.abortController) {
        this.abortController.abort();
      }
      this.abortController = new AbortController();
      this.loadingRegions.clear();
      this.updatePendingRegions();
    }
    
    this.lastViewportBounds = bounds;
    
    if (this.viewportChangeTimeout) {
      clearTimeout(this.viewportChangeTimeout);
    }
    
    this.viewportChangeTimeout = setTimeout(() => {
      this.loadVisibleRegions();
      this.saveMapState();
    }, 50);
  }

  private async cancelServerRequest(): Promise<void> {
    if (this.currentRequestId > 0) {
      try {
        await fetch(`${API_BASE}/cancel-request/${this.currentRequestId}`, { method: 'POST' });
      } catch {}
    }
  }

  private shouldCancelPendingRequests(newBounds: ViewportBounds): boolean {
    if (!this.lastViewportBounds) return false;
    if (this.loadingRegions.size === 0) return false;
    
    const oldBounds = this.lastViewportBounds;
    const dx = Math.abs(newBounds.startX - oldBounds.startX);
    const dz = Math.abs(newBounds.startZ - oldBounds.startZ);
    const threshold = 1;
    
    return dx > threshold || dz > threshold;
  }

  private onLodChange(): void {
    this.cancelServerRequest();
    
    if (this.abortController) {
      this.abortController.abort();
    }
    this.abortController = new AbortController();
    
    this.loadingRegions.clear();
    this.updatePendingRegions();
    this.loadVisibleRegions();
  }

  private loadVisibleRegions(): void {
    if (!this.currentWorld || !this.currentDim) return;
    
    const bounds = this.renderer.getViewportBounds();
    
    for (let x = bounds.startX; x <= bounds.endX; x++) {
      for (let z = bounds.startZ; z <= bounds.endZ; z++) {
        const key = `${x},${z}`;
        if (this.regionAccessTime.has(key)) {
          this.regionAccessTime.set(key, Date.now());
        }
      }
    }
    
    const centerRegionX = Math.floor((bounds.startX + bounds.endX) / 2);
    const centerRegionZ = Math.floor((bounds.startZ + bounds.endZ) / 2);
    
    const regionsToLoad: {x: number, z: number, dist: number}[] = [];
    
    for (let x = bounds.startX; x <= bounds.endX; x++) {
      for (let z = bounds.startZ; z <= bounds.endZ; z++) {
        const key = `${x},${z}`;
        if (this.renderer.hasRegion(x, z)) continue;
        if (this.loadingRegions.has(key)) continue;
        if (this.allRegionSet.size > 0 && !this.allRegionSet.has(key)) continue;
        
        const dist = Math.abs(x - centerRegionX) + Math.abs(z - centerRegionZ);
        regionsToLoad.push({x, z, dist});
      }
    }
    
    if (regionsToLoad.length === 0) return;
    
    regionsToLoad.sort((a, b) => a.dist - b.dist);
    
    const toLoad = regionsToLoad.slice(0, this.concurrentLoads);
    
    this.loadRegionsBatch(toLoad.map(r => ({x: r.x, z: r.z})));
  }

  private updatePendingRegions(): void {
    this.renderer.setPendingRegions(new Set(this.loadingRegions));
  }

  private processRegionsBatch(regions: { rx: number; rz: number; pixelData: Uint8Array }[], lod: number, requestId: number): void {
    const BATCH_SIZE = 4;
    let index = 0;
    
    const processNext = () => {
      if (requestId !== this.currentRequestId) return;
      
      const end = Math.min(index + BATCH_SIZE, regions.length);
      
      for (; index < end; index++) {
        const { rx, rz, pixelData } = regions[index];
        this.renderer.addRegionPixels(rx, rz, pixelData, lod);
        this.regionAccessTime.set(`${rx},${rz}`, Date.now());
      }
      
      this.renderer.render();
      
      if (index < regions.length) {
        requestAnimationFrame(processNext);
      } else {
        this.updateStats();
        this.loadCacheSize();
      }
    };
    
    processNext();
  }

  private async loadRegionsBatch(regions: {x: number, z: number}[]): Promise<void> {
    if (regions.length === 0) return;
    
    const signal = this.abortController?.signal;
    if (signal?.aborted) return;
    
    if (regions.length === 1) {
      const {x, z} = regions[0];
      const key = `${x},${z}`;
      if (this.loadingRegions.has(key)) return;
      this.loadingRegions.add(key);
      this.updatePendingRegions();
      
      try {
        await this.loadSingleRegionPixels(x, z, signal);
      } finally {
        this.loadingRegions.delete(key);
        this.updatePendingRegions();
      }
      return;
    }
    
    const toLoad = regions.filter(r => !this.loadingRegions.has(`${r.x},${r.z}`));
    if (toLoad.length === 0) return;
    
    for (const r of toLoad) {
      this.loadingRegions.add(`${r.x},${r.z}`);
    }
    this.updatePendingRegions();
    
    this.currentRequestId++;
    const requestId = this.currentRequestId;
    
    try {
      const coordsStr = toLoad.map(r => `${r.x},${r.z}`).join(';');
      const currentLod = this.renderer.getCurrentLodLevel();
      const bounds = this.renderer.getViewportBounds();
      let url = `${API_BASE}/batch-regions?requestId=${requestId}&world=${encodeURIComponent(this.currentWorld!)}&dim=${encodeURIComponent(this.currentDim!)}&coords=${encodeURIComponent(coordsStr)}&lod=${currentLod}&viewStartX=${bounds.startX}&viewStartZ=${bounds.startZ}&viewEndX=${bounds.endX}&viewEndZ=${bounds.endZ}`;
      if (this.currentMapType) {
        url += `&mapType=${encodeURIComponent(this.currentMapType)}`;
      }
      if (this.currentCaveMode > 0) {
        url += `&caveMode=${this.currentCaveMode}`;
        if (this.currentCaveMode === 1) {
          url += `&caveStart=${this.currentCaveStart}`;
        }
      }
      
      const response = await fetch(url, { signal });
      if (!response.ok) {
        for (const r of toLoad) {
          this.loadingRegions.delete(`${r.x},${r.z}`);
        }
        return;
      }
      
      const buffer = await response.arrayBuffer();
      const view = new DataView(buffer);
      
      const totalRegions = view.getUint32(0, true);
      const lodValue = view.getUint32(4, true);
      let offset = 8;
      
      const regions: { rx: number; rz: number; pixelData: Uint8Array }[] = [];
      
      for (let i = 0; i < totalRegions; i++) {
        const rx = view.getInt32(offset, true);
        const rz = view.getInt32(offset + 4, true);
        const pixelSize = view.getUint32(offset + 8, true);
        offset += 12;
        
        if (pixelSize > 0) {
          const pixelData = new Uint8Array(buffer.slice(offset, offset + pixelSize));
          regions.push({ rx, rz, pixelData });
        }
        
        offset += pixelSize;
      }
      
      if (requestId !== this.currentRequestId) {
        return;
      }
      
      this.processRegionsBatch(regions, lodValue, requestId);
    } catch (e: unknown) {
      if (e instanceof Error && e.name === 'AbortError') {
        return;
      }
      console.error('Batch load error:', e);
    } finally {
      for (const r of toLoad) {
        this.loadingRegions.delete(`${r.x},${r.z}`);
      }
      this.updatePendingRegions();
    }
  }

  private async loadSingleRegionPixels(x: number, z: number, signal?: AbortSignal): Promise<void> {
    try {
      const currentLod = this.renderer.getCurrentLodLevel();
      let url = `${API_BASE}/region-pixels?world=${encodeURIComponent(this.currentWorld!)}&dim=${encodeURIComponent(this.currentDim!)}&x=${x}&z=${z}&lod=${currentLod}`;
      if (this.currentMapType) {
        url += `&mapType=${encodeURIComponent(this.currentMapType)}`;
      }
      if (this.currentCaveMode > 0) {
        url += `&caveMode=${this.currentCaveMode}`;
        if (this.currentCaveMode === 1) {
          url += `&caveStart=${this.currentCaveStart}`;
        }
      }
      
      const response = await fetch(url, { signal });
      if (response.ok) {
        const pixelData = new Uint8Array(await response.arrayBuffer());
        this.renderer.addRegionPixels(x, z, pixelData, currentLod);
        this.regionAccessTime.set(`${x},${z}`, Date.now());
        this.renderer.render();
        this.updateStats();
        this.loadCacheSize();
      }
    } catch (e: unknown) {
      if (e instanceof Error && e.name === 'AbortError') {
        return;
      }
      console.error(`Failed to load region ${x},${z}:`, e);
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
      const { regionCount, atlasCount, lodLevel } = this.renderer.getStats();
      stats.innerHTML = `
        <div>已加载区域: ${regionCount} / ${this.allRegions.length}</div>
        <div>纹理图集: ${atlasCount}</div>
        <div>LOD: ${lodLevel}</div>
        <div>并发加载: ${this.concurrentLoads}</div>
      `;
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  new XaeroMapViewer();
});
