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
  private options: RenderOptions = DEFAULT_OPTIONS;
  
  private offsetX: number = 0;
  private offsetZ: number = 0;
  private scale: number = 1;
  
  private isDragging: boolean = false;
  private lastMouseX: number = 0;
  private lastMouseY: number = 0;
  
  private onViewportChange: ((bounds: ViewportBounds) => void) | null = null;
  private loadedRegionKeys: Set<string> = new Set();
  
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
    
    const gl = canvas.getContext('webgl', {
      alpha: false,
      antialias: false,
      preserveDrawingBuffer: true
    });
    
    if (!gl) {
      throw new Error('WebGL not supported');
    }
    
    this.gl = gl;
    
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
    const startX = Math.floor(-this.offsetX / this.scale / REGION_SIZE) - 1;
    const startZ = Math.floor(-this.offsetZ / this.scale / REGION_SIZE) - 1;
    const endX = Math.ceil((this.canvas.width - this.offsetX) / this.scale / REGION_SIZE) + 1;
    const endZ = Math.ceil((this.canvas.height - this.offsetZ) / this.scale / REGION_SIZE) + 1;
    
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
    
    this.canvas.width = container.clientWidth;
    this.canvas.height = container.clientHeight;
    
    this.gl.viewport(0, 0, this.canvas.width, this.canvas.height);
    this.scheduleRender();
  }

  setScale(scale: number): void {
    const centerX = this.canvas.width / 2;
    const centerZ = this.canvas.height / 2;
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
    this.offsetX = this.canvas.width / 2 - x * this.scale;
    this.offsetZ = this.canvas.height / 2 - z * this.scale;
    this.scheduleRender();
    this.notifyViewportChange();
  }

  private updateLodLevel(): void {
    let newLodLevel: number;
    if (this.scale >= 0.5) {
      newLodLevel = 0;
    } else if (this.scale >= 0.35) {
      newLodLevel = 1;
    } else if (this.scale >= 0.2) {
      newLodLevel = 2;
    } else if (this.scale >= 0.1) {
      newLodLevel = 3;
    } else if (this.scale >= 0.05) {
      newLodLevel = 4;
    } else if (this.scale >= 0.025) {
      newLodLevel = 5;
    } else if (this.scale >= 0.01) {
      newLodLevel = 6;
    } else {
      newLodLevel = 7;
    }
    
    if (newLodLevel !== this.currentLodLevel) {
      this.currentLodLevel = newLodLevel;
    }
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

  addRegionPixels(regionX: number, regionZ: number, pixelData: Uint8Array): void {
    const key = `${regionX},${regionZ}`;
    
    const oldSlot = this.regionSlots.get(key);
    if (oldSlot) {
      this.atlases[oldSlot.atlasIndex].freeSlots.push(oldSlot.slotIndex);
      this.regionSlots.delete(key);
    }
    
    const slot = this.allocateSlot(key);
    if (!slot) return;
    
    this.uploadToAtlas(slot, pixelData);
    
    this.loadedRegionKeys.add(key);
    this.slotLastAccess.set(key, Date.now());
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
    
    this.createAtlas();
    const slotIndex = this.atlases[this.atlases.length - 1].freeSlots.pop()!;
    const alloc: SlotAllocation = { atlasIndex: this.atlases.length - 1, slotIndex };
    this.regionSlots.set(key, alloc);
    return alloc;
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
    return this.loadedRegionKeys.has(`${x},${z}`);
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
  }

  clearRegions(): void {
    for (const slot of this.regionSlots.values()) {
      this.atlases[slot.atlasIndex].freeSlots.push(slot.slotIndex);
    }
    this.regionSlots.clear();
    this.slotLastAccess.clear();
    this.loadedRegionKeys.clear();
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
    
    if (this.loadedRegionKeys.size === 0) return;
    
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
    
    gl.useProgram(this.program);
    gl.uniform2f(this.resolutionLocation, this.canvas.width, this.canvas.height);
    gl.uniform1i(this.textureLocation, 0);
    
    gl.enableVertexAttribArray(this.dataLocation);
    
    this.renderBatched();
  }

  private renderBatched(): void {
    const gl = this.gl;
    const canvasW = this.canvas.width;
    const canvasH = this.canvas.height;
    const scale = this.scale;
    const offX = this.offsetX;
    const offZ = this.offsetZ;
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

  getStats(): { regionCount: number; atlasCount: number; lodLevel: number } {
    return {
      regionCount: this.loadedRegionKeys.size,
      atlasCount: this.atlases.length,
      lodLevel: this.currentLodLevel
    };
  }
}
