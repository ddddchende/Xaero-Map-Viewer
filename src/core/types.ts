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

export interface Waypoint {
  id: string;
  name: string;
  x: number;
  y: number;
  z: number;
  color: number;
  symbol: string;
  type: number;
  disabled: boolean;
  rotation: boolean;
  yaw: number;
  temporary: boolean;
  global: boolean;
  setName: string;
  yIncluded: boolean;
  dimension: string;
}

export interface WaypointSet {
  name: string;
  waypoints: Waypoint[];
}

export interface WaypointWorld {
  worldId: string;
  sets: WaypointSet[];
}

export const REGION_SIZE = 512;
export const TILE_SIZE = 16;

export const WAYPOINT_COLORS = [
  0xFF0000,
  0xFF8800,
  0xFFFF00,
  0x88FF00,
  0x00FF00,
  0x00FF88,
  0x00FFFF,
  0x0088FF,
  0x0000FF,
  0x8800FF,
  0xFF00FF,
  0xFF0088,
  0xFFFFFF,
  0x888888,
  0x444444,
  0x000000
];
