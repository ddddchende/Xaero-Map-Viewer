package xaero.map.server.mods;

import xaero.map.server.mods.argonauts.SupportArgonautsServer;
import xaero.map.server.mods.ftbteams.SupportFTBTeamsServer;
import xaero.map.server.mods.opac.SupportOPACServer;

public class SupportServerMods {
   private static SupportFTBTeamsServer ftbTeams;
   private static SupportArgonautsServer argonauts;
   private static SupportOPACServer opac;
   private static SupportMinimapServer minimap;

   public static void check() {
      try {
         Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
         ftbTeams = new SupportFTBTeamsServer();
      } catch (ClassNotFoundException var4) {
      }

      try {
         Class.forName("earth.terrarium.argonauts.api.ApiHelper");
         argonauts = new SupportArgonautsServer();
      } catch (ClassNotFoundException var3) {
      }

      try {
         Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
         opac = new SupportOPACServer();
      } catch (ClassNotFoundException var2) {
      }

      try {
         Class.forName("xaero.common.XaeroMinimapSession");
         minimap = new SupportMinimapServer();
      } catch (ClassNotFoundException var1) {
      }

   }

   public static boolean hasFtbTeams() {
      return ftbTeams != null;
   }

   public static SupportFTBTeamsServer getFtbTeams() {
      return ftbTeams;
   }

   public static boolean hasArgonauts() {
      return argonauts != null;
   }

   public static SupportArgonautsServer getArgonauts() {
      return argonauts;
   }

   public static boolean hasOpac() {
      return opac != null;
   }

   public static SupportOPACServer getOpac() {
      return opac;
   }

   public static boolean hasMinimap() {
      return minimap != null;
   }

   public static SupportMinimapServer getMinimap() {
      return minimap;
   }
}
