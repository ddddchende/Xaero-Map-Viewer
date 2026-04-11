package xaero.map;

import java.util.ArrayList;
import xaero.map.task.MapRunnerTask;

public class MapRunner implements Runnable {
   private boolean stopped;
   private ArrayList<MapRunnerTask> tasks = new ArrayList();

   public void run() {
      while(!this.stopped) {
         WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
         if (worldmapSession != null && worldmapSession.isUsable()) {
            MapProcessor mapProcessor = worldmapSession.getMapProcessor();
            mapProcessor.run(this);
         } else {
            this.doTasks((MapProcessor)null);
         }

         try {
            Thread.sleep(100L);
         } catch (InterruptedException var3) {
         }
      }

   }

   public void doTasks(MapProcessor mapProcessor) {
      while(true) {
         MapRunnerTask task;
         label24: {
            if (!this.tasks.isEmpty()) {
               synchronized(this.tasks) {
                  if (!this.tasks.isEmpty()) {
                     task = (MapRunnerTask)this.tasks.remove(0);
                     break label24;
                  }
               }
            }

            return;
         }

         task.run(mapProcessor);
      }
   }

   public void addTask(MapRunnerTask task) {
      synchronized(this.tasks) {
         this.tasks.add(task);
      }
   }

   public void stop() {
      this.stopped = true;
   }
}
