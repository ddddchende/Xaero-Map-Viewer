package xaero.map.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import xaero.lib.XaeroLib;
import xaero.lib.client.online.decrypt.DecryptInputStream;
import xaero.lib.common.config.primary.option.LibPrimaryCommonConfigOptions;
import xaero.lib.patreon.Patreon;
import xaero.map.WorldMap;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;

public class Internet {
   public static Cipher cipher = null;

   public static void checkModVersion() {
      if ((Boolean)XaeroLib.INSTANCE.getLibConfigChannel().getPrimaryCommonConfigManager().getEffective(LibPrimaryCommonConfigOptions.ALLOW_INTERNET)) {
         int keyVersion = Patreon.getKEY_VERSION2();
         String s = "http://data.chocolateminecraft.com/Versions_" + keyVersion + "/WorldMap" + (keyVersion >= 4 ? ".dat" : ".txt");
         s = s.replaceAll(" ", "%20");

         try {
            if (cipher == null) {
               throw new Exception("Cipher instance is null!");
            }

            URL url = new URL(s);
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(900);
            conn.setConnectTimeout(900);
            if (conn.getContentLengthLong() > 524288L) {
               throw new IOException("Input too long to trust!");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(new DecryptInputStream(conn.getInputStream(), cipher), "UTF8"));
            WorldMap.isOutdated = true;
            boolean updateNotificationConfig = (Boolean)WorldMap.INSTANCE.getConfigs().getPrimaryClientConfigManager().getEffective(WorldMapPrimaryClientConfigOptions.UPDATE_NOTIFICATIONS);
            int ignoredUpdateConfig = (Integer)WorldMap.INSTANCE.getConfigs().getPrimaryClientConfigManager().getEffective(WorldMapPrimaryClientConfigOptions.IGNORED_UPDATE);
            String line = reader.readLine();
            if (line != null) {
               WorldMap.newestUpdateID = Integer.parseInt(line);
               if (!updateNotificationConfig || WorldMap.newestUpdateID == ignoredUpdateConfig) {
                  WorldMap.isOutdated = false;
                  reader.close();
                  return;
               }
            }

            boolean versionFound = false;
            String[] current = WorldMap.INSTANCE.getVersionID().split("_");

            label87:
            while(true) {
               String[] args;
               do {
                  do {
                     do {
                        if ((line = reader.readLine()) == null) {
                           break label87;
                        }

                        if (line.equals(WorldMap.INSTANCE.getVersionID())) {
                           WorldMap.isOutdated = false;
                           break label87;
                        }
                     } while(!Patreon.getHasAutoUpdates());

                     if (versionFound) {
                        if (line.startsWith("meta;")) {
                           args = line.substring(5).split(";");
                           WorldMap.latestVersionMD5 = args[0];
                        }

                        versionFound = false;
                     }
                  } while(!line.startsWith(current[0] + "_"));

                  args = line.split("_");
               } while(args.length != current.length);

               boolean sameType = true;
               if (current.length > 2) {
                  for(int i = 2; i < current.length && sameType; ++i) {
                     if (!args[i].equals(current[i])) {
                        sameType = false;
                     }
                  }
               }

               if (sameType) {
                  WorldMap.latestVersion = args[1];
                  versionFound = true;
               }
            }

            reader.close();
         } catch (IOException var13) {
            WorldMap.LOGGER.error("io exception while checking versions: {}", var13.getMessage());
            WorldMap.isOutdated = false;
         } catch (Throwable var14) {
            WorldMap.LOGGER.error("suppressed exception", var14);
            WorldMap.isOutdated = false;
         }

      }
   }

   static {
      try {
         cipher = Cipher.getInstance("RSA");
         KeyFactory factory = KeyFactory.getInstance("RSA");
         byte[] byteKey = Base64.getDecoder().decode(Patreon.getPublicKeyString2().getBytes());
         X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
         PublicKey publicKey = factory.generatePublic(X509publicKey);
         cipher.init(2, publicKey);
      } catch (Exception var4) {
         cipher = null;
         WorldMap.LOGGER.error("suppressed exception", var4);
      }

   }
}
