package contacts.async.data

import contacts.async.ASYNC_DISPATCHER
import contacts.data.DataUpdate
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Suspends the current coroutine, performs the operation in the given [context], then returns the
 * result.
 *
 * Computations automatically stops if the parent coroutine scope / job is cancelled.
 *
 * See [DataUpdate.commit].
 */
suspend fun DataUpdate.commitWithContext(context: CoroutineContext = ASYNC_DISPATCHER): DataUpdate.Result =
    withContext(context) { commit { !isActive } }

/**
 * Creates a [CoroutineScope] with the given [context], performs the operation in that scope, then
 * returns the [Deferred] result.
 *
 * Computations automatically stops if the parent coroutine scope / job is cancelled.
 *
 * See [DataUpdate.commit].
 */
fun DataUpdate.commitAsync(context: CoroutineContext = ASYNC_DISPATCHER): Deferred<DataUpdate.Result> =
    CoroutineScope(context).async { commit { !isActive } }