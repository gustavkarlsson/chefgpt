package se.gustavkarlsson.chefgpt.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import se.gustavkarlsson.chefgpt.facts.TemperatureUnit
import se.gustavkarlsson.chefgpt.facts.VolumeUnits
import se.gustavkarlsson.chefgpt.facts.WeightUnits
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.navigation.Screen.Id
import se.gustavkarlsson.chefgpt.theme.ChefGptTheme

// FIXME make sure navigator can handle system back-button clicks while on this screen
@Serializable
@SerialName("onboarding")
data class OnboardingScreen(
    override val id: Id = Id.new(),
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<OnboardingViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState)
    }
}

/**
 * Renders the main content surface based on the provided UI state.
 *
 * When [UiState.Loading] is active, a centered circular progress indicator is displayed.
 * When [UiState.Loaded] is active, the current step's content is shown, along with an optional
 * back button if a back navigation action is available.
 *
 * @param uiState the current state of the UI, either loading or loaded with step information
 * @param modifier the modifier to apply to the root surface
 */
@Composable
private fun Content(
    uiState: UiState,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Loaded -> {
                // FIXME Back handler
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val uiStep = uiState.steps[uiState.currentStepIndex]) {
                        is UiStep.NameStep -> NameStep(uiStep)
                        is UiStep.TemperatureUnitStep -> TemperatureUnitStep(uiStep)
                        is UiStep.WeightUnitsStep -> WeightUnitsStep(uiStep)
                        is UiStep.VolumeUnitsStep -> VolumeUnitsStep(uiStep)
                        is UiStep.DietaryStep -> DietaryStep(uiStep)
                    }
                    uiState.onClickBack?.let { onClick ->
                        IconButton(
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).safeDrawingPadding(),
                            onClick = onClick,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.NameStep(
    uiStep: UiStep.NameStep,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.align(Alignment.Center).padding(32.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepHeader(title = "What should we call you?", description = "We'll use it when we talk to you.")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            placeholder = { Text(uiStep.placeholder) },
            value = uiStep.input,
            onValueChange = uiStep.onChangeInput,
            singleLine = true,
            modifier = Modifier.widthIn(min = 300.dp),
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = uiStep.onClickConfirm) {
            val name = uiStep.input.trim().takeIf { it.isNotBlank() } ?: uiStep.placeholder
            Text("Call me $name")
        }
    }
}

@Composable
private fun BoxScope.TemperatureUnitStep(
    uiStep: UiStep.TemperatureUnitStep,
    modifier: Modifier = Modifier,
) {
    OptionsStep(modifier = modifier, title = "Temperature", description = "How should temperatures be shown?") {
        ListOption(
            onClick = { uiStep.onClick(TemperatureUnit.Celsius) },
            title = "Celsius (°C)",
            description = null,
        )
        Spacer(Modifier.height(8.dp))
        ListOption(
            onClick = { uiStep.onClick(TemperatureUnit.Fahrenheit) },
            title = "Fahrenheit (°F)",
            description = null,
        )
    }
}

@Composable
private fun BoxScope.WeightUnitsStep(
    uiStep: UiStep.WeightUnitsStep,
    modifier: Modifier = Modifier,
) {
    OptionsStep(modifier = modifier, title = "Weight", description = "How do you measure weight?") {
        ListOption(
            onClick = { uiStep.onClick(WeightUnits.Metric) },
            title = "Metric",
            description = "g, kg",
        )
        Spacer(Modifier.height(8.dp))
        ListOption(
            onClick = { uiStep.onClick(WeightUnits.UsImperial) },
            title = "US / Imperial",
            description = "oz, lb",
        )
    }
}

@Composable
private fun BoxScope.VolumeUnitsStep(
    uiStep: UiStep.VolumeUnitsStep,
    modifier: Modifier = Modifier,
) {
    OptionsStep(
        modifier = modifier,
        title = "Volume",
        description = "How do you measure volume for fluids and flour?",
    ) {
        ListOption(
            onClick = { uiStep.onClick(VolumeUnits.Metric) },
            title = "Metric",
            description = "ml, l",
        )
        Spacer(Modifier.height(8.dp))
        ListOption(
            onClick = { uiStep.onClick(VolumeUnits.UsCustomary) },
            title = "US / Customary",
            description = "fl oz, cups",
        )
    }
}

@Composable
private fun BoxScope.OptionsStep(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    options: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.align(Alignment.Center).padding(32.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepHeader(title = title, description = description)
        Spacer(Modifier.height(16.dp))
        options()
    }
}

@Composable
private fun ListOption(
    onClick: () -> Unit,
    title: String,
    description: String?,
) {
    ListItem(
        modifier =
            Modifier
                .heightIn(min = 72.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
        tonalElevation = 4.dp,
        headlineContent = {
            Text(title)
        },
        trailingContent = {
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        },
        supportingContent =
            description?.let {
                {
                    Text(description)
                }
            },
    )
}

@Composable
private fun BoxScope.DietaryStep(
    uiStep: UiStep.DietaryStep,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.align(Alignment.Center).padding(24.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepHeader(
            title = "Dietary restrictions",
            description = "Select any that apply, or add your own.",
        )
        Spacer(Modifier.height(16.dp))
        FlowRow {
            for (chip in uiStep.dietaryChips) {
                DietaryChip(modifier = Modifier.padding(4.dp), chip = chip)
            }
        }
        // FIXME animated content?
        // FIXME Custom input
    }
}

@Composable
private fun DietaryChip(
    chip: DietaryChip,
    modifier: Modifier = Modifier,
) {
    when (chip) {
        is DietaryChip.Suggested -> {
            FilterChip(
                modifier = modifier,
                selected = chip.selected,
                onClick = { chip.onClick(chip.name) },
                label = { Text(chip.name) },
                leadingIcon =
                    if (chip.selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
            )
        }

        is DietaryChip.Custom -> {
            InputChip(
                modifier = modifier,
                selected = true,
                onClick = { chip.onClick(chip.name) },
                label = { Text(chip.name) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(InputChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

@Composable
private fun StepHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun PreviewStep1() {
    ChefGptTheme {
    }
}
