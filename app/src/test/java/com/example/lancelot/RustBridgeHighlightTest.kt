package com.example.lancelot

import com.example.lancelot.rust.RustBridge
import org.junit.Assert.assertTrue
import org.junit.Test

class RustBridgeHighlightTest {
    @Test
    fun highlightReturnsResult() {
        val result = RustBridge.realTimeHighlightSafe(
            "int main() { return 0; }",
            "cpp",
            "{}",
            "{}",
            "{}",
            "{}",
            "[]"
        )
        when (result) {
            is RustResult.Success -> assertTrue(result.data.isNotBlank())
            is RustResult.Error -> throw AssertionError("Highlight failed: ${result.error}")
        }
    }
}
