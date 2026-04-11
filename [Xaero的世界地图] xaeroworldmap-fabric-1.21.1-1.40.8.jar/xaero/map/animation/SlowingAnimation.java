package xaero.map.animation;

public class SlowingAnimation extends Animation {
   public static final double animationThing = 16.666666666666668D;
   private double dest;
   private double zero;
   private double factor;

   public SlowingAnimation(double from, double to, double factor, double zero) {
      super(from, to, 0L);
      this.dest = to;
      this.zero = zero;
      this.factor = factor;
   }

   public double getCurrent() {
      double times = (double)(System.currentTimeMillis() - this.start) / 16.666666666666668D;
      double currentOff = this.off * Math.pow(this.factor, times);
      return this.dest - (Math.abs(currentOff) <= this.zero ? 0.0D : currentOff);
   }

   public double getDestination() {
      return this.dest;
   }
}
