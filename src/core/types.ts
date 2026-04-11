export interface MapBlock {
  s: string | null;
  h: number;
  l: number;
  b: string | null;
}

export interface MapTile {
  blocks: MapBlock[][];
  loaded: boolean;
}

export interface MapTileChunk {
  x: number;
  z: number;
  tiles: (MapTile | null)[][];
  loadState: number;
}

export interface MapRegion {
  regionX: number;
  regionZ: number;
  worldId: string;
  dimId: string;
  mwId: string;
  caveLayer: number;
  chunks: (MapTileChunk | null)[][];
  version: number;
}

export const REGION_SIZE = 512;
export const TILE_SIZE = 16;
