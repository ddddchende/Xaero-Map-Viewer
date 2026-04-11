package xaero.map.util.linked;

public interface ILinkedChainNode<V extends ILinkedChainNode<V>> {
   void setNext(V var1);

   void setPrevious(V var1);

   V getNext();

   V getPrevious();

   boolean isDestroyed();

   void onDestroyed();
}
