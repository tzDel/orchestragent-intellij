package com.github.tzdel.orchestragentintellij.services

import com.github.tzdel.orchestragentintellij.models.*
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Paths
import java.time.Instant

@ExtendWith(MockKExtension::class)
class SessionManagerServiceTest {

    @RelaxedMockK
    private lateinit var mcpClientService: MCPClientService

    private lateinit var sessionManagerService: SessionManagerService

    @BeforeEach
    fun setUp() {
        sessionManagerService = SessionManagerService(mcpClientService)
    }

    @Test
    fun `SHOULD return empty list WHEN no sessions exist`() = runBlocking {
        // arrange
        coEvery { mcpClientService.callTool("get_sessions", emptyMap()) } returns MCPResponse(
            jsonrpc = "2.0",
            id = "test-id",
            result = buildJsonArray { }
        )

        // act
        val sessions = sessionManagerService.getAllSessions()

        // assert
        assertEquals(0, sessions.size)
    }

    @Test
    fun `SHOULD refresh sessions from MCP server WHEN refreshSessions is called`() = runBlocking {
        // arrange
        val mockResponse = MCPResponse(
            jsonrpc = "2.0",
            id = "test-id",
            result = buildJsonArray {
                add(buildJsonObject {
                    put("id", "test-session")
                    put("worktreePath", "/tmp/worktree")
                    put("branchName", "orchestragent-test")
                    put("status", "OPEN")
                    put("linesAdded", 10)
                    put("linesRemoved", 5)
                    put("filesChanged", 3)
                    put("createdAt", Instant.now().toString())
                    put("lastModified", Instant.now().toString())
                })
            }
        )
        coEvery { mcpClientService.callTool("get_sessions", emptyMap()) } returns mockResponse

        // act
        sessionManagerService.refreshSessions()
        val sessions = sessionManagerService.getAllSessions()

        // assert
        assertEquals(1, sessions.size)
        assertEquals("test-session", sessions[0].id)
    }

    @Test
    fun `SHOULD get session by id WHEN session exists`() = runBlocking {
        // arrange
        val sessionId = "test-session"
        val session = Session(
            id = sessionId,
            worktreePath = Paths.get("/tmp/worktree"),
            branchName = "orchestragent-test",
            status = SessionStatus.OPEN,
            statistics = GitStatistics(10, 5, 3),
            createdAt = Instant.now(),
            lastModified = Instant.now()
        )
        sessionManagerService.updateSession(session)

        // act
        val result = sessionManagerService.getSessionById(sessionId)

        // assert
        assertNotNull(result)
        assertEquals(sessionId, result.id)
    }

    @Test
    fun `SHOULD return null WHEN session does not exist`() {
        // arrange
        val sessionId = "nonexistent"

        // act
        val result = sessionManagerService.getSessionById(sessionId)

        // assert
        assertNull(result)
    }

    @Test
    fun `SHOULD update session WHEN updateSession is called`() = runBlocking {
        // arrange
        val sessionId = "test-session"
        val session = Session(
            id = sessionId,
            worktreePath = Paths.get("/tmp/worktree"),
            branchName = "orchestragent-test",
            status = SessionStatus.OPEN,
            statistics = GitStatistics(10, 5, 3),
            createdAt = Instant.now(),
            lastModified = Instant.now()
        )

        // act
        sessionManagerService.updateSession(session)
        val result = sessionManagerService.getSessionById(sessionId)

        // assert
        assertNotNull(result)
        assertEquals(session.id, result.id)
        assertEquals(session.branchName, result.branchName)
    }

    @Test
    fun `SHOULD remove session WHEN removeSession is called`() = runBlocking {
        // arrange
        val sessionId = "test-session"
        val session = Session(
            id = sessionId,
            worktreePath = Paths.get("/tmp/worktree"),
            branchName = "orchestragent-test",
            status = SessionStatus.OPEN,
            statistics = GitStatistics(10, 5, 3),
            createdAt = Instant.now(),
            lastModified = Instant.now()
        )
        sessionManagerService.updateSession(session)

        // act
        sessionManagerService.removeSession(sessionId)
        val result = sessionManagerService.getSessionById(sessionId)

        // assert
        assertNull(result)
    }

    @Test
    fun `SHOULD emit updated sessions WHEN sessions state changes`() = runBlocking {
        // arrange
        val sessionId = "test-session"
        val session = Session(
            id = sessionId,
            worktreePath = Paths.get("/tmp/worktree"),
            branchName = "orchestragent-test",
            status = SessionStatus.OPEN,
            statistics = GitStatistics(10, 5, 3),
            createdAt = Instant.now(),
            lastModified = Instant.now()
        )

        // act
        sessionManagerService.updateSession(session)
        val sessions = sessionManagerService.sessionsFlow.first()

        // assert
        assertEquals(1, sessions.size)
        assertEquals(sessionId, sessions[0].id)
    }

    @Test
    fun `SHOULD initialize from MCP server WHEN initializeFromServer is called`() = runBlocking {
        // arrange
        val mockResponse = MCPResponse(
            jsonrpc = "2.0",
            id = "test-id",
            result = buildJsonArray {
                add(buildJsonObject {
                    put("id", "session-1")
                    put("worktreePath", "/tmp/worktree1")
                    put("branchName", "orchestragent-session-1")
                    put("status", "OPEN")
                    put("linesAdded", 20)
                    put("linesRemoved", 10)
                    put("filesChanged", 5)
                    put("createdAt", Instant.now().toString())
                    put("lastModified", Instant.now().toString())
                })
            }
        )
        coEvery { mcpClientService.callTool("get_sessions", emptyMap()) } returns mockResponse

        // act
        sessionManagerService.initializeFromServer()
        val sessions = sessionManagerService.getAllSessions()

        // assert
        assertEquals(1, sessions.size)
        assertEquals("session-1", sessions[0].id)
        coVerify { mcpClientService.callTool("get_sessions", emptyMap()) }
    }
}
