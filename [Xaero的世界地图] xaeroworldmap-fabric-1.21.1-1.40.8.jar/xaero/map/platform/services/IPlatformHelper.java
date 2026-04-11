package xaero.map.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {
   String getPlatformName();

   boolean isModLoaded(String var1);

   default boolean checkModForMixin(String modId) {
      return this.isModLoaded(modId);
   }

   boolean isDevelopmentEnvironment();

   default String getEnvironmentName() {
      return this.isDevelopmentEnvironment() ? "development" : "production";
   }

   boolean isDedicatedServer();

   Path getGameDir();

   Path getConfigDir();
}
