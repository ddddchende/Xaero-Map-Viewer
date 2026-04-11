package xaero.map.platform.services;

import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {
   public String getPlatformName() {
      return "Fabric";
   }

   public boolean isModLoaded(String modId) {
      return FabricLoader.getInstance().isModLoaded(modId);
   }

   public boolean isDevelopmentEnvironment() {
      return FabricLoader.getInstance().isDevelopmentEnvironment();
   }

   public boolean isDedicatedServer() {
      return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
   }

   public Path getGameDir() {
      return FabricLoader.getInstance().getGameDir().normalize();
   }

   public Path getConfigDir() {
      return FabricLoader.getInstance().getConfigDir();
   }
}
