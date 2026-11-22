package org.libraryofarts.core.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import org.libraryofarts.core.common.ConnectivityObserver
import org.libraryofarts.core.common.SystemTimeProvider
import org.libraryofarts.core.common.TimeProvider
import org.libraryofarts.core.data.engine.EyesFirstEngine
import org.libraryofarts.core.data.engine.LocalModelPack
import org.libraryofarts.core.data.engine.RecommendationEngine
import org.libraryofarts.core.data.repository.PackTrust

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun timeProvider(): TimeProvider = SystemTimeProvider

    @Provides
    @Singleton
    fun recommendationEngine(): RecommendationEngine = RecommendationEngine()

    @Provides
    @Singleton
    fun eyesFirstEngine(@ApplicationContext context: Context): EyesFirstEngine =
        EyesFirstEngine(LocalModelPack(File(context.filesDir, "models")))

    @Provides
    @Singleton
    fun connectivityObserver(@ApplicationContext context: Context): ConnectivityObserver =
        ConnectivityObserver(context)

    @Provides
    @Singleton
    fun packTrust(): PackTrust = PackTrust()
}
