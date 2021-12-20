package org.igye.memoryrefresh.database

open class VersionTable(name: String): Table(name = name) {
    val verId = "version_id"
    val timestamp = "timestamp"
    val changeType = "change_type"
}