package xaero.map.mods.gui;

import net.minecraft.class_1074;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import xaero.map.animation.SlowingAnimation;

public class Waypoint implements Comparable<Waypoint> {
   private Object original;
   public static final class_2960 minimapTextures = class_2960.method_60655("xaerobetterpvp", "gui/guis.png");
   public static final int white = -1;
   private int x;
   private int y;
   private int z;
   private String text;
   private String symbol;
   private int color;
   private boolean disabled = false;
   private int type = 0;
   private boolean rotation = false;
   private int yaw = 0;
   private float destAlpha = 0.0F;
   private float alpha = 0.0F;
   private SlowingAnimation alphaAnim = null;
   private boolean editable;
   private boolean temporary;
   private boolean global;
   private String setName;
   private boolean yIncluded;
   private double dimDiv;
   private int cachedNameLength;

   public Waypoint(Object original, int x, int y, int z, String name, String symbol, int color, int type, boolean editable, String setName, boolean yIncluded, double dimDiv) {
      this.original = original;
      this.x = x;
      this.y = y;
      this.z = z;
      this.symbol = symbol;
      this.color = color;
      this.type = type;
      this.text = name;
      this.editable = editable;
      this.setName = setName;
      this.yIncluded = yIncluded;
      this.dimDiv = dimDiv;
      this.cachedNameLength = class_310.method_1551().field_1772.method_1727(this.getName());
   }

   public String getName() {
      return class_1074.method_4662(this.text, new Object[0]);
   }

   public int compareTo(Waypoint arg0) {
      return this.z > arg0.z ? 1 : (this.z != arg0.z ? -1 : 0);
   }

   public String toString() {
      return this.getName();
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   public boolean isDisabled() {
      return this.disabled;
   }

   public void setDisabled(boolean disabled) {
      this.disabled = disabled;
   }

   public int getType() {
      return this.type;
   }

   public int getYaw() {
      return this.yaw;
   }

   public void setYaw(int yaw) {
      this.yaw = yaw;
   }

   public boolean isRotation() {
      return this.rotation;
   }

   public void setRotation(boolean rotation) {
      this.rotation = rotation;
   }

   public boolean isEditable() {
      return this.editable;
   }

   public Object getOriginal() {
      return this.original;
   }

   public String getSymbol() {
      return this.symbol;
   }

   public void setTemporary(boolean temporary) {
      this.temporary = temporary;
   }

   public void setGlobal(boolean global) {
      this.global = global;
   }

   public String getSetName() {
      return this.setName;
   }

   public String getComparisonName() {
      String comparisonName = this.getName().toLowerCase().trim();
      if (comparisonName.startsWith("the ")) {
         comparisonName = comparisonName.substring(4);
      }

      if (comparisonName.startsWith("a ")) {
         comparisonName = comparisonName.substring(2);
      }

      return comparisonName;
   }

   public int getColor() {
      return this.color;
   }

   public boolean isGlobal() {
      return this.global;
   }

   public double getRenderX() {
      return this.dimDiv == 1.0D ? (double)this.x + 0.5D : Math.floor((double)this.x / this.dimDiv) + 0.5D;
   }

   public double getRenderZ() {
      return this.dimDiv == 1.0D ? (double)this.z + 0.5D : Math.floor((double)this.z / this.dimDiv) + 0.5D;
   }

   public boolean isTemporary() {
      return this.temporary;
   }

   public float getDestAlpha() {
      return this.destAlpha;
   }

   public void setDestAlpha(float destAlpha) {
      this.destAlpha = destAlpha;
   }

   public SlowingAnimation getAlphaAnim() {
      return this.alphaAnim;
   }

   public void setAlphaAnim(SlowingAnimation alphaAnim) {
      this.alphaAnim = alphaAnim;
   }

   public float getAlpha() {
      return this.alpha;
   }

   public void setAlpha(float alpha) {
      this.alpha = alpha;
   }

   public boolean isyIncluded() {
      return this.yIncluded;
   }

   public int getCachedNameLength() {
      return this.cachedNameLength;
   }
}
