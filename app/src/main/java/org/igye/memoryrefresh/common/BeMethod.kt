package org.igye.memoryrefresh.common

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class BeMethod(val restrictAccessViaHttps: Boolean = false)
