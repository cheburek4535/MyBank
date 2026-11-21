package bank.service

import bank.dto.DepositRequest
import bank.dto.TransferRequest
import bank.dto.WithdrawRequest
import bank.model.AccountNotFound
import bank.model.BalanceOverflow
import bank.model.InvalidAmount
import bank.model.NotEnoughMoney
import bank.model.SameAccount
import bank.model.Transaction
import bank.model.TransactionStatus
import bank.model.TransactionType
import bank.model.TransferResult
import bank.model.TransferSuccess
import bank.repository.AccountRepository
import bank.repository.TransactionRepository
import bank.structures.MyHashMap
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Service
class TransactionService(private val accountService: AccountService, private val txRepository: TransactionRepository,
    private val accountRepository: AccountRepository, private val txTemplate: TransactionTemplate) {

    private val hashMap = MyHashMap<String, TransferResult>()

    fun deposit(key: String, request: DepositRequest) {}
    fun withdraw(key: String, request: WithdrawRequest) {}

    fun transfer(key: String, request: TransferRequest): TransferResult {
        val senderId = request.senderAccountId
        val recipientId = request.recipientAccountId
        val amount = request.amount
        if (recipientId == senderId) return SameAccount(recipientId)
        if (amount <= 0) return InvalidAmount(amount)
        hashMap.get(key)?.let {return it}

        val senderAccount = accountService.get(senderId) ?: return AccountNotFound(senderId)

        txRepository.findByIdempotencyKey(key)?.let {
            return TransferSuccess(
                senderAccount.balance,
                it.amount,
                senderId,
                recipientId,
                it.id
            )
        }

        val result: TransferResult = txTemplate.execute { status ->
            accountRepository.decreaseBalance(senderId, amount) ?: run {
                status.setRollbackOnly()
                if (accountService.get(senderId) == null) return@execute AccountNotFound(senderId)
                else return@execute BalanceOverflow(amount, senderId)
            }
            accountRepository.increaseBalance(recipientId, amount) ?: run {
                status.setRollbackOnly()
                val existingAccount = accountService.get(recipientId)
                if (existingAccount == null) return@execute AccountNotFound(recipientId)
                else return@execute NotEnoughMoney(existingAccount.balance, amount)
            }

            val tx = Transaction(
                UUID.randomUUID().toString(),
                TransactionType.TRANSFER,
                amount,
                key,
                senderId,
                recipientId,
                TransactionStatus.SUCCESS
            )
            txRepository.save(tx)
            TransferSuccess(senderAccount.balance-amount, amount, senderId, recipientId, tx.id)
        }
        accountService.invalidateCache(recipientId)
        accountService.invalidateCache(senderId)
        return result
      }
}