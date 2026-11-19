package bank.model

class Account(val id: String, val owner: String, val balance: Long = 0L) {
    companion object {
        const val MIN_BALANCE = 0L
        const val MAX_BALANCE = Long.MAX_VALUE
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Account) return false
        return other.id == this.id
    }

    override fun hashCode(): Int = id.hashCode()
}

