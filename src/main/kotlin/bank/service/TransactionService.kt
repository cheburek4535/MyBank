package bank.service

import bank.dto.TransferRequest
import bank.model.TransferResult
import bank.model.TransferSuccess
import org.springframework.stereotype.Service

@Service
class TransactionService {
    fun transfer(key: String, request: TransferRequest): TransferResult {
        return TransferSuccess(1L, 1L, "ACC2", "ACC3", "1244235FFFS")
    }
}