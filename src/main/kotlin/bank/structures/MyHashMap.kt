package bank.structures

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class MyHashMap<K: Any, V: Any>(startCapacity: Int = 16) {
    private var capacity = startCapacity
    init {
        require(capacity > 0)
        require(capacity <= (1 shl 30))
        capacity = if (startCapacity > 1) (startCapacity-1).takeHighestOneBit() shl 1 else 1
    }
    private class Node<K, V>(val key: K, var value: V) {
        var next: Node<K, V>? = null
    }

    private var count = 0
    private val loadFactor = 0.75
    private var buckets = arrayOfNulls<Node<K, V>?>(capacity)

    private val lock = ReentrantReadWriteLock()

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

    fun put(key: K, value: V) = lock.write {
        if (add(key, value, buckets)) {
            count++
            if (capacity < (1 shl 30) && count.toDouble() / capacity >= loadFactor) resize()
        }
    }
    fun get(key: K): V? = lock.read {
        val index = calculateIndex(key)
        var currNode = buckets[index]
        while (currNode != null) {
            if (currNode.key == key) return currNode.value
            currNode = currNode.next
        }
        return null
    }
    fun remove(key: K): Unit = lock.write {
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