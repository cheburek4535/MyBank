package bank.controller

import bank.dto.DepositRequest
import bank.dto.ErrorResponse
import bank.dto.TransferRequest
import bank.dto.WithdrawRequest
import bank.model.*
import bank.service.TransactionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/transactions")
class TransactionController(private val txService: TransactionService) {
    @PostMapping("/transfer")
    fun transfer(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: TransferRequest
    ): ResponseEntity<Any> {
        if (idempotencyKey.isBlank()) return ResponseEntity.badRequest().body(ErrorResponse("Idempotency-Key пустой"))

        return when (val result = txService.transfer(idempotencyKey, request)) {
            is TransferSuccess -> ResponseEntity.ok(result)
            is InvalidAmount -> ResponseEntity.badRequest().body(ErrorResponse("Невалидная сумма"))
            is NotEnoughMoney -> ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("Недостаточно средств, баланс ${result.balance}"))
            is SameAccount -> ResponseEntity.badRequest().body(ErrorResponse("Нельзя переводить деньги самому себе"))
            is BalanceOverflow -> ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("Счет получателя переполнится"))
            is AccountNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("Счет ${result.accountId} не найден"))
            is IdempotencyConflict -> ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("Ключ идемпотентности ${result.key} конфликтует с другим запросом"))
        }
    }
    @PostMapping("/deposit")
    fun deposit(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: DepositRequest
    ): ResponseEntity<Any> {
        if (idempotencyKey.isBlank()) return ResponseEntity.badRequest().body(ErrorResponse("idempotency-Key пустой"))

        return when (val result = txService.deposit(idempotencyKey, request)) {
            is DepositSuccess -> ResponseEntity.ok(result)
            is InvalidAmount -> ResponseEntity.badRequest().body(ErrorResponse("Невалидная сумма"))
            is BalanceOverflow -> ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("Счет переполнится"))
            is AccountNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("Счет ${result.accountId} не найден"))
            is IdempotencyConflict -> ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("Ключ идемпотентности ${result.key} конфликтует с другим запросом"))
        }
    }
    @PostMapping("/withdraw")
    fun withdraw(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: WithdrawRequest
    ): ResponseEntity<Any> {
        if (idempotencyKey.isBlank()) return ResponseEntity.badRequest().body(ErrorResponse("Idempotency-Key пустой"))

        return when (val result = txService.withdraw(idempotencyKey, request)) {
            is WithdrawSuccess -> ResponseEntity.ok(result)
            is InvalidAmount -> ResponseEntity.badRequest().body(ErrorResponse("Невалидная сумма"))
            is NotEnoughMoney -> ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("Недостаточно средств, баланс ${result.balance}"))
            is AccountNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("Счет ${result.accountId} не найден"))
            is IdempotencyConflict -> ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("Ключ идемпотентности ${result.key} конфликтует с другим запросом"))
        }
    }
}