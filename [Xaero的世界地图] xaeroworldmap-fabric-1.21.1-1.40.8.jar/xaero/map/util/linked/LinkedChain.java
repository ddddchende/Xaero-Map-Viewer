package xaero.map.util.linked;

import com.google.common.collect.Streams;
import java.util.Iterator;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LinkedChain<V extends ILinkedChainNode<V>> implements Iterable<V> {
   private boolean destroyed;
   private V head;

   public void add(V element) {
      if (this.destroyed) {
         throw new RuntimeException(new IllegalAccessException("Trying to use a destroyed chain!"));
      } else if (element.isDestroyed()) {
         throw new IllegalArgumentException("Trying to reintroduce a removed chain element!");
      } else {
         if (this.head != null) {
            element.setNext(this.head);
            this.head.setPrevious(element);
         }

         this.head = element;
      }
   }

   public void remove(V element) {
      if (this.destroyed) {
         throw new RuntimeException(new IllegalAccessException("Trying to use a cleared chain!"));
      } else if (!element.isDestroyed()) {
         V prev = element.getPrevious();
         V next = element.getNext();
         if (prev != null) {
            prev.setNext(next);
         }

         if (next != null) {
            next.setPrevious(prev);
         }

         if (element == this.head) {
            this.head = next;
         }

         element.onDestroyed();
      }
   }

   public void destroy() {
      this.head = null;
      this.destroyed = true;
   }

   public void reset() {
      this.head = null;
      this.destroyed = false;
   }

   @Nonnull
   public Iterator<V> iterator() {
      return new Iterator<V>() {
         private V next;

         {
            this.next = LinkedChain.this.head;
         }

         private V reachValidNext() {
            if (LinkedChain.this.destroyed) {
               this.next = null;
               return null;
            } else {
               while(this.next != null && this.next.isDestroyed()) {
                  this.next = this.next.getNext();
               }

               return this.next;
            }
         }

         public boolean hasNext() {
            return this.reachValidNext() != null;
         }

         @Nullable
         public V next() {
            V result = this.reachValidNext();
            if (result != null) {
               this.next = result.getNext();
            }

            return result;
         }
      };
   }

   @Nonnull
   public Stream<V> stream() {
      return Streams.stream(this);
   }
}
