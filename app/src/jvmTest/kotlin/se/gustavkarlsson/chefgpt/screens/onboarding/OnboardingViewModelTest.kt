package se.gustavkarlsson.chefgpt.screens.onboarding

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.facts.TemperatureUnit
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.facts.VolumeUnits
import se.gustavkarlsson.chefgpt.facts.WeightUnits
import se.gustavkarlsson.chefgpt.facts.usecases.GetFacts
import se.gustavkarlsson.chefgpt.facts.usecases.SetFacts
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.home.HomeScreen
import se.gustavkarlsson.chefgpt.sessions.SessionCredentials
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.sessions.UserName
import se.gustavkarlsson.chefgpt.sessions.usecases.GetCurrentSession
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @Test
    fun `saves selected dietary restrictions when confirming the dietary step`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startOnboarding()
                val dietaryStep = answerStepsUpToDietary(setup)

                dietaryStep.dietaryChips.first { it.name == "Vegan" }.onClick("Vegan")
                dietaryStep.onClickConfirm()
                runCurrent()

                assertEquals(setOf("Vegan"), setup.savedFacts.single().dietary)
                assertIs<HomeScreen>(
                    setup.navigator.backStack.value
                        .single(),
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `records no dietary restrictions when confirming without selecting any`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startOnboarding()
                val dietaryStep = answerStepsUpToDietary(setup)

                dietaryStep.onClickConfirm()
                runCurrent()

                assertEquals(emptySet(), setup.savedFacts.single().dietary)
                assertIs<HomeScreen>(
                    setup.navigator.backStack.value
                        .single(),
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun TestScope.startOnboarding(): Setup {
        val savedFacts = mutableListOf<UserFacts>()
        val navigator = Navigator()
        val states = mutableListOf<UiState>()
        val viewModel =
            OnboardingViewModel(
                getCurrentSession = FakeGetCurrentSession(),
                getFacts = FakeGetFacts(UserFacts.Empty),
                setFacts = FakeSetFacts(savedFacts),
                showSnackbar = ShowSnackbar { _, _ -> },
                navigator = navigator,
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect(states::add)
        }
        return Setup(savedFacts, navigator, states)
    }

    private suspend fun TestScope.answerStepsUpToDietary(setup: Setup): UiStep.DietaryStep {
        assertIs<UiStep.NameStep>(currentLoaded(setup.states).steps[0]).onClickConfirm()
        assertIs<UiStep.TemperatureUnitStep>(currentLoaded(setup.states).steps[1]).onClick(TemperatureUnit.Celsius)
        assertIs<UiStep.WeightUnitsStep>(currentLoaded(setup.states).steps[2]).onClick(WeightUnits.Metric)
        assertIs<UiStep.VolumeUnitsStep>(currentLoaded(setup.states).steps[3]).onClick(VolumeUnits.Metric)
        return assertIs<UiStep.DietaryStep>(currentLoaded(setup.states).steps[4])
    }

    private suspend fun TestScope.currentLoaded(states: List<UiState>): UiState.Loaded {
        runCurrent()
        return assertIs<UiState.Loaded>(states.last())
    }

    private class Setup(
        val savedFacts: MutableList<UserFacts>,
        val navigator: Navigator,
        val states: MutableList<UiState>,
    )
}

private class FakeGetCurrentSession : GetCurrentSession {
    override suspend fun invoke(): Result<SessionCredentials?, Unit> =
        Ok(SessionCredentials(UserName("gustav"), SessionId("session")))
}

private class FakeGetFacts(
    private val facts: UserFacts,
) : GetFacts {
    override suspend fun invoke(sessionId: SessionId): Result<UserFacts, ClientError> = Ok(facts)
}

private class FakeSetFacts(
    private val saved: MutableList<UserFacts>,
) : SetFacts {
    override suspend fun invoke(
        sessionId: SessionId,
        facts: UserFacts,
    ): Result<Unit, ClientError> {
        saved += facts
        return Ok(Unit)
    }
}
