package bank.service

import bank.dto.CreateAccountRequest
import bank.model.Account
import bank.model.AccountStatus
import bank.repository.AccountRepository
import bank.structures.BloomFilter
import bank.structures.LRUCache
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AccountService(private val accountRepository: AccountRepository) {
    private val bloomFilter = BloomFilter<String>(1000000, 10)
    private val lruCache = LRUCache<String, Account>(10000)

    @EventListener(ApplicationReadyEvent::class)
    fun prepareBloomFilter() = accountRepository.getAllIds().forEach { bloomFilter.set(it) }

    fun create(request: CreateAccountRequest): Account {
        val id = UUID.randomUUID().toString()
        val account = Account.create(id, request.owner, request.initialBalance)
        accountRepository.save(account)
        bloomFilter.set(id)
        lruCache.put(id, account)
        return account
    }
    fun get(id: String): Account? {
        if (!bloomFilter.mightContain(id)) return null
        lruCache.get(id)?.let { return it}
        val fromDb = accountRepository.findById(id)
        if (fromDb != null) {
            lruCache.put(id, fromDb)
        }
        return fromDb
    }
    fun close(id: String): Boolean {
        return accountRepository.closeAccount(id) > 0
            .also { invalidateCache(id) }
    }
    fun invalidateCache(id: String) = lruCache.remove(id)
    fun mightExists(id: String): Boolean = bloomFilter.mightContain(id)

}