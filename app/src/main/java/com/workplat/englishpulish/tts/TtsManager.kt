package com.workplat.englishpulish.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over the platform [TextToSpeech] engine.
 *
 * Lifecycle:
 * - Initialized lazily on the first speak() call; subsequent calls reuse the
 *   same engine instance.
 * - Never shut down — the engine lives for the process lifetime, which matches
 *   the app's usage pattern (the user opens it many times a day).
 *
 * Locale handling:
 * - Prefers US English. Falls back to UK, then platform default if neither is
 *   available on the device (rare, but possible on stripped-down ROMs).
 */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var engine: TextToSpeech? = null

    @Volatile
    private var ready: Boolean = false

    fun speak(text: String) {
        if (text.isBlank()) return
        ensureEngine().also { tts ->
            if (!ready) return
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
        }
    }

    @Synchronized
    private fun ensureEngine(): TextToSpeech {
        engine?.let { return it }
        val tts = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.w(TAG, "TTS init failed with status=$status")
                return@TextToSpeech
            }
            engine?.let { e ->
                val locale = pickLocale(e)
                if (locale != null) e.language = locale
                ready = true
            }
        }
        engine = tts
        return tts
    }

    private fun pickLocale(tts: TextToSpeech): Locale? {
        val preferred = listOf(Locale.US, Locale.UK, Locale.ENGLISH)
        for (l in preferred) {
            val result = tts.isLanguageAvailable(l)
            if (result == TextToSpeech.LANG_AVAILABLE ||
                result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            ) return l
        }
        return null
    }

    companion object {
        private const val TAG = "TtsManager"
    }
}
