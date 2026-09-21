package se.gustavkarlsson.chefgpt.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.files.CropRegion
import se.gustavkarlsson.chefgpt.files.ImageCropper

/**
 * Lets the agent crop a picture down to the part worth keeping.
 */
class CropImageTool(
    private val cropper: ImageCropper,
    private val availableImageUrls: suspend () -> List<String>, // TODO Figure out something cleaner to limit access
) : ToolSet {
    @Tool
    @LLMDescription(
        "Crop a picture down to the part worth keeping, such as just the finished dish on a page " +
            "that also holds text. Returns the url of the cut-down picture, which you can use like " +
            "any other picture url. The region is given as fractions of the picture, so x 0.1 and " +
            "width 0.5 keeps the half starting a tenth in from the left.",
    )
    suspend fun cropImage(
        @LLMDescription("The url of the picture to cut down.")
        url: String,
        @LLMDescription("Left edge of the part to keep, as a fraction of the width, from 0 to 1.")
        x: Double,
        @LLMDescription("Top edge of the part to keep, as a fraction of the height, from 0 to 1.")
        y: Double,
        @LLMDescription("Width of the part to keep, as a fraction of the picture's width.")
        width: Double,
        @LLMDescription("Height of the part to keep, as a fraction of the picture's height.")
        height: Double,
    ): String {
        require(availableImageUrls().any { it == url }) {
            "No picture to crop has the url $url"
        }
        val region =
            runCatching { CropRegion(x, y, width, height) }
                .getOrElse { error("That is not a region inside the picture: ${it.message}") }
        return cropper.crop(ImageUrl(url), region).value
    }
}

// TODO Add a thumbnail creation tool
