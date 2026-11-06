package com.caceras.aibrief.data

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleFlightTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Test
    fun concurrentCallsForOneKeyShareOneRun() = runBlocking {
        val flight = SingleFlight<String, String>(scope)
        val runs = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val callers = List(5) {
            async(Dispatchers.Default) {
                flight.run("cover") { runs.incrementAndGet(); gate.await(); "bytes" }
            }
        }
        while (runs.get() == 0) delay(5)
        delay(50) // let every caller join the run in progress
        gate.complete(Unit)
        assertEquals(List(5) { "bytes" }, callers.awaitAll())
        assertEquals(1, runs.get())
        scope.cancel()
    }

    @Test
    fun keyIsReleasedOnceTheRunEnds() = runBlocking {
        val flight = SingleFlight<String, Int>(scope)
        val runs = AtomicInteger()
        flight.run("a") { runs.incrementAndGet() }
        flight.run("a") { runs.incrementAndGet() }
        assertEquals(2, runs.get())
        withTimeout(1_000) { while (flight.size > 0) delay(5) }
        scope.cancel()
    }

    @Test
    fun differentKeysRunSeparately() = runBlocking {
        val flight = SingleFlight<String, String>(scope)
        assertEquals("a", flight.run("a") { "a" })
        assertEquals("b", flight.run("b") { "b" })
        scope.cancel()
    }

    @Test
    fun aCancelledCallerDoesNotCancelTheRunOthersWaitFor() = runBlocking {
        val flight = SingleFlight<String, String>(scope)
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val leaver = launch(Dispatchers.Default) {
            flight.run("cover") { started.complete(Unit); gate.await(); "bytes" }
        }
        started.await()
        val stayer = async(Dispatchers.Default) { flight.run("cover") { "second run" } }
        delay(50)
        leaver.cancel()
        gate.complete(Unit)
        assertEquals("bytes", stayer.await())
        scope.cancel()
    }

    @Test
    fun failuresReachEveryCallerAndReleaseTheKey() = runBlocking {
        val flight = SingleFlight<String, String>(scope)
        val failure = runCatching { flight.run("bad") { error("offline") } }
        assertTrue(failure.isFailure)
        withTimeout(1_000) { while (flight.size > 0) delay(5) }
        assertEquals("ok", flight.run("bad") { "ok" })
        scope.cancel()
    }
}
