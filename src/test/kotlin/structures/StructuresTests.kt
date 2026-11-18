package structures

import bank.structures.LRUCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StructuresTests {
    @Test
    fun `check delete last in lru`() {
        val cache = LRUCache<String, Int>(2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.get("a")
        cache.put("c", 3)

        assertEquals(1, cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals(3, cache.get("c"))
    }

        @Test
        fun `update existing key value without deleting other nodes`() {
            val cache = LRUCache<String, Int>(2)
            cache.put("a", 1)
            cache.put("b", 2)
            cache.put("a", 10)

            assertEquals(10, cache.get("a"))
            assertEquals(2, cache.get("b"))
        }

        @Test
        fun `throws exception when capacity is zero`() {
            assertFailsWith<IllegalArgumentException> {
                LRUCache<String, Int>(0)
            }
        }
        @Test
        fun `exception when capacity is not power of two`() {
            assertFailsWith<IllegalArgumentException> {
                LRUCache<String, Int>(777)
            }
        }
}
