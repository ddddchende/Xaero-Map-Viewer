package xaero.map.common.config;

import com.google.common.collect.Sets;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import net.minecraft.class_1937;
import net.minecraft.class_2960;
import xaero.lib.common.config.profile.ConfigProfile;
import xaero.lib.common.util.IOUtils;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.common.config.primary.option.WorldMapPrimaryCommonConfigOptions;

public class LegacyCommonConfigIO {
   private final Path configFilePath;
   private boolean allowCaveModeOnServer;
   private boolean allowNetherCaveModeOnServer;
   private boolean shouldEnableEveryoneTracksEveryone;

   public LegacyCommonConfigIO(Path configFilePath) {
      this.configFilePath = configFilePath;
   }

   public void load() {
      ConfigProfile defaultEnforcedProfile = WorldMap.INSTANCE.getConfigs().getServerConfigManager().getDefaultEnforcedProfile();

      try {
         BufferedInputStream bufferedOutput = new BufferedInputStream(new FileInputStream(this.configFilePath.toFile()));

         label191: {
            label190: {
               label189: {
                  try {
                     BufferedReader reader;
                     label186: {
                        label185: {
                           label197: {
                              reader = new BufferedReader(new InputStreamReader(bufferedOutput));

                              try {
                                 try {
                                    while(true) {
                                       String line;
                                       if ((line = reader.readLine()) == null) {
                                          if (this.allowCaveModeOnServer && this.allowNetherCaveModeOnServer) {
                                             break label185;
                                          }

                                          if (!this.allowCaveModeOnServer && !this.allowNetherCaveModeOnServer) {
                                             defaultEnforcedProfile.set(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED, false);
                                             break label197;
                                          }

                                          if (this.allowCaveModeOnServer) {
                                             defaultEnforcedProfile.set(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED_DIMENSIONS, Sets.newHashSet(new class_2960[]{class_1937.field_25179.method_29177(), class_1937.field_25181.method_29177()}));
                                             break label186;
                                          }

                                          defaultEnforcedProfile.set(WorldMapProfiledConfigOptions.CAVE_MODE_ALLOWED_DIMENSIONS, Sets.newHashSet(new class_2960[]{class_1937.field_25180.method_29177()}));
                                          break;
                                       }

                                       this.readLine(line.split(":"));
                                    }
                                 } finally {
                                    WorldMap.INSTANCE.getConfigs().getPrimaryCommonConfigManagerIO().save();
                                    WorldMap.INSTANCE.getConfigs().getServerConfigProfileIO().save(defaultEnforcedProfile);
                                    reader.close();
                                    IOUtils.tryQuickFileBackupMove(this.configFilePath, 10);
                                 }
                              } catch (Throwable var15) {
                                 try {
                                    reader.close();
                                 } catch (Throwable var13) {
                                    var15.addSuppressed(var13);
                                 }

                                 throw var15;
                              }

                              reader.close();
                              break label191;
                           }

                           reader.close();
                           break label189;
                        }

                        reader.close();
                        break label190;
                     }

                     reader.close();
                  } catch (Throwable var16) {
                     try {
                        bufferedOutput.close();
                     } catch (Throwable var12) {
                        var16.addSuppressed(var12);
                     }

                     throw var16;
                  }

                  bufferedOutput.close();
                  return;
               }

               bufferedOutput.close();
               return;
            }

            bufferedOutput.close();
            return;
         }

         bufferedOutput.close();
      } catch (IOException var17) {
         throw new RuntimeException(var17);
      }
   }

   private boolean readLine(String[] args) {
      if (args[0].equals("allowCaveModeOnServer")) {
         this.allowCaveModeOnServer = args[1].equals("true");
         return true;
      } else if (args[0].equals("allowNetherCaveModeOnServer")) {
         this.allowNetherCaveModeOnServer = args[1].equals("true");
         return true;
      } else if (args[0].equals("registerStatusEffects")) {
         WorldMap.INSTANCE.getConfigs().getPrimaryCommonConfigManager().getConfig().set(WorldMapPrimaryCommonConfigOptions.REGISTER_EFFECTS, args[1].equals("true"));
         return true;
      } else if (args[0].equals("everyoneTracksEveryone") && args[1].equals("true")) {
         this.shouldEnableEveryoneTracksEveryone = true;
         return true;
      } else {
         return false;
      }
   }

   public boolean shouldEnableEveryoneTracksEveryone() {
      return this.shouldEnableEveryoneTracksEveryone;
   }
}
