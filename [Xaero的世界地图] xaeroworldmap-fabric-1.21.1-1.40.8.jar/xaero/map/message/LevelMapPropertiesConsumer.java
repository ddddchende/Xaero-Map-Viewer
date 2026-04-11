package xaero.map.message;

import java.util.function.Consumer;
import xaero.map.WorldMapSession;
import xaero.map.server.level.LevelMapProperties;

public class LevelMapPropertiesConsumer implements Consumer<LevelMapProperties> {
   public void accept(LevelMapProperties t) {
      WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
      worldmapSession.getMapProcessor().onServerLevelId(t.getId());
   }
}
