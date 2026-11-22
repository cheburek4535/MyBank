package bank.model

class Account private constructor(val id: String, val owner: String, initBalance: Long = 0L, initStatus: AccountStatus = AccountStatus.OPEN) {
    var balance: Long = initBalance
        private set

    var status: AccountStatus = initStatus
    private set

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Account) return false
        return other.id == this.id
    }

    override fun hashCode(): Int = id.hashCode()


    companion object {
        const val MIN_BALANCE = 0L
        const val MAX_BALANCE = Long.MAX_VALUE

        fun create(id: String, owner: String, balance: Long = 0L, status: AccountStatus = AccountStatus.OPEN): Account {
            require(id.isNotBlank()) {"id должен быть непустым"}
            require(owner.isNotBlank()) {"owner не должен быть пустым"}
            require(balance in MIN_BALANCE..MAX_BALANCE) {"баланс должен быть в пределах мин и макс значений"}
            return Account(id, owner, balance, status)
        }
    }
    fun close() {
        status = AccountStatus.CLOSED
    }

}

enum class AccountStatus {OPEN, CLOSED, FROZEN}

