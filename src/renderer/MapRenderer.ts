import type { MapRegion, MapBlock } from '../core/types';
import { getBlockColor, isGrassBlock, isFoliageBlock, isWaterBlock } from '../data/blockColors';
import { getGrassColor, getFoliageColor, getWaterColor } from '../data/biomeColors';

export interface RenderOptions {
  showLighting: boolean;
  showHeightShading: boolean;
  showWater: boolean;
}

export interface ViewportBounds {
  startX: number;
  startZ: number;
  endX: number;
  endZ: number;
}

const DEFAULT_OPTIONS: RenderOptions = {
  showLighting: true,
  showHeightShading: true,
  showWater: true
};

const MAX_CACHE_SIZE = 64;
const LOD_LEVELS = [
  { scale: 1, size: 512 },
  { scale: 0.5, size: 256 },
  { scale: 0.25, size: 128 },
  { scale: 0.1, size: 64 },
  { scale: 0.05, size: 32 },
  { scale: 0, size: 16 }
];

interface CacheEntry {
  canvas: HTMLCanvasElement;
  lastAccess: number;
  lodLevel: number;
}

export class MapRenderer {
  private canvas: HTMLCanvasElement;
  private ctx: CanvasRenderingContext2D;
  private regions: Map<string, MapRegion> = new Map();
  private options: RenderOptions = DEFAULT_OPTIONS;
  
  private offsetX: number = 0;
  private offsetZ: number = 0;
  private scale: number = 1;
  
  private isDragging: boolean = false;
  private lastMouseX: number = 0;
  private lastMouseY: number = 0;
  
  private onViewportChange: ((bounds: ViewportBounds) => void) | null = null;
  private loadedRegionKeys: Set<string> = new Set();
  
  private cache: Map<string, CacheEntry> = new Map();
  private renderQueued: boolean = false;

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Failed to get 2D context');
    this.ctx = ctx;
    
