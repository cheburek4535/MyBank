package bank.structures

class LRUCache<K, V>(val capacity: Int) {
    private class Node<K, V>(val key: K?, var value: V?) {
        var prev: Node<K, V>? = null
        var next: Node<K, V>? = null
    }
    private val head = Node<K, V>(null, null)
    private val tail = Node<K, V>(null, null)
    private val map = MyHashMap<K, Node<K, V>>(capacity)
    private var count = 0

    init {
        require(capacity > 0)
        head.next = tail
        tail.prev = head
    }

    private fun unlinkNode(node: Node<K, V>) {
        node.prev?.next = node.next
        node.next?.prev = node.prev
    }

    private fun addToHead(node: Node<K, V>) {
        val next = head.next
        head.next = node
        node.prev = head
        node.next = next
        next?.prev = node
    }

    private fun moveToHead(node: Node<K, V>) {
        unlinkNode(node)
        addToHead(node)
    }

    private fun deleteNode(node: Node<K, V>) {
        unlinkNode(node)
        node.key?.let { map.remove(it) }
        count--
    }

    fun get(key: K): V? {
        val node = map.get(key) ?: return null
        moveToHead(node)
        return node.value
    }

    fun put(key: K, value: V) {
        val existNode = map.get(key)
        if (existNode != null) {
            existNode.value = value
            moveToHead(existNode)
            return
        }
        if (count >= capacity) {
            tail.prev?.let { deleteNode(it) }
        }
        val newNode = Node(key, value)
        addToHead(newNode)
        map.put(key, newNode)
        count++
    }

    fun remove(key: K) {
        val node = map.get(key) ?: return
        deleteNode(node)
    }
}