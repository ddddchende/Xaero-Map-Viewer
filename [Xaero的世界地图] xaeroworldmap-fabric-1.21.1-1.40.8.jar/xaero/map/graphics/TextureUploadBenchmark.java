package xaero.map.graphics;

import java.util.Arrays;
import org.lwjgl.opengl.GL11;
import xaero.map.misc.Misc;

public class TextureUploadBenchmark {
   private long[] accumulators;
   private long[] results;
   private int[] totals;
   private boolean[] finished;
   private int[] nOfElements;
   private int nOfFinished;
   private boolean allFinished;

   public TextureUploadBenchmark(int... nOfElements) {
      int nOfTypes = nOfElements.length;
      this.accumulators = new long[nOfTypes];
      this.totals = new int[nOfTypes];
      this.results = new long[nOfTypes];
      this.finished = new boolean[nOfTypes];
      this.nOfElements = nOfElements;
   }

   public void pre() {
      Misc.timerPre();
   }

   public void post(int type) {
      GL11.glFinish();
      int passed = Misc.timerResult();
      long[] var10000 = this.accumulators;
      var10000[type] += (long)passed;
      int var10002 = this.totals[type]++;
      if (this.totals[type] == this.nOfElements[type]) {
         this.finish(type);
      }

   }

   private void finish(int type) {
      this.results[type] = this.accumulators[type] / (long)this.totals[type];
      this.finished[type] = true;
      ++this.nOfFinished;
      if (this.nOfFinished == this.finished.length) {
         this.allFinished = true;
      }

   }

   public boolean isFinished() {
      return this.allFinished;
   }

   public boolean isFinished(int type) {
      return this.finished[type];
   }

   public long getAverage(int type) {
      return this.finished[type] ? this.results[type] : this.accumulators[type] / (long)this.totals[type];
   }

   public String getTotalsString() {
      return Arrays.toString(this.totals);
   }
}
