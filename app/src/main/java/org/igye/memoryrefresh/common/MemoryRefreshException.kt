package org.igye.memoryrefresh.common

import org.igye.memoryrefresh.ErrorCode

class MemoryRefreshException(msg: String, val errCode: ErrorCode): Exception(msg)