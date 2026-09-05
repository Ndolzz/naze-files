package com.naze.files.media

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
    addListener(
        {
            try {
                cont.resume(get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        },
        MoreExecutors.directExecutor(),
    )
    cont.invokeOnCancellation { cancel(false) }
}
