package xaero.map.radar.tracker.system;

import java.util.Iterator;

public interface IPlayerTrackerSystem<P> {
   ITrackedPlayerReader<P> getReader();

   Iterator<P> getTrackedPlayerIterator();
}
