package xaero.map.controls;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.class_304;

public class ControlsRegister {
   public static final class_304 keyOpenMap = new class_304("gui.xaero_open_map", 77, "Xaero's World Map");
   public static final class_304 keyOpenSettings = new class_304("gui.xaero_open_settings", 93, "Xaero's World Map");
   public static final class_304 keyOpenServerSettings = new class_304("gui.xaero_world_map_server_settings", -1, "Xaero's World Map");
   public static final class_304 keyZoomIn = new class_304("gui.xaero_map_zoom_in", -1, "Xaero's World Map");
   public static final class_304 keyZoomOut = new class_304("gui.xaero_map_zoom_out", -1, "Xaero's World Map");
   public static final class_304 keyQuickConfirm = new class_304("gui.xaero_quick_confirm", 344, "Xaero's World Map");
   public static final class_304 keyToggleDimension = new class_304("gui.xaero_toggle_dimension", -1, "Xaero's World Map");
   public static class_304 keyToggleTrackedPlayers;
   public static class_304 keyTogglePacChunkClaims;
   public final List<class_304> keybindings;

   public ControlsRegister() {
      this.keybindings = Lists.newArrayList(new class_304[]{keyOpenMap, keyOpenSettings, keyOpenServerSettings, keyZoomIn, keyZoomOut, keyQuickConfirm, keyToggleDimension});
   }

   public void register(Consumer<class_304> registry) {
      Iterator var2 = this.keybindings.iterator();

      while(var2.hasNext()) {
         class_304 kb = (class_304)var2.next();
         registry.accept(kb);
      }

      try {
         Class.forName("xaero.common.IXaeroMinimap");
      } catch (ClassNotFoundException var4) {
         keyToggleTrackedPlayers = new class_304("gui.xaero_toggle_tracked_players", -1, "Xaero's World Map");
         registry.accept(keyToggleTrackedPlayers);
         keyTogglePacChunkClaims = new class_304("gui.xaero_toggle_pac_chunk_claims", -1, "Xaero's World Map");
         registry.accept(keyTogglePacChunkClaims);
      }

   }
}
