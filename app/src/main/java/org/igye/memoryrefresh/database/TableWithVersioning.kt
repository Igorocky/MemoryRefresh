package org.igye.memoryrefresh.database

abstract class TableWithVersioning(name: String): Table(name = name) {
    val ver = VersionTable(name = name + "_VER")
}