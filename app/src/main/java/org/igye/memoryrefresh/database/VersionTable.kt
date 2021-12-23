package org.igye.memoryrefresh.database

open class VersionTable(name: String): Table(name = name) {
    val verId = "VER_ID"
    val timestamp = "TIMESTAMP"
}