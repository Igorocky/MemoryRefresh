package org.igye.memoryrefresh.dto.common

data class ListOfItems<T>(val complete: Boolean, val items: List<T>)