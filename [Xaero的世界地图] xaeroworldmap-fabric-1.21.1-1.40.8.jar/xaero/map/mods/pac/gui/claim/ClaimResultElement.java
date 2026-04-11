package xaero.map.mods.pac.gui.claim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_5250;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.misc.Area;
import xaero.pac.common.claims.result.api.AreaClaimResult;
import xaero.pac.common.claims.result.api.ClaimResult.Type;

public class ClaimResultElement {
   private final Area key;
   private final AreaClaimResult result;
   private final List<Type> filteredResultTypes;
   private long fadeOutStartTime;
   private final long creationTime;
   private final Tooltip tooltip;
   private final boolean hasPositive;
   private final boolean hasNegative;

   private ClaimResultElement(Area key, AreaClaimResult result, List<Type> filteredResultTypes, Tooltip tooltip, long fadeOutStartTime, long creationTime, boolean hasPositive, boolean hasNegative) {
      this.key = key;
      this.result = result;
      this.fadeOutStartTime = fadeOutStartTime;
      this.creationTime = creationTime;
      this.filteredResultTypes = filteredResultTypes;
      this.tooltip = tooltip;
      this.hasPositive = hasPositive;
      this.hasNegative = hasNegative;
   }

   public Area getKey() {
      return this.key;
   }

   public long getFadeOutStartTime() {
      return this.fadeOutStartTime;
   }

   public long getCreationTime() {
      return this.creationTime;
   }

   public void setFadeOutStartTime(long fadeOutStartTime) {
      this.fadeOutStartTime = fadeOutStartTime;
   }

   public int getLeft() {
      return this.result.getLeft();
   }

   public int getTop() {
      return this.result.getTop();
   }

   public int getRight() {
      return this.result.getRight();
   }

   public int getBottom() {
      return this.result.getBottom();
   }

   public Tooltip getTooltip() {
      return this.tooltip;
   }

   public boolean hasNegative() {
      return this.hasNegative;
   }

   public boolean hasPositive() {
      return this.hasPositive;
   }

   public Iterator<Type> getFilteredResultTypeIterator() {
      return this.filteredResultTypes.iterator();
   }

   public static final class Builder {
      private Area key;
      private AreaClaimResult result;

      private Builder() {
      }

      private ClaimResultElement.Builder setDefault() {
         this.setResult((AreaClaimResult)null);
         this.setKey((Area)null);
         return this;
      }

      public ClaimResultElement.Builder setKey(Area key) {
         this.key = key;
         return this;
      }

      public ClaimResultElement.Builder setResult(AreaClaimResult result) {
         this.result = result;
         return this;
      }

      public ClaimResultElement build() {
         if (this.result != null && this.key != null) {
            long time = System.currentTimeMillis();
            class_5250 tooltipText = class_2561.method_43470("");
            boolean hasPositive = false;
            boolean hasNegative = false;
            Iterator var6 = this.result.getResultTypesIterable().iterator();

            while(var6.hasNext()) {
               Type type = (Type)var6.next();
               if (type.success) {
                  hasPositive = true;
               }

               if (type.fail) {
                  hasNegative = true;
               }

               if (hasPositive && hasNegative) {
                  break;
               }
            }

            List<Type> filteredResultTypes = new ArrayList();
            boolean first = true;
            boolean filteredHasPositive = false;
            boolean filteredHasNegative = false;
            Iterator var10 = this.result.getResultTypesIterable().iterator();

            while(true) {
               Type type;
               do {
                  if (!var10.hasNext()) {
                     Tooltip tooltip = new Tooltip(tooltipText);
                     return new ClaimResultElement(this.key, this.result, Collections.unmodifiableList(filteredResultTypes), tooltip, time, time, filteredHasPositive, filteredHasNegative);
                  }

                  type = (Type)var10.next();
               } while(hasPositive && !type.success && type != Type.TOO_MANY_CHUNKS && type != Type.TOO_FAR);

               if (!first) {
                  tooltipText.method_10855().add(class_2561.method_43470(" \n "));
               }

               tooltipText.method_10855().add(type.message);
               filteredResultTypes.add(type);
               first = false;
               if (type.success) {
                  filteredHasPositive = true;
               }

               if (type.fail) {
                  filteredHasNegative = true;
               }
            }
         } else {
            throw new IllegalStateException();
         }
      }

      public static ClaimResultElement.Builder begin() {
         return (new ClaimResultElement.Builder()).setDefault();
      }
   }
}