    this.setupEventListeners();
    this.resize();
  }

  setOnViewportChange(callback: (bounds: ViewportBounds) => void): void {
    this.onViewportChange = callback;
  }

  private setupEventListeners(): void {
    this.canvas.addEventListener('wheel', this.handleWheel.bind(this));
    this.canvas.addEventListener('mousedown', this.handleMouseDown.bind(this));
    this.canvas.addEventListener('mousemove', this.handleMouseMove.bind(this));
    this.canvas.addEventListener('mouseup', this.handleMouseUp.bind(this));
    this.canvas.addEventListener('mouseleave', this.handleMouseUp.bind(this));
    window.addEventListener('resize', this.resize.bind(this));
  }

  private handleWheel(e: WheelEvent): void {
    e.preventDefault();
    const rect = this.canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;
    
    const worldX = (mouseX - this.offsetX) / this.scale;
    const worldZ = (mouseY - this.offsetZ) / this.scale;
    
    const zoomFactor = e.deltaY > 0 ? 0.9 : 1.1;
    this.scale = Math.max(0.0625, Math.min(16, this.scale * zoomFactor));
    
    this.offsetX = mouseX - worldX * this.scale;
    this.offsetZ = mouseY - worldZ * this.scale;
    
    this.scheduleRender();
    this.updateZoomDisplay();
    this.notifyViewportChange();
  }

  private handleMouseDown(e: MouseEvent): void {
    if (e.button === 0) {
      this.isDragging = true;
      this.lastMouseX = e.clientX;
      this.lastMouseY = e.clientY;
      this.canvas.style.cursor = 'grabbing';
    }
  }

  private handleMouseMove(e: MouseEvent): void {
    const rect = this.canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;
    
    const worldX = Math.floor((mouseX - this.offsetX) / this.scale);
    const worldZ = Math.floor((mouseY - this.offsetZ) / this.scale);
    
    this.updateCoordsDisplay(worldX, worldZ);
    
    if (this.isDragging) {
      const deltaX = e.clientX - this.lastMouseX;
      const deltaY = e.clientY - this.lastMouseY;
      this.offsetX += deltaX;
      this.offsetZ += deltaY;
      this.lastMouseX = e.clientX;
      this.lastMouseY = e.clientY;
      this.scheduleRender();
      this.notifyViewportChange();
    }
  }

  private handleMouseUp(): void {
    this.isDragging = false;
    this.canvas.style.cursor = 'grab';
  }

  private notifyViewportChange(): void {
    if (this.onViewportChange) {
      const bounds = this.getViewportBounds();
      this.onViewportChange(bounds);
    }
  }

  getViewportBounds(): ViewportBounds {
    const regionSize = 512;
    const startX = Math.floor(-this.offsetX / this.scale / regionSize) - 1;
    const startZ = Math.floor(-this.offsetZ / this.scale / regionSize) - 1;
    const endX = Math.ceil((this.canvas.width - this.offsetX) / this.scale / regionSize) + 1;
    const endZ = Math.ceil((this.canvas.height - this.offsetZ) / this.scale / regionSize) + 1;
    
    return { startX, startZ, endX, endZ };
  }

  private updateCoordsDisplay(x: number, z: number): void {
    const coordsEl = document.getElementById('coords');
    if (coordsEl) {
      coordsEl.textContent = `X: ${x}, Z: ${z}`;
    }
  }

  private updateZoomDisplay(): void {
    const zoomEl = document.getElementById('zoomLevel');
    if (zoomEl) {
      zoomEl.textContent = `${this.scale.toFixed(2)}x`;
    }
    const sliderEl = document.getElementById('zoomSlider') as HTMLInputElement;
    if (sliderEl) {
      sliderEl.value = String(Math.round(Math.log2(this.scale)));
    }
  }

  resize(): void {
    const container = this.canvas.parentElement;
    if (!container) return;
    
    this.canvas.width = container.clientWidth;
    this.canvas.height = container.clientHeight;
    this.scheduleRender();
  }

  setScale(scale: number): void {
    const centerX = this.canvas.width / 2;
    const centerZ = this.canvas.height / 2;
    const worldX = (centerX - this.offsetX) / this.scale;
    const worldZ = (centerZ - this.offsetZ) / this.scale;
    
    this.scale = Math.max(0.0625, Math.min(16, scale));
    
    this.offsetX = centerX - worldX * this.scale;
    this.offsetZ = centerZ - worldZ * this.scale;
    
    this.scheduleRender();
    this.updateZoomDisplay();
    this.notifyViewportChange();
  }

  centerOn(x: number, z: number): void {
    this.offsetX = this.canvas.width / 2 - x * this.scale;
    this.offsetZ = this.canvas.height / 2 - z * this.scale;
    this.scheduleRender();
    this.notifyViewportChange();
  }

  addRegion(region: MapRegion): void {
    const key = `${region.regionX},${region.regionZ}`;
    this.regions.set(key, region);
    this.loadedRegionKeys.add(key);
    this.cache.delete(key);
  }

  hasRegion(x: number, z: number): boolean {
    return this.loadedRegionKeys.has(`${x},${z}`);
  }

  clearRegions(): void {
    this.regions.clear();
    this.loadedRegionKeys.clear();
    this.cache.clear();
  }

  private getLodLevel(scale: number): number {
    for (let i = 0; i < LOD_LEVELS.length; i++) {
      if (scale >= LOD_LEVELS[i].scale) {
        return i;
      }
    }
    return LOD_LEVELS.length - 1;
  }

  private pruneCache(): void {
    if (this.cache.size <= MAX_CACHE_SIZE) return;
    
    const entries = Array.from(this.cache.entries())
      .sort((a, b) => a[1].lastAccess - b[1].lastAccess);
    
    const toRemove = entries.slice(0, this.cache.size - MAX_CACHE_SIZE);
    for (const [key] of toRemove) {
      this.cache.delete(key);
    }
  }

  private getOrCreateCache(region: MapRegion, lodLevel: number): HTMLCanvasElement | null {
    const key = `${region.regionX},${region.regionZ}`;
    
    const cached = this.cache.get(key);
    if (cached) {
      if (cached.lodLevel === lodLevel) {
        cached.lastAccess = Date.now();
        return cached.canvas;
      }
    }
    
    const canvas = this.createRegionCanvas(region, lodLevel);
    if (!canvas) {
      return cached ? cached.canvas : null;
    }
    
    this.cache.set(key, {
      canvas,
      lastAccess: Date.now(),
      lodLevel
    });
    this.pruneCache();
    
    return canvas;
  }

  private createRegionCanvas(region: MapRegion, lodLevel: number): HTMLCanvasElement | null {
    const lodSize = LOD_LEVELS[lodLevel].size;
    const scaleFactor = lodSize / 512;
    
    const canvas = document.createElement('canvas');
    canvas.width = lodSize;
    canvas.height = lodSize;
    
    const ctx = canvas.getContext('2d');
    if (!ctx) return null;
    
    const imageData = ctx.createImageData(lodSize, lodSize);
    const data = imageData.data;
    
    const step = Math.max(1, Math.round(512 / lodSize));
    
    for (let chunkX = 0; chunkX < 8; chunkX++) {
      for (let chunkZ = 0; chunkZ < 8; chunkZ++) {
        const chunk = region.chunks[chunkX]?.[chunkZ];
        if (!chunk) continue;
        
        for (let tileX = 0; tileX < 4; tileX++) {
          for (let tileZ = 0; tileZ < 4; tileZ++) {
            const tile = chunk.tiles[tileX]?.[tileZ];
            if (!tile || !tile.blocks) continue;
            
            for (let x = 0; x < 16; x += step) {
              for (let z = 0; z < 16; z += step) {
                const block = tile.blocks[x]?.[z];
                if (!block || !block.s) continue;
                
                const worldPixelX = chunkX * 64 + tileX * 16 + x;
                const worldPixelZ = chunkZ * 64 + tileZ * 16 + z;
                
                const pixelX = Math.floor(worldPixelX * scaleFactor);
                const pixelZ = Math.floor(worldPixelZ * scaleFactor);
                
                if (pixelX >= lodSize || pixelZ >= lodSize) continue;
                
                const idx = (pixelZ * lodSize + pixelX) * 4;
                const color = this.getBlockColor(block);
                
                data[idx] = (color >> 16) & 0xFF;
                data[idx + 1] = (color >> 8) & 0xFF;
                data[idx + 2] = color & 0xFF;
                data[idx + 3] = 255;
              }
            }
          }
        }
      }
    }
    
    ctx.putImageData(imageData, 0, 0);
    return canvas;
  }

  private scheduleRender(): void {
    if (this.renderQueued) return;
    this.renderQueued = true;
    
    requestAnimationFrame(() => {
      this.renderQueued = false;
      this.doRender();
    });
  }

  render(): void {
    this.scheduleRender();
  }

  private doRender(): void {
    this.ctx.fillStyle = '#1a1a1a';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
    
    if (this.regions.size === 0) {
      this.ctx.fillStyle = '#666';
      this.ctx.font = '16px sans-serif';
      this.ctx.textAlign = 'center';
      this.ctx.fillText('请选择Xaero地图文件夹', this.canvas.width / 2, this.canvas.height / 2);
      return;
    }
    
    this.ctx.imageSmoothingEnabled = false;
    
    const bounds = this.getViewportBounds();
    const lodLevel = this.getLodLevel(this.scale);
    
    for (let rx = bounds.startX; rx <= bounds.endX; rx++) {
      for (let rz = bounds.startZ; rz <= bounds.endZ; rz++) {
        const key = `${rx},${rz}`;
        const region = this.regions.get(key);
        if (region) {
          this.renderRegionCached(region, lodLevel);
        }
      }
    }
  }

  private renderRegionCached(region: MapRegion, lodLevel: number): void {
    const cache = this.getOrCreateCache(region, lodLevel);
    
    const regionWorldX = region.regionX * 512;
    const regionWorldZ = region.regionZ * 512;
    
    const screenX = regionWorldX * this.scale + this.offsetX;
    const screenZ = regionWorldZ * this.scale + this.offsetZ;
    const screenSize = 512 * this.scale;
    
    if (screenX + screenSize < 0 || screenX > this.canvas.width ||
        screenZ + screenSize < 0 || screenZ > this.canvas.height) {
      return;
    }
    
    if (cache) {
      this.ctx.drawImage(cache, screenX, screenZ, screenSize, screenSize);
    } else {
      this.ctx.fillStyle = '#2a2a2a';
      this.ctx.fillRect(screenX, screenZ, screenSize, screenSize);
    }
  }

  private getBlockColor(block: MapBlock): number {
    if (!block.s) return 0x000000;
    
    let color = getBlockColor(block.s);
    
    if (isGrassBlock(block.s)) {
      color = getGrassColor(block.b);
    } else if (isFoliageBlock(block.s)) {
      color = getFoliageColor(block.b);
    } else if (isWaterBlock(block.s)) {
      color = getWaterColor(block.b);
    }
    
    if (this.options.showLighting && block.l < 15) {
      const factor = 0.4 + (block.l / 15) * 0.6;
      const r = Math.floor(((color >> 16) & 0xFF) * factor);
      const g = Math.floor(((color >> 8) & 0xFF) * factor);
      const b = Math.floor((color & 0xFF) * factor);
      color = (r << 16) | (g << 8) | b;
    }
    
    return color;
  }

  getStats(): { regionCount: number; loadedPixels: number; cacheSize: number } {
    let loadedPixels = 0;
    for (const region of this.regions.values()) {
      for (let cx = 0; cx < 8; cx++) {
        for (let cz = 0; cz < 8; cz++) {
          if (region.chunks[cx]?.[cz]) {
            loadedPixels += 64 * 64;
          }
        }
      }
    }
    return {
      regionCount: this.regions.size,
      loadedPixels,
      cacheSize: this.cache.size
    };
  }
}
