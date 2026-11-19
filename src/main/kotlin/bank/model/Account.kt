package bank.model

class Account private constructor(val id: String, val owner: String, var initBalance: Long = 0L) {
    var balance: Long = initBalance
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

        fun create(id: String, owner: String, balance: Long = 0L): Account {
            require(id.isNotBlank()) {"id должен быть непустым"}
            require(owner.isNotBlank()) {"owner не должен быть пустым"}
            require(balance in MIN_BALANCE..MAX_BALANCE) {"баланс должен быть в пределах мин и макс значений"}
            return Account(id, owner, balance)
        }
    }
    fun deposit(amount: Long): Long {
        require(amount > 0) {"сумма пополнения должна быть положительной"}
        require(MAX_BALANCE - balance >= amount)
        balance += amount
        return balance
    }
    fun withdraw(amount: Long): Long {
        require(amount > 0) {"сумма списания должна быть положительной"}
        require(balance >= amount) {"сумма списания не может быть больше баланса"}
        balance += amount
        return balance
    }


}

