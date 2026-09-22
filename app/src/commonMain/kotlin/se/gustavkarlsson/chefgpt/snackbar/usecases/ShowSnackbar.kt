package se.gustavkarlsson.chefgpt.snackbar.usecases

import se.gustavkarlsson.chefgpt.snackbar.SnackbarManager

fun interface ShowSnackbar {
    operator fun invoke(
        text: String,
        isError: Boolean,
    )
}

class RealShowSnackbar(
    private val snackbarManager: SnackbarManager,
) : ShowSnackbar {
    override operator fun invoke(
        text: String,
        isError: Boolean,
    ) = snackbarManager.show(text, isError = isError)
}
