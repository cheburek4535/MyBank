package bank.service

import bank.dto.CreateAccountRequest
import bank.model.Account
import org.springframework.stereotype.Service

@Service
class AccountService {
    fun create(request: CreateAccountRequest): Account {
        return Account.create("ACC1", request.owner, request.initialBalance)
    }
    fun get(id: String): Account? {
        return null
    }
}