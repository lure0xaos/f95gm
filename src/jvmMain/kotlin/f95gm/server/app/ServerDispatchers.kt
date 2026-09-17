package f95gm.server.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Explicit execution contexts owned by the composition boundary. */
internal data class ServerDispatchers(
    val application: CoroutineDispatcher = Dispatchers.Default,
    val database: CoroutineDispatcher = Dispatchers.IO
)
