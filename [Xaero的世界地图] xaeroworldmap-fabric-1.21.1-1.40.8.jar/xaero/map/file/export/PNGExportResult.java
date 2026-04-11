package xaero.map.file.export;

import java.nio.file.Path;
import net.minecraft.class_2561;

public class PNGExportResult {
   private final PNGExportResultType type;
   private final Path folderToOpen;

   public PNGExportResult(PNGExportResultType type, Path folderToOpen) {
      this.type = type;
      this.folderToOpen = folderToOpen;
   }

   public PNGExportResultType getType() {
      return this.type;
   }

   public Path getFolderToOpen() {
      return this.folderToOpen;
   }

   public class_2561 getMessage() {
      return this.type.getMessage();
   }
}
