package org.igye.memoryrefresh.database

open class VersionTable(name: String): Table(tableName = name) {
    val verId = "VER_ID"
    val timestamp = "TIMESTAMP"
}