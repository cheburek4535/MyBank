package bank.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class TransferRequest(
    @field:Positive
    val amount: Long,

    @field:NotBlank
    val senderAccountId: String,

    @field:NotBlank
    val recipientAccountId: String
)
data class WithdrawRequest(
    @field:Positive
    val amount: Long,

    @field:NotBlank
    val accountId: String
)
data class DepositRequest(
    @field:Positive
    val amount: Long,

    @field:NotBlank
    val accountId: String
)