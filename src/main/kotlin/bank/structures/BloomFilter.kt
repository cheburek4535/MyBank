package bank.structures
import java.util.BitSet

class BloomFilter<T>(val capacity: Int, val hashFuncs: Int = 3) {
    init {
        require(capacity > 0)
        require(hashFuncs > 0)
    }
    private val bitset = BitSet(capacity)

    private fun getIndex(value: T, seed: Int): Int {
        val hash1 = value.hashCode()
        val hash2 = hash1 xor (hash1 ushr 16) or 1
        return (hash1+seed*hash2).mod(capacity)
    }
    fun set(value: T) {
        for (i in 0 until hashFuncs) {
            bitset.set(getIndex(value, i))
        }
    }
    fun mightContain(value: T): Boolean {
        for (i in 0 until hashFuncs) {
            if (!bitset[getIndex(value, i)]) return false
        }
        return true
    }
}