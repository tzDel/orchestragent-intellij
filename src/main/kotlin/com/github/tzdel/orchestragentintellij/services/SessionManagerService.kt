package com.github.tzdel.orchestragentintellij.services

import com.github.tzdel.orchestragentintellij.models.GitStatistics
import com.github.tzdel.orchestragentintellij.models.Session
import com.github.tzdel.orchestragentintellij.models.SessionStatus
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class SessionManagerService {

    private val logger = thisLogger()
    private val sessionCache = ConcurrentHashMap<String, Session>()
    private val sessionsStateFlow = MutableStateFlow<List<Session>>(emptyList())

    val sessionsFlow: StateFlow<List<Session>> = sessionsStateFlow.asStateFlow()

    private var mcpClientService: MCPClientService

    constructor() {
        // Default constructor for IntelliJ service instantiation
        this.mcpClientService = com.intellij.openapi.components.service()
    }

    constructor(mcpClientService: MCPClientService) {
        // Test constructor with mocked dependencies
        this.mcpClientService = mcpClientService
    }

    suspend fun initializeFromServer() {
        logger.info("Initializing session list from MCP server")
        refreshSessions()
    }

    suspend fun refreshSessions() {
        try {
            logger.info("Refreshing sessions from MCP server")
            val response = mcpClientService.callTool("get_sessions", emptyMap())

            if (response?.error != null) {
                logger.error("Failed to refresh sessions: ${response.error.message}")
                return
            }

            val sessions = response?.result?.jsonArray?.mapNotNull { sessionJson ->
                try {
                    val sessionObj = sessionJson.jsonObject
                    val id = sessionObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val worktreePath = sessionObj["worktreePath"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val branchName = sessionObj["branchName"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val statusString = sessionObj["status"]?.jsonPrimitive?.content ?: "OPEN"
                    val linesAdded = sessionObj["linesAdded"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val linesRemoved = sessionObj["linesRemoved"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val filesChanged = sessionObj["filesChanged"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val createdAtString = sessionObj["createdAt"]?.jsonPrimitive?.content
                    val lastModifiedString = sessionObj["lastModified"]?.jsonPrimitive?.content

                    val status = try {
                        SessionStatus.valueOf(statusString)
                    } catch (e: IllegalArgumentException) {
                        SessionStatus.OPEN
                    }

                    val createdAt = createdAtString?.let { Instant.parse(it) } ?: Instant.now()
                    val lastModified = lastModifiedString?.let { Instant.parse(it) } ?: Instant.now()

                    Session(
                        id = id,
                        worktreePath = Paths.get(worktreePath),
                        branchName = branchName,
                        status = status,
                        statistics = GitStatistics(linesAdded, linesRemoved, filesChanged),
                        createdAt = createdAt,
                        lastModified = lastModified
                    )
                } catch (e: Exception) {
                    logger.error("Failed to parse session from JSON", e)
                    null
                }
            } ?: emptyList()

            sessionCache.clear()
            sessions.forEach { session ->
                sessionCache[session.id] = session
            }

            updateStateFlow()
            logger.info("Refreshed ${sessions.size} sessions from MCP server")
        } catch (e: Exception) {
            logger.error("Error refreshing sessions", e)
        }
    }

    fun getAllSessions(): List<Session> {
        return sessionCache.values.toList()
    }

    fun getSessionById(sessionId: String): Session? {
        return sessionCache[sessionId]
    }

    fun updateSession(session: Session) {
        sessionCache[session.id] = session
        updateStateFlow()
        logger.info("Updated session: ${session.id}")
    }

    fun removeSession(sessionId: String) {
        sessionCache.remove(sessionId)
        updateStateFlow()
        logger.info("Removed session: $sessionId")
    }

    private fun updateStateFlow() {
        sessionsStateFlow.value = sessionCache.values.toList()
    }
}
