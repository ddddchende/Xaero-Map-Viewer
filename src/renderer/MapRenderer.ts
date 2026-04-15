import type { MapRegion, MapBlock } from '../core/types';
import { getBlockColor, getBlockAlpha, isGrassBlock, isFoliageBlock, isWaterBlock } from '../data/blockColors';
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

const REGION_SIZE = 512;
const MAX_ATLASES = 256;

const VERTEX_SHADER = `
attribute vec4 a_data;

uniform vec2 u_resolution;

varying vec2 v_texCoord;

void main() {
  vec2 pos = a_data.xy;
  vec2 clipSpace = (pos / u_resolution) * 2.0 - 1.0;
  gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);
  v_texCoord = a_data.zw;
}
`;

const FRAGMENT_SHADER = `
precision mediump float;

varying vec2 v_texCoord;
uniform sampler2D u_texture;

void main() {
  gl_FragColor = texture2D(u_texture, v_texCoord);
}
`;

interface AtlasInfo {
  texture: WebGLTexture;
  freeSlots: number[];
}

interface SlotAllocation {
  atlasIndex: number;
  slotIndex: number;
}

export class MapRenderer {
  private canvas: HTMLCanvasElement;
  private gl: WebGLRenderingContext;
  private overlayCanvas: HTMLCanvasElement;
  private overlayCtx: CanvasRenderingContext2D;
  private options: RenderOptions = DEFAULT_OPTIONS;
  
  private devicePixelRatio: number;
  
  private offsetX: number = 0;
  private offsetZ: number = 0;
  private scale: number = 1;
  
  private isDragging: boolean = false;
  private lastMouseX: number = 0;
  private lastMouseY: number = 0;
  private mouseWorldX: number | null = null;
  private mouseWorldZ: number | null = null;
  
  private touchStartDistance: number | null = null;
  private touchStartScale: number | null = null;
  private lastTouchX: number = 0;
  private lastTouchY: number = 0;
  
  private currentDim: string | null = null;
  
  private onViewportChangeCallback: ((bounds: ViewportBounds) => void) | null = null;
  private onLodChangeCallback: (() => void) | null = null;
  private loadedRegionKeys: Set<string> = new Set();
  private pendingRegionKeys: Set<string> = new Set();
  private allRegionKeys: Set<string> = new Set();
  private regionLodLevels: Map<string, number> = new Map();
  
  private renderQueued: boolean = false;
  private currentLodLevel: number = 0;
  
  private atlasSize: number;
  private slotsPerRow: number;
  private slotsPerAtlas: number;
  private atlases: AtlasInfo[] = [];
  private regionSlots: Map<string, SlotAllocation> = new Map();
  private slotLastAccess: Map<string, number> = new Map();
  
  private batchBuffer: WebGLBuffer;
  private vertexData: Float32Array = new Float32Array(0);
  
  private program: WebGLProgram;
  private dataLocation: number;
  private resolutionLocation: WebGLUniformLocation;
  private textureLocation: WebGLUniformLocation;

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    this.devicePixelRatio = Math.min(window.devicePixelRatio || 1, 2);
    
    const gl = canvas.getContext('webgl', {
      alpha: false,
      antialias: false,
      preserveDrawingBuffer: true
    });
    
    if (!gl) {
      throw new Error('WebGL not supported');
    }
    
    this.gl = gl;
    
    this.overlayCanvas = document.createElement('canvas');
    this.overlayCanvas.style.cssText = 'position:absolute;top:0;left:0;pointer-events:none;';
    canvas.parentElement?.appendChild(this.overlayCanvas);
    
    const ctx = this.overlayCanvas.getContext('2d');
    if (!ctx) {
      throw new Error('2D context not supported');
    }
    this.overlayCtx = ctx;
    
    const maxTextureSize = gl.getParameter(gl.MAX_TEXTURE_SIZE) as number;
    this.atlasSize = Math.min(4096, maxTextureSize);
    this.slotsPerRow = this.atlasSize / REGION_SIZE;
    this.slotsPerAtlas = this.slotsPerRow * this.slotsPerRow;
    
    this.program = this.createProgram(gl, VERTEX_SHADER, FRAGMENT_SHADER);
    
    this.dataLocation = gl.getAttribLocation(this.program, 'a_data');
    this.resolutionLocation = gl.getUniformLocation(this.program, 'u_resolution')!;
    this.textureLocation = gl.getUniformLocation(this.program, 'u_texture')!;
    
    this.batchBuffer = gl.createBuffer()!;
    
