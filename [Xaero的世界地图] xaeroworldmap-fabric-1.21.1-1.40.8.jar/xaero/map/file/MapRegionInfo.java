package xaero.map.file;

import java.io.File;

public interface MapRegionInfo {
   boolean shouldCache();

   File getRegionFile();

   File getCacheFile();

   String getWorldId();

   String getDimId();

   String getMwId();

   int getRegionX();

   int getRegionZ();

   void setShouldCache(boolean var1, String var2);

   void setCacheFile(File var1);

   boolean hasLookedForCache();
}
