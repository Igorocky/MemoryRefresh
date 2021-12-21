package org.igye.memoryrefresh.common

sealed class Try<out T> {
    abstract fun isSuccess(): Boolean
    abstract fun isFailure(): Boolean
    abstract fun get(): T
    abstract fun getIfSuccessOrElse(other: (Throwable) -> @UnsafeVariance T): T
    abstract fun ifErrorThen(other: (Throwable) -> Try<@UnsafeVariance T>): Try<T>

    fun <B> map(mapper: (T) -> B): Try<B> {
        return when (this) {
            is Success -> Try {
                mapper(value)
            }
            is Failure -> this as Failure<B>
        }
    }

    fun <B> flatMap(mapper: (T) -> Try<B>): Try<B> {
        return when (this) {
            is Success -> mapper(value)
            is Failure -> this as Failure<B>
        }
    }

    fun <B> apply(operations: (Try<T>) -> B): B = operations(this)

    companion object {
        operator fun <T> invoke(body: () -> T): Try<T> {
            return try {
                Success(body())
            } catch (e: Exception) {
                Failure(e)
            }
        }
    }
}

data class Success<out T>(val value: T) : Try<T>() {
    override fun isSuccess(): Boolean = true
    override fun isFailure(): Boolean = false
    override fun get() = value
    override fun getIfSuccessOrElse(other: (Throwable) -> @UnsafeVariance T): T = value
    override fun ifErrorThen(other: (Throwable) -> Try<@UnsafeVariance T>): Try<T> = this
}

data class Failure<out T>(val throwable: Throwable) : Try<T>() {
    override fun isSuccess(): Boolean = false
    override fun isFailure(): Boolean = true
    override fun get(): T = throw throwable
    override fun getIfSuccessOrElse(other: (Throwable) -> @UnsafeVariance T): T = other(throwable)
    override fun ifErrorThen(other: (Throwable) -> Try<@UnsafeVariance T>): Try<T> = other(throwable)
}