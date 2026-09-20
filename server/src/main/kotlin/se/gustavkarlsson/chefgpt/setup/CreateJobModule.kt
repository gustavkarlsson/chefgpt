package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import kotlinx.serialization.json.Json
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.jobs.AgentJobScope
import se.gustavkarlsson.chefgpt.jobs.InMemoryJobRepository
import se.gustavkarlsson.chefgpt.jobs.JobRepository
import se.gustavkarlsson.chefgpt.jobs.JobRunner

fun Application.createJobModule() =
    module {
        single { InMemoryJobRepository() } bind JobRepository::class
        single { AgentJobScope() }
        single { JobRunner(get<JobRepository>(), get<AgentJobScope>(), get<Json>()) }
    }
