package bank.repository

import bank.model.Account
import bank.model.AccountStatus
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
        return jdbcClient.sql("select id, owner, balance, status from accounts where id = :id")
            .param("id", id)
            .query {rs, _ ->
                Account.create(
                    id = rs.getString("id"),
                    balance = rs.getLong("balance"),
                    owner = rs.getString("owner"),
                    status = AccountStatus.valueOf(rs.getString("status"))
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

    fun findByIdsAndLock(first: String, second: String): List<Account> {
        return jdbcClient.sql("""
            select id, balance, owner, status from accounts
            where id in (:first, :second)
            order by id
            for update
        """.trimIndent()
        ).param("first", first)
            .param("second", second)
            .query {rs, _ ->
                Account.create(
                    rs.getString("id"),
                    rs.getString("owner"),
                    rs.getLong("balance"),
                    AccountStatus.valueOf(rs.getString("status"))
                )
            }.list()
    }
}