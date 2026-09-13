package com.myvoice.app.core

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** An error that is safe to show directly to the user. */
class AssistantException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Maps any throwable into a short, human-friendly message. */
fun friendlyMessage(t: Throwable): String = when (t) {
    is AssistantException -> t.message ?: "Something went wrong."
    is UnknownHostException -> "No internet connection. Cloud features need network access."
    is SocketTimeoutException -> "The request timed out. Please try again."
    is IOException -> "Network error: ${t.message ?: "connection failed"}"
    else -> t.message ?: t.toString()
}
