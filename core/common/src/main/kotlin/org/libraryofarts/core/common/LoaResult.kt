package org.libraryofarts.core.common

/**
 * A tiny, framework-free result type used at repository boundaries.
 * Errors are always human readable and never contain credentials.
 */
sealed interface LoaResult<out T> {
    data class Success<T>(val data: T) : LoaResult<T>
    data class Failure(val error: AppError) : LoaResult<Nothing>

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): AppError? = (this as? Failure)?.error
    fun <R> map(transform: (T) -> R): LoaResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }
}

data class AppError(
    val message: String,
    val kind: ErrorKind = ErrorKind.UNKNOWN,
    val retryable: Boolean = false,
    val cause: Throwable? = null,
) {
    enum class ErrorKind {
        NETWORK,
        OFFLINE,
        NOT_FOUND,
        RATE_LIMITED,
        UNAUTHORIZED_SOURCE,
        STORAGE_FULL,
        CORRUPT_DATA,
        VERIFICATION,
        UNKNOWN,
    }

    companion object {
        fun offline() = AppError(
            message = "You're offline. Your saved works and installed packs are still available.",
            kind = ErrorKind.OFFLINE,
            retryable = true,
        )

        fun network() = AppError(
            message = "The museum's server didn't answer. Check your connection and try again.",
            kind = ErrorKind.NETWORK,
            retryable = true,
        )

        fun notFound() = AppError("That artwork could not be found.", ErrorKind.NOT_FOUND)
        fun storage() = AppError(
            "There isn't enough free storage for this pack. Free some space or choose a smaller quality.",
            ErrorKind.STORAGE_FULL,
        )
        fun verification() = AppError(
            "This pack failed its integrity check and was not installed. It may have been tampered with.",
            ErrorKind.VERIFICATION,
        )
        fun unknown(cause: Throwable? = null) = AppError(
            "Something went wrong. Nothing you saved was lost.",
            ErrorKind.UNKNOWN,
            retryable = true,
            cause = cause,
        )
    }
}

inline fun <T> runCatchingLoa(block: () -> T): LoaResult<T> = try {
    LoaResult.Success(block())
} catch (t: Throwable) {
    LoaResult.Failure(AppError.unknown(t))
}
