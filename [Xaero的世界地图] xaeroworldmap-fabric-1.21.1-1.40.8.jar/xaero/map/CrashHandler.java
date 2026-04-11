package xaero.map;

public class CrashHandler {
   private Throwable crashedBy;

   public void checkForCrashes() throws RuntimeException {
      if (this.crashedBy != null) {
         Throwable crash = this.crashedBy;
         this.crashedBy = null;
         throw new RuntimeException("Xaero's World Map (" + WorldMap.INSTANCE.getVersionID() + ") has crashed! Please report here: bit.ly/XaeroWMIssues", crash);
      }
   }

   public Throwable getCrashedBy() {
      return this.crashedBy;
   }

   public void setCrashedBy(Throwable crashedBy) {
      if (this.crashedBy == null) {
         this.crashedBy = crashedBy;
      }

   }
}
