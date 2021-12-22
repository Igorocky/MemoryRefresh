package org.igye.memoryrefresh.database

open class VersionTable(name: String): Table(name = name) {
    val verId = "ver_id"
    val timestamp = "timestamp"
}