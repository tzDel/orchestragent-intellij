package com.github.tzdel.orchestragentintellij.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import java.time.Instant

class SessionTest {

    @Test
    fun `SHOULD create Session WHEN all values are valid`() {
        // arrange
        val id = "test-session"
        val worktreePath = Paths.get("/tmp/worktree")
        val branchName = "orchestragent-test"
        val status = SessionStatus.OPEN
        val statistics = GitStatistics(10, 5, 3)
        val createdAt = Instant.now()
        val lastModified = createdAt.plusSeconds(60)

        // act
        val session = Session(
            id = id,
            worktreePath = worktreePath,
            branchName = branchName,
            status = status,
            statistics = statistics,
            createdAt = createdAt,
            lastModified = lastModified
        )

        // assert
        assertEquals(id, session.id)
        assertEquals(worktreePath, session.worktreePath)
        assertEquals(branchName, session.branchName)
        assertEquals(status, session.status)
        assertEquals(statistics, session.statistics)
        assertEquals(createdAt, session.createdAt)
        assertEquals(lastModified, session.lastModified)
    }

    @Test
    fun `SHOULD throw exception WHEN id is blank`() {
        // arrange
        val id = ""
        val worktreePath = Paths.get("/tmp/worktree")
        val branchName = "orchestragent-test"
        val status = SessionStatus.OPEN
        val statistics = GitStatistics(10, 5, 3)
        val createdAt = Instant.now()
        val lastModified = createdAt.plusSeconds(60)

        // act & assert
        assertThrows<IllegalArgumentException> {
            Session(
                id = id,
                worktreePath = worktreePath,
                branchName = branchName,
                status = status,
                statistics = statistics,
                createdAt = createdAt,
                lastModified = lastModified
            )
        }
    }

    @Test
    fun `SHOULD throw exception WHEN branchName is blank`() {
        // arrange
        val id = "test-session"
        val worktreePath = Paths.get("/tmp/worktree")
        val branchName = ""
        val status = SessionStatus.OPEN
        val statistics = GitStatistics(10, 5, 3)
        val createdAt = Instant.now()
        val lastModified = createdAt.plusSeconds(60)

        // act & assert
        assertThrows<IllegalArgumentException> {
            Session(
                id = id,
                worktreePath = worktreePath,
                branchName = branchName,
                status = status,
                statistics = statistics,
                createdAt = createdAt,
                lastModified = lastModified
            )
        }
    }

    @Test
    fun `SHOULD throw exception WHEN createdAt is after lastModified`() {
        // arrange
        val id = "test-session"
        val worktreePath = Paths.get("/tmp/worktree")
        val branchName = "orchestragent-test"
        val status = SessionStatus.OPEN
        val statistics = GitStatistics(10, 5, 3)
        val createdAt = Instant.now()
        val lastModified = createdAt.minusSeconds(60)

        // act & assert
        assertThrows<IllegalArgumentException> {
            Session(
                id = id,
                worktreePath = worktreePath,
                branchName = branchName,
                status = status,
                statistics = statistics,
                createdAt = createdAt,
                lastModified = lastModified
            )
        }
    }
}
