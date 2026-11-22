package org.libraryofarts.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main.immediate
}

object AppDispatchers {
    var provider: DispatcherProvider = DefaultDispatcherProvider()
    val io get() = provider.io
    val default get() = provider.default
    val main get() = provider.main
}
