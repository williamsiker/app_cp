package com.example.lancelot.execution.data

import com.example.lancelot.execution.domain.ExecutionRepository
import com.example.lancelot.execution.model.ExecutionResult
import com.example.lancelot.rust.RustBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RustExecutionRepository : ExecutionRepository {
    override suspend fun execute(code: String, language: String, input: String): ExecutionResult = withContext(Dispatchers.IO) {
        val json = RustBridge.executeCodeDetailed(code, language, input)
        val obj = JSONObject(json)
        if (obj.has("error")) {
            throw RuntimeException(obj.getString("error"))
        }
        ExecutionResult(
            output = obj.optString("output"),
            compileOutput = obj.optString("compile_output", "null")
        )
    }
}
