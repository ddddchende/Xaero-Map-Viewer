package xaero.map.graphics;

import com.google.common.collect.ImmutableList;
import net.minecraft.class_1921;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_4668;
import net.minecraft.class_293.class_5596;
import net.minecraft.class_4668.class_4683;
import net.minecraft.class_4668.class_5942;
import xaero.lib.client.graphics.XaeroRenderType;
import xaero.lib.client.graphics.XaeroRenderType.MultiPhaseBuilder;
import xaero.lib.client.graphics.XaeroRenderType.MultiPhaseRenderType;
import xaero.lib.client.graphics.shader.LibShaders;
import xaero.map.WorldMap;

public class CustomRenderTypes extends class_1921 {
   public static final class_1921 GUI_BILINEAR;
   public static final class_1921 GUI_BILINEAR_PREMULTIPLIED;
   public static final class_1921 GUI_NEAREST;
   public static final class_1921 MAP;
   public static final class_1921 MAP_COLOR_OVERLAY;
   public static final class_1921 MAP_FRAME_TEXTURE_OVER_TRANSPARENT;
   public static final class_1921 MAP_COLOR_FILLER;
   public static final class_1921 MAP_ELEMENT_TEXT_BG;

   private CustomRenderTypes(String name, class_293 vertexFormat, class_5596 drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
      super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
   }

   static {
      ImmutableList<class_4668> multiPhaseParameters = (new MultiPhaseBuilder()).texture(new class_4683(WorldMap.guiTextures, false, false)).transparency(XaeroRenderType.DEFAULT_TRANSLUCENT_TRANSPARENCY).shader(new class_5942(() -> {
         return LibShaders.POSITION_COLOR_TEX;
      })).cull(field_21345).target(XaeroRenderType.KEEP_TARGET).build();
      GUI_NEAREST = new MultiPhaseRenderType("xaero_wm_gui_nearest", XaeroRenderType.POSITION_COLOR_TEX, class_5596.field_27382, 256, false, false, multiPhaseParameters);
      multiPhaseParameters = (new MultiPhaseBuilder()).texture(new class_4683(WorldMap.guiTextures, true, false)).transparency(XaeroRenderType.DEFAULT_TRANSLUCENT_TRANSPARENCY).shader(new class_5942(() -> {
         return LibShaders.POSITION_COLOR_TEX;
      })).cull(field_21345).target(XaeroRenderType.KEEP_TARGET).build();
      GUI_BILINEAR = new MultiPhaseRenderType("xaero_wm_gui_bilinear", XaeroRenderType.POSITION_COLOR_TEX, class_5596.field_27382, 256, false, false, multiPhaseParameters);
      multiPhaseParameters = (new MultiPhaseBuilder()).texture(new class_4683(WorldMap.guiTextures, true, false)).transparency(XaeroRenderType.PREMULTIPLIED_TRANSPARENCY).shader(new class_5942(() -> {
         return LibShaders.POSITION_COLOR_TEX_PRE;
      })).cull(field_21345).target(XaeroRenderType.KEEP_TARGET).build();
      GUI_BILINEAR_PREMULTIPLIED = new MultiPhaseRenderType("xaero_wm_gui_bilinear_pre", XaeroRenderType.POSITION_COLOR_TEX, class_5596.field_27382, 256, false, false, multiPhaseParameters);
      multiPhaseParameters = (new MultiPhaseBuilder()).texture(new class_4683(WorldMap.guiTextures, false, false)).transparency(XaeroRenderType.DEST_TRANSPARENCY).shader(new class_5942(() -> {
         return LibShaders.WORLD_MAP;
      })).cull(field_21345).target(XaeroRenderType.KEEP_TARGET).build();
      MAP = new MultiPhaseRenderType("xaero_wm_map_with_light", class_290.field_1585, class_5596.field_27382, 256, false, false, multiPhaseParameters);
      multiPhaseParameters = (new MultiPhaseBuilder()).transparency(XaeroRenderType.DEFAULT_TRANSLUCENT_TRANSPARENCY).shader(new class_5942(() -> {
         return LibShaders.POSITION_COLOR;
      })).cull(field_21345).target(XaeroRenderType.KEEP_TARGET).build();
      MAP_COLOR_OVERLAY = new MultiPhaseRenderType("xaero_wm_world_map_overlay", class_290.field_1576, class_5596.field_27382, 256, false, false, multiPhaseParameters);
      multiPhaseParameters = (new MultiPhaseBuilder()).texture(new class_4683(WorldMap.guiTextures, true, false)).transparency(XaeroRenderType.DEST_TRANSPARENCY).shader(new class_5942(() -> {
         return LibShaders.POSITION_COLOR_TEX;
      })).cull(field_21345).target(XaeroRenderType.KEEP_TARGET).build();
      MAP_FRAME_TEXTURE_OVER_TRANSPARENT = new MultiPhaseRenderType("xaero_wm_frame_texture", XaeroRenderType.POSITION_COLOR_TEX, class_5596.field_27382, 256, false, false, multiPhaseParameters);
      multiPhaseParameters = (new MultiPhaseBuilder()).transparency(XaeroRenderType.DEFAULT_TRANSLUCENT_TRANSPARENCY).shader(new class_5942(() -> {
         return LibShaders.POSITION_COLOR;
      })).target(XaeroRenderType.KEEP_TARGET).build();
      MAP_COLOR_FILLER = new MultiPhaseRenderType("xaero_wm_world_map_filler", class_290.field_1576, class_5596.field_27382, 256, false, false, multiPhaseParameters);
      multiPhaseParameters = (new MultiPhaseBuilder()).transparency(XaeroRenderType.DEFAULT_TRANSLUCENT_TRANSPARENCY).shader(new class_5942(() -> {
         return LibShaders.POSITION_COLOR;
      })).target(XaeroRenderType.KEEP_TARGET).build();
      MAP_ELEMENT_TEXT_BG = new MultiPhaseRenderType("xaero_wm_world_map_waypoint_name_bg", class_290.field_1576, class_5596.field_27382, 42, false, false, multiPhaseParameters);
   }
}
