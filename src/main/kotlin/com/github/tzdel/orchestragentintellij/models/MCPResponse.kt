package com.github.tzdel.orchestragentintellij.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MCPRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: JsonElement? = null
)

@Serializable
data class MCPResponse(
    val jsonrpc: String,
    val id: String,
    val result: JsonElement? = null,
    val error: MCPError? = null
)

@Serializable
data class MCPError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class MCPToolCallParams(
    val name: String,
    val arguments: Map<String, String>
)
