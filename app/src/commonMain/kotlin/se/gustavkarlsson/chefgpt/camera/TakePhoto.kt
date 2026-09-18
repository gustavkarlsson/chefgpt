package se.gustavkarlsson.chefgpt.camera

import androidx.compose.runtime.Composable

@Composable
expect fun TakePhoto(
    onSuccess: (photoPath: String) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
)
