package se.gustavkarlsson.chefgpt.screens.onboarding

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.facts.TemperatureUnit
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.facts.VolumeUnits
import se.gustavkarlsson.chefgpt.facts.WeightUnits
import se.gustavkarlsson.chefgpt.facts.usecases.GetFacts
import se.gustavkarlsson.chefgpt.facts.usecases.SetFacts
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.StateViewModel
import se.gustavkarlsson.chefgpt.screens.home.HomeScreen
import se.gustavkarlsson.chefgpt.screens.login.LoginScreen
import se.gustavkarlsson.chefgpt.sessions.SessionCredentials
import se.gustavkarlsson.chefgpt.sessions.usecases.GetCurrentSession
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar

private val log = Logger.withTag("${OnboardingViewModel::class.simpleName}")

// Entry order determines step order
enum class OnboardingStep {
    Name,
    TemperatureUnit,
    WeightUnits,
    VolumeUnits,
    Dietary,
}

class OnboardingViewModel(
    private val getCurrentSession: GetCurrentSession,
    private val getFacts: GetFacts,
    private val setFacts: SetFacts,
    private val showSnackbar: ShowSnackbar,
    private val navigator: Navigator,
) : StateViewModel<State, UiState>() {
    override fun createInitialState() =
        State(
            initialized = false,
            credentials = null,
            collectedFacts = UserFacts(null, null, null, null, null),
            currentStepIndex = 0,
            steps = emptyList(),
            nameInput = "",
            dietaryInput = "",
            saving = false,
        )

    override fun State.toUiState(): UiState =
        if (!initialized) {
            UiState.Loading
        } else {
            UiState.Loaded(
                onClickBack = if (currentStepIndex > 0) ::goBack else null,
                currentStepIndex = currentStepIndex,
                steps =
                    steps.map { step ->
                        when (step) {
                            OnboardingStep.Name -> {
                                UiStep.NameStep(
                                    placeholder = FALLBACK_NAME,
                                    input = nameInput,
                                    onChangeInput = ::updateNameInput,
                                    onClickConfirm = ::confirmName,
                                )
                            }

                            OnboardingStep.TemperatureUnit -> {
                                UiStep.TemperatureUnitStep(onClick = ::confirmTemperatureUnit)
                            }

                            OnboardingStep.WeightUnits -> {
                                UiStep.WeightUnitsStep(onClick = ::confirmWeightUnits)
                            }

                            OnboardingStep.VolumeUnits -> {
                                UiStep.VolumeUnitsStep(onClick = ::confirmVolumeUnits)
                            }

                            OnboardingStep.Dietary -> {
                                UiStep.DietaryStep(
                                    dietaryChips = buildDietaryChips(selected = collectedFacts.dietary.orEmpty()),
                                    customInput = dietaryInput,
                                    onChangeCustomInput = ::updateDietaryInput,
                                    onClickAddCustom = ::addCustomDietary,
                                    onClickConfirm = ::confirmDietary,
                                )
                            }
                        }
                    },
            )
        }

    private fun buildDietaryChips(selected: Set<String>): List<DietaryChip> {
        val suggestedLowercase = SUGGESTED_DIETARY_RESTRICTIONS.map { it.lowercase() }
        val selectedLowercase = selected.map { it.lowercase() }
        val suggested =
            SUGGESTED_DIETARY_RESTRICTIONS.map { name ->
                DietaryChip.Suggested(
                    name = name,
                    onClick = ::toggleDietary,
                    selected = name.lowercase() in selectedLowercase,
                )
            }
        val custom =
            selected
                .filterNot { it.lowercase() in suggestedLowercase }
                .map { name ->
                    DietaryChip.Custom(
                        name = name,
                        onClick = ::toggleDietary,
                    )
                }
        return suggested + custom
    }

    init {
        viewModelScope.launch {
            val (credentials, _) = getCurrentSession()
            if (credentials == null) {
                navigator.replaceAll(LoginScreen())
                return@launch
            }
            // TODO Implement better recovery
            val facts = getFacts(credentials.sessionId).getOr(UserFacts.Empty)
            val unknownSteps = OnboardingStep.entries.filter { facts.isUnknown(it) }
            if (unknownSteps.isEmpty()) {
                navigator.replaceAll(HomeScreen())
                return@launch
            }
            innerState.update {
                it.copy(
                    initialized = true,
                    credentials = credentials,
                    collectedFacts = facts,
                    steps = unknownSteps,
                    nameInput = facts.preferredName.orEmpty(),
                )
            }
        }
    }

    private fun UserFacts.isUnknown(step: OnboardingStep): Boolean =
        when (step) {
            OnboardingStep.Name -> preferredName == null
            OnboardingStep.TemperatureUnit -> temperatureUnit == null
            OnboardingStep.WeightUnits -> weightUnits == null
            OnboardingStep.VolumeUnits -> volumeUnits == null
            OnboardingStep.Dietary -> dietary == null
        }

    private fun goBack() {
        innerState.update {
            if (it.currentStepIndex > 0) it.copy(currentStepIndex = it.currentStepIndex - 1) else it
        }
    }

    private fun updateNameInput(value: String) {
        innerState.update { it.copy(nameInput = value) }
    }

    private fun confirmName() {
        val confirmedName =
            innerState.value.nameInput
                .trim()
                .ifEmpty { null }
        innerState.update {
            it.copy(
                collectedFacts =
                    it.collectedFacts.copy(
                        preferredName = confirmedName ?: FALLBACK_NAME,
                    ),
            )
        }
        advance()
    }

    private fun confirmTemperatureUnit(value: TemperatureUnit) {
        innerState.update { it.copy(collectedFacts = it.collectedFacts.copy(temperatureUnit = value)) }
        advance()
    }

    private fun confirmWeightUnits(value: WeightUnits) {
        innerState.update { it.copy(collectedFacts = it.collectedFacts.copy(weightUnits = value)) }
        advance()
    }

    private fun confirmVolumeUnits(value: VolumeUnits) {
        innerState.update { it.copy(collectedFacts = it.collectedFacts.copy(volumeUnits = value)) }
        advance()
    }

    private fun toggleDietary(value: String) {
        innerState.update { state ->
            val selected = state.collectedFacts.dietary.orEmpty()
            val selectedLowercase = selected.map { it.lowercase() }
            val isSelected = value.lowercase() in selectedLowercase
            val newSelection =
                if (isSelected) {
                    selected.filterNot { it.equals(value, ignoreCase = true) }.toSet()
                } else {
                    selected + value
                }
            state.copy(collectedFacts = state.collectedFacts.copy(dietary = newSelection))
        }
    }

    private fun updateDietaryInput(value: String) {
        innerState.update { it.copy(dietaryInput = value) }
    }

    private fun addCustomDietary() {
        innerState.update { state ->
            val value = state.dietaryInput.trim()
            if (value.isEmpty()) return
            val updatedDietary = state.collectedFacts.dietary.orEmpty() + value
            val updatedFacts = state.collectedFacts.copy(dietary = updatedDietary)
            state.copy(collectedFacts = updatedFacts, dietaryInput = "")
        }
    }

    private fun confirmDietary() {
        innerState.update {
            it.copy(collectedFacts = it.collectedFacts.copy(dietary = it.collectedFacts.dietary.orEmpty()))
        }
        advance()
    }

    private fun advance() {
        val state = innerState.value
        val nextStepIndex = state.currentStepIndex + 1
        if (nextStepIndex in state.steps.indices) {
            innerState.update { it.copy(currentStepIndex = nextStepIndex) }
        } else {
            finish()
        }
    }

    private fun finish() {
        val lastState =
            innerState.getAndUpdate {
                if (it.credentials == null) return
                it.copy(saving = true)
            }
        if (lastState.saving) return // Already saving
        val credentials = checkNotNull(lastState.credentials) // Should never happen as we check it in the update lambda
        val facts = lastState.collectedFacts
        viewModelScope.launch {
            try {
                setFacts(credentials.sessionId, facts)
                    .onOk {
                        navigator.replaceAll(HomeScreen())
                    }.onErr {
                        log.e { "Failed to save facts: $it" }
                        showSnackbar("Couldn't save your preferences", isError = true)
                    }
            } finally {
                innerState.update { it.copy(saving = false) }
            }
        }
    }

    private companion object {
        const val FALLBACK_NAME = "Chef"
    }
}

