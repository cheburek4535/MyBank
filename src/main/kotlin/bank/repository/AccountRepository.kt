package bank.repository

import bank.model.Account
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class AccountRepository(
    private val jdbcClient: JdbcClient
) {
    fun save(account: Account) {
        jdbcClient.sql("insert into accounts (id, owner, balance) values (:id, :owner, :balance)")
            .param("id", account.id)
            .param("owner", account.owner)
            .param("balance", account.balance)
            .update()
    }
    fun findById(id: String): Account? {
        return jdbcClient.sql("select id, owner, balance from accounts where id = :id")
            .param("id", id)
            .query {rs, _ ->
                Account.create(
                    id = rs.getString("id"),
                    balance = rs.getLong("balance"),
                    owner = rs.getString("owner")
                )
            }
            .optional()
            .orElse(null)
    }
    fun increaseBalance(id: String, amount: Long): Long? {
        require(amount > 0)
        return jdbcClient.sql(
            """
                update accounts
                set balance = balance + :amount
                where id = :id and :maxBal - :amount >= balance and status = 'OPEN'
                returning balance
            """.trimIndent()
        )
            .param("amount", amount)
            .param("id", id)
            .param("maxBal", Account.MAX_BALANCE)
            .query(Long::class.java).optional().orElse(null)
    }
    fun decreaseBalance(id: String, amount: Long): Long? {
        require(amount > 0)
        return jdbcClient.sql(
            """
                update accounts
                set balance = balance - :amount
                where id = :id and balance >= :amount and status = 'OPEN'
                returning balance
            """.trimIndent()
        )
            .param("amount", amount)
            .param("id", id)
            .query(Long::class.java).optional().orElse(null)

    }

    fun getAll(): List<Account> {
        return jdbcClient.sql("select id, balance, owner from accounts").query {rs, _ ->
            Account.create(
                rs.getString("id"),
                rs.getString("owner"),
                rs.getLong("balance")
            )
        }.list()
    }
    fun getAllIds(): List<String> {
        return jdbcClient.sql("select id from accounts").query{
            rs, _ -> rs.getString("id")
        }.list()
    }
    fun delete(id: String): Boolean {
        return jdbcClient.sql("delete from accounts where id = :id").param("id", id).update() > 0
    }
    fun closeAccount(id: String) = jdbcClient.sql("update accounts set status = 'CLOSED' where id = :id").param("id", id).update()
}