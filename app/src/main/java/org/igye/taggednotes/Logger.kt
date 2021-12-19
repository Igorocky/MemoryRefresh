package org.igye.taggednotes

import android.util.Log
import org.slf4j.Logger
import org.slf4j.Marker

class LoggerImpl(private val loggerName: String): Logger {
    private val tag = "tn/$loggerName"
    private val defaultFormat = "%s"

    override fun getName(): String {
        return loggerName
    }

    override fun isTraceEnabled(): Boolean {
        return true
    }

    override fun isTraceEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun trace(msg: String?) {
        Log.v(tag, msg?:"")
    }

    override fun trace(format: String?, arg: Any?) {
        Log.v(tag, String.format(format?: defaultFormat, arg))
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        Log.v(tag, String.format(format?:defaultFormat, arg1, arg2))
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        Log.v(tag, String.format(format?:defaultFormat, *arguments))
    }

    override fun trace(msg: String?, t: Throwable?) {
        Log.v(tag, msg, t)
    }

    override fun trace(marker: Marker?, msg: String?) {
        trace(msg)
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        trace(format, arg)
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        trace(format, arg1, arg2)
    }

    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
        trace(format, *argArray)
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        trace(msg, t)
    }

    override fun isDebugEnabled(): Boolean {
        return true
    }

    override fun isDebugEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun debug(msg: String?) {
        Log.d(tag, msg?:"")
    }

    override fun debug(format: String?, arg: Any?) {
        Log.d(tag, String.format(format?: defaultFormat, arg))
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        Log.d(tag, String.format(format?:defaultFormat, arg1, arg2))
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        Log.d(tag, String.format(format?:defaultFormat, *arguments))
    }

    override fun debug(msg: String?, t: Throwable?) {
        Log.d(tag, msg, t)
    }

    override fun debug(marker: Marker?, msg: String?) {
        debug(msg)
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        debug(format, arg)
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        debug(format, arg1, arg2)
    }

    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
        debug(format, *arguments)
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        debug(msg, t)
    }

    override fun isInfoEnabled(): Boolean {
        return true
    }

    override fun isInfoEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun info(msg: String?) {
        Log.i(tag, msg?:"")
    }

    override fun info(format: String?, arg: Any?) {
        Log.i(tag, String.format(format?: defaultFormat, arg))
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        Log.i(tag, String.format(format?:defaultFormat, arg1, arg2))
    }

    override fun info(format: String?, vararg arguments: Any?) {
        Log.i(tag, String.format(format?:defaultFormat, *arguments))
    }

    override fun info(msg: String?, t: Throwable?) {
        Log.i(tag, msg, t)
    }

    override fun info(marker: Marker?, msg: String?) {
        info(msg)
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        info(format, arg)
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        info(format, arg1, arg2)
    }

    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        info(format, *arguments)
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        info(msg, t)
    }

    override fun isWarnEnabled(): Boolean {
        return true
    }

    override fun isWarnEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun warn(msg: String?) {
        Log.w(tag, msg?:"")
    }

    override fun warn(format: String?, arg: Any?) {
        Log.w(tag, String.format(format?: defaultFormat, arg))
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        Log.w(tag, String.format(format?:defaultFormat, *arguments))
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        Log.w(tag, String.format(format?:defaultFormat, arg1, arg2))
    }

    override fun warn(msg: String?, t: Throwable?) {
        Log.w(tag, msg, t)
    }

    override fun warn(marker: Marker?, msg: String?) {
        warn(msg)
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        warn(format, arg)
    }

    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        warn(format, arg1, arg2)
    }

    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
        warn(format, *arguments)
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        warn(msg, t)
    }

    override fun isErrorEnabled(): Boolean {
        return true
    }

    override fun isErrorEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun error(msg: String?) {
        Log.e(tag, msg?:"")
    }

    override fun error(format: String?, arg: Any?) {
        Log.e(tag, String.format(format?: defaultFormat, arg))
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        Log.e(tag, String.format(format?:defaultFormat, arg1, arg2))
    }

    override fun error(format: String?, vararg arguments: Any?) {
        Log.e(tag, String.format(format?:defaultFormat, *arguments))
    }

    override fun error(msg: String?, t: Throwable?) {
        Log.e(tag, msg, t)
    }

    override fun error(marker: Marker?, msg: String?) {
        error(msg)
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        error(format, arg)
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        error(format, arg1, arg2)
    }

    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        error(format, *arguments)
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        error(msg, t)
    }
}