data class State(
    val initialized: Boolean,
    val credentials: SessionCredentials?,
    val collectedFacts: UserFacts,
    val currentStepIndex: Int,
    val steps: List<OnboardingStep>,
    val nameInput: String,
    val dietaryInput: String,
    val saving: Boolean,
)

sealed interface UiState {
    data object Loading : UiState

    data class Loaded(
        val onClickBack: (() -> Unit)?,
        val currentStepIndex: Int,
        val steps: List<UiStep>,
    ) : UiState
}

sealed interface UiStep {
    data class NameStep(
        val placeholder: String,
        val input: String,
        val onChangeInput: (String) -> Unit,
        val onClickConfirm: () -> Unit,
    ) : UiStep

    data class TemperatureUnitStep(
        val onClick: (TemperatureUnit) -> Unit,
    ) : UiStep

    data class WeightUnitsStep(
        val onClick: (WeightUnits) -> Unit,
    ) : UiStep

    data class VolumeUnitsStep(
        val onClick: (VolumeUnits) -> Unit,
    ) : UiStep

    data class DietaryStep(
        val dietaryChips: List<DietaryChip>,
        val customInput: String,
        val onChangeCustomInput: (String) -> Unit,
        val onClickAddCustom: () -> Unit,
        val onClickConfirm: () -> Unit,
    ) : UiStep
}

sealed interface DietaryChip {
    val name: String
    val onClick: (String) -> Unit

    data class Suggested(
        override val name: String,
        override val onClick: (String) -> Unit,
        val selected: Boolean,
    ) : DietaryChip

    data class Custom(
        override val name: String,
        override val onClick: (String) -> Unit,
    ) : DietaryChip
}

private val SUGGESTED_DIETARY_RESTRICTIONS =
    listOf(
        "Vegetarian",
        "Vegan",
        "Pescatarian",
        "Gluten-free",
        "Dairy-free",
        "Egg-free",
        "Nut-free",
        "Peanut-free",
        "Soy-free",
        "Shellfish-free",
        "Halal",
        "Kosher",
        "Low-carb/keto",
    )
