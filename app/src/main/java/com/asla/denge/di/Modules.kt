package com.asla.denge.di

import com.asla.denge.data.local.db.AdsFreeDatabase
import com.asla.denge.data.remote.innertube.InnertubeClient
import com.asla.denge.data.repository.AuthRepositoryImpl
import com.asla.denge.data.repository.GenreRepositoryImpl
import com.asla.denge.data.repository.MusicRepositoryImpl
import com.asla.denge.domain.repository.AuthRepository
import com.asla.denge.domain.repository.GenreRepository
import com.asla.denge.domain.repository.MusicRepository
import com.asla.denge.player.AudioEffectsManager
import com.asla.denge.player.PlayerManager
import com.asla.denge.ui.screens.home.HomeViewModel
import com.asla.denge.ui.screens.library.LibraryViewModel
import com.asla.denge.ui.screens.player.PlayerViewModel
import com.asla.denge.ui.screens.search.SearchViewModel
import com.asla.denge.ui.screens.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * App-level dependencies.
 */
val appModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}

/**
 * Networking dependencies.
 */
val networkModule = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(get())
            }
            install(Logging) {
                level = LogLevel.NONE
            }
        }
    }
    single { InnertubeClient(httpClient = get(), json = get()) }
}

/**
 * Local database dependencies (Room).
 */
val databaseModule = module {
    single { AdsFreeDatabase.create(androidContext()) }
    single { get<AdsFreeDatabase>().trackDao() }
    single { get<AdsFreeDatabase>().playlistDao() }
    single { get<AdsFreeDatabase>().playlistTrackDao() }
    single { get<AdsFreeDatabase>().historyDao() }
    single { get<AdsFreeDatabase>().queueDao() }
    single { get<AdsFreeDatabase>().searchHistoryDao() }
    single { get<AdsFreeDatabase>().eqPresetDao() }

    single<AuthRepository> {
        AuthRepositoryImpl(context = androidContext(), innertubeClient = get())
    }

    single<GenreRepository> {
        GenreRepositoryImpl(context = androidContext(), json = get())
    }

    single<MusicRepository> {
        MusicRepositoryImpl(
            innertubeClient = get(),
            trackDao = get(),
            playlistDao = get(),
            playlistTrackDao = get(),
            historyDao = get(),
            authRepository = get(),
        )
    }
}

/**
 * Player-related dependencies.
 */
val playerModule = module {
    single { PlayerManager(context = androidContext(), musicRepository = get()) }
    single(createdAtStart = true) { AudioEffectsManager(playerManager = get(), eqPresetDao = get()) }

    viewModel { PlayerViewModel(playerManager = get(), musicRepository = get()) }
    viewModel { HomeViewModel(musicRepository = get(), playerManager = get(), authRepository = get(), genreRepository = get()) }
    viewModel { SearchViewModel(musicRepository = get(), playerManager = get(), searchHistoryDao = get()) }
    viewModel { LibraryViewModel(musicRepository = get(), playerManager = get()) }
    viewModel { SettingsViewModel(authRepository = get(), audioEffectsManager = get(), musicRepository = get(), genreRepository = get()) }
}
