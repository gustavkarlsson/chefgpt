package se.gustavkarlsson.chefgpt.ui

import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "1. Dark",
    uiMode = UI_MODE_NIGHT_YES,
)
@Preview(
    name = "2. Light",
    uiMode = UI_MODE_NIGHT_NO,
)
@Preview(
    name = "3. Landscape",
    uiMode = UI_MODE_NIGHT_YES,
    device = "spec:parent=pixel_5,orientation=landscape",
)
@Preview(
    name = "4. Tablet",
    uiMode = UI_MODE_NIGHT_YES,
    device = "spec:width=1280dp,height=800dp,dpi=240",
)
annotation class MultiPreview
