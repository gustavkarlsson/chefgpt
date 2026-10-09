package se.gustavkarlsson.chefgpt.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.navigation.Screen.Id
import se.gustavkarlsson.chefgpt.plus

@Serializable
@SerialName("login")
data class LoginScreen(
    override val id: Id = Id.new(),
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<LoginViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState)
    }
}

@Composable
private fun Content(
    uiState: UiState,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = modifier.fillMaxSize()) {
            val usernameFocusRequester = remember { FocusRequester() }
            val passwordFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { usernameFocusRequester.requestFocus() }
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.ime),
                contentPadding =
                    WindowInsets.safeDrawing
                        .exclude(
                            WindowInsets.ime,
                        ).asPaddingValues() + PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                item {
                    Text(
                        text = "Welcome to ChefGPT",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                item {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                        text = "Sign in to get started",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = uiState.onUsernameChange,
                        label = { Text("Username") },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Next,
                            ),
                        keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                        modifier = Modifier.focusRequester(usernameFocusRequester),
                    )
                }
                item {
                    var showPassword by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = uiState.onPasswordChange,
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation =
                            if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector =
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        modifier =
                            Modifier
                                .padding(top = 8.dp, bottom = 16.dp)
                                .focusRequester(passwordFocusRequester),
                    )
                }
                item {
                    Button(onClick = { uiState.onClickRegister?.invoke() }, enabled = uiState.onClickRegister != null) {
                        Text("Register")
                    }
                }
                item {
                    Button(onClick = { uiState.onClickLogin?.invoke() }, enabled = uiState.onClickLogin != null) {
                        Text("Sign in")
                    }
                }
            }
        }
    }
}
