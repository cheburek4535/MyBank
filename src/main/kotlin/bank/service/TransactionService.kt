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

    private data class CachedEntry(val request: Any, val result: TransactionResult)
    private val idempotencyCache = MyHashMap<String, CachedEntry>()

    private fun generateId(): String = UUID.randomUUID().toString()

    fun deposit(key: String, request: DepositRequest): DepositResult {
        findIdempotentDeposit(key, request)?.let {return it}
        if (request.amount <= 0) return InvalidAmount(request.amount)
        if (!accountService.mightExists(request.accountId)) return AccountNotFound(request.accountId)

        val result = executeDeposit(request, key)
        if (result is DepositSuccess) {
            accountService.invalidateCache(request.accountId)
            idempotencyCache.put(key, CachedEntry(request, result))
        }
        return result
    }
    fun withdraw(key: String, request: WithdrawRequest): WithdrawResult {
        findIdempotentWithdraw(key, request)?.let {return it}
        if (request.amount <= 0) return InvalidAmount(request.amount)
        if (!accountService.mightExists(request.accountId)) return AccountNotFound(request.accountId)

        val result = executeWithdraw(request, key)
        if (result is WithdrawSuccess) {
            accountService.invalidateCache(result.accountId)
            idempotencyCache.put(key, CachedEntry(request, result))
        }
        return result
    }
    fun transfer(key: String, request: TransferRequest): TransferResult {
        findIdempotentTransfer(key, request)?.let {return it}

        val senderId = request.senderAccountId
        val recipientId = request.recipientAccountId
        val amount = request.amount

        if (recipientId == senderId) return SameAccount(recipientId)
        if (amount <= 0) return InvalidAmount(amount)
        if (!accountService.mightExists(recipientId)) return AccountNotFound(recipientId)
        if(!accountService.mightExists(senderId)) return AccountNotFound(senderId)

        val result = executeTransfer(request, key)

        if (result is TransferSuccess) {
            accountService.invalidateCache(recipientId)
            accountService.invalidateCache(senderId)
            idempotencyCache.put(key, CachedEntry(request, result))
        }
        return result
    }


    private fun matchSavedTransfer(saved: Transaction, request: TransferRequest): TransferResult {
        return if (saved.type == TransactionType.TRANSFER && saved.amount == request.amount && saved.senderId == request.senderAccountId && saved.recipientId == request.recipientAccountId) {
            TransferSuccess(saved.balanceAfter, saved.amount, saved.senderId, saved.recipientId, saved.id)
        } else {
            IdempotencyConflict(saved.idempotencyKey)
        }
    }
    private fun matchSavedDeposit(saved: Transaction, request: DepositRequest): DepositResult {
        return if (saved.type == TransactionType.DEPOSIT && saved.recipientId == request.accountId && saved.amount == request.amount) {
            DepositSuccess(saved.balanceAfter, saved.amount, request.accountId)
        } else {
            IdempotencyConflict(saved.idempotencyKey)
        }
    }
    private fun matchSavedWithdraw(saved: Transaction, request: WithdrawRequest): WithdrawResult {
        return if (saved.type == TransactionType.WITHDRAW && saved.senderId == request.accountId && saved.amount == request.amount) {
            WithdrawSuccess(saved.balanceAfter, saved.amount, request.accountId)
        } else {
            IdempotencyConflict(saved.idempotencyKey)
        }
    }

    private fun findIdempotentTransfer(key: String, request: TransferRequest): TransferResult? {
        idempotencyCache.get(key)?.let {existing ->
            if (existing.request != request) return IdempotencyConflict(key)
            return when (val res = existing.result) {
                is TransferResult -> res
                else-> IdempotencyConflict(key)
            }
        }

        val saved = txRepository.findByIdempotencyKey(key) ?: return null
        return matchSavedTransfer(saved, request).also { if (it !is IdempotencyConflict) idempotencyCache.put(key, CachedEntry(request, it))}
    }
    private fun findIdempotentDeposit(key: String, request: DepositRequest): DepositResult? {
        idempotencyCache.get(key)?.let {existing ->
            if (existing.request != request) return IdempotencyConflict(key)
            return when (val res = existing.result) {
                is DepositResult ->  res
                else -> IdempotencyConflict(key)
            }
        }
        val saved = txRepository.findByIdempotencyKey(key) ?: return null
        return matchSavedDeposit(saved, request).also { if (it !is IdempotencyConflict) idempotencyCache.put(key, CachedEntry(request, it))}
    }
    private fun findIdempotentWithdraw(key: String, request: WithdrawRequest): WithdrawResult? {
        idempotencyCache.get(key)?.let {existing ->
            if (existing.request != request) return IdempotencyConflict(key)
            return when (val res = existing.result) {
                is WithdrawResult ->  res
                else -> IdempotencyConflict(key)
            }
        }

        val saved = txRepository.findByIdempotencyKey(key) ?: return null
        return matchSavedWithdraw(saved, request).also { if (it !is IdempotencyConflict) idempotencyCache.put(key, CachedEntry(request, it))}
    }
    private fun executeTransfer(request: TransferRequest, key: String): TransferResult {
        val senderId = request.senderAccountId
        val amount = request.amount
        val recipientId = request.recipientAccountId

        return try {
            txTemplate.execute { status ->
                val newSenderBalance = accountRepository.decreaseBalance(senderId, amount) ?: run {
                    status.setRollbackOnly()
                    val senderAccount = accountRepository.findById(senderId)
                    return@execute if (senderAccount == null || senderAccount.status != AccountStatus.OPEN) AccountNotFound(senderId)
                    else NotEnoughMoney(senderAccount.balance, amount)
                }
                accountRepository.increaseBalance(recipientId, amount) ?: run {
                    status.setRollbackOnly()
                    val recipientAccount = accountRepository.findById(recipientId)
                    return@execute if (recipientAccount == null || recipientAccount.status != AccountStatus.OPEN) AccountNotFound(recipientId)
                    else BalanceOverflow(amount, recipientId)

                }
                val tx = Transaction(
                    generateId(),
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
            matchSavedTransfer(saved, request)
        }
    }
    private fun executeDeposit(request: DepositRequest, key: String): DepositResult {
        return try {
            txTemplate.execute { status ->
                val newBalance = accountRepository.increaseBalance(request.accountId, request.amount) ?: run {
                    status.setRollbackOnly()
                    val account = accountRepository.findById(request.accountId)
                    return@execute if (account == null || account.status != AccountStatus.OPEN) AccountNotFound(request.accountId)
                    else BalanceOverflow(request.amount, request.accountId)
                }
                val tx = Transaction(
                    generateId(),
                    TransactionType.DEPOSIT,
                    request.amount,
                    newBalance,
                    key,
                    null,
                    request.accountId
                )
                txRepository.save(tx)
                DepositSuccess(newBalance, request.amount, request.accountId)
            }
        } catch (e: DuplicateKeyException) {
            val saved = txRepository.findByIdempotencyKey(key) ?: throw IllegalStateException("Ключ идемпотентности повторился но не найден в БД", e)
            matchSavedDeposit(saved, request)
        }
    }
    private fun executeWithdraw(request: WithdrawRequest, key: String): WithdrawResult {
        return try {
            txTemplate.execute { status ->
                val newBalance = accountRepository.decreaseBalance(request.accountId, request.amount) ?: run {
                    status.setRollbackOnly()
                    val account = accountRepository.findById(request.accountId)
                    return@execute if (account == null || account.status != AccountStatus.OPEN) AccountNotFound(request.accountId)
                    else NotEnoughMoney(account.balance, request.amount)
                }
                val tx = Transaction(
                    generateId(),
                    TransactionType.WITHDRAW,
                    request.amount,
                    newBalance,
                    key,
                    request.accountId,
                    null
                )
                txRepository.save(tx)
                WithdrawSuccess(newBalance, request.amount, request.accountId)
            }
        } catch (e: DuplicateKeyException) {
            val saved = txRepository.findByIdempotencyKey(key) ?: throw IllegalStateException("ключ идемпотентности повторился но не найден в БД", e)
            matchSavedWithdraw(saved, request)
        }
    }

}