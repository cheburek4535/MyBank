package bank.controller

import bank.dto.AccountResponse
import bank.dto.CreateAccountRequest
import bank.dto.StandardResponse
import bank.service.AccountService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accounts")
class AccountController(private val accountService: AccountService) {
    @PostMapping
    fun createAccount(@Valid @RequestBody request: CreateAccountRequest): ResponseEntity<AccountResponse> {
        val account = accountService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse(account.id, account.balance,  account.owner, account.status.name))
    }
    @GetMapping("/{id}")
    fun getAccount(@PathVariable id: String): ResponseEntity<AccountResponse> {
        val account = accountService.get(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(AccountResponse(account.id, account.balance, account.owner, account.status.name))
    }
    @DeleteMapping("/{id}")
    fun deleteAccount(@PathVariable id: String): ResponseEntity<StandardResponse> {
        val success = accountService.delete(id)
        if (!success) return ResponseEntity.notFound().build()
        return ResponseEntity.ok().body(StandardResponse("Аккаунт $id удален успешно"))
    }

}