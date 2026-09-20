package se.gustavkarlsson.chefgpt.recipes

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.postgres.migratePostgresDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.toJavaUuid

@Testcontainers
class PostgresRecipeRepositoryTest {
    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.3")

        private val dataSource: HikariDataSource by lazy {
            val port = postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
            migratePostgresDatabase(
                MapApplicationConfig(
                    "host" to postgres.host,
                    "port" to port.toString(),
                    "database" to postgres.databaseName,
                    "username" to postgres.username,
                    "password" to postgres.password,
                ),
            )
            HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = postgres.jdbcUrl
                    username = postgres.username
                    password = postgres.password
                },
            )
        }

        private val repository: RecipeRepository by lazy {
            RecipeRepository(PostgresRecipePersistence(DatabaseAccess(ChefGptDatabase(dataSource.asJdbcDriver()))))
        }
    }

    private val userId = UserId.random()
    private val otherUserId = UserId.random()

    @BeforeEach
    fun insertUser() {
        dataSource.connection.use { connection ->
            connection
                .prepareStatement("INSERT INTO \"user\" (id, username, password_hash) VALUES (?, ?, ?)")
                .use { statement ->
                    statement.setObject(1, userId.value.toJavaUuid())
                    statement.setString(2, "user-${userId.value}")
                    statement.setString(3, "irrelevant")
                    statement.executeUpdate()
                }
        }
    }

    @Test
    fun `save then read a recipe back whole`() =
        runTest {
            val saved = repository.saveRecipe(userId, carbonara())

            val read = repository.getRecipe(userId, saved.id)

            assertEquals(saved, read)
            assertEquals(saved.steps, read?.steps)
            assertEquals(saved.ingredients, read?.ingredients)
            assertEquals(saved.nutrients, read?.nutrients)
        }

    @Test
    fun `modify creates a modification that stands in for the original`() =
        runTest {
            val saved = repository.saveRecipe(userId, carbonara())

            val modified = repository.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            assertEquals(listOf(modified?.id), repository.getRecipeSummaries(userId).map { it.id })
        }

    @Test
    fun `modify updates an existing modification in place`() =
        runTest {
            val saved = repository.saveRecipe(userId, carbonara())
            val modified = repository.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            val remodified = repository.modifyRecipe(userId, modified!!.id, RecipeUpdate(title = "Vegan carbonara"))

            assertEquals(modified.id, remodified?.id)
            assertEquals("Vegan carbonara", remodified?.title)
        }

    @Test
    fun `overwriteOriginal deletes the original and keeps the modification`() =
        runTest {
            val saved = repository.saveRecipe(userId, carbonara())
            val modified = repository.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            val overwritten = repository.overwriteOriginal(userId, modified!!.id)

            assertNull(repository.getRecipe(userId, saved.id))
            assertEquals(modified.copy(modifiedFrom = null), overwritten)
        }

    @Test
    fun `saveAsCopy keeps both recipes`() =
        runTest {
            val saved = repository.saveRecipe(userId, carbonara())
            val modified = repository.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            val copy = repository.saveAsCopy(userId, modified!!.id)

            assertNull(copy?.modifiedFrom)
            assertEquals(setOf(saved.id, modified.id), repository.getRecipeSummaries(userId).map { it.id }.toSet())
        }

    @Test
    fun `setFavorite updates the recipe`() =
        runTest {
            val saved = repository.saveRecipe(userId, carbonara())

            val updated = repository.setFavorite(userId, saved.id, favorite = true)

            assertTrue(updated?.favorite == true)
        }

    @Test
    fun `deleting a recipe detaches its modification`() =
        runTest {
            val saved = repository.saveRecipe(userId, carbonara())
            val modified = repository.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            repository.deleteRecipe(userId, saved.id)

            assertEquals(modified?.copy(modifiedFrom = null), repository.getRecipe(userId, modified!!.id))
        }

    @Test
    fun `recipes are independent per user`() =
        runTest {
            repository.saveRecipe(userId, carbonara())

            assertTrue(repository.getRecipeSummaries(otherUserId).isEmpty())
        }
}

private fun carbonara() =
    NewRecipe(
        title = "Carbonara",
        steps = listOf("Boil the pasta", "Fry the pancetta"),
        spoonacularId = null,
        imageUrl = null,
        description = "A classic Italian pasta dish.",
        preparationDuration = 10.minutes,
        cookingDuration = 20.minutes,
        duration = 30.minutes,
        servings = 4..4,
        ingredients = listOf(ApiRecipeIngredient("spaghetti", "400", "g")),
        nutrients = listOf(ApiNutrient("Calories", "450", "kcal")),
    )
