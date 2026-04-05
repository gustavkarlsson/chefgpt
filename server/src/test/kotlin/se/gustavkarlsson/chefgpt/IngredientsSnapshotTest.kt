package se.gustavkarlsson.chefgpt

import io.ktor.client.request.get
import io.ktor.client.request.header
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.dsl.module
import org.koin.module.requestScope
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class IngredientsSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.get("/ingredients")
        }

    @Test
    fun `list ingredients empty`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.get("/ingredients") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `list ingredients with some`() =
        snapshotTestApplication(
            snapshotContext,
            extraKoinModules =
                listOf(
                    module(override = true) {
                        requestScope {
                            scoped<IngredientStore> {
                                object : IngredientStore {
                                    override suspend fun getIngredients() = listOf("tomato", "basil")

                                    override suspend fun addIngredients(ingredients: List<String>) = emptyList<String>()

                                    override suspend fun removeIngredients(ingredients: List<String>) = emptyList<String>()

                                    override suspend fun clearIngredients() = emptyList<String>()
                                }
                            }
                        }
                    },
                ),
        ) { client ->
            val sessionId = registerUser()

            client.get("/ingredients") {
                header("Session-Id", sessionId)
            }
        }
}
