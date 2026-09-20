package se.gustavkarlsson.chefgpt.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Runs agent work that must outlive the request that started it. A SupervisorJob keeps one
// agent failure from cancelling the scope (and the other jobs running in it).
class AgentJobScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default)
