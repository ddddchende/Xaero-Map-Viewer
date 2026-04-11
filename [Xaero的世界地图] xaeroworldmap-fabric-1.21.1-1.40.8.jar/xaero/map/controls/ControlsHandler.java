package xaero.map.controls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_437;
import xaero.lib.client.controls.util.KeyMappingUtils;
import xaero.lib.client.gui.config.context.BuiltInEditConfigScreenContexts;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiWorldMapSettings;

public class ControlsHandler {
   private MapProcessor mapProcessor;
   private ArrayList<KeyEvent> keyEvents = new ArrayList();
   private ArrayList<KeyEvent> oldKeyEvents = new ArrayList();

   public ControlsHandler(MapProcessor mapProcessor) {
      this.mapProcessor = mapProcessor;
   }

   private boolean eventExists(class_304 kb) {
      Iterator var2 = this.keyEvents.iterator();

      KeyEvent o;
      do {
         if (!var2.hasNext()) {
            return this.oldEventExists(kb);
         }

         o = (KeyEvent)var2.next();
      } while(o.getKb() != kb);

      return true;
   }

   private boolean oldEventExists(class_304 kb) {
      Iterator var2 = this.oldKeyEvents.iterator();

      KeyEvent o;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         o = (KeyEvent)var2.next();
      } while(o.getKb() != kb);

      return true;
   }

   public static boolean isKeyRepeat(class_304 kb) {
      return kb != ControlsRegister.keyOpenMap && kb != ControlsRegister.keyOpenSettings && kb != ControlsRegister.keyOpenServerSettings && kb != ControlsRegister.keyToggleDimension;
   }

   public void keyDown(class_304 kb, boolean tickEnd, boolean isRepeat) {
      class_310 mc = class_310.method_1551();
      if (!tickEnd) {
         if (kb == ControlsRegister.keyOpenMap) {
            mc.method_1507(new GuiMap((class_437)null, (class_437)null, this.mapProcessor, mc.method_1560()));
         } else if (kb == ControlsRegister.keyOpenSettings) {
            mc.method_1507(new GuiWorldMapSettings(BuiltInEditConfigScreenContexts.CLIENT));
         } else if (kb == ControlsRegister.keyOpenServerSettings) {
            mc.method_1507(new GuiWorldMapSettings(BuiltInEditConfigScreenContexts.SERVER));
         } else if (kb == ControlsRegister.keyQuickConfirm) {
            WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
            MapProcessor mapProcessor = worldmapSession.getMapProcessor();
            synchronized(mapProcessor.uiPauseSync) {
               if (!mapProcessor.isUIPaused()) {
                  mapProcessor.quickConfirmMultiworld();
               }
            }
         } else if (kb == ControlsRegister.keyToggleDimension) {
            this.mapProcessor.getMapWorld().toggleDimension(!class_437.method_25442());
            String messageType = this.mapProcessor.getMapWorld().getCustomDimensionId() == null ? "gui.xaero_switched_to_current_dimension" : "gui.xaero_switched_to_dimension";
            class_2960 messageDimLoc = this.mapProcessor.getMapWorld().getFutureDimensionId() == null ? null : this.mapProcessor.getMapWorld().getFutureDimensionId().method_29177();
            mc.field_1705.method_1743().method_1812(class_2561.method_43469(messageType, new Object[]{messageDimLoc.toString()}));
         }
      }

   }

   public void keyUp(class_304 kb, boolean tickEnd) {
      if (!tickEnd) {
      }

   }

   public void handleKeyEvents() {
      class_310 mc = class_310.method_1551();
      this.onKeyInput(mc);

      int i;
      KeyEvent ke;
      for(i = 0; i < this.keyEvents.size(); ++i) {
         ke = (KeyEvent)this.keyEvents.get(i);
         if (mc.field_1755 == null) {
            this.keyDown(ke.getKb(), ke.isTickEnd(), ke.isRepeat());
         }

         if (!ke.isRepeat()) {
            if (!this.oldEventExists(ke.getKb())) {
               this.oldKeyEvents.add(ke);
            }

            this.keyEvents.remove(i);
            --i;
         } else if (!KeyMappingUtils.isPhysicallyDown(ke.getKb())) {
            this.keyUp(ke.getKb(), ke.isTickEnd());
            this.keyEvents.remove(i);
            --i;
         }
      }

      for(i = 0; i < this.oldKeyEvents.size(); ++i) {
         ke = (KeyEvent)this.oldKeyEvents.get(i);
         if (!KeyMappingUtils.isPhysicallyDown(ke.getKb())) {
            this.keyUp(ke.getKb(), ke.isTickEnd());
            this.oldKeyEvents.remove(i);
            --i;
         }
      }

   }

   public void onKeyInput(class_310 mc) {
      List<class_304> kbs = WorldMap.controlsRegister.keybindings;

      for(int i = 0; i < kbs.size(); ++i) {
         class_304 kb = (class_304)kbs.get(i);

         try {
            boolean pressed = kb.method_1436();

            while(kb.method_1436()) {
            }

            if (mc.field_1755 == null && !this.eventExists(kb) && pressed) {
               this.keyEvents.add(new KeyEvent(kb, false, isKeyRepeat(kb), true));
            }
         } catch (Exception var6) {
         }
      }

   }
}
