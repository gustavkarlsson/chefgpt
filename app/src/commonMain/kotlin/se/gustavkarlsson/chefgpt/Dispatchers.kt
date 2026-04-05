package se.gustavkarlsson.chefgpt

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

expect val Dispatchers.IoOrDefault: CoroutineDispatcher
