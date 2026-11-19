package bank.model

class Account(val id: String, val owner: String, var balance: Long = 0L) {
    companion object {
        const val MIN_BALANCE = 0L
        const val MAX_BALANCE = Long.MAX_VALUE

        fun create(id: String, owner: String, balance: Long = 0L): Account {
            require(id.isNotEmpty()) {"id должен быть непустым"}
            require(owner.isNotEmpty()) {"owner не должен быть пустым"}
            require(balance in MIN_BALANCE..MAX_BALANCE) {"баланс должен быть в пределах мин и макс значений"}
            return Account(id, owner, balance)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Account) return false
        return other.id == this.id
    }

    override fun hashCode(): Int = id.hashCode()

    fun deposit(amount: Long): Long {
        require(amount > 0) {"сумма пополнения должна быть положительной"}
        balance += amount
        return balance
    }
}

