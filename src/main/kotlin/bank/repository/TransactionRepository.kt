package bank.repository

import bank.model.Transaction
import bank.model.TransactionStatus
import bank.model.TransactionType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class TransactionRepository(private val jdbcClient: JdbcClient) {
    fun save(tx: Transaction) {
        jdbcClient.sql("""
            insert into transactions (id, type, amount, balance_after, idempotency_key, sender_account_id, recipient_account_id, status)
            values (:id, :type, :amount, :balance_after, :idempotency_key, :sender_account_id, :recipient_account_id, :status)
            """.trimIndent()
        )
            .param("id", tx.id)
            .param("type", tx.type.name)
            .param("amount", tx.amount)
            .param("balance_after", tx.balanceAfter)
            .param("idempotency_key", tx.idempotencyKey)
            .param("sender_account_id", tx.senderId)
            .param("recipient_account_id", tx.recipientId)
            .param("status", tx.status.name)
            .update()
    }
    fun findByIdempotencyKey(key: String): Transaction? {
        return jdbcClient.sql("select * from transactions where idempotency_key = :key")
            .param("key", key)
            .query {rs, _ ->
                Transaction(
                    id = rs.getString("id"),
                    type = TransactionType.valueOf(rs.getString("type")),
                    amount = rs.getLong("amount"),
                    balanceAfter = rs.getLong("balance_after"),
                    idempotencyKey = rs.getString("idempotency_key"),
                    senderId = rs.getString("sender_account_id"),
                    recipientId = rs.getString("recipient_account_id"),
                    status = TransactionStatus.valueOf(rs.getString("status"))
                )
            }
            .optional()
            .orElse(null)
    }
}