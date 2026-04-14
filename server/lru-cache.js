export class LRUCache {
  constructor(maxSize) {
    this.maxSize = maxSize;
    this.cache = new Map();
    this.head = { prev: null, next: null };
    this.tail = { prev: null, next: null };
    this.head.next = this.tail;
    this.tail.prev = this.head;
  }

  get(key) {
    const node = this.cache.get(key);
    if (node === undefined) return null;
    this._removeNode(node);
    this._addToFront(node);
    return node.value;
  }

  set(key, value) {
    const existing = this.cache.get(key);
    if (existing !== undefined) {
      existing.value = value;
      this._removeNode(existing);
      this._addToFront(existing);
      return;
    }
    if (this.cache.size >= this.maxSize) {
      const lru = this.tail.prev;
      this._removeNode(lru);
      this.cache.delete(lru.key);
    }
    const node = { key, value, prev: null, next: null };
    this.cache.set(key, node);
    this._addToFront(node);
  }

  has(key) {
    return this.cache.has(key);
  }

  delete(key) {
    const node = this.cache.get(key);
    if (node === undefined) return false;
    this._removeNode(node);
    this.cache.delete(key);
    return true;
  }

  clear() {
    this.cache.clear();
    this.head.next = this.tail;
    this.tail.prev = this.head;
  }

  get size() {
    return this.cache.size;
  }

  _removeNode(node) {
    node.prev.next = node.next;
    node.next.prev = node.prev;
  }

  _addToFront(node) {
    node.next = this.head.next;
    node.prev = this.head;
    this.head.next.prev = node;
    this.head.next = node;
  }
}
