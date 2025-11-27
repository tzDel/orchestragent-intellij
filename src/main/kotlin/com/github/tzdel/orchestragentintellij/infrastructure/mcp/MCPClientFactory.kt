package com.github.tzdel.orchestragentintellij.infrastructure.mcp

import com.intellij.openapi.diagnostic.Logger
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

private const val DEFAULT_CLIENT_NAME = "orchestragent-intellij"
private const val DEFAULT_CLIENT_VERSION = "0.1.0"

private fun createStdioTransport(process: Process): Transport =
    StdioClientTransport(
        process.inputStream.asSource().buffered(),
        process.outputStream.asSink().buffered(),
    )

class MCPClientFactory(
    private val processManager: ProcessManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logger: Logger = Logger.getInstance(MCPClientFactory::class.java),
    private val clientFactory: (Implementation, ClientOptions) -> Client = ::Client,
    private val transportFactory: (Process) -> Transport = ::createStdioTransport,
) {

    private fun buildClientInfo(
        name: String = DEFAULT_CLIENT_NAME,
        version: String = DEFAULT_CLIENT_VERSION,
    ): Implementation = Implementation(name = name, version = version)

    suspend fun startAndConnect(
        binaryPath: String,
        repositoryPath: String,
        clientInfo: Implementation = buildClientInfo(),
        clientOptions: ClientOptions = ClientOptions(),
    ): ConnectedClient = withContext(dispatcher) {
        val process = startProcess(binaryPath, repositoryPath)
        val transport = createTransport(process)
        val client = createClient(clientInfo, clientOptions)

        try {
            client.connect(transport)
            ConnectedClient(client, transport, process)
        } catch (exception: Exception) {
            logger.warn("Failed to connect to MCP server over stdio", exception)
            safelyCloseTransport(transport)
            processManager.stopProcess()
            throw MCPClientInitializationException("Failed to connect to MCP server over stdio", exception)
        }
    }

    private fun startProcess(binaryPath: String, repositoryPath: String): Process =
        try {
            processManager.startProcess(binaryPath, repositoryPath)
        } catch (exception: Exception) {
            logger.warn("Failed to start MCP server process", exception)
            throw MCPClientInitializationException("Failed to start MCP server process", exception)
        }

    private fun createClient(
        clientInfo: Implementation = buildClientInfo(),
        clientOptions: ClientOptions = ClientOptions(),
    ): Client = clientFactory(clientInfo, clientOptions)

    private fun createTransport(process: Process): Transport = transportFactory(process)

    private suspend fun safelyCloseTransport(transport: Transport) {
        try {
            transport.close()
        } catch (closeError: Exception) {
            logger.debug("Transport close failed after connection error", closeError)
        }
    }
}

data class ConnectedClient(
    val client: Client,
    val transport: Transport,
    val process: Process,
)

class MCPClientInitializationException(message: String, cause: Throwable) : Exception(message, cause)
