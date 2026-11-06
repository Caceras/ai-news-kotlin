package com.caceras.aibrief.data

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Runs one piece of work per key at a time. Callers asking for a key that is
 * already in progress wait for that work instead of starting it again, so a
 * cover shown on a card and in the opened story downloads once.
 *
 * The work runs in [scope], not in the caller, so a card scrolled away does
 * not cancel a download another caller is still waiting for. The key is
 * released when the work finishes, so a later call starts afresh.
 */
internal class SingleFlight<K : Any, V>(private val scope: CoroutineScope) {
    private val inFlight = ConcurrentHashMap<K, Deferred<V>>()

    suspend fun run(key: K, block: suspend () -> V): V {
        val work = inFlight.computeIfAbsent(key) {
            scope.async(start = CoroutineStart.LAZY) { block() }
        }
        work.invokeOnCompletion { inFlight.remove(key, work) }
        work.start()
        return work.await()
    }

    /** How many keys are in progress. */
    val size: Int get() = inFlight.size
}
