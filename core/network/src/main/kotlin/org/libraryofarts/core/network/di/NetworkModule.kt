package org.libraryofarts.core.network.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.OkHttpClient
import org.libraryofarts.core.network.MuseumRemoteSource
import org.libraryofarts.core.network.NetworkFactory
import org.libraryofarts.core.network.SourceCredentials
import org.libraryofarts.core.network.SourceHealth
import org.libraryofarts.core.network.model.AicApi
import org.libraryofarts.core.network.model.ClevelandApi
import org.libraryofarts.core.network.model.EuropeanaApi
import org.libraryofarts.core.network.model.GettyApi
import org.libraryofarts.core.network.model.HarvardApi
import org.libraryofarts.core.network.model.MetApi
import org.libraryofarts.core.network.model.NgaApi
import org.libraryofarts.core.network.model.ParisMuseesApi
import org.libraryofarts.core.network.model.RijksApi
import org.libraryofarts.core.network.model.SmithsonianApi
import org.libraryofarts.core.network.model.WikimediaApi
import org.libraryofarts.core.network.source.AicSource
import org.libraryofarts.core.network.source.ClevelandSource
import org.libraryofarts.core.network.source.EuropeanaSource
import org.libraryofarts.core.network.source.GettySource
import org.libraryofarts.core.network.source.HarvardSource
import org.libraryofarts.core.network.source.MetSource
import org.libraryofarts.core.network.source.NationalGallerySource
import org.libraryofarts.core.network.source.ParisMuseesSource
import org.libraryofarts.core.network.source.RijksmuseumSource
import org.libraryofarts.core.network.source.SmithsonianSource
import org.libraryofarts.core.network.source.WikimediaSource
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun okHttp(@ApplicationContext context: Context): OkHttpClient =
        NetworkFactory.okHttp(context.cacheDir)

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient): Retrofit = NetworkFactory.retrofit(client)

    @Provides fun metApi(r: Retrofit): MetApi = r.create(MetApi::class.java)
    @Provides fun aicApi(r: Retrofit): AicApi = r.create(AicApi::class.java)
    @Provides fun rijksApi(r: Retrofit): RijksApi = r.create(RijksApi::class.java)
    @Provides fun clevelandApi(r: Retrofit): ClevelandApi = r.create(ClevelandApi::class.java)
    @Provides fun ngaApi(r: Retrofit): NgaApi = r.create(NgaApi::class.java)
    @Provides fun wikimediaApi(r: Retrofit): WikimediaApi = r.create(WikimediaApi::class.java)
    @Provides fun smithsonianApi(r: Retrofit): SmithsonianApi = r.create(SmithsonianApi::class.java)
    @Provides fun europeanaApi(r: Retrofit): EuropeanaApi = r.create(EuropeanaApi::class.java)
    @Provides fun harvardApi(r: Retrofit): HarvardApi = r.create(HarvardApi::class.java)
    @Provides fun parisApi(r: Retrofit): ParisMuseesApi = r.create(ParisMuseesApi::class.java)
    @Provides fun gettyApi(r: Retrofit): GettyApi = r.create(GettyApi::class.java)

    @Provides
    @Singleton
    fun sourceCredentials(): SourceCredentials = SourceCredentials()

    @Provides
    @Singleton
    fun sourceHealth(): SourceHealth = SourceHealth()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SourceBindings {
    @Binds @IntoSet abstract fun met(source: MetSource): MuseumRemoteSource
    @Binds @IntoSet abstract fun aic(source: AicSource): MuseumRemoteSource
    @Binds @IntoSet abstract fun rijks(source: RijksmuseumSource): MuseumRemoteSource
    @Binds @IntoSet abstract fun cleveland(source: ClevelandSource): MuseumRemoteSource
    @Binds @IntoSet abstract fun nga(source: NationalGallerySource): MuseumRemoteSource
    @Binds @IntoSet abstract fun wikimedia(source: WikimediaSource): MuseumRemoteSource
    @Binds @IntoSet abstract fun smithsonian(source: SmithsonianSource): MuseumRemoteSource
    @Binds @IntoSet abstract fun europeana(source: EuropeanaSource): MuseumRemoteSource
    @Binds @IntoSet abstract fun harvard(source: HarvardSource): MuseumRemoteSource
    @Binds @IntoSet abstract fun paris(source: ParisMuseesSource): MuseumRemoteSource
    @Binds @IntoSet abstract fun getty(source: GettySource): MuseumRemoteSource
}
