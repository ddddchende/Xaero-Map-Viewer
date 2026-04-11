package xaero.map.world;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_7924;

public class MapConnectionManager {
   private Map<MapConnectionNode, Set<MapConnectionNode>> allConnections = new HashMap();

   public void addConnection(MapConnectionNode mapKey1, MapConnectionNode mapKey2) {
      this.addOneWayConnection(mapKey1, mapKey2);
      this.addOneWayConnection(mapKey2, mapKey1);
   }

   private void addOneWayConnection(MapConnectionNode mapKey1, MapConnectionNode mapKey2) {
      Set<MapConnectionNode> connections = (Set)this.allConnections.get(mapKey1);
      if (connections == null) {
         this.allConnections.put(mapKey1, connections = new HashSet());
      }

      ((Set)connections).add(mapKey2);
   }

   public void removeConnection(MapConnectionNode mapKey1, MapConnectionNode mapKey2) {
      this.removeOneWayConnection(mapKey1, mapKey2);
      this.removeOneWayConnection(mapKey2, mapKey1);
   }

   private void removeOneWayConnection(MapConnectionNode mapKey1, MapConnectionNode mapKey2) {
      Set<MapConnectionNode> connections = (Set)this.allConnections.get(mapKey1);
      if (connections != null) {
         connections.remove(mapKey2);
      }
   }

   public boolean isConnected(MapConnectionNode mapKey1, MapConnectionNode mapKey2) {
      if (mapKey1 != null && mapKey2 != null) {
         if (mapKey1.equals(mapKey2)) {
            return true;
         } else {
            Set<MapConnectionNode> connections = (Set)this.allConnections.get(mapKey1);
            return connections == null ? false : connections.contains(mapKey2);
         }
      } else {
         return false;
      }
   }

   public boolean isEmpty() {
      return this.allConnections.isEmpty();
   }

   public void save(PrintWriter writer) {
      if (!this.allConnections.isEmpty()) {
         Set<String> redundantConnections = new HashSet();
         Iterator var3 = this.allConnections.entrySet().iterator();

         while(var3.hasNext()) {
            Entry<MapConnectionNode, Set<MapConnectionNode>> entry = (Entry)var3.next();
            MapConnectionNode mapKey = (MapConnectionNode)entry.getKey();
            Set<MapConnectionNode> connections = (Set)entry.getValue();
            Iterator var7 = connections.iterator();

            while(var7.hasNext()) {
               MapConnectionNode c = (MapConnectionNode)var7.next();
               String var10000 = String.valueOf(mapKey);
               String fullConnection = var10000 + ":" + String.valueOf(c);
               if (!redundantConnections.contains(fullConnection)) {
                  writer.println("connection:" + fullConnection);
                  String var10001 = String.valueOf(c);
                  redundantConnections.add(var10001 + ":" + String.valueOf(mapKey));
               }
            }
         }
      }

   }

   private void swapConnections(MapConnectionNode mapKey1, MapConnectionNode mapKey2) {
      Set<MapConnectionNode> connections1 = new HashSet((Collection)this.allConnections.getOrDefault(mapKey1, new HashSet()));
      Set<MapConnectionNode> connections2 = new HashSet((Collection)this.allConnections.getOrDefault(mapKey2, new HashSet()));
      Iterator var5 = connections1.iterator();

      MapConnectionNode c;
      while(var5.hasNext()) {
         c = (MapConnectionNode)var5.next();
         this.removeConnection(mapKey1, c);
      }

      var5 = connections2.iterator();

      while(var5.hasNext()) {
         c = (MapConnectionNode)var5.next();
         this.addConnection(mapKey1, c);
      }

      var5 = connections2.iterator();

      while(var5.hasNext()) {
         c = (MapConnectionNode)var5.next();
         this.removeConnection(mapKey2, c);
      }

      var5 = connections1.iterator();

      while(var5.hasNext()) {
         c = (MapConnectionNode)var5.next();
         this.addConnection(mapKey2, c);
      }

   }

   public void renameDimension(String oldName, String newName) {
      Set<MapConnectionNode> keysCopy = new HashSet(this.allConnections.keySet());
      Iterator var4 = keysCopy.iterator();

      while(var4.hasNext()) {
         MapConnectionNode mapKey = (MapConnectionNode)var4.next();
         if (mapKey.getDimId().method_29177().toString().equals(oldName)) {
            String mwPart = mapKey.getMw();
            this.swapConnections(mapKey, new MapConnectionNode(class_5321.method_29179(class_7924.field_41223, class_2960.method_60654(newName)), mwPart));
         }
      }

   }
}
