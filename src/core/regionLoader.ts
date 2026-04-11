export function parseRegionFilename(filename: string): { x: number; z: number } | null {
  const match = filename.match(/^(-?\d+)_(-?\d+)\.(zip|xaero|xwmc)$/);
  if (!match) return null;
  return {
    x: parseInt(match[1], 10),
    z: parseInt(match[2], 10)
  };
}

export const REGION_SIZE = 512;
export const TILE_SIZE = 16;
