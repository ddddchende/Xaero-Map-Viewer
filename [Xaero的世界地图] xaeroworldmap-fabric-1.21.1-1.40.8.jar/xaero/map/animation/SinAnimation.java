package xaero.map.animation;

public class SinAnimation extends Animation {
   public SinAnimation(double from, double to, long time) {
      super(from, to, time);
   }

   public double getCurrent() {
      double passed = Math.min(1.0D, (double)(System.currentTimeMillis() - this.start) / (double)this.time);
      double angle = 1.5707963267948966D * passed;
      return this.from + this.off * Math.sin(angle);
   }
}
