package se.gustavkarlsson.chefgpt

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO Replace with proper integration tests
@Testcontainers
class PostgresContainerTest {
    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.3")
    }

    @Test
    fun `query returns inserted row`() {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE test (id SERIAL PRIMARY KEY, name TEXT NOT NULL)")
                stmt.execute("INSERT INTO test (name) VALUES ('hello')")
            }
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT name FROM test WHERE id = 1")
                rs.next()
                assertEquals("hello", rs.getString("name"))
            }
        }
    }
}