    this.setupEventListeners();
    this.resize();
  }

  private createProgram(gl: WebGLRenderingContext, vsSource: string, fsSource: string): WebGLProgram {
    const vs = this.compileShader(gl, gl.VERTEX_SHADER, vsSource);
    const fs = this.compileShader(gl, gl.FRAGMENT_SHADER, fsSource);
    
    const program = gl.createProgram()!;
    gl.attachShader(program, vs);
    gl.attachShader(program, fs);
    gl.linkProgram(program);
    
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      throw new Error('Program link failed: ' + gl.getProgramInfoLog(program));
    }
    
    return program;
  }

  private compileShader(gl: WebGLRenderingContext, type: number, source: string): WebGLShader {
    const shader = gl.createShader(type)!;
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      throw new Error('Shader compile failed: ' + gl.getShaderInfoLog(shader));
    }
    
    return shader;
  }

  setOnViewportChange(callback: (bounds: ViewportBounds) => void): void {
    this.onViewportChangeCallback = callback;
  }

  setOnLodChange(callback: () => void): void {
    this.onLodChangeCallback = callback;
  }

  setCurrentDimension(dim: string | null): void {
    this.currentDim = dim;
  }

  private isNetherDimension(): boolean {
    if (!this.currentDim) return false;
    const dim = this.currentDim.toLowerCase();
    return dim.includes('nether') || 
           this.currentDim.includes('DIM-1') || 
           dim.includes('下界');
  }

  private setupEventListeners(): void {
    this.canvas.addEventListener('wheel', this.handleWheel.bind(this));
    this.canvas.addEventListener('mousedown', this.handleMouseDown.bind(this));
    this.canvas.addEventListener('mousemove', this.handleMouseMove.bind(this));
    this.canvas.addEventListener('mouseup', this.handleMouseUp.bind(this));
    this.canvas.addEventListener('mouseleave', this.handleMouseUp.bind(this));
    this.canvas.addEventListener('touchstart', this.handleTouchStart.bind(this), { passive: false });
    this.canvas.addEventListener('touchmove', this.handleTouchMove.bind(this), { passive: false });
    this.canvas.addEventListener('touchend', this.handleTouchEnd.bind(this));
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
    this.scale = Math.max(0.005, Math.min(16, this.scale * zoomFactor));
    
    this.offsetX = mouseX - worldX * this.scale;
    this.offsetZ = mouseY - worldZ * this.scale;
    
    this.updateLodLevel();
    this.scheduleRender();
    this.updateZoomDisplay();
    this.notifyViewportChange();
  }

  private handleMouseDown(e: MouseEvent): void {
    if (e.button === 0) {
      this.isDragging = true;
      this.lastMouseX = e.clientX;
      this.lastMouseY = e.clientY;
    }
  }

  private handleMouseMove(e: MouseEvent): void {
    const rect = this.canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;
    
    const worldX = Math.floor((mouseX - this.offsetX) / this.scale);
    const worldZ = Math.floor((mouseY - this.offsetZ) / this.scale);
    
    this.mouseWorldX = worldX;
    this.mouseWorldZ = worldZ;
    
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
    } else {
      this.scheduleRender();
    }
  }

  private handleMouseUp(): void {
    this.isDragging = false;
  }

  private getTouchDistance(touches: TouchList): number {
    if (touches.length < 2) return 0;
    const dx = touches[0].clientX - touches[1].clientX;
    const dy = touches[0].clientY - touches[1].clientY;
    return Math.sqrt(dx * dx + dy * dy);
  }

  private getTouchCenter(touches: TouchList): { x: number; y: number } {
    if (touches.length === 1) {
      return { x: touches[0].clientX, y: touches[0].clientY };
    }
    return {
      x: (touches[0].clientX + touches[1].clientX) / 2,
      y: (touches[0].clientY + touches[1].clientY) / 2
    };
  }

  private handleTouchStart(e: TouchEvent): void {
    e.preventDefault();
    
    if (e.touches.length === 1) {
      this.isDragging = true;
      this.lastTouchX = e.touches[0].clientX;
      this.lastTouchY = e.touches[0].clientY;
      
      const rect = this.canvas.getBoundingClientRect();
      const touchX = e.touches[0].clientX - rect.left;
      const touchY = e.touches[0].clientY - rect.top;
      this.mouseWorldX = Math.floor((touchX - this.offsetX) / this.scale);
      this.mouseWorldZ = Math.floor((touchY - this.offsetZ) / this.scale);
      this.updateCoordsDisplay(this.mouseWorldX, this.mouseWorldZ);
      this.scheduleRender();
    } else if (e.touches.length === 2) {
      this.isDragging = false;
      this.touchStartDistance = this.getTouchDistance(e.touches);
      this.touchStartScale = this.scale;
      const center = this.getTouchCenter(e.touches);
      this.lastTouchX = center.x;
      this.lastTouchY = center.y;
    }
  }

  private handleTouchMove(e: TouchEvent): void {
    e.preventDefault();
    
    if (e.touches.length === 1 && this.isDragging) {
      const rect = this.canvas.getBoundingClientRect();
      const touchX = e.touches[0].clientX - rect.left;
      const touchY = e.touches[0].clientY - rect.top;
      
      this.mouseWorldX = Math.floor((touchX - this.offsetX) / this.scale);
      this.mouseWorldZ = Math.floor((touchY - this.offsetZ) / this.scale);
      this.updateCoordsDisplay(this.mouseWorldX, this.mouseWorldZ);
      
      const deltaX = e.touches[0].clientX - this.lastTouchX;
      const deltaY = e.touches[0].clientY - this.lastTouchY;
      this.offsetX += deltaX;
      this.offsetZ += deltaY;
      this.lastTouchX = e.touches[0].clientX;
      this.lastTouchY = e.touches[0].clientY;
      this.scheduleRender();
      this.notifyViewportChange();
    } else if (e.touches.length === 2 && this.touchStartDistance !== null && this.touchStartScale !== null) {
      const currentDistance = this.getTouchDistance(e.touches);
      const scaleRatio = currentDistance / this.touchStartDistance;
      const newScale = Math.max(0.005, Math.min(16, this.touchStartScale * scaleRatio));
      
      const center = this.getTouchCenter(e.touches);
      const rect = this.canvas.getBoundingClientRect();
      const touchX = center.x - rect.left;
      const touchY = center.y - rect.top;
      
      const worldX = (touchX - this.offsetX) / this.scale;
      const worldZ = (touchY - this.offsetZ) / this.scale;
      
      this.scale = newScale;
      this.offsetX = touchX - worldX * this.scale;
      this.offsetZ = touchY - worldZ * this.scale;
      
      this.mouseWorldX = Math.floor(worldX);
      this.mouseWorldZ = Math.floor(worldZ);
      this.updateCoordsDisplay(this.mouseWorldX, this.mouseWorldZ);
      
      this.lastTouchX = center.x;
      this.lastTouchY = center.y;
      
      this.updateLodLevel();
      this.scheduleRender();
      this.updateZoomDisplay();
      this.notifyViewportChange();
    }
  }

  private handleTouchEnd(e: TouchEvent): void {
    if (e.touches.length === 0) {
      this.isDragging = false;
      this.touchStartDistance = null;
      this.touchStartScale = null;
    } else if (e.touches.length === 1) {
      this.isDragging = true;
      this.lastTouchX = e.touches[0].clientX;
      this.lastTouchY = e.touches[0].clientY;
      this.touchStartDistance = null;
      this.touchStartScale = null;
    }
  }

  private notifyViewportChange(): void {
    if (this.onViewportChangeCallback) {
      const bounds = this.getViewportBounds();
      this.onViewportChangeCallback(bounds);
    }
  }

  getViewportBounds(): ViewportBounds {
    const displayWidth = this.canvas.width / this.devicePixelRatio;
    const displayHeight = this.canvas.height / this.devicePixelRatio;
    const startX = Math.floor(-this.offsetX / this.scale / REGION_SIZE) - 1;
    const startZ = Math.floor(-this.offsetZ / this.scale / REGION_SIZE) - 1;
    const endX = Math.ceil((displayWidth - this.offsetX) / this.scale / REGION_SIZE) + 1;
    const endZ = Math.ceil((displayHeight - this.offsetZ) / this.scale / REGION_SIZE) + 1;
    
    return { startX, startZ, endX, endZ };
  }

  private updateCoordsDisplay(x: number, z: number): void {
    const coordsEl = document.getElementById('coords');
    if (coordsEl) {
      const text = `X: ${x}, Z: ${z}`;
      coordsEl.textContent = text;
    }
  }

  private updateZoomDisplay(): void {
    const zoomEl = document.getElementById('zoomLevel');
    if (zoomEl) {
      if (this.scale >= 1) {
        zoomEl.textContent = `${this.scale.toFixed(1)}x`;
      } else {
        zoomEl.textContent = `${this.scale.toFixed(3)}x`;
      }
    }
    const sliderEl = document.getElementById('zoomSlider') as HTMLInputElement;
    if (sliderEl) {
      sliderEl.value = String(Math.log2(this.scale).toFixed(1));
    }
  }

  resize(): void {
    const container = this.canvas.parentElement;
    if (!container) return;
    
    const displayWidth = container.clientWidth;
    const displayHeight = container.clientHeight;
    
    this.canvas.style.width = displayWidth + 'px';
    this.canvas.style.height = displayHeight + 'px';
    
    const actualWidth = Math.floor(displayWidth * this.devicePixelRatio);
    const actualHeight = Math.floor(displayHeight * this.devicePixelRatio);
    
    this.canvas.width = actualWidth;
    this.canvas.height = actualHeight;
    
    this.overlayCanvas.style.width = displayWidth + 'px';
    this.overlayCanvas.style.height = displayHeight + 'px';
    this.overlayCanvas.width = Math.floor(displayWidth * this.devicePixelRatio);
    this.overlayCanvas.height = Math.floor(displayHeight * this.devicePixelRatio);
    
    this.overlayCtx.scale(this.devicePixelRatio, this.devicePixelRatio);
    
    this.gl.viewport(0, 0, actualWidth, actualHeight);
    this.scheduleRender();
  }

  setScale(scale: number): void {
    const displayWidth = this.canvas.width / this.devicePixelRatio;
    const displayHeight = this.canvas.height / this.devicePixelRatio;
    const centerX = displayWidth / 2;
    const centerZ = displayHeight / 2;
    const worldX = (centerX - this.offsetX) / this.scale;
    const worldZ = (centerZ - this.offsetZ) / this.scale;
    
    this.scale = Math.max(0.005, Math.min(16, scale));
    
    this.offsetX = centerX - worldX * this.scale;
    this.offsetZ = centerZ - worldZ * this.scale;
    
    this.updateLodLevel();
    this.scheduleRender();
    this.updateZoomDisplay();
    this.notifyViewportChange();
  }

  centerOn(x: number, z: number): void {
    const displayWidth = this.canvas.width / this.devicePixelRatio;
    const displayHeight = this.canvas.height / this.devicePixelRatio;
    this.offsetX = displayWidth / 2 - x * this.scale;
    this.offsetZ = displayHeight / 2 - z * this.scale;
    this.scheduleRender();
    this.notifyViewportChange();
  }

  getViewState(): { scale: number; centerX: number; centerZ: number } {
    const displayWidth = this.canvas.width / this.devicePixelRatio;
    const displayHeight = this.canvas.height / this.devicePixelRatio;
    const centerX = (displayWidth / 2 - this.offsetX) / this.scale;
    const centerZ = (displayHeight / 2 - this.offsetZ) / this.scale;
    return { scale: this.scale, centerX, centerZ };
  }

  setViewState(scale: number, centerX: number, centerZ: number): void {
    const displayWidth = this.canvas.width / this.devicePixelRatio;
    const displayHeight = this.canvas.height / this.devicePixelRatio;
    this.scale = Math.max(0.005, Math.min(16, scale));
    this.offsetX = displayWidth / 2 - centerX * this.scale;
    this.offsetZ = displayHeight / 2 - centerZ * this.scale;
    this.updateLodLevel();
    this.updateZoomDisplay();
    this.scheduleRender();
    this.notifyViewportChange();
  }

  private updateLodLevel(): void {
    let newLodLevel: number;
    if (this.scale >= 0.25) {
      newLodLevel = 0;
    } else if (this.scale >= 0.1) {
      newLodLevel = 1;
    } else if (this.scale >= 0.04) {
      newLodLevel = 2;
    } else {
      newLodLevel = 3;
    }
    
    if (newLodLevel !== this.currentLodLevel) {
      this.currentLodLevel = newLodLevel;
      if (this.onLodChangeCallback) {
        this.onLodChangeCallback();
      }
    }
  }

  clearAllRegions(): void {
    for (const atlas of this.atlases) {
      atlas.freeSlots = Array.from({ length: this.slotsPerAtlas }, (_, i) => i);
    }
    this.regionSlots.clear();
    this.loadedRegionKeys.clear();
    this.pendingRegionKeys.clear();
    this.allRegionKeys.clear();
    this.regionLodLevels.clear();
    this.slotLastAccess.clear();
    this.scheduleRender();
  }

  setPendingRegions(regions: Set<string>): void {
    this.pendingRegionKeys = regions;
  }

  setAllRegions(regions: Set<string>): void {
    this.allRegionKeys = regions;
  }

  getLoadingStats(): { loaded: number; pending: number; total: number } {
    return {
      loaded: this.loadedRegionKeys.size,
      pending: this.pendingRegionKeys.size,
      total: this.allRegionKeys.size
    };
  }

  addRegion(region: MapRegion): void {
    const key = `${region.regionX},${region.regionZ}`;
    
    const oldSlot = this.regionSlots.get(key);
    if (oldSlot) {
      this.atlases[oldSlot.atlasIndex].freeSlots.push(oldSlot.slotIndex);
      this.regionSlots.delete(key);
    }
    
    const pixelData = this.createRegionPixelData(region);
    
    const slot = this.allocateSlot(key);
    if (!slot) return;
    
    this.uploadToAtlas(slot, pixelData);
    
    this.loadedRegionKeys.add(key);
    this.slotLastAccess.set(key, Date.now());
  }

  addRegionPixels(regionX: number, regionZ: number, pixelData: Uint8Array, lod: number = 0): void {
    const key = `${regionX},${regionZ}`;
    
    const oldSlot = this.regionSlots.get(key);
    if (oldSlot) {
      this.atlases[oldSlot.atlasIndex].freeSlots.push(oldSlot.slotIndex);
      this.regionSlots.delete(key);
    }
    
    const slot = this.allocateSlot(key);
    if (!slot) return;
    
    let finalPixelData = pixelData;
    if (lod > 0) {
      finalPixelData = this.upsamplePixels(pixelData, 512 >> lod, 512);
    }
    
    this.uploadToAtlas(slot, finalPixelData);
    
    this.loadedRegionKeys.add(key);
    this.regionLodLevels.set(key, lod);
    this.slotLastAccess.set(key, Date.now());
  }

  private upsamplePixels(pixels: Uint8Array, srcSize: number, dstSize: number): Uint8Array {
    const result = new Uint8Array(dstSize * dstSize * 4);
    const ratio = srcSize / dstSize;
    
    for (let dy = 0; dy < dstSize; dy++) {
      const sy = Math.floor(dy * ratio);
      const srcRowOffset = sy * srcSize * 4;
      const dstRowOffset = dy * dstSize * 4;
      
      for (let dx = 0; dx < dstSize; dx++) {
        const sx = Math.floor(dx * ratio);
        const srcIdx = srcRowOffset + sx * 4;
        const dstIdx = dstRowOffset + dx * 4;
        
        result[dstIdx] = pixels[srcIdx];
        result[dstIdx + 1] = pixels[srcIdx + 1];
        result[dstIdx + 2] = pixels[srcIdx + 2];
        result[dstIdx + 3] = pixels[srcIdx + 3];
      }
    }
    
    return result;
  }

  private allocateSlot(key: string): SlotAllocation | null {
    for (let i = 0; i < this.atlases.length; i++) {
      if (this.atlases[i].freeSlots.length > 0) {
        const slotIndex = this.atlases[i].freeSlots.pop()!;
        const alloc: SlotAllocation = { atlasIndex: i, slotIndex };
        this.regionSlots.set(key, alloc);
        return alloc;
      }
    }

    if (this.atlases.length < MAX_ATLASES) {
      this.createAtlas();
      const slotIndex = this.atlases[this.atlases.length - 1].freeSlots.pop()!;
      const alloc: SlotAllocation = { atlasIndex: this.atlases.length - 1, slotIndex };
      this.regionSlots.set(key, alloc);
      return alloc;
    }

    if (!this.evictOldestSlot()) return null;

    for (let i = 0; i < this.atlases.length; i++) {
      if (this.atlases[i].freeSlots.length > 0) {
        const slotIndex = this.atlases[i].freeSlots.pop()!;
        const alloc: SlotAllocation = { atlasIndex: i, slotIndex };
        this.regionSlots.set(key, alloc);
        return alloc;
      }
    }

    return null;
  }

  private createAtlas(): void {
    const gl = this.gl;
    const texture = gl.createTexture()!;
    
    gl.bindTexture(gl.TEXTURE_2D, texture);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, this.atlasSize, this.atlasSize, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    
    const freeSlots: number[] = [];
    for (let i = this.slotsPerAtlas - 1; i >= 0; i--) {
      freeSlots.push(i);
    }
    
    this.atlases.push({ texture, freeSlots });
  }

  private evictOldestSlot(): boolean {
    let oldestKey = '';
    let oldestTime = Infinity;
    for (const [key, time] of this.slotLastAccess) {
      if (time < oldestTime) {
        oldestTime = time;
        oldestKey = key;
      }
    }
    if (!oldestKey) return false;

    const slot = this.regionSlots.get(oldestKey);
    if (slot) {
      this.atlases[slot.atlasIndex].freeSlots.push(slot.slotIndex);
      this.regionSlots.delete(oldestKey);
    }
    this.slotLastAccess.delete(oldestKey);
    this.loadedRegionKeys.delete(oldestKey);
    this.regionLodLevels.delete(oldestKey);
    return true;
  }

  private uploadToAtlas(slot: SlotAllocation, pixelData: Uint8Array): void {
    const gl = this.gl;
    const atlas = this.atlases[slot.atlasIndex];
    const col = slot.slotIndex % this.slotsPerRow;
    const row = Math.floor(slot.slotIndex / this.slotsPerRow);
    
    gl.bindTexture(gl.TEXTURE_2D, atlas.texture);
    gl.texSubImage2D(
      gl.TEXTURE_2D, 0,
      col * REGION_SIZE, row * REGION_SIZE,
      REGION_SIZE, REGION_SIZE,
      gl.RGBA, gl.UNSIGNED_BYTE,
      pixelData
    );
  }

  private createRegionPixelData(region: MapRegion): Uint8Array {
    const size = REGION_SIZE;
    const pixelData = new Uint8Array(size * size * 4);
    
    for (let chunkX = 0; chunkX < 8; chunkX++) {
      for (let chunkZ = 0; chunkZ < 8; chunkZ++) {
        const chunk = region.chunks[chunkX]?.[chunkZ];
        if (!chunk) continue;
        
        for (let tileX = 0; tileX < 4; tileX++) {
          for (let tileZ = 0; tileZ < 4; tileZ++) {
            const tile = chunk.tiles[tileX]?.[tileZ];
            if (!tile || !tile.blocks) continue;
            
            for (let x = 0; x < 16; x++) {
              for (let z = 0; z < 16; z++) {
                const block = tile.blocks[x]?.[z];
                if (!block || !block.s) continue;
                
                const pixelX = chunkX * 64 + tileX * 16 + x;
                const pixelZ = chunkZ * 64 + tileZ * 16 + z;
                
                const idx = (pixelZ * size + pixelX) * 4;
                const { color, alpha } = this.getBlockColorAndAlpha(block);
                
                pixelData[idx] = (color >> 16) & 0xFF;
                pixelData[idx + 1] = (color >> 8) & 0xFF;
                pixelData[idx + 2] = color & 0xFF;
                pixelData[idx + 3] = alpha;
              }
            }
          }
        }
      }
    }
    
    return pixelData;
  }

  hasRegion(x: number, z: number): boolean {
    const key = `${x},${z}`;
    if (!this.loadedRegionKeys.has(key)) return false;
    const loadedLod = this.regionLodLevels.get(key);
    if (loadedLod === undefined) return false;
    return loadedLod <= this.currentLodLevel;
  }

  hasAnyLod(x: number, z: number): boolean {
    const key = `${x},${z}`;
    return this.loadedRegionKeys.has(key) && this.regionLodLevels.has(key);
  }

  getRegionLod(x: number, z: number): number | undefined {
    const key = `${x},${z}`;
    return this.regionLodLevels.get(key);
  }

  removeRegion(x: number, z: number): void {
    const key = `${x},${z}`;
    const slot = this.regionSlots.get(key);
    if (slot) {
      this.atlases[slot.atlasIndex].freeSlots.push(slot.slotIndex);
      this.regionSlots.delete(key);
    }
    this.slotLastAccess.delete(key);
    this.loadedRegionKeys.delete(key);
    this.regionLodLevels.delete(key);
  }

  clearRegions(): void {
    for (const slot of this.regionSlots.values()) {
      this.atlases[slot.atlasIndex].freeSlots.push(slot.slotIndex);
    }
    this.regionSlots.clear();
    this.slotLastAccess.clear();
    this.loadedRegionKeys.clear();
    this.pendingRegionKeys.clear();
    this.regionLodLevels.clear();
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
    const gl = this.gl;
    
    gl.clearColor(0, 0, 0, 1);
    gl.clear(gl.COLOR_BUFFER_BIT);
    
    if (this.loadedRegionKeys.size === 0) {
      this.renderOverlay();
      return;
    }
    
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
    
    gl.useProgram(this.program);
    gl.uniform2f(this.resolutionLocation, this.canvas.width, this.canvas.height);
    gl.uniform1i(this.textureLocation, 0);
    
    gl.enableVertexAttribArray(this.dataLocation);
    
    this.renderBatched();
    this.renderOverlay();
  }
  
  private renderOverlay(): void {
    const ctx = this.overlayCtx;
    const w = this.overlayCanvas.width / this.devicePixelRatio;
    const h = this.overlayCanvas.height / this.devicePixelRatio;
    
    ctx.setTransform(this.devicePixelRatio, 0, 0, this.devicePixelRatio, 0, 0);
    ctx.clearRect(0, 0, w, h);
    
    this.renderPendingPlaceholders(ctx, w, h);
    
    if (this.mouseWorldX !== null && this.mouseWorldZ !== null) {
      const isNether = this.isNetherDimension();
      
      let text: string;
      if (isNether) {
        const overworldX = this.mouseWorldX * 8;
        const overworldZ = this.mouseWorldZ * 8;
        text = `X: ${this.mouseWorldX}, Z: ${this.mouseWorldZ}  [X: ${overworldX}, Z: ${overworldZ}]`;
      } else {
        const netherX = Math.floor(this.mouseWorldX / 8);
        const netherZ = Math.floor(this.mouseWorldZ / 8);
        text = `X: ${this.mouseWorldX}, Z: ${this.mouseWorldZ}  [X: ${netherX}, Z: ${netherZ}]`;
      }
      
      let fontSize = 24;
      const minFontSize = 14;
      const padding = 16;
      const maxWidth = w - padding * 2;
      
      ctx.font = `${fontSize}px "Minecraft", "Courier New", monospace`;
      let textWidth = ctx.measureText(text).width;
      
      while (textWidth > maxWidth && fontSize > minFontSize) {
        fontSize -= 1;
        ctx.font = `${fontSize}px "Minecraft", "Courier New", monospace`;
        textWidth = ctx.measureText(text).width;
      }
      
      ctx.textAlign = 'center';
      ctx.textBaseline = 'top';
      
      const x = w / 2;
      const y = 12;
      const boxHeight = fontSize + 8;
      
      ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
      ctx.fillRect(x - textWidth / 2 - 8, y - 4, textWidth + 16, boxHeight);
      
      ctx.fillStyle = '#FFFFFF';
      ctx.fillText(text, x, y);
    }
    
    this.renderLoadingProgress(ctx, w, h);
  }
  
  private renderPendingPlaceholders(ctx: CanvasRenderingContext2D, w: number, h: number): void {
    if (this.allRegionKeys.size === 0) return;
    
    const bounds = this.getViewportBounds();
    
    for (let rx = bounds.startX; rx <= bounds.endX; rx++) {
      for (let rz = bounds.startZ; rz <= bounds.endZ; rz++) {
        const key = `${rx},${rz}`;
        if (!this.allRegionKeys.has(key)) continue;
        if (this.loadedRegionKeys.has(key)) continue;
        
        const screenX = rx * REGION_SIZE * this.scale + this.offsetX;
        const screenZ = rz * REGION_SIZE * this.scale + this.offsetZ;
        const size = REGION_SIZE * this.scale;
        
        if (screenX + size < 0 || screenX > w || screenZ + size < 0 || screenZ > h) continue;
        
        ctx.fillStyle = 'rgba(60, 60, 80, 0.3)';
        ctx.fillRect(screenX, screenZ, size, size);
        
        ctx.strokeStyle = 'rgba(145, 145, 145, 0.25)';
        ctx.lineWidth = 1;
        ctx.strokeRect(screenX + 0.5, screenZ + 0.5, size - 1, size - 1);
      }
    }
  }
  
  private renderLoadingProgress(ctx: CanvasRenderingContext2D, w: number, h: number): void {
    const stats = this.getLoadingStats();
    if (stats.pending === 0) return;
    
    const text = `请求中: ${stats.pending} 个区域`;
    
    ctx.font = '14px "Minecraft", "Courier New", monospace';
    ctx.textAlign = 'right';
    ctx.textBaseline = 'bottom';
    
    const textWidth = ctx.measureText(text).width;
    const padding = 8;
    const boxWidth = textWidth + padding * 2;
    const boxHeight = 24;
    const x = w - 10;
    const y = h - 10;
    
    ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    ctx.fillRect(x - boxWidth, y - boxHeight, boxWidth, boxHeight);
    
    ctx.fillStyle = '#AAAAAA';
    ctx.fillText(text, x - padding, y - padding);
  }

  private renderBatched(): void {
    const gl = this.gl;
    const canvasW = this.canvas.width;
    const canvasH = this.canvas.height;
    const scale = this.scale * this.devicePixelRatio;
    const offX = this.offsetX * this.devicePixelRatio;
    const offZ = this.offsetZ * this.devicePixelRatio;
    const spr = this.slotsPerRow;
    const now = Date.now();
    
    const atlasFloatCounts: Map<number, number> = new Map();
    const atlasOffsets: Map<number, number> = new Map();
    
    let totalFloats = 0;
    
    for (const key of this.loadedRegionKeys) {
      const slot = this.regionSlots.get(key);
      if (!slot) continue;
      
      const sep = key.indexOf(',');
      const rx = parseInt(key.substring(0, sep), 10);
      const rz = parseInt(key.substring(sep + 1), 10);
      
      const screenX = rx * REGION_SIZE * scale + offX;
      const screenZ = rz * REGION_SIZE * scale + offZ;
      const size = REGION_SIZE * scale;
      
      if (screenX + size < 0 || screenX > canvasW ||
          screenZ + size < 0 || screenZ > canvasH) continue;
      
      const count = (atlasFloatCounts.get(slot.atlasIndex) || 0) + 24;
      atlasFloatCounts.set(slot.atlasIndex, count);
      totalFloats += 24;
    }
    
    if (totalFloats === 0) return;
    
    let accumOffset = 0;
    const sortedAtlases = Array.from(atlasFloatCounts.keys()).sort((a, b) => a - b);
    for (const ai of sortedAtlases) {
      atlasOffsets.set(ai, accumOffset);
      accumOffset += atlasFloatCounts.get(ai)!;
    }
    
    const currentOffsets = new Map(atlasOffsets);
    if (this.vertexData.length < totalFloats) {
      this.vertexData = new Float32Array(totalFloats);
    }
    const data = this.vertexData;
    
    for (const key of this.loadedRegionKeys) {
      const slot = this.regionSlots.get(key);
      if (!slot) continue;
      
      const sep = key.indexOf(',');
      const rx = parseInt(key.substring(0, sep), 10);
      const rz = parseInt(key.substring(sep + 1), 10);
      
      const screenX = rx * REGION_SIZE * scale + offX;
      const screenZ = rz * REGION_SIZE * scale + offZ;
      const size = REGION_SIZE * scale;
      
      if (screenX + size < 0 || screenX > canvasW ||
          screenZ + size < 0 || screenZ > canvasH) continue;
      
      const col = slot.slotIndex % spr;
      const row = Math.floor(slot.slotIndex / spr);
      const u0 = col / spr;
      const v0 = row / spr;
      const u1 = (col + 1) / spr;
      const v1 = (row + 1) / spr;
      
      const sx2 = screenX + size;
      const sz2 = screenZ + size;
      
      const o = currentOffsets.get(slot.atlasIndex)!;
      currentOffsets.set(slot.atlasIndex, o + 24);
      
      data[o]    = screenX; data[o+1]  = screenZ; data[o+2]  = u0; data[o+3]  = v0;
      data[o+4]  = sx2;     data[o+5]  = screenZ; data[o+6]  = u1; data[o+7]  = v0;
      data[o+8]  = screenX; data[o+9]  = sz2;     data[o+10] = u0; data[o+11] = v1;
      data[o+12] = screenX; data[o+13] = sz2;     data[o+14] = u0; data[o+15] = v1;
      data[o+16] = sx2;     data[o+17] = screenZ; data[o+18] = u1; data[o+19] = v0;
      data[o+20] = sx2;     data[o+21] = sz2;     data[o+22] = u1; data[o+23] = v1;
      
      this.slotLastAccess.set(key, now);
    }
    
    gl.bindBuffer(gl.ARRAY_BUFFER, this.batchBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, data.subarray(0, totalFloats), gl.DYNAMIC_DRAW);
    
    gl.vertexAttribPointer(this.dataLocation, 4, gl.FLOAT, false, 0, 0);
    
    for (const ai of sortedAtlases) {
      const offset = atlasOffsets.get(ai)!;
      const count = atlasFloatCounts.get(ai)!;
      
      gl.activeTexture(gl.TEXTURE0);
      gl.bindTexture(gl.TEXTURE_2D, this.atlases[ai].texture);
      
      gl.drawArrays(gl.TRIANGLES, offset / 4, count / 4);
    }
  }

  private getBlockColorAndAlpha(block: MapBlock): { color: number; alpha: number } {
    if (!block.s) return { color: 0x000000, alpha: 0 };
    
    let color = getBlockColor(block.s);
    let alpha = getBlockAlpha(block.s);
    
    if (isGrassBlock(block.s)) {
      color = getGrassColor(block.b);
    } else if (isFoliageBlock(block.s)) {
      color = getFoliageColor(block.b);
    } else if (isWaterBlock(block.s)) {
      color = getWaterColor(block.b);
      alpha = 180;
    }
    
    if (this.options.showLighting && block.l < 15) {
      const factor = 0.4 + (block.l / 15) * 0.6;
      const r = Math.floor(((color >> 16) & 0xFF) * factor);
      const g = Math.floor(((color >> 8) & 0xFF) * factor);
      const b = Math.floor((color & 0xFF) * factor);
      color = (r << 16) | (g << 8) | b;
    }
    
    return { color, alpha };
  }

  getCurrentLodLevel(): number {
    return this.currentLodLevel;
  }

  getStats(): { regionCount: number; atlasCount: number; lodLevel: number } {
    return {
      regionCount: this.loadedRegionKeys.size,
      atlasCount: this.atlases.length,
      lodLevel: this.currentLodLevel
    };
  }
}
