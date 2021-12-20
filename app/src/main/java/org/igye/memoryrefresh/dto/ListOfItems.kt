package org.igye.memoryrefresh.dto

data class ListOfItems<T>(val complete: Boolean, val items: List<T>)