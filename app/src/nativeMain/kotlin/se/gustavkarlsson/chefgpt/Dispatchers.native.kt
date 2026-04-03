package se.gustavkarlsson.chefgpt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual val Dispatchers.IoOrDefault: kotlinx.coroutines.CoroutineDispatcher
    get() = Dispatchers.IO
