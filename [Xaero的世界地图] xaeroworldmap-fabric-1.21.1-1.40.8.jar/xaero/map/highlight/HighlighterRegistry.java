package xaero.map.highlight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighlighterRegistry {
   private List<AbstractHighlighter> highlighters = new ArrayList();

   public void register(AbstractHighlighter highlighter) {
      this.highlighters.add(highlighter);
   }

   public void end() {
      this.highlighters = Collections.unmodifiableList(this.highlighters);
   }

   public List<AbstractHighlighter> getHighlighters() {
      return this.highlighters;
   }
}
