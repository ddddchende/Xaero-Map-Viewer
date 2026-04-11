const API_BASE = 'http://localhost:3001/api';

interface RegionRequest {
  world: string;
  dim: string;
  x: number;
  z: number;
  mapType: string | null;
}

self.onmessage = async (e: MessageEvent<RegionRequest>) => {
  const { world, dim, x, z, mapType } = e.data;
  
  try {
    let url = `${API_BASE}/region?world=${encodeURIComponent(world)}&dim=${encodeURIComponent(dim)}&x=${x}&z=${z}`;
    if (mapType) {
      url += `&mapType=${encodeURIComponent(mapType)}`;
    }
    
    const response = await fetch(url);
    if (response.ok) {
      const region = await response.json();
      self.postMessage({ success: true, region, x, z });
    } else {
      self.postMessage({ success: false, error: `HTTP ${response.status}`, x, z });
    }
  } catch (error) {
    self.postMessage({ success: false, error: String(error), x, z });
  }
};

export {};
