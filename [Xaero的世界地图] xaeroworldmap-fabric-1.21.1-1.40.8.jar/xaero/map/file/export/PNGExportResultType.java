package xaero.map.file.export;

import net.minecraft.class_124;
import net.minecraft.class_2561;

public enum PNGExportResultType {
   NOT_PREPARED(class_2561.method_43471("gui.xaero_png_result_not_prepared").method_27694((s) -> {
      return s.method_10977(class_124.field_1061);
   }), false),
   EMPTY(class_2561.method_43471("gui.xaero_png_result_empty").method_27694((s) -> {
      return s.method_10977(class_124.field_1061);
   }), false),
   TOO_BIG(class_2561.method_43471("gui.xaero_png_result_too_big").method_27694((s) -> {
      return s.method_10977(class_124.field_1061);
   }), false),
   IMAGE_TOO_BIG(class_2561.method_43471("gui.xaero_png_result_image_too_big").method_27694((s) -> {
      return s.method_10977(class_124.field_1061);
   }), false),
   OUT_OF_MEMORY(class_2561.method_43471("gui.xaero_png_result_out_of_memory").method_27694((s) -> {
      return s.method_10977(class_124.field_1061);
   }), false),
   BAD_FBO(class_2561.method_43471("gui.xaero_png_result_bad_fbo").method_27694((s) -> {
      return s.method_10977(class_124.field_1061);
   }), false),
   IO_EXCEPTION(class_2561.method_43471("gui.xaero_png_result_io_exception").method_27694((s) -> {
      return s.method_10977(class_124.field_1061);
   }), false),
   SUCCESS(class_2561.method_43471("gui.xaero_png_result_success").method_27694((s) -> {
      return s.method_10977(class_124.field_1060);
   }), true);

   private final class_2561 message;
   private final boolean success;

   private PNGExportResultType(class_2561 message, boolean success) {
      this.message = message;
      this.success = success;
   }

   public class_2561 getMessage() {
      return this.message;
   }

   public boolean isSuccess() {
      return this.success;
   }

   // $FF: synthetic method
   private static PNGExportResultType[] $values() {
      return new PNGExportResultType[]{NOT_PREPARED, EMPTY, TOO_BIG, IMAGE_TOO_BIG, OUT_OF_MEMORY, BAD_FBO, IO_EXCEPTION, SUCCESS};
   }
}
