package com.example.lancelot.execution.domain

import com.example.lancelot.execution.model.ExecutionResult

interface ExecutionRepository {
    suspend fun execute(code: String, language: String, input: String): ExecutionResult
}
