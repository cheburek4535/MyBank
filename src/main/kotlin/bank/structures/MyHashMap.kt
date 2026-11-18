package bank.structures

class MyHashMap<K, V>(startCapacity: Int = 16) {
    init {
        require(startCapacity > 0 && (startCapacity and (startCapacity-1) == 0)) {"вместимость должна быть степенью двойки"}
    }
    private class Node<K, V>(val key: K, var value: V) {
        var next: Node<K, V>? = null
    }
    private var capacity = startCapacity
    private var count = 0
    private val loadFactor = 0.75
    private var buckets = arrayOfNulls<Node<K, V>?>(capacity)

    private fun calculateIndex(key: K): Int {
        val hash = key.hashCode()
        return (hash xor (hash ushr 16)) and (capacity-1)
    }

    private fun add(key: K, value: V, array: Array<Node<K, V>?>): Boolean {
        val index = calculateIndex(key)
        var currNode = array[index]

        while (currNode?.next != null) {
            if (currNode.key == key) {
                currNode.value = value
                return false
            }
            currNode = currNode.next
        }
        if (currNode != null) {
            if (currNode.key == key) {
                currNode.value = value
                return false
            }
            currNode.next = Node(key, value)
        } else {
            array[index] = Node(key, value)
        }
        return true
    }
    private fun resize() {
        capacity *= 2
        val newArray = arrayOfNulls<Node<K, V>>(capacity)
        for (bucket in buckets) {
            var currNode = bucket
            while (currNode != null) {
                add(currNode.key, currNode.value, newArray)
                currNode = currNode.next
            }
        }
        buckets = newArray
    }

    fun put(key: K, value: V) {
        if (add(key, value, buckets)) {
            count++
            if (count.toDouble() / capacity >= loadFactor) resize()
        }
    }
    fun get(key: K): V? {
        val index = calculateIndex(key)
        var currNode = buckets[index]
        while (currNode != null) {
            if (currNode.key == key) return currNode.value
            currNode = currNode.next
        }
        return null
    }
    fun remove(key: K) {
        val index = calculateIndex(key)
        var currNode = buckets[index]
        var prev: Node<K, V>? = null
        while (currNode != null) {
            if (currNode.key == key) {
                if (prev == null) {
                    buckets[index] = currNode.next
                } else {
                    prev.next = currNode.next
                }
                count--
                return
            }
            prev = currNode
            currNode = currNode.next
        }
    }

}