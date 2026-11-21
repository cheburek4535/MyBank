package bank.service

import bank.dto.DepositRequest
import bank.dto.TransferRequest
import bank.dto.WithdrawRequest
import bank.model.*
import bank.repository.AccountRepository
import bank.repository.TransactionRepository
import bank.structures.MyHashMap
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Service
class TransactionService(private val accountService: AccountService, private val txRepository: TransactionRepository,
    private val accountRepository: AccountRepository, private val txTemplate: TransactionTemplate) {

    private data class CachedEntry(val request: Any, var result: TransactionResult)

    private val idempotencyCache = MyHashMap<String, CachedEntry>()

    fun deposit(key: String, request: DepositRequest) {}
    fun withdraw(key: String, request: WithdrawRequest) {}

    fun transfer(key: String, request: TransferRequest): TransferResult {
        when (val existing = idempotencyCache.get(key)) {
            null -> Unit
            else -> {
                if (existing.request != request) return IdempotencyConflict(key)
                return when (val res = existing.result) {
                    is TransferResult -> res
                    is DepositSuccess, is WithdrawSuccess -> IdempotencyConflict(key)
                }
            }
        }

        val senderId = request.senderAccountId
        val recipientId = request.recipientAccountId
        val amount = request.amount
        if (recipientId == senderId) return SameAccount(recipientId)
        if (amount <= 0) return InvalidAmount(amount)

        val senderAccount = accountService.get(senderId) ?: return AccountNotFound(senderId)

        txRepository.findByIdempotencyKey(key)?.let {existing ->
            return TransferSuccess(
                existing.balanceAfter,
                existing.amount,
                senderId,
                recipientId,
                existing.id
            ).also { idempotencyCache.put(key, CachedEntry(request, it)) }
        }
        if (!accountService.mightExists(recipientId)) return AccountNotFound(recipientId)

        val result: TransferResult = try {
            txTemplate.execute { status ->
                val newSenderBalance = accountRepository.decreaseBalance(senderId, amount) ?: run {
                    status.setRollbackOnly()
                    if (accountRepository.findById(senderId) == null) return@execute AccountNotFound(senderId)
                    else return@execute NotEnoughMoney(senderAccount.balance, amount)
                }
                accountRepository.increaseBalance(recipientId, amount) ?: run {
                    status.setRollbackOnly()
                    val existingAccount = accountRepository.findById(recipientId)
                    if (existingAccount == null) return@execute AccountNotFound(recipientId)
                    else return@execute BalanceOverflow(amount, recipientId)

                }

                val tx = Transaction(
                    UUID.randomUUID().toString(),
                    TransactionType.TRANSFER,
                    amount,
                    newSenderBalance,
                    key,
                    senderId,
                    recipientId,
                    TransactionStatus.SUCCESS
                )
                txRepository.save(tx)
                TransferSuccess(newSenderBalance, amount, senderId, recipientId, tx.id)
            }
        } catch (e: DuplicateKeyException) {
            val saved = txRepository.findByIdempotencyKey(key) ?: throw IllegalStateException("Ключ идемпотентности $key повторился но не найден в бд", e)
            if (saved.type == TransactionType.TRANSFER && saved.amount == amount && saved.senderId == senderId && saved.recipientId == recipientId) {
                TransferSuccess(saved.balanceAfter, saved.amount, senderId, recipientId, saved.id)
            } else {
                IdempotencyConflict(key)
            }
        }
        if (result is TransferSuccess) {
            accountService.invalidateCache(recipientId)
            accountService.invalidateCache(senderId)
            idempotencyCache.put(key, CachedEntry(request, result))
        }
        return result
      }
}