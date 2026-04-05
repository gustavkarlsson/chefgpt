package se.gustavkarlsson.chefgpt

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val Dispatchers.IoOrDefault: CoroutineDispatcher
    get() = Dispatchers.IO
