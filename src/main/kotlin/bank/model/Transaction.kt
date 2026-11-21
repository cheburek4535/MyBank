package bank.model

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Long,
    val balanceAfter: Long,
    val idempotencyKey: String,
    val senderId: String? = null,
    val recipientId: String? = null,
    val status: TransactionStatus = TransactionStatus.SUCCESS
)

enum class TransactionType {TRANSFER, DEPOSIT, WITHDRAW}
enum class TransactionStatus {SUCCESS, FAILED}