package xaero.map.region.state;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.class_2246;
import net.minecraft.class_2487;
import net.minecraft.class_2507;
import net.minecraft.class_2680;

public class UnknownBlockState extends class_2680 {
   private class_2487 nbt;
   private String stringRepresentation;

   public UnknownBlockState(class_2487 nbt) {
      super(class_2246.field_10124, new Reference2ObjectArrayMap(), (MapCodec)null);
      this.nbt = nbt;
      this.stringRepresentation = "Unknown: " + String.valueOf(nbt);
   }

   public void write(DataOutputStream out) throws IOException {
      class_2507.method_10628(this.nbt, out);
   }

   public String toString() {
      return this.stringRepresentation;
   }
}
