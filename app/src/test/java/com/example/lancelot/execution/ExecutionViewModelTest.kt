package com.example.lancelot.execution

import com.example.lancelot.execution.domain.ExecutionRepository
import com.example.lancelot.execution.model.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeRepo : ExecutionRepository {
    var shouldFail = false
    override suspend fun execute(code: String, language: String, input: String): ExecutionResult {
        if (shouldFail) throw RuntimeException("fail")
        return ExecutionResult("out", null)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExecutionViewModelTest {
    private lateinit var repo: FakeRepo
    private lateinit var vm: ExecutionViewModel
    private val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    @Before
    fun setup() {
        repo = FakeRepo()
        vm = ExecutionViewModel(repo)
    }

    @Test
    fun successUpdatesState() = runTest(dispatcher) {
        vm.run("code","lang","input")
        dispatcher.scheduler.advanceUntilIdle()
        val state = vm.state
        assert(state is ExecutionState.Success)
        state as ExecutionState.Success
        assertEquals("out", state.result.output)
    }

    @Test
    fun errorUpdatesState() = runTest(dispatcher) {
        repo.shouldFail = true
        vm.run("code","lang","input")
        dispatcher.scheduler.advanceUntilIdle()
        val state = vm.state
        assert(state is ExecutionState.Error)
    }
}
