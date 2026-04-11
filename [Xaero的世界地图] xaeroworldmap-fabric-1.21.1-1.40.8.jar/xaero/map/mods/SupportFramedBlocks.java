package xaero.map.mods;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2378;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_5321;
import net.minecraft.class_7924;
import xaero.lib.common.reflection.util.ReflectionUtils;
import xaero.map.WorldMap;

public class SupportFramedBlocks {
   private Class<?> framedTileBlockClass;
   private Method framedTileEntityCamoStateMethod;
   private Method framedTileEntityCamoMethod;
   private Method camoContainerStateMethod;
   private Method camoContainerContentMethod;
   private Method camoContentStateMethod;
   private boolean usable;
   private Set<class_2248> framedBlocks;

   public SupportFramedBlocks() {
      try {
         this.framedTileBlockClass = Class.forName("xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity");
      } catch (ClassNotFoundException var11) {
         try {
            this.framedTileBlockClass = Class.forName("xfacthd.framedblocks.common.tileentity.FramedTileEntity");
         } catch (ClassNotFoundException var10) {
            try {
               this.framedTileBlockClass = Class.forName("xfacthd.framedblocks.api.block.FramedBlockEntity");
            } catch (ClassNotFoundException var9) {
               WorldMap.LOGGER.info("Failed to init Framed Blocks support!", var9);
               return;
            }
         }
      }

      try {
         this.framedTileEntityCamoStateMethod = this.framedTileBlockClass.getDeclaredMethod("getCamoState");
      } catch (SecurityException | NoSuchMethodException var8) {
         try {
            Class camoContainerClass;
            try {
               camoContainerClass = Class.forName("xfacthd.framedblocks.api.data.CamoContainer");
            } catch (ClassNotFoundException var6) {
               camoContainerClass = Class.forName("xfacthd.framedblocks.api.camo.CamoContainer");
            }

            this.framedTileEntityCamoMethod = this.framedTileBlockClass.getDeclaredMethod("getCamo");

            try {
               this.camoContainerStateMethod = camoContainerClass.getDeclaredMethod("getState");
            } catch (SecurityException | NoSuchMethodException var5) {
               this.camoContainerContentMethod = camoContainerClass.getDeclaredMethod("getContent");
               Class<?> camoContentClass = Class.forName("xfacthd.framedblocks.api.camo.CamoContent");
               this.camoContentStateMethod = camoContentClass.getDeclaredMethod("getAppearanceState");
            }
         } catch (NoSuchMethodException | SecurityException | ClassNotFoundException var7) {
            WorldMap.LOGGER.info("Failed to init Framed Blocks support!", var8);
            WorldMap.LOGGER.info("Failed to init Framed Blocks support!", var7);
         }
      }

      this.usable = this.framedTileBlockClass != null && (this.framedTileEntityCamoStateMethod != null || this.framedTileEntityCamoMethod != null && (this.camoContainerStateMethod != null || this.camoContainerContentMethod != null && this.camoContentStateMethod != null));
   }

   public void onWorldChange() {
      this.framedBlocks = null;
   }

   private void findFramedBlocks(class_1937 world, class_2378<class_2248> registry) {
      if (this.framedBlocks == null) {
         this.framedBlocks = new HashSet();
         if (registry == null) {
            registry = world.method_30349().method_30530(class_7924.field_41254);
         }

         registry.method_29722().forEach((entry) -> {
            class_5321<class_2248> key = (class_5321)entry.getKey();
            if (key.method_29177().method_12836().equals("framedblocks") && key.method_29177().method_12832().startsWith("framed_")) {
               this.framedBlocks.add((class_2248)entry.getValue());
            }

         });
      }

   }

   public boolean isFrameBlock(class_1937 world, class_2378<class_2248> registry, class_2680 state) {
      if (!this.usable) {
         return false;
      } else {
         this.findFramedBlocks(world, registry);
         return this.framedBlocks.contains(state.method_26204());
      }
   }

   public class_2680 unpackFramedBlock(class_1937 world, class_2378<class_2248> registry, class_2680 original, class_2586 tileEntity) {
      if (!this.usable) {
         return original;
      } else if (this.framedTileBlockClass.isAssignableFrom(tileEntity.getClass())) {
         if (this.framedTileEntityCamoStateMethod != null) {
            return (class_2680)ReflectionUtils.getReflectMethodValue(tileEntity, this.framedTileEntityCamoStateMethod, new Object[0]);
         } else {
            Object camoContainer = ReflectionUtils.getReflectMethodValue(tileEntity, this.framedTileEntityCamoMethod, new Object[0]);
            if (this.camoContainerStateMethod != null) {
               return (class_2680)ReflectionUtils.getReflectMethodValue(camoContainer, this.camoContainerStateMethod, new Object[0]);
            } else {
               Object camoContent = ReflectionUtils.getReflectMethodValue(camoContainer, this.camoContainerContentMethod, new Object[0]);
               if (camoContent == null) {
                  return original;
               } else {
                  class_2680 state = (class_2680)ReflectionUtils.getReflectMethodValue(camoContent, this.camoContentStateMethod, new Object[0]);
                  return state == null ? original : state;
               }
            }
         }
      } else {
         return original;
      }
   }
}
