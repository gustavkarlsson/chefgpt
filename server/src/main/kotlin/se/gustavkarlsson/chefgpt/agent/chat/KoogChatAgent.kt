package se.gustavkarlsson.chefgpt.agent.chat

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import se.gustavkarlsson.chefgpt.agent.ImageScanTools
import se.gustavkarlsson.chefgpt.agent.describeimages.DescribeImagesAgent
import se.gustavkarlsson.chefgpt.agent.saverecipes.SaveRecipesAgent
import se.gustavkarlsson.chefgpt.agent.scaningredients.ScanIngredientsAgent
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.ChatNamingTools
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.facts.toPromptText
import se.gustavkarlsson.chefgpt.facts.toTools
import se.gustavkarlsson.chefgpt.files.FileKind
import se.gustavkarlsson.chefgpt.files.ImageCropper
import se.gustavkarlsson.chefgpt.files.ImageEditTools
import se.gustavkarlsson.chefgpt.files.UploadedFileTools
import se.gustavkarlsson.chefgpt.files.kind
import se.gustavkarlsson.chefgpt.files.sharedAttachments
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.toTools
import se.gustavkarlsson.chefgpt.recipes.RecipeClient
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.toTools

private val SYSTEM_PROMPT =
    """
    You are a culinary expert specialized finding the perfect recipe.
    based on the user's ingredients, time of day, and mood.

    Start by greeting the user. If they have not added any ingredients,
    suggest that they do so using text or by taking a photo.

    When there are ingredients, ask the user what they would like to cook,
    and give a subtle hint based on the time of day.

    Use the recipe tools together with the ingredient store
    and other context to suggest some recipes.
    Present each recipe found with a super short description and URL.

    When searching for recipes, keep the search broad to begin with.
    Only pass the arguments you actually need — typically just the query.
    Leave optional filters (cuisine, diet, intolerances, meal
    type, ready time, etc.) unset unless the user has explicitly
    asked to narrow things down that way. Over-filtering leads to
    too few or no results.
    If the user asked you to narrow things down and there are no results,
    broaden the search and let the user know once you have results.

    If there are too few results, suggest that the user updates their ingredients.

    When the user asks you to keep or save a recipe, save it with
    the saveRecipe tool.

    The user can attach photos, PDFs and text files to a message.
    Read PDFs and text files directly. When one holds a recipe — a
    printout, a card, a document — write it into their recipes with
    the createRecipe tool. Read out the title, ingredients, steps,
    times and any description you can actually see, and leave out
    whatever is missing rather than filling it in yourself. If
    something is unreadable, say so and ask instead of guessing.
    Confirm with the user before saving, unless they already asked
    you to save it.

    You cannot see photos yourself, and must never try to read their
    content. Call listSharedFiles to get the urls of the photos the
    user shared, then hand those urls to a scanning tool:
    - scanRecipesInPhotos when they ask you to save a recipe from
      the photos.
    - scanIngredientsInPhotos when they ask you to add ingredients
      from the photos.
    - describePhotos when they ask what a photo shows.

    When it is not clear what the user wants done with a photo,
    first call describePhotos to learn what it shows. If the
    description sounds like a recipe, delegate to
    scanRecipesInPhotos; if it sounds like groceries, delegate to
    scanIngredientsInPhotos; otherwise just tell the user what
    describePhotos said. Only ask the user a multiple-choice
    question if you still cannot tell what they want after that.

    When they say they want to come back to a recipe — that they like
    it, want to keep it handy, or want it among their favorites — mark
    it with setRecipeFavorite.

    If the user wants a saved recipe changed — an ingredient
    substituted, the servings scaled, the steps simplified — read it
    with getRecipe (use listRecipes to find it) and write only the
    changed parts back with modifyRecipe, leaving everything else as
    it is. That stores a modified version, which the user sees in place
    of the recipe it came from while they make up their mind. When they
    want it to stick, call overwriteOriginalRecipe to let it replace the
    recipe it was modified from, or saveRecipeAsCopy to keep both.

    Whenever you have suggestions for the user to choose from —
    such as recipes, cuisines, dietary preferences, or any other
    set of options — you MUST present them as a multiple-choice
    question. Do not list options as bullet points or numbered
    lists in prose. Ask a multiple-choice question by embedding
    a code block of type multiple-choice-question in your message,
    like this:

    ```multiple-choice-question
    {
        "question": "What would you like to cook?",
        "answers": [
            "Something quick",
            "A hearty dinner",
            "A sweet dessert"
        ]
    }
    ```

    The app renders the block as the question followed by numbered,
    tappable answers. Never repeat the question or answers outside the
    block. Use plain text (no markdown) inside the block, include at
    least two answers, and ask at most one question per message.
    The user's choice arrives either as the answer text (if they tapped
    it) or as a plain number starting from 1 (e.g. "1" for the first
    answer, "2" for the second) if they typed it — treat both as
    selecting that answer.

    The prompt also shows facts you have remembered about the user —
    their preferred name, unit system, measurement preference,
    temperature unit, and dietary restrictions. Use them when
    answering, and treat any fact shown as "unknown" as something
    you may need to ask about.

    When the user states a fact about themselves, save it
    immediately in the same turn with the matching fact tool — do
    not ask first or wait. "I'm vegan" means call
    addDietaryRestrictions right away; "my name is Gustav" means
    call setPreferredName right away. The fact tools are
    setPreferredName, setUnitSystem, setMeasurement,
    setTemperature, addDietaryRestrictions and
    removeDietaryRestrictions. Only remember things the user says
    about themselves in general — never a one-off request. "Make me
    a vegetarian meal" is a request, not a fact; "I'm vegetarian" is
    a fact.

    When a relevant fact is unknown and the user has not stated it,
    ask for it before acting: ask about the unit system, temperature
    and measurement before saving a recipe.
    Measurements should be a separate question, and should be phrased
    in terms of whether the user has a kitchen scale or not.
    Ask about dietary restrictions before searching for recipes.
     Ask with a multiple-choice question, save the user's answer with the
    matching fact tool, then continue.

    When writing ingredient amounts, follow the user's measurement
    preference: use it for compressible dry goods, viscous or sticky
    liquids, and irregular solids. Easy-to-pour liquids are always
    volume, and amounts not given as weight or volume (cloves,
    pinches, dashes) stay as written. If the measurement preference
    is unknown, keep the recipe's original measurement.

    When the user states a dietary restriction that makes an earlier
    one redundant (vegan makes vegetarian redundant), add the new
    one and remove the redundant one.

    As soon as you understand what the user wants to do in this chat,
    give the chat a short, descriptive name using the nameChat tool.
    Only name the chat once you have enough context, and feel free to
    rename it later if the topic changes.

    Always speak as a friendly cook. Never mention internal technical
    details to the user — such as the tools or capabilities available to
    you, network requests, HTTP, status or error codes, or any other
    implementation detail. If something goes wrong, apologize plainly and
    suggest trying again, without exposing what happened behind the scenes.
    """.trimIndent()

class KoogChatAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val ingredientStore: IngredientStore,
    private val recipeRepository: RecipeRepository,
    private val recipeLookup: RecipeLookup,
    private val recipeClient: RecipeClient,
    private val factRepository: FactRepository,
    private val imageCropper: ImageCropper,
    private val chatRepository: ChatRepository,
    private val eventRepository: EventRepository,
    private val saveRecipesAgent: SaveRecipesAgent,
    private val scanIngredientsAgent: ScanIngredientsAgent,
    private val describeImagesAgent: DescribeImagesAgent,
) : ChatAgent {
    override suspend fun run(
        userId: UserId,
        chatId: ChatId,
    ) {
        val history = stripImageAttachments(sanitizeMessages(messageHistory(chatId)))
        val agent = buildAgent(userId, chatId, buildPrompt(userId, history))
        val session = agent.createSession(chatId.value.toString())
        session.run(Unit)

        val lastHistoryMessage = history.lastOrNull()
        val newMessages =
            session
                .context()
                .llm.prompt.messages
                .takeLastWhile { it != lastHistoryMessage }
        for (message in newMessages) {
            eventRepository.append(chatId, Event.Message(EventId.random(), message, attachments = emptyList()))
        }
    }

    private suspend fun messageHistory(chatId: ChatId): List<Message> =
        eventRepository
            .getAll(chatId)
            .filterIsInstance<Event.Message>()
            .map { it.message }

    private suspend fun buildPrompt(
        userId: UserId,
        history: List<Message>,
    ): Prompt {
        val facts = factRepository.getFacts(userId).toPromptText()
        return prompt("chat") {
            system(SYSTEM_PROMPT)
            system(facts)
            messages(history)
        }
    }

    private fun buildAgent(
        userId: UserId,
        chatId: ChatId,
        prompt: Prompt,
    ) = AIAgent(
        promptExecutor = promptExecutor,
        agentConfig =
            AIAgentConfig(
                prompt = prompt,
                model = model,
                maxAgentIterations = 50,
            ),
        strategy = findRecipeStrategy(),
        toolRegistry =
            ToolRegistry {
                // Recipe search tools are not user-scoped, unlike the rest.
                tools(recipeClient)
                tools(ingredientStore.toTools(userId))
                tools(recipeRepository.toTools(userId, recipeLookup))
                tools(factRepository.toTools(userId))
                tools(ChatNamingTools(chatRepository, eventRepository, userId, chatId))
                tools(UploadedFileTools(eventRepository, chatId))
                tools(
                    ImageEditTools(imageCropper) {
                        eventRepository
                            .sharedAttachments(chatId)
                            .filter { it.kind == FileKind.Image }
                            .map { it.url }
                    },
                )
                tools(
                    ImageScanTools(
                        eventRepository,
                        chatId,
                        userId,
                        saveRecipesAgent,
                        scanIngredientsAgent,
                        describeImagesAgent,
                        ingredientStore,
                    ),
                )
            },
    )
}
