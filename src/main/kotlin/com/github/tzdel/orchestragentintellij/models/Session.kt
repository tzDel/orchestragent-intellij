package com.github.tzdel.orchestragentintellij.models

import java.nio.file.Path
import java.time.Instant

data class Session(
    val id: String,
    val worktreePath: Path,
    val branchName: String,
    val status: SessionStatus,
    val statistics: GitStatistics,
    val createdAt: Instant,
    val lastModified: Instant
) {
    init {
        require(id.isNotBlank()) { "Session id must not be blank" }
        require(branchName.isNotBlank()) { "Branch name must not be blank" }
        require(!createdAt.isAfter(lastModified)) { "createdAt must not be after lastModified" }
    }
}
