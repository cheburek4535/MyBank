package bank.model

sealed interface DepositResult
sealed interface WithdrawResult
sealed interface TransferResult

data class AccountNotFound(val accountId: String) : DepositResult, WithdrawResult, TransferResult
data class InvalidAmount(val amount: Long) : DepositResult, WithdrawResult, TransferResult

data class NotEnoughMoney(val balance: Long, val requestedAmount: Long): WithdrawResult, TransferResult
data class BalanceOverflow(val amount: Long, val accountId: String) : DepositResult, TransferResult

data class DepositSuccess(val newBalance: Long, val amount: Long, val accountId: String) : DepositResult
data class WithdrawSuccess(val newBalance: Long, val amount: Long, val accountId: String) : WithdrawResult
data class TransferSuccess(val newSenderBalance: Long, val amount: Long, val senderAccountId: String, val recipientAccountId: String, val transactionId: String) : TransferResult

data class SameAccount(val accountId: String): TransferResult