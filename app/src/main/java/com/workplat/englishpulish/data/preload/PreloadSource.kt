package com.workplat.englishpulish.data.preload

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreloadSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<PreloadEntry> =
        context.assets.open(ASSET_NAME).use { stream ->
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(PreloadEntry.serializer()),
                stream.bufferedReader().readText(),
            )
        }

    companion object {
        private const val ASSET_NAME = "preload.json"
    }
}
