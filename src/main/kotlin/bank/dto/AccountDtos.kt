package bank.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero

data class CreateAccountRequest(
    @field:NotBlank
    val owner: String,

    @field:PositiveOrZero
    val initialBalance: Long = 0L
)
data class DeleteAccountRequest(
    @field:NotBlank
    val accountId: String
)

data class AccountResponse(
    val id: String,
    val balance: Long,
    val owner: String
)