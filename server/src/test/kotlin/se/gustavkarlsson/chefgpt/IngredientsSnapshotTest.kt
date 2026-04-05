package se.gustavkarlsson.chefgpt

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.module.requestScope
import se.gustavkarlsson.chefgpt.ingredients.InMemoryIngredientStore
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
    fun unauthenticated() =
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
                    module {
                        requestScope {
                            scoped {
                                InMemoryIngredientStore(mutableListOf("tomato", "basil"))
                            } bind IngredientStore::class
                        }
                    },
                ),
        ) { client ->
            val sessionId = registerUser()

            client.get("/ingredients") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete ingredient`() =
        snapshotTestApplication(
            snapshotContext,
            extraKoinModules =
                listOf(
                    module {
                        requestScope {
                            scoped {
                                InMemoryIngredientStore(mutableListOf("tomato", "basil"))
                            } bind IngredientStore::class
                        }
                    },
                ),
        ) { client ->
            val sessionId = registerUser()

            client.delete("/ingredients/tomato") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete ingredient not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("/ingredients/tomato") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete ingredient unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.delete("/ingredients/tomato")
        }

    @Test
    fun `put ingredient new`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.put("/ingredients/tomato") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `put ingredient existing`() =
        snapshotTestApplication(
            snapshotContext,
            extraKoinModules =
                listOf(
                    module {
                        requestScope {
                            scoped {
                                InMemoryIngredientStore(mutableListOf("tomato"))
                            } bind IngredientStore::class
                        }
                    },
                ),
        ) { client ->
            val sessionId = registerUser()

            client.put("/ingredients/tomato") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `put ingredient unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.put("/ingredients/tomato")
        }
}
