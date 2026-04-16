import { MapRenderer, ViewportBounds } from './renderer/MapRenderer';
import type { Waypoint } from './core/types';

const STORAGE_KEY_MAP_STATE = 'xaero_map_state';
const STORAGE_KEY_SECTIONS = 'xaero_map_sections';
const STORAGE_KEY_CONCURRENT = 'xaero_map_concurrent';
const STORAGE_KEY_SHOW_WAYPOINTS = 'xaero_map_show_waypoints';
const STORAGE_KEY_SHOW_DISABLED_WAYPOINTS = 'xaero_map_show_disabled_waypoints';
const DEFAULT_CONCURRENT_LOADS = 256;

const API_BASE = `${window.location.protocol}//${window.location.host}/api`;
const WS_BASE = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}`;

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
  private abortController: AbortController | null = null;
  private cacheSizeTimeout: ReturnType<typeof setTimeout> | null = null;
  private viewportChangeTimeout: ReturnType<typeof setTimeout> | null = null;
  private currentRequestId: number = 0;
  private ws: WebSocket | null = null;
  private wsConnected: boolean = false;
  private wsReconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private pendingWsRequests: Map<number, { resolve: Function; reject: Function }> = new Map();
  private waypoints: Waypoint[] = [];
  private showWaypoints: boolean = true;
  
  private idleUpgradeQueue: {x: number, z: number}[] = [];
  private idleUpgradeInProgress: boolean = false;
  private idleUpgradeBatchSize: number = 16;
  private static readonly IDLE_UPGRADE_DELAY_MS = 100;
  
  private loadingRegionStartTime: Map<string, number> = new Map();
  private static readonly REQUEST_TIMEOUT_MS = 1000; //区域请求超时1秒
  private timeoutCheckInterval: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.loadConcurrentSetting();
    
    const canvas = document.getElementById('mapCanvas') as HTMLCanvasElement;
    this.renderer = new MapRenderer(canvas);
    
    this.renderer.setOnViewportChange((bounds) => this.onViewportChange(bounds));
    this.renderer.setOnLodChange(() => this.onLodChange());
    this.renderer.setOnIdle(() => this.onIdle());
    this.renderer.setOnContextMenu((x, z, screenX, screenY) => this.handleContextMenu(x, z, screenX, screenY));
    
    this.setupUI();
    this.setupContextMenu();
    this.setupGotoModal();
    this.connectWebSocket();
    this.startAutoLoad();
    this.startTimeoutCheck();
  }
  
  private startTimeoutCheck(): void {
    if (this.timeoutCheckInterval) return;
    
    this.timeoutCheckInterval = setInterval(() => {
      this.checkTimeoutRequests();
    }, 2000);
  }
  
  private checkTimeoutRequests(): void {
    const now = Date.now();
    const timedOutRegions: string[] = [];
    
    for (const [key, startTime] of this.loadingRegionStartTime) {
      if (now - startTime > XaeroMapViewer.REQUEST_TIMEOUT_MS) {
        timedOutRegions.push(key);
      }
    }
    
    if (timedOutRegions.length === 0) return;
    
    console.warn(`请求超时，重新加载 ${timedOutRegions.length} 个区域`);
    
    for (const key of timedOutRegions) {
      this.loadingRegions.delete(key);
      this.loadingRegionStartTime.delete(key);
    }
    
    this.updatePendingRegions();
    
    const regionsToRetry = timedOutRegions.map(key => {
      const [x, z] = key.split(',').map(Number);
      return {x, z};
    });
    
    setTimeout(() => {
      this.loadRegionsBatch(regionsToRetry);
    }, 100);
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

  private startAutoLoad(): void {
    // 已优化：移除无条件定时器
    // loadVisibleRegions() 已在以下场景被调用：
    // 1. onViewportChange() - 视口变化时（50ms 防抖）
    // 2. onLodChange() - LOD 变化时
    // 3. processRegionsBatch() - 批量加载完成后
    // 4. loadRegionList() - 初始化时
    // 无需额外的定时器轮询
  }

  private setupUI(): void {
    const worldSelector = document.getElementById('worldSelector') as HTMLSelectElement;
    const dimSelector = document.getElementById('dimSelector') as HTMLSelectElement;
    const mapTypeSelector = document.getElementById('mapTypeSelector') as HTMLSelectElement;
    const zoomSlider = document.getElementById('zoomSlider') as HTMLInputElement;
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
      this.preserveViewAndReload();
    });
    const caveStartSlider = document.getElementById('caveStartSlider') as HTMLInputElement;
    caveStartSlider?.addEventListener('input', (e) => {
      const value = parseInt((e.target as HTMLInputElement).value);
      this.currentCaveStart = value;
      const caveStartValue = document.getElementById('caveStartValue');
      if (caveStartValue) caveStartValue.textContent = value.toString();
      this.saveMapState();
      this.preserveViewAndReload();
    });
    
    const clearCacheBtn = document.getElementById('clearCacheBtn') as HTMLButtonElement;
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
    
    this.loadCacheSize();
    
    this.loadWaypointSettings();
    this.setupWaypointUI();
    
    this.syncServerConfig();
    
    this.showLoading(false);
  }

  private async syncServerConfig(): Promise<void> {
    try {
      const response = await fetch(`${API_BASE}/config`);
      if (response.ok) {
        const config = await response.json();
        const numWorkers = config.numWorkers || 4;
        
        this.idleUpgradeBatchSize = numWorkers;
        
        const concurrentSlider = document.getElementById('concurrentSlider') as HTMLInputElement;
        if (concurrentSlider) {
          concurrentSlider.min = '1';
          concurrentSlider.max = String(numWorkers);
          
          if (this.concurrentLoads > numWorkers) {
            this.concurrentLoads = numWorkers;
            concurrentSlider.value = String(numWorkers);
            const concurrentValue = document.getElementById('concurrentValue');
            if (concurrentValue) concurrentValue.textContent = String(numWorkers);
          }
        }
      }
    } catch {}
  }

  private loadWaypointSettings(): void {
    try {
      const saved = localStorage.getItem(STORAGE_KEY_SHOW_WAYPOINTS);
      if (saved !== null) {
        this.showWaypoints = saved === 'true';
      }
    } catch {}
    this.renderer.setShowWaypoints(this.showWaypoints);
  }

  private waypointDimFilter: string = 'all';

  private setupWaypointUI(): void {
    const waypointBtn = document.getElementById('waypointBtn');
    const waypointModal = document.getElementById('waypointModal');
    const closeWaypointModal = document.getElementById('closeWaypointModal');
    const waypointSearch = document.getElementById('waypointSearch') as HTMLInputElement;
    const showDisabledWaypoints = document.getElementById('showDisabledWaypoints') as HTMLInputElement;

    let savedShowDisabled = false;
    try {
      const saved = localStorage.getItem(STORAGE_KEY_SHOW_DISABLED_WAYPOINTS);
      if (saved !== null) {
        savedShowDisabled = saved === 'true';
      }
    } catch {}
    if (showDisabledWaypoints) {
      showDisabledWaypoints.checked = savedShowDisabled;
    }
    this.renderer.setShowDisabledWaypoints(savedShowDisabled);

    const showWaypointsToggle = document.getElementById('showWaypointsToggle') as HTMLInputElement;
    if (showWaypointsToggle) {
      showWaypointsToggle.checked = this.showWaypoints;
      showWaypointsToggle.addEventListener('change', () => {
        this.showWaypoints = showWaypointsToggle.checked;
        this.renderer.setShowWaypoints(this.showWaypoints);
        localStorage.setItem(STORAGE_KEY_SHOW_WAYPOINTS, String(this.showWaypoints));
      });
    }

    waypointBtn?.addEventListener('click', () => {
      this.openWaypointModal();
    });

    closeWaypointModal?.addEventListener('click', () => {
      waypointModal?.classList.remove('open');
    });

    waypointModal?.addEventListener('click', (e) => {
      if (e.target === waypointModal) {
        waypointModal.classList.remove('open');
      }
    });

    waypointSearch?.addEventListener('input', () => {
      this.updateWaypointModal();
    });

    showDisabledWaypoints?.addEventListener('change', () => {
      localStorage.setItem(STORAGE_KEY_SHOW_DISABLED_WAYPOINTS, String(showDisabledWaypoints.checked));
      this.renderer.setShowDisabledWaypoints(showDisabledWaypoints.checked);
      this.updateWaypointModal();
    });

    const dimFilterBtns = document.querySelectorAll('.dim-filter-btn');
    dimFilterBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        dimFilterBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.waypointDimFilter = (btn as HTMLElement).dataset.dim || 'all';
        this.updateWaypointModal();
      });
    });

    this.renderer.setOnWaypointClick((waypoint) => this.handleWaypointClick(waypoint));
  }

  private openWaypointModal(): void {
    const waypointModal = document.getElementById('waypointModal');
    waypointModal?.classList.add('open');
    this.updateWaypointModal();
  }

  private updateWaypointModal(): void {
    const groupsEl = document.getElementById('waypointGroups');
    const searchInput = document.getElementById('waypointSearch') as HTMLInputElement;
    const showDisabledCheckbox = document.getElementById('showDisabledWaypoints') as HTMLInputElement;
    
    if (!groupsEl) return;

    const searchTerm = searchInput?.value.toLowerCase() || '';
    const showDisabled = showDisabledCheckbox?.checked || false;

    const groups = this.groupWaypoints(this.waypoints);
    
    groupsEl.innerHTML = '';

    for (const [setName, waypoints] of groups) {
      let filteredWaypoints = waypoints;
      
      if (this.waypointDimFilter !== 'all') {
        filteredWaypoints = filteredWaypoints.filter(w => {
          const dimType = this.getWaypointDimensionType(w);
          return dimType === this.waypointDimFilter;
        });
      }

      if (searchTerm) {
        filteredWaypoints = filteredWaypoints.filter(w => 
          w.name.toLowerCase().includes(searchTerm) ||
          `${w.x}, ${w.y}, ${w.z}`.includes(searchTerm)
        );
      }

      if (!showDisabled) {
        filteredWaypoints = filteredWaypoints.filter(w => !w.disabled);
      }

      if (filteredWaypoints.length === 0) continue;

      const groupEl = document.createElement('div');
      groupEl.className = 'waypoint-group';

      const headerEl = document.createElement('div');
      headerEl.className = 'waypoint-group-header';
      headerEl.innerHTML = `
        <div>
          <span class="waypoint-group-name">${setName}</span>
          <span class="waypoint-group-count">(${filteredWaypoints.length})</span>
        </div>
        <span class="waypoint-group-toggle">▼</span>
      `;

      const itemsEl = document.createElement('div');
      itemsEl.className = 'waypoint-group-items';

      const sortedWaypoints = [...filteredWaypoints].sort((a, b) => {
        if (a.disabled !== b.disabled) return a.disabled ? 1 : -1;
        return a.name.localeCompare(b.name);
      });

      for (const waypoint of sortedWaypoints) {
        const itemEl = document.createElement('div');
        itemEl.className = 'modal-waypoint-item' + (waypoint.disabled ? ' disabled' : '');

        const color = waypoint.color;
        const r = (color >> 16) & 0xFF;
        const g = (color >> 8) & 0xFF;
        const b = color & 0xFF;

        const coordScale = this.getWaypointCoordScale(waypoint);
        const displayX = Math.floor(waypoint.x * coordScale);
        const displayZ = Math.floor(waypoint.z * coordScale);

        const dimName = this.getDimensionDisplayName(waypoint.dimension);
        const dimType = this.getWaypointDimensionType(waypoint);
        const dimClass = dimType !== 'unknown' ? ` dim-${dimType}` : '';

        itemEl.innerHTML = `
          <div class="modal-waypoint-color" style="background-color: rgb(${r}, ${g}, ${b})">${waypoint.symbol}</div>
          <div class="modal-waypoint-info">
            <div class="modal-waypoint-name">${waypoint.name}${dimName ? ` <span class="waypoint-dim-tag${dimClass}">${dimName}</span>` : ''}</div>
            <div class="modal-waypoint-coords">X: ${displayX}, Y: ${waypoint.y}, Z: ${displayZ}</div>
          </div>
          <div class="modal-waypoint-actions">
            <button class="goto-btn" title="跳转">→</button>
          </div>
        `;

        const gotoBtn = itemEl.querySelector('.goto-btn');
        gotoBtn?.addEventListener('click', (e) => {
          e.stopPropagation();
          this.handleWaypointClick(waypoint);
          document.getElementById('waypointModal')?.classList.remove('open');
        });

        itemEl.addEventListener('click', () => {
          this.handleWaypointClick(waypoint);
          document.getElementById('waypointModal')?.classList.remove('open');
        });

        itemsEl.appendChild(itemEl);
      }

      headerEl.addEventListener('click', () => {
        groupEl.classList.toggle('collapsed');
      });

      groupEl.appendChild(headerEl);
      groupEl.appendChild(itemsEl);
      groupsEl.appendChild(groupEl);
    }

    if (groupsEl.children.length === 0) {
      groupsEl.innerHTML = '<div style="text-align: center; color: var(--mc-text-gray); padding: 20px;">没有找到路径点</div>';
    }
  }

  private groupWaypoints(waypoints: Waypoint[]): Map<string, Waypoint[]> {
    const groups = new Map<string, Waypoint[]>();
    
    for (const waypoint of waypoints) {
      const setName = waypoint.setName || 'default';
      if (!groups.has(setName)) {
        groups.set(setName, []);
      }
      groups.get(setName)!.push(waypoint);
    }

    return groups;
  }

  private async loadWaypoints(): Promise<void> {
    if (!this.currentWorld) return;
    
    try {
      const response = await fetch(`${API_BASE}/waypoints/server/${encodeURIComponent(this.currentWorld)}`);
      if (response.ok) {
        const data = await response.json();
        const dimWaypoints = data.waypoints || {};
        
        let allWaypoints: Waypoint[] = [];
        for (const dimName of Object.keys(dimWaypoints)) {
          const wps = dimWaypoints[dimName];
          for (const wp of wps) {
            wp.dimension = dimName;
          }
          allWaypoints = allWaypoints.concat(wps);
        }
        
        this.waypoints = allWaypoints;
        this.renderer.setWaypoints(this.waypoints);
      }
    } catch (e) {
      console.error('Failed to load waypoints:', e);
    }
  }

  private handleWaypointClick(waypoint: Waypoint): void {
    const dimType = this.getWaypointDimensionType(waypoint);
    const isNether = this.isNetherDimension();
    const isEnd = this.isEndDimension();
    
    if (dimType === 'end' && !isEnd) {
      this.switchToDimension('DIM1');
      setTimeout(() => { this.renderer.centerOn(waypoint.x, waypoint.z); }, 500);
      return;
    }

    if (dimType === 'nether' && !isNether) {
      this.switchToDimension('DIM-1');
      setTimeout(() => { this.renderer.centerOn(waypoint.x, waypoint.z); }, 500);
      return;
    }

    if (dimType === 'overworld' && (isNether || isEnd)) {
      this.switchToDimension('null');
      setTimeout(() => { this.renderer.centerOn(waypoint.x, waypoint.z); }, 500);
      return;
    }

    this.renderer.centerOn(waypoint.x, waypoint.z);
  }

  private async switchToDimension(dimValue: string): Promise<void> {
    const dimSelector = document.getElementById('dimSelector') as HTMLSelectElement;
    if (!dimSelector) return;

    const targetOption = Array.from(dimSelector.options).find(opt => opt.value === dimValue);
    if (targetOption) {
      dimSelector.value = targetOption.value;
      await this.handleDimChange();
    }
  }

  private getWaypointDimensionType(waypoint: Waypoint): 'overworld' | 'nether' | 'end' | 'unknown' {
    if (!waypoint.dimension) return 'overworld';
    const dim = waypoint.dimension.toLowerCase();
    
    if (dim.includes('nether') || waypoint.dimension.includes('DIM-1') || dim.includes('下界')) {
      return 'nether';
    }
    if (dim.includes('end') || waypoint.dimension.includes('DIM1') || dim.includes('末地')) {
      return 'end';
    }
    if (dim.includes('overworld') || waypoint.dimension === 'null' || dim.includes('主世界')) {
      return 'overworld';
    }
    
    return 'unknown';
  }

  private getWaypointCoordScale(waypoint: Waypoint): number {
    if (!waypoint.dimension) return 1;
    
    const isNether = this.isNetherDimension();
    const isEnd = this.isEndDimension();
    const dim = waypoint.dimension.toLowerCase();
    
    const isWaypointNether = dim.includes('nether') || waypoint.dimension.includes('DIM-1') || dim.includes('下界');
    const isWaypointEnd = dim.includes('end') || waypoint.dimension.includes('DIM1') || dim.includes('末地');
    const isWaypointOverworld = dim.includes('overworld') || waypoint.dimension === 'null' || dim.includes('主世界') || (!isWaypointNether && !isWaypointEnd);

    if (isEnd || isWaypointEnd) {
      return 1;
    }

    if (isNether && isWaypointOverworld) {
      return 1/8;
    }

    if (!isNether && isWaypointNether) {
      return 8;
    }

    return 1;
  }

  private getDimensionDisplayName(dimension: string): string {
    if (!dimension) return '';
    const dim = dimension.toLowerCase();
    
    if (dim.includes('nether') || dimension.includes('DIM-1') || dim.includes('下界')) {
      return '下界';
    }
    if (dim.includes('end') || dimension.includes('DIM1') || dim.includes('末地')) {
      return '末地';
    }
    if (dim.includes('overworld') || dimension === 'null' || dim.includes('主世界')) {
      return '主世界';
    }
    
    return '';
  }

  private setupContextMenu(): void {
    const contextMenu = document.getElementById('contextMenu');
    const ctxGotoNether = document.getElementById('ctxGotoNether');
    const ctxGotoOverworld = document.getElementById('ctxGotoOverworld');
    const ctxCopyCoords = document.getElementById('ctxCopyCoords');

    const closeMenu = () => {
      contextMenu?.classList.remove('open');
    };

    document.addEventListener('click', closeMenu);
    document.addEventListener('touchstart', (e) => {
      if (contextMenu?.classList.contains('open')) {
        const target = e.target as HTMLElement;
        if (!contextMenu.contains(target)) {
          closeMenu();
        }
      }
    }, { passive: true });

    contextMenu?.addEventListener('click', (e) => {
      e.stopPropagation();
    });

    contextMenu?.addEventListener('touchstart', (e) => {
      e.stopPropagation();
    }, { passive: true });

    ctxGotoNether?.addEventListener('click', () => {
      this.gotoNether();
      closeMenu();
    });

    ctxGotoOverworld?.addEventListener('click', () => {
      this.gotoOverworld();
      closeMenu();
    });

    ctxCopyCoords?.addEventListener('click', () => {
      this.copyContextCoords();
      closeMenu();
    });
  }

  private contextWorldX: number = 0;
  private contextWorldZ: number = 0;

  private handleContextMenu(worldX: number, worldZ: number, screenX: number, screenY: number): void {
    this.contextWorldX = worldX;
    this.contextWorldZ = worldZ;

    const contextMenu = document.getElementById('contextMenu');
    const ctxGotoNether = document.getElementById('ctxGotoNether');
    const ctxGotoOverworld = document.getElementById('ctxGotoOverworld');
    const ctxNetherCoords = document.getElementById('ctxNetherCoords');
    const ctxOverworldCoords = document.getElementById('ctxOverworldCoords');

    if (!contextMenu) return;

    const isNether = this.isNetherDimension();
    const isEnd = this.isEndDimension();

    if (ctxGotoNether && ctxGotoOverworld) {
      if (isEnd) {
        ctxGotoNether.style.display = 'none';
        ctxGotoOverworld.style.display = 'none';
      } else if (isNether) {
        ctxGotoNether.style.display = 'none';
        ctxGotoOverworld.style.display = 'flex';
        const overworldX = worldX * 8;
        const overworldZ = worldZ * 8;
        if (ctxOverworldCoords) {
          ctxOverworldCoords.textContent = `(${overworldX}, ${overworldZ})`;
        }
      } else {
        ctxGotoNether.style.display = 'flex';
        ctxGotoOverworld.style.display = 'none';
        const netherX = Math.floor(worldX / 8);
        const netherZ = Math.floor(worldZ / 8);
        if (ctxNetherCoords) {
          ctxNetherCoords.textContent = `(${netherX}, ${netherZ})`;
        }
      }
    }

    contextMenu.style.left = '0px';
    contextMenu.style.top = '0px';
    contextMenu.classList.add('open');

    const menuWidth = contextMenu.offsetWidth;
    const menuHeight = contextMenu.offsetHeight;
    const windowWidth = window.innerWidth;
    const windowHeight = window.innerHeight;
    const padding = 8;

    let finalX = screenX;
    let finalY = screenY;

    if (finalX + menuWidth + padding > windowWidth) {
      finalX = windowWidth - menuWidth - padding;
    }
    if (finalX < padding) {
      finalX = padding;
    }
    if (finalY + menuHeight + padding > windowHeight) {
      finalY = windowHeight - menuHeight - padding;
    }
    if (finalY < padding) {
      finalY = padding;
    }

    contextMenu.style.left = `${finalX}px`;
    contextMenu.style.top = `${finalY}px`;
  }

  private isNetherDimension(): boolean {
    if (!this.currentDim) return false;
    const dim = this.currentDim.toLowerCase();
    return dim.includes('nether') || 
           this.currentDim.includes('DIM-1') || 
           dim.includes('下界');
  }

  private isEndDimension(): boolean {
    if (!this.currentDim) return false;
    const dim = this.currentDim.toLowerCase();
    return dim.includes('end') || 
           this.currentDim.includes('DIM1') || 
           dim.includes('末地');
  }

  private async gotoNether(): Promise<void> {
    if (!this.currentWorld) return;

    const netherX = Math.floor(this.contextWorldX / 8);
    const netherZ = Math.floor(this.contextWorldZ / 8);

    const dimSelector = document.getElementById('dimSelector') as HTMLSelectElement;
    if (!dimSelector) return;

    const netherOption = Array.from(dimSelector.options).find(opt => 
      opt.value === 'DIM-1' || 
      opt.value.toLowerCase().includes('nether') ||
      opt.textContent?.includes('下界')
    );

    if (netherOption) {
      dimSelector.value = netherOption.value;
      await this.handleDimChange();
      this.renderer.centerOn(netherX, netherZ);
    } else {
      alert('当前世界没有下界维度');
    }
  }

  private async gotoOverworld(): Promise<void> {
    if (!this.currentWorld) return;

    const overworldX = this.contextWorldX * 8;
    const overworldZ = this.contextWorldZ * 8;

    const dimSelector = document.getElementById('dimSelector') as HTMLSelectElement;
    if (!dimSelector) return;

    const overworldOption = Array.from(dimSelector.options).find(opt => 
      opt.value === 'null' || 
      opt.value.toLowerCase().includes('overworld') ||
      opt.textContent?.includes('主世界')
    );

    if (overworldOption) {
      dimSelector.value = overworldOption.value;
      await this.handleDimChange();
      this.renderer.centerOn(overworldX, overworldZ);
    } else {
      alert('当前世界没有主世界维度');
    }
  }

  private copyContextCoords(): void {
    const text = `${this.contextWorldX}  ${this.contextWorldZ}`;
    navigator.clipboard.writeText(text).then(() => {
      console.log('坐标已复制:', text);
    }).catch(err => {
      console.error('复制失败:', err);
    });
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
    }, 100);
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

  private async loadWorlds(): Promise<void> {
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
      
      let savedWorld: string | undefined;
      try {
        const saved = localStorage.getItem(STORAGE_KEY_MAP_STATE);
        if (saved) {
          const state = JSON.parse(saved);
          savedWorld = state.world;
        }
      } catch {}
      
      const savedWorldExists = savedWorld && worlds.includes(savedWorld);
      
      if (savedWorldExists) {
        selector.value = savedWorld!;
      }
      await this.handleWorldChange();
    } catch (e) {
      console.error('Failed to load worlds:', e);
    }
  }

  private async handleWorldChange(): Promise<void> {
    if (this.abortController) {
      this.abortController.abort();
    }
    this.abortController = new AbortController();
    
    const selector = document.getElementById('worldSelector') as HTMLSelectElement;
    this.currentWorld = selector.value;
    
    if (!this.currentWorld) return;
    
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
      
      let savedDim: string | null = null;
      try {
        const saved = localStorage.getItem(STORAGE_KEY_MAP_STATE);
        if (saved) {
          const state = JSON.parse(saved);
          if (state.world === this.currentWorld) {
            savedDim = state.dim;
          }
        }
      } catch {}
      
      const savedDimExists = savedDim && dimensions.some(d => d.path === savedDim);
      
      if (savedDimExists) {
        dimSelector.value = savedDim!;
        await this.handleDimChange();
      } else {
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
    
    let savedCaveMode: number | null = null;
    let savedCaveStart: number | null = null;
    try {
      const saved = localStorage.getItem(STORAGE_KEY_MAP_STATE);
      if (saved) {
        const state = JSON.parse(saved);
        if (state.dim === this.currentDim) {
          savedCaveMode = state.caveMode;
          savedCaveStart = state.caveStart;
        }
      }
    } catch {}
    
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
      
      let savedMapType: string | null = null;
      try {
        const saved = localStorage.getItem(STORAGE_KEY_MAP_STATE);
        if (saved) {
          const state = JSON.parse(saved);
          if (state.dim === this.currentDim) {
            savedMapType = state.mapType;
          }
        }
      } catch {}
      
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
      
      this.loadWaypoints();
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
    
    if (this.currentMapType) {
      this.loadRegionList();
    }
  }

  private preserveViewAndReload(): void {
    const viewState = this.renderer.getViewState();
    const state: MapState = {
      scale: viewState.scale,
      centerX: viewState.centerX,
      centerZ: viewState.centerZ,
      world: this.currentWorld || '',
      dim: this.currentDim || 'null',
      mapType: this.currentMapType || '',
      caveMode: this.currentCaveMode,
      caveStart: this.currentCaveStart
    };
    localStorage.setItem(STORAGE_KEY_MAP_STATE, JSON.stringify(state));
    this.loadRegionList();
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
      
      let savedScale: number | undefined;
      let savedCenterX: number | undefined;
      let savedCenterZ: number | undefined;
      try {
        const saved = localStorage.getItem(STORAGE_KEY_MAP_STATE);
        if (saved) {
          const state = JSON.parse(saved);
          savedScale = state.scale;
          savedCenterX = state.centerX;
          savedCenterZ = state.centerZ;
        }
      } catch {}
      
      if (savedScale !== undefined && savedCenterX !== undefined && savedCenterZ !== undefined) {
        this.renderer.setViewState(savedScale, savedCenterX, savedCenterZ);
      } else if (this.allRegions.length === 0) {
        const currentView = this.renderer.getViewState();
        this.renderer.setViewState(currentView.scale, currentView.centerX, currentView.centerZ);
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
    this.idleUpgradeQueue = [];
    
    const regionsToRemove = this.getRegionsOutsideViewport(bounds);
    
    if (regionsToRemove.size > 0) {
      const allRegionsOutside = regionsToRemove.size === this.loadingRegions.size;
      
      if (allRegionsOutside) {
        this.cancelServerRequest();
        if (this.abortController) {
          this.abortController.abort();
        }
        this.abortController = new AbortController();
        this.loadingRegions.clear();
        this.loadingRegionStartTime.clear();
      } else {
        for (const key of regionsToRemove) {
          this.loadingRegions.delete(key);
          this.loadingRegionStartTime.delete(key);
        }
      }
      this.updatePendingRegions();
    }
    
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

  private getRegionsOutsideViewport(bounds: ViewportBounds): Set<string> {
    const outside = new Set<string>();
    
    for (const key of this.loadingRegions) {
      const [x, z] = key.split(',').map(Number);
      if (x < bounds.startX || x > bounds.endX || z < bounds.startZ || z > bounds.endZ) {
        outside.add(key);
      }
    }
    
    return outside;
  }

  private onLodChange(): void {
    this.cancelServerRequest();
    this.idleUpgradeQueue = [];
    this.idleUpgradeInProgress = false;
    this.loadingRegionStartTime.clear();
    
    if (this.abortController) {
      this.abortController.abort();
    }
    this.abortController = new AbortController();
    
    this.loadingRegions.clear();
    this.updatePendingRegions();
    this.loadVisibleRegions();
  }

  private onIdle(): void {
    if (this.idleUpgradeInProgress) return;
    if (!this.currentWorld || !this.currentDim) return;
    
    const bounds = this.renderer.getViewportBounds();
    const currentLod = this.renderer.getCurrentLodLevel();
    const centerRegionX = Math.floor((bounds.startX + bounds.endX) / 2);
    const centerRegionZ = Math.floor((bounds.startZ + bounds.endZ) / 2);
    
    const upgradeCandidates: {x: number, z: number, dist: number}[] = [];
    
    for (let x = bounds.startX; x <= bounds.endX; x++) {
      for (let z = bounds.startZ; z <= bounds.endZ; z++) {
        const key = `${x},${z}`;
        if (this.loadingRegions.has(key)) continue;
        if (this.allRegionSet.size > 0 && !this.allRegionSet.has(key)) continue;
        
        const regionLod = this.renderer.getRegionLod(x, z);
        if (regionLod === undefined) continue;
        if (regionLod <= currentLod) continue;
        
        const dist = Math.abs(x - centerRegionX) + Math.abs(z - centerRegionZ);
        upgradeCandidates.push({x, z, dist});
      }
    }
    
    if (upgradeCandidates.length === 0) return;
    
    upgradeCandidates.sort((a, b) => a.dist - b.dist);
    this.idleUpgradeQueue = upgradeCandidates.map(c => ({x: c.x, z: c.z}));
    
    this.processIdleUpgrade();
  }

  private processIdleUpgrade(): void {
    if (!this.renderer.getIdleState()) {
      this.idleUpgradeQueue = [];
      this.idleUpgradeInProgress = false;
      return;
    }
    
    if (this.idleUpgradeQueue.length === 0) {
      this.idleUpgradeInProgress = false;
      return;
    }
    
    if (this.loadingRegions.size >= this.concurrentLoads) {
      setTimeout(() => this.processIdleUpgrade(), XaeroMapViewer.IDLE_UPGRADE_DELAY_MS);
      return;
    }
    
    this.idleUpgradeInProgress = true;
    
    const availableSlots = this.concurrentLoads - this.loadingRegions.size;
    const batchSize = Math.min(availableSlots, this.idleUpgradeBatchSize, this.idleUpgradeQueue.length);
    
    if (batchSize === 0) {
      setTimeout(() => this.processIdleUpgrade(), XaeroMapViewer.IDLE_UPGRADE_DELAY_MS);
      return;
    }
    
    const toUpgrade = this.idleUpgradeQueue.splice(0, batchSize);
    const currentLod = this.renderer.getCurrentLodLevel();
    
    this.loadRegionsBatch(toUpgrade, currentLod).then(() => {
      if (this.renderer.getIdleState() && this.idleUpgradeQueue.length > 0) {
        setTimeout(() => this.processIdleUpgrade(), XaeroMapViewer.IDLE_UPGRADE_DELAY_MS);
      } else {
        this.idleUpgradeInProgress = false;
      }
    }).catch(() => {
      this.idleUpgradeInProgress = false;
    });
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
    
    const currentLod = this.renderer.getCurrentLodLevel();
    const overviewLod = Math.min(currentLod + 1, 3);
    
    const noDataRegions: {x: number, z: number, dist: number}[] = [];
    const upgradeRegions: {x: number, z: number, dist: number}[] = [];
    
    for (let x = bounds.startX; x <= bounds.endX; x++) {
      for (let z = bounds.startZ; z <= bounds.endZ; z++) {
        const key = `${x},${z}`;
        if (this.loadingRegions.has(key)) continue;
        if (this.allRegionSet.size > 0 && !this.allRegionSet.has(key)) continue;
        
        const dist = Math.abs(x - centerRegionX) + Math.abs(z - centerRegionZ);
        
        if (this.renderer.hasRegion(x, z)) continue;
        
        if (this.renderer.hasAnyLod(x, z)) {
          upgradeRegions.push({x, z, dist});
        } else {
          noDataRegions.push({x, z, dist});
        }
      }
    }
    
    if (noDataRegions.length === 0 && upgradeRegions.length === 0) return;
    
    noDataRegions.sort((a, b) => a.dist - b.dist);
    upgradeRegions.sort((a, b) => a.dist - b.dist);
    
    if (noDataRegions.length > 0) {
      const toLoad = noDataRegions.slice(0, this.concurrentLoads);
      this.loadRegionsBatch(toLoad.map(r => ({x: r.x, z: r.z})), overviewLod);
      return;
    }
    
    const toLoad = upgradeRegions.slice(0, this.concurrentLoads);
    this.loadRegionsBatch(toLoad.map(r => ({x: r.x, z: r.z})), currentLod);
  }

  private updatePendingRegions(): void {
    this.renderer.setPendingRegions(new Set(this.loadingRegions));
  }

  private processRegionsBatch(regions: { rx: number; rz: number; pixelData: Uint8Array }[], lod: number, requestId: number): void {
    const BATCH_BUDGET_MS = 8;
    let index = 0;
    
    const processNext = () => {
      if (requestId !== this.currentRequestId) return;
      const start = performance.now();
      
      while (index < regions.length) {
        const { rx, rz, pixelData } = regions[index];
        this.renderer.addRegionPixels(rx, rz, pixelData, lod);
        this.regionAccessTime.set(`${rx},${rz}`, Date.now());
        index++;
        
        if (performance.now() - start > BATCH_BUDGET_MS) break;
      }
      
      this.renderer.render();
      
      if (index < regions.length) {
        requestAnimationFrame(processNext);
      } else {
        this.updateStats();
        this.loadCacheSize();
        this.loadVisibleRegions();
      }
    };
    
    processNext();
  }

  private async loadRegionsBatch(regions: {x: number, z: number}[], lod?: number): Promise<void> {
    if (regions.length === 0) return;
    
    const signal = this.abortController?.signal;
    if (signal?.aborted) return;
    
    if (regions.length === 1) {
      const {x, z} = regions[0];
      const key = `${x},${z}`;
      if (this.loadingRegions.has(key)) return;
      this.loadingRegions.add(key);
      this.loadingRegionStartTime.set(key, Date.now());
      this.updatePendingRegions();
      
      try {
        await this.loadSingleRegionPixels(x, z, signal, lod);
      } finally {
        this.loadingRegions.delete(key);
        this.loadingRegionStartTime.delete(key);
        this.updatePendingRegions();
        this.loadVisibleRegions();
      }
      return;
    }
    
    const toLoad = regions.filter(r => !this.loadingRegions.has(`${r.x},${r.z}`));
    if (toLoad.length === 0) return;
    
    const now = Date.now();
    for (const r of toLoad) {
      const key = `${r.x},${r.z}`;
      this.loadingRegions.add(key);
      this.loadingRegionStartTime.set(key, now);
    }
    this.updatePendingRegions();
    
    this.currentRequestId++;
    const requestId = this.currentRequestId;
    
    try {
      const coordsStr = toLoad.map(r => `${r.x},${r.z}`).join(';');
      const requestLod = lod ?? this.renderer.getCurrentLodLevel();
      const bounds = this.renderer.getViewportBounds();
      
      const payload: any = {
        world: this.currentWorld,
        dim: this.currentDim,
        coords: coordsStr,
        lod: requestLod,
        viewStartX: bounds.startX,
        viewStartZ: bounds.startZ,
        viewEndX: bounds.endX,
        viewEndZ: bounds.endZ
      };
      
      if (this.currentMapType) {
        payload.mapType = this.currentMapType;
      }
      if (this.currentCaveMode > 0) {
        payload.caveMode = this.currentCaveMode;
        if (this.currentCaveMode === 1) {
          payload.caveStart = this.currentCaveStart;
        }
      }
      
      const results = await this.sendWsRequestWithRetry('batch-regions', payload, requestId);
      
      if (requestId !== this.currentRequestId) {
        return;
      }
      
      this.processRegionsBatch(results.regions, results.lod, requestId);
    } catch (e: unknown) {
      if (e instanceof Error && e.name === 'AbortError') {
        return;
      }
      console.error('Batch load error:', e);
    } finally {
      for (const r of toLoad) {
        const key = `${r.x},${r.z}`;
        this.loadingRegions.delete(key);
        this.loadingRegionStartTime.delete(key);
      }
      this.updatePendingRegions();
    }
  }

  private async loadSingleRegionPixels(x: number, z: number, signal?: AbortSignal, lod?: number): Promise<void> {
    try {
      const requestLod = lod ?? this.renderer.getCurrentLodLevel();
      let url = `${API_BASE}/region-pixels?world=${encodeURIComponent(this.currentWorld!)}&dim=${encodeURIComponent(this.currentDim!)}&x=${x}&z=${z}&lod=${requestLod}`;
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
        this.renderer.addRegionPixels(x, z, pixelData, requestLod);
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

  private setupGotoModal(): void {
    const gotoBtn = document.getElementById('gotoBtn');
    const gotoModal = document.getElementById('gotoModal');
    const closeGotoModal = document.getElementById('closeGotoModal');
    const gotoConfirm = document.getElementById('gotoConfirm');

    gotoBtn?.addEventListener('click', () => {
      gotoModal?.classList.add('open');
    });

    closeGotoModal?.addEventListener('click', () => {
      gotoModal?.classList.remove('open');
    });

    gotoModal?.addEventListener('click', (e) => {
      if (e.target === gotoModal) {
        gotoModal.classList.remove('open');
      }
    });

    gotoConfirm?.addEventListener('click', () => {
      this.handleGotoCoords();
      gotoModal?.classList.remove('open');
    });
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

  private connectWebSocket(): void {
    if (this.wsReconnectTimeout) {
      clearTimeout(this.wsReconnectTimeout);
      this.wsReconnectTimeout = null;
    }

    try {
      this.ws = new WebSocket(WS_BASE);
      this.ws.binaryType = 'arraybuffer';
      
      this.ws.onopen = () => {
        this.wsConnected = true;
        console.log('WebSocket connected');
        this.loadWorlds();
      };
      
      this.ws.onclose = () => {
        this.wsConnected = false;
        console.log('WebSocket disconnected, reconnecting...');
        this.wsReconnectTimeout = setTimeout(() => this.connectWebSocket(), 3000);
      };
      
      this.ws.onerror = (e) => {
        console.error('WebSocket error:', e);
      };
      
      this.ws.onmessage = (event) => {
        try {
          if (event.data instanceof ArrayBuffer) {
            this.handleWsBinaryMessage(event.data);
          } else {
            const msg = JSON.parse(event.data);
            this.handleWsMessage(msg);
          }
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e);
        }
      };
    } catch (e) {
      console.error('Failed to create WebSocket:', e);
      this.wsReconnectTimeout = setTimeout(() => this.connectWebSocket(), 3000);
    }
  }

  private handleWsMessage(msg: any): void {
    const { type, requestId, error, config } = msg;
    
    if (type === 'server-config' && config) {
      this.applyServerConfig(config);
      return;
    }
    
    if (type === 'error' && requestId && error) {
      const pending = this.pendingWsRequests.get(requestId);
      if (pending) {
        this.pendingWsRequests.delete(requestId);
        pending.reject(new Error(error));
      }
    }
  }
  
  private applyServerConfig(config: { cacheDirectory?: string; mapDirectory?: string; baseMapDirectory?: string }): void {
    if (config.cacheDirectory) {
      const cacheDirInput = document.getElementById('cacheDirInput') as HTMLInputElement;
      if (cacheDirInput) {
        cacheDirInput.value = config.cacheDirectory;
      }
    }
  }

  private handleWsBinaryMessage(buffer: ArrayBuffer): void {
    const view = new DataView(buffer);
    const requestId = view.getUint32(0, true);
    const totalRegions = view.getUint32(4, true);
    const lodValue = view.getUint32(8, true);
    
    const pending = this.pendingWsRequests.get(requestId);
    if (!pending) return;
    
    if (totalRegions === 0) {
      this.pendingWsRequests.delete(requestId);
      pending.resolve({ regions: [], lod: lodValue });
      return;
    }
    
    const regions: { rx: number; rz: number; pixelData: Uint8Array }[] = [];
    let offset = 12;
    
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
    
    this.pendingWsRequests.delete(requestId);
    pending.resolve({ regions, lod: lodValue });
  }

  private async sendWsRequest(type: string, payload: any, existingRequestId?: number): Promise<any> {
    return new Promise((resolve, reject) => {
      if (!this.ws || !this.wsConnected) {
        reject(new Error('WebSocket not connected'));
        return;
      }

      const requestId = existingRequestId ?? ++this.currentRequestId;
      this.pendingWsRequests.set(requestId, { resolve, reject });
      
      const msg = JSON.stringify({ type, requestId, payload });
      this.ws.send(msg);
      
      setTimeout(() => {
        if (this.pendingWsRequests.has(requestId)) {
          this.pendingWsRequests.delete(requestId);
          reject(new Error('Request timeout'));
        }
      }, 30000);
    });
  }

  private async sendWsRequestWithRetry(type: string, payload: any, existingRequestId?: number, maxRetries: number = 3): Promise<any> {
    for (let attempt = 0; attempt < maxRetries; attempt++) {
      if (!this.wsConnected) {
        await new Promise<void>(resolve => {
          const checkInterval = setInterval(() => {
            if (this.wsConnected) {
              clearInterval(checkInterval);
              resolve();
            }
          }, 100);
          
          setTimeout(() => {
            clearInterval(checkInterval);
            resolve();
          }, 5000);
        });
      }
      
      try {
        return await this.sendWsRequest(type, payload, existingRequestId);
      } catch (e) {
        if (attempt === maxRetries - 1) throw e;
        await new Promise(resolve => setTimeout(resolve, 500));
      }
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  new XaeroMapViewer();
});
