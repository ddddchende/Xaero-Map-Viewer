package xaero.map.animation;

public class Animation {
   protected long start;
   protected long time;
   protected double from;
   protected double off;

   public Animation(double from, double to, long time) {
      this.from = from;
      this.off = to - from;
      this.time = time;
      this.start = System.currentTimeMillis();
   }

   public double getCurrent() {
      return this.from + Math.min(1.0D, (double)(System.currentTimeMillis() - this.start) / (double)this.time) * this.off;
   }

   public double getDestination() {
      return this.from + this.off;
   }

   public long getPassed() {
      return System.currentTimeMillis() - this.start;
   }
}
